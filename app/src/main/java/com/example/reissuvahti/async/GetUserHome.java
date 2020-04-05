package com.example.reissuvahti.async;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.reissuvahti.R;
import com.example.reissuvahti.overpass.OverpassLocation;
import com.example.reissuvahti.view.SettingsActivity;
import com.example.reissuvahti.view.TripActivity;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
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
import static com.example.reissuvahti.common.Constants.USER_ENDPOINT;

public class GetUserHome extends AsyncTask<String, Void, String> {
    private WeakReference<SettingsActivity> _settingsActivity;

    public GetUserHome(WeakReference<SettingsActivity> reference){
        _settingsActivity = reference;
    }

    @Override
    protected void onPreExecute() {
        ProgressBar progressBar = _settingsActivity.get().findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.animate();
    }

    @Override
    protected final String doInBackground(String... userNames) {
        if (userNames.length == 0) return "Käyttäjää ei löytynyt";
        String user = userNames[0];
        HttpURLConnection urlConn = null;
        String result = null;
        try {
            URL endpoint = new URL(USER_ENDPOINT.concat("/coordinates"));
            urlConn = (HttpURLConnection) endpoint.openConnection();
            urlConn.setConnectTimeout(CONNECTION_TIMEOUT);
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            urlConn.setRequestProperty("Accept","application/json");

            String apiQuery = "{\"userName\":\"".concat(user).concat("\"}");

            BufferedOutputStream outputStream = new BufferedOutputStream
                    (urlConn.getOutputStream());
            outputStream.write(apiQuery.getBytes());
            outputStream.flush();
            outputStream.close();

            BufferedInputStream in = new BufferedInputStream(urlConn.getInputStream());
            JsonElement element = JsonParser.parseReader(new InputStreamReader(in));
            JSONObject apiResponse = new JSONObject(element.getAsJsonObject().toString());
            JSONObject homeCoords = new JSONObject(apiResponse.getString("homeCoordinates"));
            String coordinates = homeCoords.getString("latitude").concat(", ").concat(homeCoords.getString("longitude"));

            if (urlConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result = coordinates;
            } else {
                result = "Epäonnistui";
            }

            in.close();

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
        ProgressBar progressBar = _settingsActivity.get().findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        TextView homeCoords = _settingsActivity.get().findViewById(R.id.currentHomeText);
        homeCoords.setText(result);
    }
}