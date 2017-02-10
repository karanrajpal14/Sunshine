package com.example.karan.sunshine;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.karan.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends android.support.v4.app.Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 0; //Assigning an id to the loader
    public ForecastAdapter forecastAdapter;
    private Cursor weatherCursor;

    public MainActivityFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        forecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        ListView listView = (ListView) view.findViewById(R.id.listView_forecast);
        listView.setAdapter(forecastAdapter);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader;
        String locationSetting = Utility.getPreferredLocation(getActivity());
        final String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"; //Sort Order : Ascending, by date
        Uri weatherForLocationUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithStartDate(locationSetting,
                        System.currentTimeMillis());

        return new CursorLoader(getActivity(), weatherForLocationUri,
                null,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        forecastAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        forecastAdapter.swapCursor(null);
    }
}