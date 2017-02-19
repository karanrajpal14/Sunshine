package com.example.karan.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements Callback {

    //Tag for the detail fragment
    private final String DETAILFRAGMENT_TAG = "DFTAG";
    //Store current location to verify location change
    String currentKnownLocation;
    //Flag to verify if device is a large screen device or not
    private boolean twoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.weather_detail_container) != null) {
            twoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.weather_detail_container,
                                new DetailActivityFragment(),
                                DETAILFRAGMENT_TAG
                        ).commit();
            }

        } else {
            twoPane = false;
            //Remove action bar shadow
            getSupportActionBar().setElevation(0f);
        }
        currentKnownLocation = Utility.getPreferredLocation(getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String newLocationSetting = sharedPreferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value)
        );

        String newUnitSetting = sharedPreferences.getString(
                getString(R.string.pref_units_key),
                getString(R.string.pref_units_metric_key)
        );

        if (!currentKnownLocation.equals(newLocationSetting)) {

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
                detailFragment.onLocationChanged(newLocationSetting);
            }

            currentKnownLocation = newLocationSetting;
        }

        // TODO: 18-Feb-17 Do the same for unit change as well
    }

    @Override
    public void onItemSelected(Uri dateUri) {

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
        //If device isn't a large scrren device then start the Detail Activity
        else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.setData(dateUri);
            startActivity(intent);
        }
    }
}