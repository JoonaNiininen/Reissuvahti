package com.example.reissuvahti;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.example.reissuvahti.overpass.OverpassLocation;
import com.example.reissuvahti.view.TripActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

import static com.example.reissuvahti.Common.TRIP_STOP_LIST;
import static com.example.reissuvahti.Constants.HTTP_OK;
import static com.example.reissuvahti.Constants.LOCALHOST_URL;

public class FinishTripTask extends AsyncTask<Void, Void, String> {
    private WeakReference<TripActivity> _tripActivity;
    private static List<OverpassLocation> passedList = TRIP_STOP_LIST;

    public FinishTripTask(WeakReference<TripActivity> reference){
        _tripActivity = reference;
    }

    @Override
    protected void onPreExecute() {
        ProgressBar progressBar = _tripActivity.get().findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.animate();
    }

    @Override
    protected final String doInBackground(Void... aVoid) {
        HttpURLConnection urlConn = null;
        Calendar calendar = Calendar.getInstance();
        String tripName = Integer.toString(calendar.get(Calendar.HOUR_OF_DAY))
                .concat(Integer.toString(calendar.get(Calendar.MINUTE)))
                .concat(Integer.toString(calendar.get(Calendar.DATE)))
                .concat(Integer.toString(calendar.get(Calendar.YEAR)));

        for(int i=0; i<passedList.size(); i++) {


            try {
                URL endpoint = new URL(LOCALHOST_URL);
                urlConn = (HttpURLConnection) endpoint.openConnection();
                urlConn.setDoOutput(true);
                urlConn.setRequestMethod("POST");
                urlConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                urlConn.setRequestProperty("Accept","application/json");

                JSONObject locationObject = new JSONObject();
                locationObject.put("tripName", tripName);
                locationObject.put("name", passedList.get(i).getTags().getName());
                locationObject.put("latitude", passedList.get(i).getLat());
                locationObject.put("longitude", passedList.get(i).getLon());

                BufferedOutputStream outputStream = new BufferedOutputStream
                        (urlConn.getOutputStream());
                outputStream.write(locationObject.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                if (urlConn.getResponseCode()==HTTP_OK) {
                    BufferedReader in = new BufferedReader
                            (new InputStreamReader(urlConn.getInputStream()));
                    in.close();
                }
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
        return "OK";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        ProgressBar progressBar = _tripActivity.get().findViewById(R.id.progressBar);
        passedList.clear();
        progressBar.setVisibility(View.GONE);
        _tripActivity.get().finishTrip();
    }
}