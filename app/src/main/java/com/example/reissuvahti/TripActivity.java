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
import android.view.Window;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TripActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_LOCATION = 1;
    Double latitude = null;
    Double longitude = null;
    List<String> currentTrip = new ArrayList<>();
    List<OverpassLocation> nearbyLocations = new ArrayList<>();
    List<Button> nearbyButtons = new ArrayList<>();
    List<Button> overviewButtons = new ArrayList<>();

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
        Button addStop = findViewById(R.id.addStop);
        final TextView addressText = findViewById(R.id.currentNearbyLocations);
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
                    addLocation(name);
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



        if(latitude == null || longitude == null) {
            addStop.setEnabled(false);
            getCurrentLocation();
        }
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
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                nearbyLocations = getNearbyLocations();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    StringBuilder temp = new StringBuilder();
                    int iter = 0;
                    for (OverpassLocation loc : nearbyLocations) {
                        temp.append(loc.getTags().getName()).append(" ");
                        if (iter < 5) {
                            nearbyButtons.get(iter).setVisibility(View.VISIBLE);
                            nearbyButtons.get(iter).setText(loc.getTags().getName());
                        }
                        iter++;
                    }
                    addressText.setText(temp);

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

    public List<OverpassLocation> getNearbyLocations() throws IOException {
        List<OverpassLocation> nearbyLocations;
        URL endpoint = new URL("https://overpass-api.de/api/interpreter");
        HttpURLConnection urlConn = (HttpURLConnection) endpoint.openConnection();
        if(latitude == null || longitude == null)return null;
        String apiQuery = "data=[out:json][timeout:25];node(around:70,".concat(latitude.toString()).concat(",").concat(longitude.toString()).concat(")[name];\n" +
                "out;");
        try {
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
        } finally {
            urlConn.disconnect();

        }

        return nearbyLocations;

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

    public void finishTrip(View view) {
        Intent finish = new Intent(this, Main.class);
        startActivity(finish);
    }

}
