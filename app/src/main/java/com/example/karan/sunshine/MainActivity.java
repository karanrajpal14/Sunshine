package com.example.karan.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.karan.sunshine.data.WeatherContract;
import com.example.karan.sunshine.sync.SunshineSyncAdapter;

public class MainActivity extends AppCompatActivity implements Callback {

    //Tag for the detail fragment
    private final String DETAILFRAGMENT_TAG = "DFTAG";
    //Store current location and unit to verify change
    private String currentSetLocation;
    private String currentSetUnit;
    //Flag to verify if device is a large screen device or not
    private boolean twoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Uri contentUri = getIntent() != null ? getIntent().getData() : null;

        if (findViewById(R.id.weather_detail_container) != null) {
            //Device has a wide screen
            twoPane = true;
            if (savedInstanceState == null) {
                DetailActivityFragment fragment = new DetailActivityFragment();
                if (contentUri != null) {
                    Bundle args = new Bundle();
                    args.putParcelable(DetailActivityFragment.DETAIL_URI, contentUri);
                    fragment.setArguments(args);
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG).commit();
            }

        } else {
            //Remove action bar shadow on smaller screen devices
            getSupportActionBar().setElevation(0f);
            //Device has a small screen
            twoPane = false;
        }

        MainActivityFragment mainActivityFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main);
        mainActivityFragment.setUseTodayLayout(!twoPane);

        if (contentUri != null) {
            mainActivityFragment.setInitialSelectedDate(
                    WeatherContract.WeatherEntry.getDateFromUri(contentUri));
        }

        //Get the currently set values for Location and Units from SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentSetLocation = preferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value)
        );
        currentSetUnit = preferences.getString(
                getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric_key)
        );

        SunshineSyncAdapter.initializeSyncAdapter(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Retrieve location and unit values from Shared Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String newSetLocation = sharedPreferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value)
        );
        String newSetUnit = sharedPreferences.getString(
                getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric_key)
        );

        //Check if they have been changed
        //If yes, request for an update
        //If not, let the current data remain
        if (!currentSetLocation.equals(newSetLocation)) {

            //Get main fragment object
            MainActivityFragment mainFragment = (MainActivityFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_main);

            if (mainFragment != null) {
                //Update main fragment due to location change
                mainFragment.onLocationChanged();
            }

            //Get detail fragment object
            DetailActivityFragment detailFragment = (DetailActivityFragment) getSupportFragmentManager()
                    .findFragmentByTag(DETAILFRAGMENT_TAG);

            if (detailFragment != null) {
                detailFragment.onLocationChanged(newSetLocation);
            }

            currentSetLocation = newSetLocation;
        }

        if (!currentSetUnit.equals(newSetUnit)) {

            //Get main fragment object
            MainActivityFragment mainFragment = (MainActivityFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_main);

            if (mainFragment != null) {
                //Update main fragment due to unit change
                mainFragment.onUnitChanged();
            }

            //Get detail fragment object
            DetailActivityFragment detailFragment = (DetailActivityFragment) getSupportFragmentManager()
                    .findFragmentByTag(DETAILFRAGMENT_TAG);

            if (detailFragment != null) {
                detailFragment.onUnitChanged();
            }

            currentSetUnit = newSetUnit;
        }
    }

    @Override
    public void onItemSelected(Uri dateUri, ForecastAdapter.ForecastViewHolder viewHolder) {

        //Replace the fragment in the container
        if (twoPane) {
            //We need to pass the URI here with date using a bundle
            Bundle dateURIBundle = new Bundle();
            dateURIBundle.putParcelable(
                    DetailActivityFragment.DETAIL_URI, dateUri);

            DetailActivityFragment detailActivityFragment = new DetailActivityFragment();
            detailActivityFragment.setArguments(dateURIBundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container,
                            detailActivityFragment,
                            DETAILFRAGMENT_TAG)
                    .commit();
        }
        //If device isn't a large screen device then start the Detail Activity
        else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.setData(dateUri);

            ActivityOptionsCompat activityOptions =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this,
                            new Pair<View, String>(viewHolder.iconView, getString(R.string.detail_icon_transition_name)
                            )
                    );
            ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
        }
    }

}