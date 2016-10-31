package com.alanb.gesturetextinput;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alanb.gesturecommon.WatchWriteInputView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity
    implements  WatchWriteInputView.OnTouchEventListener
{
    private final String TAG = this.getClass().getName();
    private BoxInsetLayout mContainerView;
    private GoogleApiClient m_googleApiClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        WatchWriteInputView.Builder wwbuilder = new WatchWriteInputView.Builder(this);
        wwbuilder.setOnTouchEventListener(this);
        wwbuilder.setBackground(R.drawable.w_touch_back);
        WatchWriteInputView touchInputView = wwbuilder.build();

        ((LinearLayout)(findViewById(R.id.w_char_touch))).addView(touchInputView);

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
        builder.addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
        {
            @Override
            public void onConnected(@Nullable Bundle bundle)
            {
                Log.d(TAG, "connected");
            }

            @Override
            public void onConnectionSuspended(int i)
            {
                Log.d(TAG, "connection suspended");
            }
        });
        builder.addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
        {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
            {
                Log.d(TAG, "connection failed");
            }
        });
        builder.addApi(Wearable.API);
        m_googleApiClient = builder.build();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (m_googleApiClient != null)
        {
            m_googleApiClient.connect();
        }
    }

    @Override
    protected void onStop()
    {
        if (m_googleApiClient != null)
        {
            m_googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
//            mTextView.setTextColor(getResources().getColor(android.R.color.white));
//            mClockView.setVisibility(View.VISIBLE);

//           mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
//            mTextView.setTextColor(getResources().getColor(android.R.color.black));
//            mClockView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onTouchEvent(WatchWriteInputView.TouchEvent te)
    {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/touchevent");
        putDataMapReq.getDataMap().putString(getResources().getString(R.string.wear_touch_key),
                te.name());
        putDataMapReq.setUrgent();

        PutDataRequest dataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(m_googleApiClient, dataReq);
    }
}
