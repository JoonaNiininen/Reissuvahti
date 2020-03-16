package com.example.reissuvahti;

import android.Manifest;
import android.annotation.SuppressLint;
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TripActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_LOCATION = 1;
    Double latitude = null;
    Double longitude = null;
    List<String> currentTrip = new ArrayList<>();
    List<Button> nearbyButtons = new ArrayList<>();
    List<Button> overviewButtons = new ArrayList<>();
    List<OverpassLocation> tripStops = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){
            e.printStackTrace();
        }
        setContentView(R.layout.activity_trip);
        getCurrentLocation();
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
    protected void onStart() {
        super.onStart();
        final Button finish = findViewById(R.id.finishTrip);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FinishTripTask finishTrip = new FinishTripTask();
                finishTrip.execute(tripStops);
            }
        });

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        getCurrentLocation();
                    }
                }, 0, 10, TimeUnit.SECONDS);

        Button addStop = findViewById(R.id.addStop);
        final TextView latitudeText = findViewById(R.id.currentStopLatitude);
        final TextView longitudeText = findViewById(R.id.currentStopLongitude);

        addStop.setKeepScreenOn(true);

        nearbyButtons.add((Button) findViewById(R.id.btnNearbyA));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyB));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyC));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyD));
        nearbyButtons.add((Button) findViewById(R.id.btnNearbyE));

        Button.OnClickListener nearbyList = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.isEnabled()) {
                    String name = ((Button) v).getText().toString();
                    OverpassLocation loc = new OverpassLocation();
                    OverpassTag tag = new OverpassTag();
                    tag.setName(name);
                    loc.setTags(tag);
                    loc.setLat(latitude);
                    loc.setLon(longitude);

                    addLocation(name);
                    tripStops.add(loc);

                    for (int i = 0; i < 5; i++) {
                        nearbyButtons.get(i).setVisibility(View.GONE);
                    }

                    currentTrip.add(name);

                }
            }
        };

        for (int i = 0; i<5; i++) {
            nearbyButtons.get(i).setOnClickListener(nearbyList);
        }



        getCurrentLocation();
        addStop.setEnabled(true);

        addStop.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            TripActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION
                    );
                } else {
                    if(latitude == null || longitude == null) {
                        latitudeText.setText("Latitude null");
                        longitudeText.setText("Longitude null");
                        return;
                    }

                    for(int i=0 ; i<5; i++){
                        nearbyButtons.get(i).setVisibility(View.GONE);
                    }
                    getCurrentLocation();
                    List<Double> taskParams = new ArrayList<>();
                    taskParams.add(latitude);
                    taskParams.add(longitude);
                    NearbyTask fetchLocations = new NearbyTask();
                    fetchLocations.execute(taskParams);
                }
            }
        });

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
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
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

    public void addLocation(String name) {
        Button stopButton = new Button(this);
        stopButton.setText(name);

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
        tripOverviewLayout.removeView(view);
    }

    public void finishTrip() {
        Intent finish = new Intent(this, Main.class);
        startActivity(finish);
    }

    @SuppressLint("StaticFieldLeak")
    public class NearbyTask extends AsyncTask<List<Double>, Void, List<OverpassLocation>> {
        final TextView addressText = findViewById(R.id.currentNearbyLocations);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        @Override
        protected void onPreExecute() {
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
                endpoint = new URL("https://overpass-api.de/api/interpreter");
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

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(List<OverpassLocation> locations) {
            StringBuilder temp = new StringBuilder();
            int iter = 0;
            if (locations.isEmpty()) {
            addressText.setText("Ei paikkoja lähistöllä.");
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

    @SuppressLint("StaticFieldLeak")
    public class FinishTripTask extends AsyncTask<List<OverpassLocation>, Void, Void> {
        ProgressBar progressBar = findViewById(R.id.progressBar);

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.animate();
        }

        @SafeVarargs
        @Override
        protected final Void doInBackground(List<OverpassLocation>... locations) {
            List<OverpassLocation> passedList = locations[0];
            URL endpoint = null;
            try {
                endpoint = new URL("http://192.168.1.10:8080/api/locations");
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
                    urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    String apiPost = "name=".concat(passedList.get(i).getTags().getName()).concat("&latitude=").concat(
                            passedList.get(i).getLat().toString()).concat("&longitude=").concat(
                            passedList.get(i).getLon().toString());
                    BufferedOutputStream outputStream = new BufferedOutputStream(urlConn.getOutputStream());
                    OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                    writer.write(apiPost);
                    writer.flush();
                    writer.close();

                    int responseCode = (urlConn.getResponseCode());

                    if (responseCode==200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                urlConn.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        System.out.println(response);
                    }



                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    assert urlConn != null;
                    urlConn.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.GONE);
            finishTrip();
        }
    }


}
