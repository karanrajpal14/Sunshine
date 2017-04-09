package com.example.karan.sunshine;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.karan.sunshine.data.WeatherContract;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.support.v7.widget.RecyclerView}.
 */
class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    public final String TAG = this.getClass().getSimpleName();
    private final int VIEW_TYPE_COUNT = 2;
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;
    final private ForecastAdapterOnClickHandler clickHandler;
    final private View emptyView;
    private boolean useTodayLayout = true;
    private ItemChoiceManager itemChoiceManager;

    private Cursor cursor;
    private Context context;

    ForecastAdapter(Context context, ForecastAdapterOnClickHandler clickHandler, View emptyView, int choiceMode) {
        this.context = context;
        this.clickHandler = clickHandler;
        this.emptyView = emptyView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            itemChoiceManager = new ItemChoiceManager(this);
            itemChoiceManager.setChoiceMode(choiceMode);
        }
    }

    @Override
    public ForecastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            int layoutID = -1;
            if (viewType == VIEW_TYPE_TODAY)
                layoutID = R.layout.list_item_forecast_today;
            else if (viewType == VIEW_TYPE_FUTURE_DAY)
                layoutID = R.layout.list_item_forecast;

            View view = LayoutInflater.from(parent.getContext()).inflate(layoutID, parent, false);
            view.setFocusable(true);
            return new ForecastViewHolder(view);

        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(ForecastViewHolder holder, int position) {
        cursor.moveToPosition(position);
        int weatherConditionID = cursor.getInt(MainActivityFragment.COL_WEATHER_CONDITION_ID);
        int defaultImage;

        switch (getItemViewType(position)) {
            case VIEW_TYPE_TODAY:
                defaultImage = Utility.getArtResourceForWeatherCondition(weatherConditionID);
                break;
            default:
                defaultImage = Utility.getIconResourceForWeatherCondition(weatherConditionID);
        }

        if (Utility.usingLocalGraphics(context)) {
            holder.iconView.setImageResource(defaultImage);
        } else {

            Glide.with(context)
                    .load(Utility.getArtUrlForWeatherCondition(context, weatherConditionID))
                    .error(defaultImage)
                    .crossFade()
                    .into(holder.iconView);
        }

        // this enables better animations. even if we lose state due to a device rotation,
        // the animator can use this to re-find the original view
        ViewCompat.setTransitionName(holder.iconView, "iconView" + position);

        holder.dateTextView.setText(
                Utility.getFriendlyDayString(
                        context,
                        cursor.getLong(MainActivityFragment.COL_WEATHER_DATE)
                )
        );

        String description = Utility.getStringForWeatherCondition(context, weatherConditionID);
        holder.descTextView.setText(description);
        holder.descTextView.setContentDescription(context.getString(R.string.a11y_forecast, description));

        double high = cursor.getDouble(MainActivityFragment.COL_WEATHER_MAX_TEMP);
        String highString = Utility.formatTemperature(context, high, Utility.isMetric(context));
        holder.highTextView.setText(highString);
        holder.highTextView.setContentDescription(context.getString(R.string.a11y_high_temp, highString));

        double low = cursor.getDouble(MainActivityFragment.COL_WEATHER_MIN_TEMP);
        String lowString = Utility.formatTemperature(context, low, Utility.isMetric(context));
        holder.lowTextView.setText(lowString);
        holder.lowTextView.setContentDescription(context.getString(R.string.a11y_low_temp, lowString));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            itemChoiceManager.onBindViewHolder(holder, position);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        itemChoiceManager.onRestoreInstanceState(savedInstanceState);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onSaveInstanceState(Bundle outState) {
        itemChoiceManager.onSaveInstanceState(outState);
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && useTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getItemCount() {
        if (cursor == null) return 0;
        return cursor.getCount();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public int getSelectedItemPosition() {
        return itemChoiceManager.getSelectedItemPosition();
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        this.useTodayLayout = useTodayLayout;
    }

    public void swapCursor(Cursor newCursor) {
        cursor = newCursor;
        notifyDataSetChanged();
        emptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ForecastViewHolder) {
            ForecastViewHolder vfh = (ForecastViewHolder) viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }

    interface ForecastAdapterOnClickHandler {
        void onClick(Long date, ForecastViewHolder viewHolder);
    }

    class ForecastViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView iconView;
        final TextView dateTextView;
        final TextView descTextView;
        final TextView highTextView;
        final TextView lowTextView;

        ForecastViewHolder(View view) {
            super(view);
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateTextView = (TextView) view.findViewById(R.id.list_item_date_textView);
            descTextView = (TextView) view.findViewById(R.id.list_item_forecast_textView);
            highTextView = (TextView) view.findViewById(R.id.list_item_high_textView);
            lowTextView = (TextView) view.findViewById(R.id.list_item_low_textView);
            view.setOnClickListener(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                itemChoiceManager.onClick(this);
            }
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            cursor.moveToPosition(adapterPosition);
            int dateColumnIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            clickHandler.onClick(cursor.getLong(dateColumnIndex), this);
        }


    }
}