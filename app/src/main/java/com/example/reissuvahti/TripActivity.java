package com.example.reissuvahti;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;

public class TripActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_LOCATION = 1;
    protected Double latitude = null;
    protected Double longitude = null;
    /*
    protected Double latitude = 63.826729;
    protected Double longitude = 23.1528061;
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        if(latitude == null || longitude == null) {
            getCurrentLocation();
        }
        findViewById(R.id.addStop).setOnClickListener(new View.OnClickListener() {
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

                    final TextView addressText = findViewById(R.id.currentNearbyLocations);

                    if(latitude == null || longitude == null) {
                        TextView latitudeText = findViewById(R.id.currentStopLatitude);
                        TextView longitudeText = findViewById(R.id.currentStopLongitude);
                        latitudeText.setText("Latitude null");
                        longitudeText.setText("Longitude null");
                        return;
                    }
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                addressText.setText(getNearbyLocations());
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
                            latitude = round(latitude,6);
                            longitude = round(longitude, 7);

                            TextView latitudeText = findViewById(R.id.currentStopLatitude);
                            TextView longitudeText = findViewById(R.id.currentStopLongitude);
                            latitudeText.setText(String.format("%s", latitude));
                            longitudeText.setText(String.format("%s", longitude));
                        }
                    }
                }, Looper.getMainLooper());
    }

    public String getNearbyLocations() throws IOException {
        String nearbyLocations = "";
        URL endpoint = new URL("https://overpass-api.de/api/interpreter");
        HttpURLConnection urlConn = (HttpURLConnection) endpoint.openConnection();
        if(latitude == null || longitude == null)return null;
        /*
        String apiQuery = "data=[out:json][timeout:25];node("
                .concat(latitude.toString()).concat(",").concat(longitude.toString())
                .concat(",").concat(latitude.toString()).concat(",")
                .concat(longitude.toString())
                .concat(");node(around:150)[\"shop\"];out body;");
        */
        String apiQuery = "data=[out:json][timeout:25];node(around:100,".concat(latitude.toString()).concat(",").concat(longitude.toString()).concat(")[shop];\n" +
                "out;");
        try {
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            BufferedOutputStream writer = new BufferedOutputStream(urlConn.getOutputStream());
            writer.write(apiQuery.getBytes());
            writer.flush();
            writer.close();

            StringBuilder response = new StringBuilder();
            InputStreamReader in = new InputStreamReader(urlConn.getInputStream());
            JsonReader reader = new JsonReader(in);

            JsonToken testToken = reader.peek();
            if (testToken == JsonToken.BEGIN_ARRAY) {
                reader.beginArray();
                testToken = reader.peek();
                if (testToken == JsonToken.END_ARRAY) return "";
                reader.beginObject();
            } else reader.beginObject();
            while (reader.hasNext()) {
                String element = reader.nextName();
                if (element.equals("elements")) {
                    testToken = reader.peek();
                    if (testToken == JsonToken.BEGIN_ARRAY) {
                        reader.beginArray();
                        testToken = reader.peek();
                        if (testToken == JsonToken.END_ARRAY) return "Ei löydetty läheisiä paikkoja";
                        reader.beginObject();
                    } else reader.beginObject();
                    while (reader.hasNext()) {
                        String tags = reader.nextName();
                        if (tags.equals("tags")) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String name = reader.nextName();
                                if (name.equals("name")) {
                                    response = response.append(" "+reader.nextString());
                                    //break;
                                } else
                                    reader.skipValue();
                            }
                        } else
                            reader.skipValue();
                    }
                } else
                    reader.skipValue();
                }
            reader.endObject();

            nearbyLocations = response.toString();

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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
