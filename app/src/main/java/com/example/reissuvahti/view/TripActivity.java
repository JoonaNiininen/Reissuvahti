package com.example.reissuvahti.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.reissuvahti.FinishTripTask;
import com.example.reissuvahti.NearbyTask;
import com.example.reissuvahti.R;
import com.example.reissuvahti.overpass.OverpassLocation;
import com.example.reissuvahti.overpass.OverpassTag;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.example.reissuvahti.Common.LATITUDE;
import static com.example.reissuvahti.Common.LONGITUDE;
import static com.example.reissuvahti.Common.TRIP_STOP_LIST;
import static com.example.reissuvahti.Constants.GPS_INITIAL_DELAY;
import static com.example.reissuvahti.Constants.GPS_UPDATE_INTERVAL;
import static com.example.reissuvahti.Constants.LOCATION_FASTEST_INTERVAL;
import static com.example.reissuvahti.Constants.LOCATION_UPDATE_INTERVAL;
import static com.example.reissuvahti.Constants.REQUEST_CODE_LOCATION;


public class TripActivity extends AppCompatActivity {
    private List<Button> nearbyButtons = new ArrayList<>();
    private List<Button> overviewButtons = new ArrayList<>();

    public List<Button> getNearbyButtons(){
        return nearbyButtons;
    }



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

                            LATITUDE =
                                    locationResult.getLocations().get(latestLocationIndex).getLatitude();
                            LONGITUDE =
                                    locationResult.getLocations().get(latestLocationIndex).getLongitude();
                            TextView latitudeText = findViewById(R.id.currentStopLatitude);
                            TextView longitudeText = findViewById(R.id.currentStopLongitude);
                            latitudeText.setText(String.format("%s", LATITUDE));
                            longitudeText.setText(String.format("%s", LONGITUDE));
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
        Intent finish = new Intent(this, MainActivity.class);
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
                if(LATITUDE == null || LONGITUDE == null) {
                    latitudeText.setText(null);
                    longitudeText.setText(null);
                    return;
                }

                for(int i=0 ; i<5; i++){
                    nearbyButtons.get(i).setVisibility(View.GONE);
                }
                getCurrentLocation();

                NearbyTask fetchLocationsTask = new NearbyTask(new WeakReference<>(TripActivity.this));
                fetchLocationsTask.execute();
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
                loc.setLat(LATITUDE);
                loc.setLon(LONGITUDE);


                addLocation(loc);
                TRIP_STOP_LIST.add(loc);

                for (int i = 0; i < 5; i++) {
                    nearbyButtons.get(i).setVisibility(View.GONE);
                }
            }
        }
    };

    View.OnClickListener finishTripListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            FinishTripTask finishTask = new FinishTripTask(new WeakReference<>(TripActivity.this));
            finishTask.execute();
    }
    };





}
