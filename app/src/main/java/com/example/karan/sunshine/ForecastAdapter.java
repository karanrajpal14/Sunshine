package com.example.karan.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
class ForecastAdapter extends CursorAdapter {

    public final String TAG = this.getClass().getSimpleName();
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    private boolean useTodayLayout;

    ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        this.useTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && useTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
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

        ViewHolder viewHolder = new ViewHolder(view);

        int viewType = getItemViewType(cursor.getPosition());
        int weatherConditionID = cursor.getInt(MainActivityFragment.COL_WEATHER_CONDITION_ID);
        int fallbackIconId = 0;

        switch (viewType) {
            case VIEW_TYPE_TODAY:
                fallbackIconId = Utility.getArtResourceForWeatherCondition(weatherConditionID);
                break;
            default:
                fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherConditionID);
                break;
        }

        Glide.with(context)
                .load(Utility.getArtUrlForWeatherCondition(context, weatherConditionID))
                .error(fallbackIconId)
                .crossFade()
                .into(viewHolder.iconView);

        String date = Utility.getFriendlyDayString(mContext, cursor.getLong(MainActivityFragment.COL_WEATHER_DATE));
        viewHolder.dateTextView.setText(date);

        String description = cursor.getString(MainActivityFragment.COL_WEATHER_DESC);
        viewHolder.descTextView.setText(description);
        viewHolder.descTextView.setContentDescription(context.getString(R.string.a11y_forecast, description));


        String high = Utility.formatTemperature(mContext, cursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP), Utility.isMetric(mContext));
        viewHolder.highTextView.setText(high);
        viewHolder.highTextView.setContentDescription(context.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(mContext, cursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP), Utility.isMetric(mContext));
        viewHolder.lowTextView.setText(low);
        viewHolder.lowTextView.setContentDescription(context.getString(R.string.a11y_low_temp, low));
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