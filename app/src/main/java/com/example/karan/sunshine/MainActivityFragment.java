package com.example.karan.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends android.app.Fragment implements onMenuItemSelected {

    public static ArrayAdapter forecastAdapter;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHER STATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };

        ArrayList<String> weekForecast = new ArrayList<>(Arrays.asList(data));
        forecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.textView_listItem, weekForecast);
        ListView listView = (ListView) view.findViewById(R.id.listView_forecast);
        listView.setAdapter(forecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent weatherDetailIntent = new Intent(getActivity(), DetailActivity.class);
                weatherDetailIntent.putExtra("clickedDayForecast", forecastAdapter.getItem(i).toString());
                //Toast.makeText(getActivity(), Intent.EXTRA_TEXT, Toast.LENGTH_LONG).show();
                startActivity(weatherDetailIntent);
            }
        });
        return view;
    }

    @Override
    public void onRefreshSelected(String countryCode) {
        new FetchWeatherTask().execute(countryCode);
    }
}