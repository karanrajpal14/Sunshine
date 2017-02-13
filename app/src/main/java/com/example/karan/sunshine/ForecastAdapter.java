package com.example.karan.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
class ForecastAdapter extends CursorAdapter {

    public final String TAG = this.getClass().getSimpleName();
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;

    ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        return Utility.formatTemperature(mContext, high, isMetric) + "/" + Utility.formatTemperature(mContext, low, isMetric);
    }

    /*
        This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
        string.
     */
    private String convertCursorRowToUXFormat(Cursor cursor) {
        // get row indices for our cursor
        String highAndLow = formatHighLows(
                cursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP));

        return Utility.formatDate(cursor.getLong(MainActivityFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(MainActivityFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutID = -1;
        if (viewType == VIEW_TYPE_TODAY)
            layoutID = R.layout.list_item_forecast_today;
        else if (viewType == VIEW_TYPE_FUTURE_DAY)
            layoutID = R.layout.list_item_forecast;
        return LayoutInflater.from(context).inflate(layoutID, parent, false);
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.

        /*TextView tv = (TextView) view.findViewById(R.id.textView_listItem);
        tv.setText(convertCursorRowToUXFormat(cursor));*/

        ViewHolder viewHolder = new ViewHolder(view);

        int viewType = getItemViewType(cursor.getPosition());
        int weatherConditionID = cursor.getInt(MainActivityFragment.COL_WEATHER_CONDITION_ID);
        int imageResourceID = 0;

        if (viewType == VIEW_TYPE_TODAY) {
            imageResourceID = Utility.getArtResourceForWeatherCondition(weatherConditionID);
        } else if (viewType == VIEW_TYPE_FUTURE_DAY) {
            imageResourceID = Utility.getIconResourceForWeatherCondition(weatherConditionID);
        }

        viewHolder.iconView.setImageResource(imageResourceID);

        String date = Utility.getFriendlyDayString(mContext, cursor.getLong(MainActivityFragment.COL_WEATHER_DATE));
        viewHolder.dateTextView.setText(date);

        String description = cursor.getString(MainActivityFragment.COL_WEATHER_DESC);
        viewHolder.descTextView.setText(description);

        String high = Utility.formatTemperature(mContext, cursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP), Utility.isMetric(mContext));
        viewHolder.highTextView.setText(high);

        String low = Utility.formatTemperature(mContext, cursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP), Utility.isMetric(mContext));
        viewHolder.lowTextView.setText(low);
    }

    private static class ViewHolder {
        final ImageView iconView;
        final TextView dateTextView;
        final TextView descTextView;
        final TextView highTextView;
        final TextView lowTextView;

        ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateTextView = (TextView) view.findViewById(R.id.list_item_date_textView);
            descTextView = (TextView) view.findViewById(R.id.list_item_forecast_textView);
            highTextView = (TextView) view.findViewById(R.id.list_item_high_textView);
            lowTextView = (TextView) view.findViewById(R.id.list_item_low_textView);
        }
    }
}