package com.example.karan.sunshine;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Created by karan on 21-Feb-17.
 */
public class CompassView extends View implements View.OnClickListener {

    private static final String LOG_TAG = CompassView.class.getSimpleName();
    final DisplayMetrics metrics = getResources().getDisplayMetrics();
    int centerX;
    int centerY;
    int radius;
    float vaneDirection;
    String mWeatherDescription;
    private AccessibilityManager mAccessibilityManager;

    {
        registerListeners();
    }

    public CompassView(Context context) {
        super(context);
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CompassView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //Method to register for clicks
    private void registerListeners() {
        setOnClickListener(this);

        //Retrieve the accessibility manager
        mAccessibilityManager = (AccessibilityManager) getContext().
                getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    public void setVaneDirection(float windDirection, String weatherDescription) {
        mWeatherDescription = weatherDescription;

        //We are getting direction from which wind is blowing, to find vane direction
        //we add 180 degrees. This is the direction measured with the y axis (North)
        //We convert it to angle measure w.r.t +X axis, i.e. pointing East.
        float vaneDirectionDegrees = 90 - (windDirection + 180);

        //Convert vane direction from degrees to radians
        vaneDirection = (float) ((vaneDirectionDegrees / 360.0) * (2 * Math.PI));

        //Update the view that has been already drawn, because updated values are
        //available once the detail fragment is done with loading data.
        this.invalidate();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        //Create a 100x100 view, which is by default the width and height
        setMeasuredDimension((int) dpTopx(100), (int) dpTopx(100));
        centerX = (int) (dpTopx(100) / 2.0);
        centerY = (int) (dpTopx(100) / 2.0);
        radius = centerX - (int) dpTopx(4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawVane(canvas);
    }

    private void drawVane(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);

        paint.setStrokeWidth(dpTopx(4));

        canvas.drawCircle(centerX, centerY, radius, paint);

        // Draw thin Red line.
        paint.setStrokeWidth(dpTopx(2));
        paint.setColor(Color.RED);
        int lineLength = radius - (int) dpTopx(10);
        float lineAngle = vaneDirection;
        int endX = centerX + worldToViewX((int) polarToCartesianX(lineLength, lineAngle));
        int endY = centerY + worldToViewY((int) polarToCartesianY(lineLength, lineAngle));
        canvas.drawLine(centerX, centerY, endX, endY, paint);

        //Draw thick Black line.
        paint.setStrokeWidth(dpTopx(6));
        paint.setColor(Color.BLACK);
        lineLength = radius - (int) dpTopx(20);
        endX = centerX + worldToViewX((int) polarToCartesianX(lineLength, lineAngle));
        endY = centerY + worldToViewY((int) polarToCartesianY(lineLength, lineAngle));
        canvas.drawLine(centerX, centerY, endX, endY, paint);

    }

    private float dpTopx(float dp) {
        return metrics.density * dp;
    }

    private int worldToViewX(int x) {
        return x;
    }

    private int worldToViewY(int y) {
        return -y;
    }

    private double polarToCartesianX(float length, float angle) {
        return (length * Math.cos(angle));
    }

    private double polarToCartesianY(float length, float angle) {
        return (length * Math.sin(angle));
    }

    @Override
    public void onClick(View v) {
        if (mAccessibilityManager.isEnabled()) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.dispatchPopulateAccessibilityEvent(event);

        //Get the friendly wind description
        event.getText().add(mWeatherDescription);
        return true;
    }
}
