package com.example.reissuvahti;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.reissuvahti.overpass.OverpassLocation;
import com.example.reissuvahti.overpass.OverpassResponse;
import com.example.reissuvahti.overpass.OverpassTag;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TripActivity extends AppCompatActivity {

    private static final int GPS_INITIAL_DELAY = 0;
    private static final int GPS_UPDATE_INTERVAL = 10;
    private static final int REQUEST_CODE_LOCATION = 1;
    private static final long LOCATION_FASTEST_INTERVAL = 3000;
    private static final long LOCATION_UPDATE_INTERVAL = 5000;
    private static final String LOCALHOST_URL = "http://192.168.1.10:8080/api/trips";
    private static Double latitude = null;
    private static Double longitude = null;
    private static final String LATITUDE_NULL = null;
    private static final String LONGITUDE_NULL = null;
    private static final String ENDPOINT_URL = "https://overpass-api.de/api/interpreter";
    private static List<Button> nearbyButtons = new ArrayList<>();
    private static List<Button> overviewButtons = new ArrayList<>();
    private static List<OverpassLocation> tripStopList = new ArrayList<>();
    private static final int HTTP_OK = 200;
    private static final int FIRST_LOCATION = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try
        {
            if (this.getSupportActionBar()!=null){
                this.getSupportActionBar().hide();
            }
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
        setContentView(R.layout.activity_trip);

    }



    @Override
    protected void onStart() {
        super.onStart();
        final Button finish = findViewById(R.id.finishTrip);
        final Button addStop = findViewById(R.id.addStop);
        addStop.setKeepScreenOn(true);
        finish.setOnClickListener(finishTripListener);
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyA));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyB));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyC));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyD));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyE));
        for (int i = 0; i<5; i++) {
            nearbyButtons.get(i).setOnClickListener(nearbyListListener);
        }
        addStop.setEnabled(true);
        addStop.setOnClickListener(addStopListener);

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        getCurrentLocation();
                    }
                }, GPS_INITIAL_DELAY, GPS_UPDATE_INTERVAL, TimeUnit.SECONDS);
    }



    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Oletko varma?")
                .setMessage("Haluatko varmasti peruuttaa reissun?")
                .setPositiveButton("Kyllä", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("Ei", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCurrentLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void getCurrentLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(TripActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(TripActivity.this)
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0) {
                            int latestLocationIndex = locationResult.getLocations().size() - 1;

                            latitude =
                                    locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            longitude =
                                    locationResult.getLocations().get(latestLocationIndex).getLongitude();
                            TextView latitudeText = findViewById(R.id.currentStopLatitude);
                            TextView longitudeText = findViewById(R.id.currentStopLongitude);
                            latitudeText.setText(String.format("%s", latitude));
                            longitudeText.setText(String.format("%s", longitude));
                        }
                    }
                }, Looper.getMainLooper());
    }

    public void addLocation(OverpassLocation loc) {
        Button stopButton = new Button(this);
        stopButton.setText(loc.getTags().getName());
        overviewButtons.add(stopButton);

        LinearLayout tripOverviewLayout = findViewById(R.id.tripOverviewBar);
        LinearLayout.LayoutParams tripOverviewParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

        tripOverviewLayout.addView(stopButton, tripOverviewParam);

        View.OnClickListener remove = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeLocation(v);
            }
        };

        stopButton.setOnClickListener(remove);
    }

    public void removeLocation(View view) {
        LinearLayout tripOverviewLayout = findViewById(R.id.tripOverviewBar);
        Button findButton = (Button) view;
        for (Iterator<Button> iterator = overviewButtons.iterator(); iterator.hasNext(); ) {
            if (findButton.getText().equals((iterator.next().getText())))
                iterator.remove();
        }
        tripOverviewLayout.removeView(view);
    }

    public void finishTrip() {
        Intent finish = new Intent(this, Main.class);
        startActivity(finish);
    }

    View.OnClickListener addStopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final TextView latitudeText = findViewById(R.id.currentStopLatitude);
            final TextView longitudeText = findViewById(R.id.currentStopLongitude);
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        TripActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_LOCATION
                );
            } else {
                if(latitude == null || longitude == null) {
                    latitudeText.setText(LATITUDE_NULL);
                    longitudeText.setText(LONGITUDE_NULL);
                    return;
                }

                for(int i=0 ; i<5; i++){
                    nearbyButtons.get(i).setVisibility(View.GONE);
                }
                getCurrentLocation();
                List<Double> taskParams = new ArrayList<>();
                taskParams.add(latitude);
                taskParams.add(longitude);

                NearbyTask fetchLocationsTask = new NearbyTask();
                fetchLocationsTask.execute(taskParams);
            }
        }
    };

    View.OnClickListener nearbyListListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.isEnabled()) {
                String name = ((Button) view).getText().toString();
                OverpassLocation loc = new OverpassLocation();
                OverpassTag tag = new OverpassTag();
                tag.setName(name);
                loc.setTags(tag);
                loc.setLat(latitude);
                loc.setLon(longitude);


                addLocation(loc);
                tripStopList.add(loc);

                for (int i = 0; i < 5; i++) {
                    nearbyButtons.get(i).setVisibility(View.GONE);
                }
            }
        }
    };

    View.OnClickListener finishTripListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            FinishTripTask finishTask = new FinishTripTask();
            finishTask.execute(tripStopList);
    }
    };


    protected class NearbyTask extends AsyncTask<List<Double>, Void, List<OverpassLocation>> {

        @Override
        protected void onPreExecute() {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.animate();
        }

        @SafeVarargs
        @Override
        protected final List<OverpassLocation> doInBackground(List<Double>... coordinates) {
            List<Double> passedList = coordinates[0];
            Double latitude = passedList.get(0);
            Double longitude = passedList.get(1);
            List<OverpassLocation> nearbyLocations = new ArrayList<>();
            URL endpoint = null;
            try {
                endpoint = new URL(ENDPOINT_URL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection urlConn = null;
            try {
                assert endpoint != null;
                urlConn = (HttpURLConnection) endpoint.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(latitude == null || longitude == null)return null;
            String apiQuery = "data=[out:json][timeout:25];node(around:70,".concat(latitude.toString()).concat(",").concat(longitude.toString()).concat(")[name];" +
                    "out;");
            try {
                assert urlConn != null;
                urlConn.setDoOutput(true);
                urlConn.setRequestMethod("POST");
                urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                BufferedOutputStream writer = new BufferedOutputStream(urlConn.getOutputStream());
                writer.write(apiQuery.getBytes());
                writer.flush();
                writer.close();

                InputStreamReader in = new InputStreamReader(urlConn.getInputStream());
                JsonReader reader = new JsonReader(in);
                OverpassResponse overpassResponse = new Gson().fromJson(reader, OverpassResponse.class);
                nearbyLocations = Arrays.asList(overpassResponse.getElements());
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                assert urlConn != null;
                urlConn.disconnect();
            }
            return nearbyLocations;
        }


        @Override
        protected void onPostExecute(List<OverpassLocation> locations) {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            TextView addressText = findViewById(R.id.currentNearbyLocations);
            StringBuilder temp = new StringBuilder();
            int iter = 0;
            if (locations.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            return;
            }
            for (OverpassLocation loc : locations) {
                temp.append(loc.getTags().getName()).append(" ");
                if (iter < 5) {
                    nearbyButtons.get(iter).setVisibility(View.VISIBLE);
                    nearbyButtons.get(iter).setText(loc.getTags().getName());
                }
                iter++;
            }
            addressText.setText(temp);
            progressBar.setVisibility(View.GONE);

        }
    }

    protected class FinishTripTask extends AsyncTask<List<OverpassLocation>, Void, String> {

        @Override
        protected void onPreExecute() {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.animate();
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(List<OverpassLocation>... locations) {
            List<OverpassLocation> passedList = locations[FIRST_LOCATION];
            URL endpoint = null;
            String result = null;
            Calendar calendar = Calendar.getInstance();
            String tripName = new StringBuilder
                    (Integer.toString(calendar.get(Calendar.HOUR_OF_DAY))
                    .concat(Integer.toString(calendar.get(Calendar.MINUTE)))
                    .concat(Integer.toString(calendar.get(Calendar.DATE)))
                    .concat(Integer.toString(calendar.get(Calendar.YEAR)))
                    ).toString();

            try {
                endpoint = new URL(LOCALHOST_URL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            for(int i=0; i<passedList.size(); i++) {

                HttpURLConnection urlConn = null;
                try {
                    assert endpoint != null;
                    urlConn = (HttpURLConnection) endpoint.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    assert urlConn != null;
                    urlConn.setDoOutput(true);
                    urlConn.setRequestMethod("POST");
                    urlConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    urlConn.setRequestProperty("Accept","application/json");

                    JSONObject locationObject = new JSONObject();
                    locationObject.put("tripName", tripName);
                    locationObject.put("name", passedList.get(i).getTags().getName());
                    locationObject.put("latitude", passedList.get(i).getLat());
                    locationObject.put("longitude", passedList.get(i).getLon());

                    BufferedOutputStream outputStream = new BufferedOutputStream(urlConn.getOutputStream());
                    outputStream.write(locationObject.toString().getBytes());
                    outputStream.flush();
                    outputStream.close();


                    int responseCode = (urlConn.getResponseCode());


                    if (responseCode==HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                urlConn.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        result = "Ladattu palvelimelle";
                        System.out.println(response);
                    } else result = "Epäonnistui";



                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    assert urlConn != null;
                    urlConn.disconnect();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            super.onPostExecute(result);
            tripStopList.clear();
            progressBar.setVisibility(View.GONE);
            finishTrip();
        }
    }

}
