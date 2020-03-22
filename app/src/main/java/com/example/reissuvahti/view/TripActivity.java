package com.example.reissuvahti.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.reissuvahti.async.FinishTripTask;
import com.example.reissuvahti.async.NearbyTask;
import com.example.reissuvahti.R;
import com.example.reissuvahti.overpass.OverpassLocation;
import com.example.reissuvahti.overpass.OverpassTag;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.example.reissuvahti.common.Common.LATITUDE;
import static com.example.reissuvahti.common.Common.LONGITUDE;
import static com.example.reissuvahti.common.Common.TRIP_STOP_LIST;
import static com.example.reissuvahti.common.Constants.GPS_INITIAL_DELAY;
import static com.example.reissuvahti.common.Constants.GPS_UPDATE_INTERVAL;
import static com.example.reissuvahti.common.Constants.LOCATION_FASTEST_INTERVAL;
import static com.example.reissuvahti.common.Constants.LOCATION_UPDATE_INTERVAL;
import static com.example.reissuvahti.common.Constants.REQUEST_CODE_LOCATION;


public class TripActivity extends AppCompatActivity {
    private List<Button> _nearbyButtonsList = new ArrayList<>();
    private List<Button> _overviewButtonsList = new ArrayList<>();

    public List<Button> getNearbyButtons(){
        return _nearbyButtonsList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.getSupportActionBar()!=null){
            this.getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_trip);

    }

    @Override
    protected void onStart() {
        super.onStart();
        final Button finishButton = findViewById(R.id.finishTrip);
        final Button addStopButton = findViewById(R.id.addStop);
        final Button newNearbyLocation = findViewById(R.id.btnNearbyNew);
        newNearbyLocation.setOnClickListener(addCustomLocationListener);
        finishButton.setOnClickListener(finishTripListener);
        addStopButton.setOnClickListener(addStopListener);
        addStopButton.setKeepScreenOn(true);
        _nearbyButtonsList.add((Button) findViewById(R.id.btnNearbyA));
        _nearbyButtonsList.add((Button) findViewById(R.id.btnNearbyB));
        _nearbyButtonsList.add((Button) findViewById(R.id.btnNearbyC));
        _nearbyButtonsList.add((Button) findViewById(R.id.btnNearbyD));
        _nearbyButtonsList.add((Button) findViewById(R.id.btnNearbyE));
        for (int i = 0; i<5; i++) {
            _nearbyButtonsList.get(i).setOnClickListener(nearbyListListener);
        }

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

                        }
                    }
                }, Looper.getMainLooper());
    }

    public void addLocation(OverpassLocation location) {
        Button stopButton = new Button(this);
        stopButton.setText(location.getTags().getName());
        stopButton.setOnClickListener(remove);

        _overviewButtonsList.add(stopButton);

        LinearLayout tripOverviewLayout = findViewById(R.id.tripOverviewBar);
        LinearLayout.LayoutParams tripOverviewParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        tripOverviewLayout.addView(stopButton, tripOverviewParam);
    }

    public void removeLocation(View view) {
        LinearLayout tripOverviewLayout = findViewById(R.id.tripOverviewBar);
        Button findButton = (Button) view;

        for (int i = 0; i< _overviewButtonsList.size(); i++) {
            if (findButton.getText().equals(_overviewButtonsList.get(i).getText()))
                _overviewButtonsList.remove(i);
            if (findButton.getText().equals(TRIP_STOP_LIST.get(i).getTags().getName())) {
                TRIP_STOP_LIST.remove(i);
                break;
            }
        }
        tripOverviewLayout.removeView(view);
    }

    View.OnClickListener addCustomLocationListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(TripActivity.this);
            alert.setTitle("Lisää uusi sijainti");
            alert.setMessage("Syötä sijainnin nimi");

            final EditText input = new EditText(TripActivity.this);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @SuppressLint("SetTextI18n")
                public void onClick(DialogInterface dialog, int whichButton) {
                    Button newNearbyLocation = findViewById(R.id.btnNearbyNew);
                    newNearbyLocation.setText("Lisää uusi sijainti");
                    OverpassLocation location = new OverpassLocation();
                    OverpassTag tag = new OverpassTag();
                    tag.setName(input.getText().toString());
                    location.setTags(tag);
                    location.setLat(LATITUDE);
                    location.setLon(LONGITUDE);

                    addLocation(location);
                    TRIP_STOP_LIST.add(location);
                    newNearbyLocation.setVisibility(View.GONE);
                    for (int i = 0; i < 5; i++) {
                        _nearbyButtonsList.get(i).setVisibility(View.GONE);
                    }
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            alert.show();
        }
    };

    View.OnClickListener addStopListener = new View.OnClickListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onClick(View v) {
            Button newNearbyLocation = findViewById(R.id.btnNearbyNew);
            newNearbyLocation.setText("Lisää uusi sijainti");
            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        TripActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_LOCATION
                );
            } else {
                if(LATITUDE == null || LONGITUDE == null) return;
                newNearbyLocation.setVisibility(View.GONE);
                for(int i=0 ; i<5; i++){
                    _nearbyButtonsList.get(i).setVisibility(View.GONE);
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
            Button newNearbyLocation = findViewById(R.id.btnNearbyNew);
            OverpassLocation location = new OverpassLocation();
            OverpassTag tag = new OverpassTag();
            tag.setName(((Button) view).getText().toString());
            location.setTags(tag);
            location.setLat(LATITUDE);
            location.setLon(LONGITUDE);

            addLocation(location);
            TRIP_STOP_LIST.add(location);
            newNearbyLocation.setVisibility(View.GONE);

            for (int i = 0; i < 5; i++) {
                _nearbyButtonsList.get(i).setVisibility(View.GONE);
            }
        }
    };

    View.OnClickListener remove = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            removeLocation(v);
        }
    };

    View.OnClickListener finishTripListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new AlertDialog.Builder(TripActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Oletko varma?")
                .setMessage("Onko reissu valmis?")
                .setPositiveButton("Kyllä", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FinishTripTask finishTask = new FinishTripTask(new WeakReference<>(TripActivity.this));
                        finishTask.execute();
                        finish();
                    }
                })
                .setNegativeButton("Ei", null)
                .show();
        }
    };
}
