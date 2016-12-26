package com.example.karan.sunshine;

/**
 * Created by karan on 25-Dec-16.
 */

public interface FetchWeatherResponse {
    void onFetchFinish(String[] weekForecast);
}
