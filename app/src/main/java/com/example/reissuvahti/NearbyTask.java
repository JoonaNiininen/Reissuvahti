package com.example.reissuvahti;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.example.reissuvahti.overpass.OverpassLocation;
import com.example.reissuvahti.overpass.OverpassResponse;
import com.example.reissuvahti.view.TripActivity;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.reissuvahti.Common.LATITUDE;
import static com.example.reissuvahti.Common.LONGITUDE;
import static com.example.reissuvahti.Constants.OVERPASS_ENDPOINT_URL;

public class NearbyTask extends AsyncTask<List<Double>, Void, List<OverpassLocation>> {
    private WeakReference<TripActivity> _tripActivity;

    public NearbyTask(WeakReference<TripActivity> activityReference){
        _tripActivity = activityReference;
    }

    @Override
    protected void onPreExecute() {
        ProgressBar progressBar = _tripActivity.get().findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.animate();
    }

    @SafeVarargs
    @Override
    protected final List<OverpassLocation> doInBackground(List<Double>... coordinates) {
        List<OverpassLocation> nearbyLocations = new ArrayList<>();
        HttpURLConnection urlConn = null;

        try {
            URL endpoint = new URL(OVERPASS_ENDPOINT_URL);
            urlConn = (HttpURLConnection) endpoint.openConnection();

            if (LATITUDE == null || LONGITUDE == null) return null;
            String apiQuery = "data=[out:json][timeout:25];node(around:70,".concat(LATITUDE.toString()).concat(",").concat(LONGITUDE.toString()).concat(")[name];" +
                    "out;");

            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");

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
        ProgressBar progressBar = _tripActivity.get().findViewById(R.id.progressBar);

        if (locations.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        for(int i=0; i< 5; i++) {
            _tripActivity.get().getNearbyButtons().get(i).setVisibility(View.VISIBLE);
            _tripActivity.get().getNearbyButtons().get(i).setText(locations.get(i).getTags().getName());
        }

        progressBar.setVisibility(View.GONE);

    }
}