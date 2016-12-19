package com.example.karan.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class FetchWeatherTask extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... params) {
        if (params.length == 0) {
            return null;
        }
        final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        HttpURLConnection urlConn = null;
        BufferedReader bReader = null;
        String forecastJSON = null;
        Uri.Builder builder = new Uri.Builder();
        final String QUERY_PARAM = "q";
        final String MODE_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYCOUNT_PARAM = "cnt";
        final String APIKEY_PARAM = "appid";
        builder.scheme("http")
                .authority("api.openweathermap.org")
                .appendPath("data")
                .appendPath("2.5")
                .appendPath("forecast")
                .appendPath("daily")
                .appendQueryParameter(QUERY_PARAM, params[0])
                .appendQueryParameter(MODE_PARAM, "json")
                .appendQueryParameter(UNITS_PARAM, "metric")
                .appendQueryParameter(DAYCOUNT_PARAM, "7")
                .appendQueryParameter(APIKEY_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                .build();
        Log.v(LOG_TAG, "URL Built is: " + builder);
        try {
            URL url = new URL(builder.toString());
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestMethod("GET");
            urlConn.connect();

            InputStream inputStream = urlConn.getInputStream();
            StringBuilder buffer = new StringBuilder();

            if (inputStream == null) {
                return null;
            }

            bReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bReader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }

            forecastJSON = buffer.toString();

        } catch (java.io.IOException e) {
            Log.e(LOG_TAG, "Error fetching data", e);
            return null;
        } finally {
            if (urlConn != null) {
                urlConn = null;
            }
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }
}
