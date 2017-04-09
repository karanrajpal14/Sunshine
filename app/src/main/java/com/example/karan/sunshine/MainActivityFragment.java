package com.example.karan.sunshine;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import com.example.karan.sunshine.data.WeatherContract;
import com.example.karan.sunshine.sync.SunshineSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link RecyclerView} layout.
 */
public class MainActivityFragment extends android.support.v4.app.Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private static final String TAG = MainActivityFragment.class.getSimpleName();
    //Assigning an id to the loader
    private static final int LOADER_ID = 0;
    private static final String SELECTED_POSITION_KEY = "selected_key";
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
    private RecyclerView recyclerView;
    private int lastSelectedIndex = RecyclerView.NO_POSITION;
    private boolean useTodayLayout, autoSelectView;
    private int choiceMode;
    private boolean holdForTransition;

    public MainActivityFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);
        // We hold for transition here just in-case the activity
        // needs to be re-created. In a standard return transition,
        // this doesn't actually make a difference.
        if (holdForTransition) {
            getActivity().supportPostponeEnterTransition();
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MainActivityFragment,
                0, 0);
        choiceMode = a.getInt(R.styleable.MainActivityFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        autoSelectView = a.getBoolean(R.styleable.MainActivityFragment_autoSelectView, false);
        a.getBoolean(R.styleable.MainActivityFragment_sharedElementTransitions, false);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        View emptyView = view.findViewById(R.id.recycler_view_empty);


        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView_forecast);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);

        forecastAdapter = new ForecastAdapter(getActivity(), new ForecastAdapter.ForecastAdapterOnClickHandler() {
            @Override
            public void onClick(Long date, ForecastAdapter.ForecastViewHolder viewHolder) {
                String locationSetting = Utility.getPreferredLocation(getActivity());
                ((Callback) getActivity()).onItemSelected(
                        WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, date),
                        viewHolder
                );
                lastSelectedIndex = viewHolder.getAdapterPosition();
            }
        }, emptyView, choiceMode);
        recyclerView.setAdapter(forecastAdapter);
        forecastAdapter.setUseTodayLayout(useTodayLayout);

        final View parallaxView = view.findViewById(R.id.parallax_bar);
        if (null != parallaxView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int max = parallaxView.getHeight();
                        if (dy > 0) {
                            parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - dy / 2));
                        } else {
                            parallaxView.setTranslationY(Math.min(0, parallaxView.getTranslationY() - dy / 2));
                        }
                    }
                });
            }
        }

        //Check if there is a savedInstance state.
        //If present, get it and retrieve the position using the SELECTED_POSITION_KEY
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_POSITION_KEY)) {
                // The Recycler View probably hasn't even been populated yet.  Actually perform the
                // swapout in onLoadFinished.
                lastSelectedIndex = savedInstanceState.getInt(SELECTED_POSITION_KEY);
            }
            forecastAdapter.onRestoreInstanceState(savedInstanceState);
        }
        forecastAdapter.setUseTodayLayout(useTodayLayout);
        return view;
    }

    public void updateEmptyView() {
        if (forecastAdapter.getItemCount() == 0) {
            TextView emptyView = (TextView) getView().findViewById(R.id.recycler_view_empty);
            if (emptyView != null) {
                int message = R.string.main_activity_empty_view;
                @SunshineSyncAdapter.LocationStatus int locationStatus = Utility.getLocationStatus(getActivity());
                switch (locationStatus) {
                    case SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_SERVER_DOWN:
                        message = R.string.main_activity_empty_view_server_down;
                        break;
                    case SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_SERVER_INVALID:
                        message = R.string.main_activity_empty_view_server_error;
                        break;
                    case SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_INVALID:
                        message = R.string.main_activity_empty_view_location_invalid;
                        break;
                    default:
                        if (!Utility.checkNetworkState(getActivity())) {
                            message = R.string.main_activity_empty_view_no_internet;
                        }
                }
                emptyView.setText(message);
            }
        }
    }

    public void onLocationChanged() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
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

        if (lastSelectedIndex != RecyclerView.NO_POSITION) {
            Log.d(TAG, "onLoadFinished: Scrolling to position: " + lastSelectedIndex);
            recyclerView.smoothScrollToPosition(lastSelectedIndex);
        }
        updateEmptyView();
        saveSetLocationToPreferences();

        if (data.getCount() > 0) {
            getActivity().supportStartPostponedEnterTransition();
        } else {
            recyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (recyclerView.getChildCount() > 0) {
                        recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = forecastAdapter.getSelectedItemPosition();
                        if (RecyclerView.NO_POSITION == itemPosition) itemPosition = 0;
                        RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(itemPosition);
                        if (null != vh && autoSelectView) {
                            forecastAdapter.selectView(vh);
                        }
                        if (holdForTransition) {
                            getActivity().supportStartPostponedEnterTransition();
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals(getString(R.string.pref_location_status_key))) {
            updateEmptyView();
        }
    }

    @Override
    public void onPause() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (lastSelectedIndex != RecyclerView.NO_POSITION) {
            outState.putInt(SELECTED_POSITION_KEY, lastSelectedIndex);
        }
        Log.d(TAG, "onSaveInstanceState: Position saved");
        forecastAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            recyclerView.clearOnScrollListeners();
        }
    }
}