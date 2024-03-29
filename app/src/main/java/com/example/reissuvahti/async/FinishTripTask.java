package com.example.reissuvahti.async;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.reissuvahti.R;
import com.example.reissuvahti.overpass.OverpassLocation;
import com.example.reissuvahti.view.TripActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

import static com.example.reissuvahti.common.Common.TRIP_STOP_LIST;
import static com.example.reissuvahti.common.Constants.CONNECTION_TIMEOUT;
import static com.example.reissuvahti.common.Constants.SERVER_URL;

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
        if (passedList.size() == 0) {
            return "Ei pysähdyksiä";
        }
        HttpURLConnection urlConn = null;
        Calendar calendar = Calendar.getInstance();
        String result = null;
        String tripName = Integer.toString(calendar.get(Calendar.MINUTE))
                .concat(Integer.toString(calendar.get(Calendar.HOUR_OF_DAY)))
                .concat(Integer.toString(calendar.get(Calendar.DATE)))
                .concat(Integer.toString(calendar.get(Calendar.MONTH)))
                .concat(Integer.toString(calendar.get(Calendar.YEAR)));


            try {
                URL endpoint = new URL(SERVER_URL);
                urlConn = (HttpURLConnection) endpoint.openConnection();
                urlConn.setConnectTimeout(CONNECTION_TIMEOUT);
                urlConn.setDoOutput(true);
                urlConn.setRequestMethod("POST");
                urlConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                urlConn.setRequestProperty("Accept","application/json");

                JSONObject tripObject = new JSONObject();
                tripObject.put("tripName", tripName);
                tripObject.put("userName", "pottala");
                JSONArray locationsArray = new JSONArray();
                for(int i=0; i<passedList.size(); i++) {
                    JSONObject location = new JSONObject();
                    location.put("name", passedList.get(i).getTags().getName());
                    location.put("latitude", passedList.get(i).getLat());
                    location.put("longitude", passedList.get(i).getLon());
                    locationsArray.put(i,location);
                }
                tripObject.put("locations", locationsArray);

                BufferedOutputStream outputStream = new BufferedOutputStream
                        (urlConn.getOutputStream());
                outputStream.write(tripObject.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                BufferedReader in = new BufferedReader
                        (new InputStreamReader(urlConn.getInputStream()));
                in.close();

                if (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    result = "Onnistui";
                } else {
                    result = "Epäonnistui";
                }

            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
                result = "Ei yhteyttä";
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                assert urlConn != null;
                urlConn.disconnect();
            }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        ProgressBar progressBar = _tripActivity.get().findViewById(R.id.progressBar);
        passedList.clear();
        progressBar.setVisibility(View.GONE);
        Toast.makeText(_tripActivity.get(), result, Toast.LENGTH_LONG).show();
    }
}