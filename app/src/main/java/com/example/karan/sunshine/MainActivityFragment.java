package com.example.karan.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends android.app.Fragment implements onMenuItemSelected {

    public static ArrayAdapter forecastAdapter;

    public MainActivityFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = pref.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default_value));
        fetchWeatherTask.execute(location);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        forecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.textView_listItem, new ArrayList<String>());
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