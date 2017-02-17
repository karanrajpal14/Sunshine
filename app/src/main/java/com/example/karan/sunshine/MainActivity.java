package com.example.karan.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements Callback {

    private final String DETAILFRAGMENT_TAG = "DFTAG";
    String currentKnownLocation;
    private boolean twoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentKnownLocation = Utility.getPreferredLocation(getApplication());
        if (findViewById(R.id.weather_detail_container) != null) {
            twoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailActivityFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            } else {
                twoPane = false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(getBaseContext());
        if (location != null && !currentKnownLocation.equals(location)) {
            MainActivityFragment mainFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main);
            if (mainFragment != null) {
                mainFragment.onLocationChanged();
            }
            DetailActivityFragment detailFragment = (DetailActivityFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (detailFragment != null) {
                detailFragment.onLocationChanged(location);
            }
            currentKnownLocation = location;
        }

    }

    @Override
    public void onItemSelected(Uri dateUri) {
        if (twoPane) {
            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, dateUri);

            DetailActivityFragment detailActivityFragment = new DetailActivityFragment();
            detailActivityFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(R.id.weather_detail_container, detailActivityFragment, DETAILFRAGMENT_TAG).commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            startActivity(intent);
        }
    }
}