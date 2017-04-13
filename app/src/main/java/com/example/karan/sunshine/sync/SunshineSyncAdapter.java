package com.example.karan.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.karan.sunshine.BuildConfig;
import com.example.karan.sunshine.MainActivity;
import com.example.karan.sunshine.R;
import com.example.karan.sunshine.Utility;
import com.example.karan.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.example.karan.sunshine.sync.SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_INVALID;
import static com.example.karan.sunshine.sync.SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_OK;
import static com.example.karan.sunshine.sync.SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_SERVER_DOWN;
import static com.example.karan.sunshine.sync.SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_SERVER_INVALID;
import static com.example.karan.sunshine.sync.SunshineSyncAdapter.LocationStatus.LOCATION_STATUS_UNKNOWN;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = SunshineSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute)  * 180 = 3 hours
    public static final int SYNC_INTERVAL = 5 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    public static final String ACTION_DATA_UPDATED =
            "com.example.android.sunshine.app.ACTION_DATA_UPDATED";
    //Projection used to fetch the data required by the notification
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;
    //Constants required by the notification
    private static final long DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    static private void setLocationStatus(Context context, @LocationStatus int locationStatus) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(context.getString(R.string.pref_location_status_key), locationStatus);
        editor.commit();
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Log.d(TAG, "configurePeriodicSync: configuring periodic sync");
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
        Log.d(TAG, "configurePeriodicSync: finished");
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Log.d(TAG, "syncImmediately: SyncImmediately called");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
        Log.d(TAG, "syncImmediately: Immediate sync done");
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        Log.d(TAG, "getSyncAccount: Getting sync account");
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        Log.d(TAG, "getSyncAccount: Got sync account");
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        Log.d(TAG, "onAccountCreated: start");

        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        Log.d(TAG, "onAccountCreated: Periodic sync set");
        /*
             * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        Log.d(TAG, "onAccountCreated: auto sync set");

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
        Log.d(TAG, "onAccountCreated: done");
    }

    public static void initializeSyncAdapter(Context context) {
        Log.d(TAG, "initializeSyncAdapter: initing sync adapter");
        getSyncAccount(context);
        Log.d(TAG, "initializeSyncAdapter: done initing sync adapter");
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync Called.");
        String locationQuery = Utility.getPreferredLocation(getContext());

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConn = null;
        BufferedReader bReader = null;

        // Will contain the raw JSON response as a string.
        String forecastJSON = null;

        // We no longer need just the location String, but also potentially the latitude and
        // longitude, in case we are syncing based on a new Place Picker API result.
        Context context = getContext();
        String locationLatitude = String.valueOf(Utility.getLocationLatitude(context));
        String locationLongitude = String.valueOf(Utility.getLocationLongitude(context));

        Uri.Builder baseUrlBuilder = new Uri.Builder();
        final String QUERY_PARAM = "q";
        final String LAT_PARAM = "lat";
        final String LON_PARAM = "lon";
        final String MODE_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYCOUNT_PARAM = "cnt";
        final String APIKEY_PARAM = "appid";
        final String format = "json";
        final String units = "metric";
        final int numDays = 14;
        // Construct the URL for the OpenWeatherMap query
        // Possible parameters are avaiable at OWM's forecast API page, at
        // http://openweathermap.org/API#forecast
        baseUrlBuilder.scheme("http")
                .authority("api.openweathermap.org")
                .appendPath("data")
                .appendPath("2.5")
                .appendPath("forecast")
                .appendPath("daily");

        // Instead of always building the query based off of the location string, we want to
        // potentially build a query using a lat/lon value. This will be the case when we are
        // syncing based off of a new location from the Place Picker API. So we need to check
        // if we have a lat/lon to work with, and use those when we do. Otherwise, the weather
        // service may not understand the location address provided by the Place Picker API
        // and the user could end up with no weather! The horror!
        if (Utility.isLocationLatLonAvailable(context)) {
            baseUrlBuilder.appendQueryParameter(LAT_PARAM, locationLatitude)
                    .appendQueryParameter(LON_PARAM, locationLongitude);
        } else {
            baseUrlBuilder.appendQueryParameter(QUERY_PARAM, locationQuery);
        }

        baseUrlBuilder
                .appendQueryParameter(QUERY_PARAM, locationQuery)
                .appendQueryParameter(MODE_PARAM, format)
                .appendQueryParameter(UNITS_PARAM, units)
                .appendQueryParameter(DAYCOUNT_PARAM, String.valueOf(numDays))
                .appendQueryParameter(APIKEY_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                .build();

        try {
            Log.d(TAG, "onPerformSync: Built URL: " + baseUrlBuilder.toString());
            URL url = new URL(baseUrlBuilder.toString());
            // Create the request to OpenWeatherMap, and open the connection
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestMethod("GET");
            urlConn.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConn.getInputStream();
            StringBuilder buffer = new StringBuilder();

            if (inputStream == null) {
                // Nothing to do.
                return;
            }

            bReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bReader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                setLocationStatus(getContext(), LOCATION_STATUS_SERVER_DOWN);
                return;
            }

            forecastJSON = buffer.toString();
            getWeatherDataFromJson(forecastJSON, locationQuery);


        } catch (IOException e) {
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
            Log.e(TAG, "Error fetching data", e);
            setLocationStatus(getContext(), LOCATION_STATUS_SERVER_DOWN);

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            e.printStackTrace();
            setLocationStatus(getContext(), LOCATION_STATUS_SERVER_DOWN);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }
        Log.d(TAG, "onPerformSync: Sync finished");
    }

    private void getWeatherDataFromJson(String forecastJsonStr, String locationSetting)
            throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";
        final String OWM_MESSAGE_CODE = "cod";

        try {

            JSONObject forecastJson = new JSONObject(forecastJsonStr);

            if (forecastJson.has(OWM_MESSAGE_CODE)) {
                int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);
                switch (errorCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        setLocationStatus(getContext(), LOCATION_STATUS_INVALID);
                        return;
                    default:
                        setLocationStatus(getContext(), LOCATION_STATUS_SERVER_DOWN);
                        return;
                }
            }

            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new weather information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.


            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            for (int i = 0; i < weatherArray.length(); i++) {
                long dateTime;
                double pressure;
                int humidity;
                double windSpeed;
                double windDirection;

                double high;
                double low;

                String description;
                int weatherId;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);

                pressure = dayForecast.getDouble(OWM_PRESSURE);
                humidity = dayForecast.getInt(OWM_HUMIDITY);
                windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                // description is in a child array called "weather", which is 1 element long.
                // That element also contains a weather code.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double highs = temperatureObject.getDouble(OWM_MAX);
                double lows = temperatureObject.getDouble(OWM_MIN);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, highs);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, lows);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                cVVector.add(weatherValues);
            }

            int inserted = 0;
            // add to database
            if (cVVector.size() > 0) {
                // Student: call bulkInsert to add the weatherEntries to the database here
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                inserted = getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
            }

            getContext().getContentResolver().delete(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    WeatherContract.WeatherEntry.COLUMN_DATE + "<= ?",
                    new String[]{
                            Long.toString(dayTime.setJulianDay(julianStartDay - 1))
                    }
            );
            updateWidgets();
            notifyWeather();

            Log.d(TAG, "FetchWeatherTask Complete. " + inserted + " Inserted");
            setLocationStatus(getContext(), LOCATION_STATUS_OK);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            e.printStackTrace();
            setLocationStatus(getContext(), LOCATION_STATUS_SERVER_INVALID);
        }
    }

    private void updateWidgets() {
        Context context = getContext();
        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(context.getPackageName());
        context.sendBroadcast(dataUpdatedIntent);
    }

    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        long locationID = -1;
        int columnIndex;
        Cursor locationCursor = getContext().getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null
        );

        if (locationCursor.moveToFirst()) {
            columnIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationID = locationCursor.getLong(columnIndex);
        } else {
            ContentValues values = new ContentValues();
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);
            Uri locationUri = getContext().getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, values);
            locationID = ContentUris.parseId(locationUri);
        }
        locationCursor.close();
        return locationID;
    }

    private void notifyWeather() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean displayNotification = prefs.getBoolean(
                context.getString(R.string.pref_notification_key),
                Boolean.parseBoolean(context.getString(R.string.pref_notification_default_value))
        );

        if (!displayNotification) {
            return;
        }

        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);

        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification with the weather.
            String locationQuery = Utility.getPreferredLocation(context);

            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

            // we'll query our contentProvider, as always
            Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            if (cursor.moveToFirst()) {
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String desc = cursor.getString(INDEX_SHORT_DESC);

                int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                String title = context.getString(R.string.app_name);
                boolean isMetric = Utility.isMetric(context);
                Resources resources = context.getResources();
                int artResourceId = Utility.getArtResourceForWeatherCondition(weatherId);
                String artResourceURL = Utility.getArtUrlForWeatherCondition(context, weatherId);

                //On honeycomb and higher, we can fetch the large icon size
                //Prior to that, we have to specify it explicitly
                int largeIconWidth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                        resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
                        :
                        resources.getDimensionPixelSize(R.dimen.notification_large_icon_default);

                int largeIconHeight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                        resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
                        :
                        resources.getDimensionPixelSize(R.dimen.notification_large_icon_default);

                //Retrieve large icon
                Bitmap largeIcon;
                try {
                    largeIcon = Glide.with(context)
                            .load(artResourceURL)
                            .asBitmap()
                            .error(artResourceId)
                            .fitCenter()
                            .into(largeIconWidth, largeIconHeight)
                            .get();

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    largeIcon = BitmapFactory.decodeResource(resources, artResourceId);
                }

                // Define the text of the forecast.
                String contentText = String.format(context.getString(R.string.format_notification),
                        desc,
                        Utility.formatTemperature(context, high, isMetric),
                        Utility.formatTemperature(context, low, isMetric));

                //build your notification here.
                NotificationCompat.Builder builder = new android.support.v4.app.NotificationCompat.Builder(context)
                        .setColor(resources.getColor(R.color.colorPrimaryLight))
                        .setSmallIcon(iconId)
                        .setContentTitle(title)
                        .setLargeIcon(largeIcon)
                        .setContentText(contentText);

                //Building a artificial back stack to return to
                Intent mainactivityIntent = new Intent(context, MainActivity.class);

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(mainactivityIntent);

                PendingIntent mainPendingIntent = stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

                builder.setContentIntent(mainPendingIntent);

                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(WEATHER_NOTIFICATION_ID, builder.build());

                //refreshing last sync
                prefs.edit().putLong(lastNotificationKey, System.currentTimeMillis()).apply();
            }
            cursor.close();
        }
    }

    private void deleteOldData() {

    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID, LOCATION_STATUS_UNKNOWN, LOCATION_STATUS_INVALID})
    public @interface LocationStatus {
        int LOCATION_STATUS_OK = 0;
        int LOCATION_STATUS_SERVER_DOWN = 1;
        int LOCATION_STATUS_SERVER_INVALID = 2;
        int LOCATION_STATUS_UNKNOWN = 3;
        int LOCATION_STATUS_INVALID = 4;
    }
}