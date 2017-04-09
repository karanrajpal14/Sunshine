package com.example.karan.sunshine;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        //If two panes aren't supported then the activity is launched through
        //an intent with an Uri
        if (savedInstanceState == null) {

            Bundle dateUriBundle = new Bundle();
            dateUriBundle.putParcelable(
                    DetailActivityFragment.DETAIL_URI,
                    getIntent().getData()
            );

            dateUriBundle.putBoolean(DetailActivityFragment.DETAIL_TRANSITION_ANIMATION, true);

            DetailActivityFragment detailActivityFragment = new DetailActivityFragment();
            detailActivityFragment.setArguments(dateUriBundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, detailActivityFragment)
                    .commit();

            // Being here means we are in animation mode
            supportPostponeEnterTransition();
        }
    }
}