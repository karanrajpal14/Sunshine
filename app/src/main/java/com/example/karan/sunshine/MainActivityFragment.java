package com.example.karan.sunshine;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.karan.sunshine.data.WeatherContract;
import com.example.karan.sunshine.sync.SunshineSyncAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends android.support.v4.app.Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;

    //private String currentKnownLocation;
    //private String currentSetUnit;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private static final String TAG = MainActivityFragment.class.getSimpleName();
    //Assigning an id to the loader
    private static final int LOADER_ID = 0;
    private static final String SELECTED_POSTION_KEY = "selected_key";
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    public ForecastAdapter forecastAdapter;
    private ListView listView;
    private int lastSelectedIndex = ListView.INVALID_POSITION;
    private boolean useTodayLayout;

    public MainActivityFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (lastSelectedIndex != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_POSTION_KEY, lastSelectedIndex);
        }
        Log.d(TAG, "onSaveInstanceState: Position saved");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);
        //currentKnownLocation = Utility.getPreferredLocation(getActivity());
    }

    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_forecastfragment, menu);
    }*/

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        *//*if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }*//*

        return super.onOptionsItemSelected(item);
    }*/

    private void saveSetLocationToPreferences() {
        Log.d(TAG, "saveSetLocationToPreferences: Location prefs");
        Cursor c = forecastAdapter.getCursor();
        if (c != null && c.moveToFirst()) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(
                    getString(R.string.pref_view_on_map_latitude_key),
                    c.getString(COL_COORD_LAT)
            );
            editor.putString(
                    getString(R.string.pref_view_on_map_longitude_key),
                    c.getString(COL_COORD_LONG));
            editor.apply();
        }
        Log.d(TAG, "saveSetLocationToPreferences: Location preferences written");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Creating a forecast adapter with no cursor attached
        forecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        forecastAdapter.setUseTodayLayout(useTodayLayout);

        //Getting a reference to the ListView and attaching an adapter to it
        listView = (ListView) view.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null && cursor.getCount() != 0) {

                    long date = cursor.getLong(COL_WEATHER_DATE);
                    String locationSetting = cursor.getString(COL_LOCATION_SETTING);
                    Uri weatherURIWithDate = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, date);
                    ((Callback) getActivity()).onItemSelected(weatherURIWithDate);
                }
                lastSelectedIndex = position;
            }
        });

        //listView.callOnClick();

        //Check if theres is a savedInstance state.
        //If present, get it and retrieve the position using the SELECTED_POSITION_KEY
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_POSTION_KEY)) {
            Log.d(TAG, "onCreateView: Position found. Fetching.");
            lastSelectedIndex = savedInstanceState.getInt(SELECTED_POSTION_KEY);
        }
        forecastAdapter.setUseTodayLayout(useTodayLayout);
        return view;
    }

    public void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public void onLocationChanged() {
        updateWeather();
    }

    public void onUnitChanged() {
        forecastAdapter.notifyDataSetChanged();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String locationSetting = Utility.getPreferredLocation(getActivity());

        //Sort Order : Ascending, by date
        final String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        //Build Uri for query
        Uri weatherForLocationUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithStartDate(
                        locationSetting,
                        System.currentTimeMillis()
                );

        //Create the cursor loader
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        forecastAdapter.swapCursor(data);

        if (lastSelectedIndex != ListView.INVALID_POSITION) {
            Log.d(TAG, "onLoadFinished: Scrolling to position: " + lastSelectedIndex);
            listView.smoothScrollToPosition(lastSelectedIndex);
        }

        saveSetLocationToPreferences();
    }

    @Override
    public void onLoaderReset(Loader loader) {
        forecastAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        this.useTodayLayout = useTodayLayout;
        if (forecastAdapter != null) {
            forecastAdapter.setUseTodayLayout(this.useTodayLayout);
        }
    }
}