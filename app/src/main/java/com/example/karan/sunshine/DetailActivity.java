package com.example.karan.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    private ShareActionProvider shareActionProvider;
    private String dayForecastExtra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent clickedDayForecast = getIntent();
        dayForecastExtra = clickedDayForecast.getStringExtra("clickedDayForecast");
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.fragment);
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(dayForecastExtra);
        frameLayout.addView(textView);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);

        MenuItem item = menu.findItem(R.id.menu_item_share);

        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        //shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);
        //setShareIntent(createShareIntent());
        //shareActionProvider.setShareIntent(createShareIntent());

        return (super.onCreateOptionsMenu(menu));
    }

    private Intent createShareIntent() {
        //Setting up share intent
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, dayForecastExtra.concat(" #Sunshine"));
        shareIntent.setType("text/plain");
        return shareIntent;
    }

    /*private void setShareIntent(Intent shareIntent)
    {
        if (shareActionProvider != null)
        {
            shareActionProvider.setShareIntent(shareIntent);
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

}
