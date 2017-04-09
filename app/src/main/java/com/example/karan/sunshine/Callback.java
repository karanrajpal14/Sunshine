package com.example.karan.sunshine;

import android.net.Uri;

/**
 * A callback interface that all activities containing this fragment must
 * implement. This mechanism allows activities to be notified of item
 * selections.
 */
public interface Callback {
    /**
     * DetailFragmentCallback for when an item has been selected.
     */
    void onItemSelected(Uri dateUri, ForecastAdapter.ForecastViewHolder viewHolder);
}