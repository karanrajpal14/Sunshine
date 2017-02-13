package com.example.karan.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.karan.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

    // These indices are tied to DETAIL_COLUMNS. If DETAIL_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_PRESSURE = 6;
    static final int COL_WEATHER_WIND_SPEED = 7;
    static final int COL_WEATHER_DEGREES = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();
    private static final int DETAIL_LOADER_ID = 1;
    private static final String[] DETAIL_COLUMNS = {
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
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };
    ShareActionProvider shareActionProvider;
    private String forecastStr;
    private ImageView iconView;
    private TextView friendlyDateTextView;
    private TextView dayTextView;
    private TextView descTextView;
    private TextView highTextView;
    private TextView lowTextView;
    private TextView humidityTextView;
    private TextView windTextView;
    private TextView pressureTextView;


    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getActivity().getSupportLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        iconView = (ImageView) rootView.findViewById(R.id.detail_icon_imageView);
        friendlyDateTextView = (TextView) rootView.findViewById(R.id.detail_friendly_date_textView);
        descTextView = (TextView) rootView.findViewById(R.id.detail_forecast_textView);
        dayTextView = (TextView) rootView.findViewById(R.id.detail_day_textView);
        highTextView = (TextView) rootView.findViewById(R.id.detail_high_textView);
        lowTextView = (TextView) rootView.findViewById(R.id.detail_low_textView);
        humidityTextView = (TextView) rootView.findViewById(R.id.detail_humidity_textView);
        windTextView = (TextView) rootView.findViewById(R.id.detail_wind_textView);
        pressureTextView = (TextView) rootView.findViewById(R.id.detail_pressure_textView);

        return rootView;
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, forecastStr.concat(" #Sunshine"));
        return shareIntent;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail_fragment, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);

        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        if (forecastStr != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        } else {
            Log.d(LOG_TAG, "Share action is null");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent detailsIntent = getActivity().getIntent();
        if (detailsIntent == null) {
            return null;
        }
        return new CursorLoader(
                getContext(),
                detailsIntent.getData(),
                DETAIL_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }
        int weatherID = data.getInt(COL_WEATHER_ID);
        int weatherConditionId = data.getInt(COL_WEATHER_CONDITION_ID);

        iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherConditionId));

        long date = data.getLong(COL_WEATHER_DATE);
        String dateText = Utility.getFormattedMonthDay(getActivity(), date);
        String dayString = Utility.getDayName(getContext(), date);
        dayTextView.setText(dayString);
        String freindlyDateString = Utility.getFormattedMonthDay(getContext(), date);
        friendlyDateTextView.setText(freindlyDateString);
        String weatherDesc = data.getString(COL_WEATHER_DESC);
        descTextView.setText(weatherDesc);

        boolean isMetric = Utility.isMetric(getContext());
        String high = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
        highTextView.setText(high);
        String low = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
        lowTextView.setText(low);

        float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
        humidityTextView.setText(getActivity().getString(R.string.format_humidity, humidity));

        float pressure = data.getFloat(COL_WEATHER_PRESSURE);
        pressureTextView.setText(getActivity().getString(R.string.format_pressure, pressure));

        float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
        float windDir = data.getFloat(COL_WEATHER_DEGREES);
        windTextView.setText(Utility.getFormattedWind(getContext(), windSpeed, windDir));

        forecastStr = String.format("%s - %s - %s/%s", dateText, weatherDesc, high, low);

        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        } else {
            Log.d(LOG_TAG, "Share action is null");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
