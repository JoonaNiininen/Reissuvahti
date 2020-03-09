package com.example.reissuvahti;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.JsonReader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import hu.supercluster.overpasser.library.query.OverpassQuery;

import static hu.supercluster.overpasser.library.output.OutputFormat.JSON;
import static hu.supercluster.overpasser.library.output.OutputFormat.XML;

public class TripActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION = 1;
    Double latitude = 63.826729;
    Double longitude = 23.1528061;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        findViewById(R.id.addStop).setOnClickListener(new View.OnClickListener() {
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
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                getNearbyLocations();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
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
                            try {
                                longitudeText.setText(getNearbyLocations());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, Looper.getMainLooper());
    }

    public String getNearbyLocations() throws IOException {
        String nearbyLocations = "";

        if(latitude == null && longitude == null) return null;
        String apiQuery = "data=[out:json][timeout:25];node("
                .concat(latitude.toString()).concat(",").concat(longitude.toString())
                .concat(",").concat(latitude.toString()).concat(",")
                .concat(longitude.toString())
                .concat(");node(around:50)[\"shop\"];out body;");

        URL endpoint = new URL("https://overpass-api.de/api/interpreter");
        HttpURLConnection urlConn = (HttpURLConnection) endpoint.openConnection();

        try {
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            BufferedOutputStream writer = new BufferedOutputStream(urlConn.getOutputStream());
            writer.write(apiQuery.getBytes());
            writer.flush();
            writer.close();

            int asda = urlConn.getResponseCode();

            StringBuilder response;
            BufferedInputStream reader = new BufferedInputStream(urlConn.getInputStream());
            String line = "";
            int i = 0;
            response = new StringBuilder();
            while ((i=reader.read()) != -1) {
                response.append((char)i);
            }
        } finally {
            urlConn.disconnect();
        }

        return nearbyLocations;

    }


    public void addLocation(String btnName) {

        Button testButton = new Button(this);
        testButton.setText(btnName);
        LinearLayout tripOverviewLayout = findViewById(R.id.tripOverviewBar);
        LinearLayout.LayoutParams defaultTripParameters = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

        tripOverviewLayout.addView(testButton, defaultTripParameters);

        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeLocation(v);
            }
        };

        testButton.setOnClickListener(click);
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
