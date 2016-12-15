package com.example.karan.sunshine;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchWeatherTask extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... voids) {
        final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        HttpURLConnection urlConn = null;
        BufferedReader bReader = null;
        String forecastJSON = null;

        try {
            URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&appid=d8b5e866ae6e896fc5b7d37eb80fc72e");
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
