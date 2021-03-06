package com.example.karan.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.karan.sunshine.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

    static final String DETAIL_URI = "URI";
    static final String DETAIL_TRANSITION_ANIMATION = "DTA";

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

    //Views required to set the data fetched from the loader
    private ImageView iconView;
    private TextView dateTextView;
    private TextView descTextView;
    private TextView highTextView;
    private TextView lowTextView;
    private TextView humidityLabelTextView;
    private TextView humidityTextView;
    private TextView windLabelTextView;
    private TextView windTextView;
    private TextView pressureLabelTextView;
    private TextView pressureTextView;

    private String forecastStr;
    private Uri dateUri;
    private boolean sharedDetailTransition;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(DETAIL_LOADER_ID,
                savedInstanceState, this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail_start, container, false);

        Bundle dateUriBundle = getArguments();
        if (dateUriBundle != null) {
            dateUri = dateUriBundle.getParcelable(DetailActivityFragment.DETAIL_URI);
            sharedDetailTransition = dateUriBundle.getBoolean(DetailActivityFragment.DETAIL_TRANSITION_ANIMATION, false);
        }

        iconView = (ImageView) rootView.findViewById(R.id.detail_icon_imageView);
        dateTextView = (TextView) rootView.findViewById(R.id.detail_date_textView);
        descTextView = (TextView) rootView.findViewById(R.id.detail_forecast_textView);
        highTextView = (TextView) rootView.findViewById(R.id.detail_high_textView);
        lowTextView = (TextView) rootView.findViewById(R.id.detail_low_textView);
        humidityLabelTextView = (TextView) rootView.findViewById(R.id.detail_humidity_label_textView);
        humidityTextView = (TextView) rootView.findViewById(R.id.detail_humidity_textView);
        windLabelTextView = (TextView) rootView.findViewById(R.id.detail_wind_label_textView);
        windTextView = (TextView) rootView.findViewById(R.id.detail_wind_textView);
        pressureLabelTextView = (TextView) rootView.findViewById(R.id.detail_pressure_label_textView);
        pressureTextView = (TextView) rootView.findViewById(R.id.detail_pressure_textView);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getActivity() instanceof DetailActivity) {
            // Inflate the menu, it adds items to the action bar if present
            inflater.inflate(R.menu.menu_detail_fragment, menu);
            finishCreatingMenu(menu);
        }
    }

    private void finishCreatingMenu(Menu menu) {
        //Get the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);
        menuItem.setIntent(createShareIntent());
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, forecastStr.concat(" #Sunshine"));
        return shareIntent;
    }

    void onLocationChanged(String newLocation) {
        Uri currentUri = dateUri;
        if (currentUri != null) {

            long date = WeatherContract.WeatherEntry.getDateFromUri(dateUri);

            dateUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    newLocation, date
            );

            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }
        ViewParent vp = getView().getParent();
        if (vp instanceof CardView) {
            ((View) vp).setVisibility(View.INVISIBLE);
        }
    }

    public void onUnitChanged() {
        if (getLoaderManager().getLoader(DETAIL_LOADER_ID) != null) {
            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (dateUri != null) {
            return new CursorLoader(
                    getActivity(),
                    dateUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            ViewParent vp = getView().getParent();
            if (vp instanceof CardView) {
                ((View) vp).setVisibility(View.VISIBLE);
            }
            int weatherConditionId = data.getInt(COL_WEATHER_CONDITION_ID);
            String weatherDesc = data.getString(COL_WEATHER_DESC);

            if (Utility.usingLocalGraphics(getActivity())) {
                iconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherConditionId));
            } else {

                Glide.with(this)
                        .load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherConditionId))
                        .error(Utility.getArtResourceForWeatherCondition(weatherConditionId))
                        .crossFade()
                        .into(iconView);
                iconView.setContentDescription(getString(R.string.a11y_forecast_icon, weatherDesc));
            }

            long date = data.getLong(COL_WEATHER_DATE);
            String friendlyDateString = Utility.getFullFriendlyDayString(getActivity(), date);
            this.dateTextView.setText(friendlyDateString);

            descTextView.setText(weatherDesc);
            descTextView.setContentDescription(getString(R.string.a11y_forecast, weatherDesc));

            boolean isMetric = Utility.isMetric(getContext());
            String high = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
            highTextView.setText(high);
            highTextView.setContentDescription(getString(R.string.a11y_high_temp, high));

            String low = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
            lowTextView.setText(low);
            lowTextView.setContentDescription(getString(R.string.a11y_low_temp, low));

            humidityLabelTextView.setText(R.string.humidity_label);
            float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
            humidityTextView.setText(getString(R.string.format_humidity, humidity));
            humidityTextView.setContentDescription(getString(R.string.a11y_humidity, humidityTextView.getText()));
            humidityLabelTextView.setContentDescription(humidityTextView.getContentDescription());

            pressureLabelTextView.setText(R.string.pressure_label);
            float pressure = data.getFloat(COL_WEATHER_PRESSURE);
            pressureTextView.setText(getString(R.string.format_pressure, pressure));
            pressureTextView.setContentDescription(getString(R.string.a11y_pressure, pressureTextView.getText()));
            pressureLabelTextView.setContentDescription(pressureTextView.getContentDescription());

            windLabelTextView.setText(R.string.wind_label);
            float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
            float windDir = data.getFloat(COL_WEATHER_DEGREES);
            windTextView.setText(Utility.getFormattedWind(getContext(), windSpeed, windDir));
            windTextView.setContentDescription(getString(R.string.a11y_wind, windTextView.getText()));
            windLabelTextView.setContentDescription(windTextView.getContentDescription());

            forecastStr = String.format("%s - %s - %s/%s", friendlyDateString, weatherDesc, high, low);
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);

        // We need to start the enter transition after the data has loaded
        if (sharedDetailTransition) {
            activity.supportStartPostponedEnterTransition();

            if (null != toolbarView) {
                activity.setSupportActionBar(toolbarView);

                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if (null != toolbarView) {
                Menu menu = toolbarView.getMenu();
                if (null != menu) menu.clear();
                toolbarView.inflateMenu(R.menu.menu_detail_fragment);
                finishCreatingMenu(toolbarView.getMenu());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        forecastStr = null;
    }

}