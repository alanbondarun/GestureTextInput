package com.alanb.gesturetextinput;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alanb.gesturecommon.CommonUtils;
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
{
    private final String TAG = this.getClass().getName();
    private BoxInsetLayout mContainerView;
    private GoogleApiClient m_googleApiClient = null;
    private RelativeLayout m_charTouchLayout;

    private float m_touchX;
    private float m_touchY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        m_charTouchLayout = (RelativeLayout) findViewById(R.id.w_touch_frame);
        LayoutInflater inflater = LayoutInflater.from(this);
        WatchWriteInputView touchInputView = (WatchWriteInputView) inflater.inflate(R.layout.watch_touch_area,
                m_charTouchLayout, false);
        m_charTouchLayout.addView(touchInputView);

        touchInputView.setOnTouchListener(wwTouchListener);
        touchInputView.setOnTouchEventListener(wwTouchEventListener);

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
            mContainerView.setBackgroundColor(CommonUtils.getColorVersion(this, android.R.color.black));
//            mTextView.setTextColor(getResources().getColor(android.R.color.white));
//            mClockView.setVisibility(View.VISIBLE);

//           mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
//            mTextView.setTextColor(getResources().getColor(android.R.color.black));
//            mClockView.setVisibility(View.GONE);
        }
    }

    WatchWriteInputView.OnTouchListener wwTouchListener =
            new WatchWriteInputView.OnTouchListener()
    {
        @Override
        public void onTouch(MotionEvent motionEvent)
        {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/touchpos");
            putDataMapReq.getDataMap().putFloat(getResources().getString(R.string.wear_xpos_key),
                    motionEvent.getX() / m_charTouchLayout.getWidth());
            putDataMapReq.getDataMap().putFloat(getResources().getString(R.string.wear_ypos_key),
                    motionEvent.getY() / m_charTouchLayout.getHeight());
            putDataMapReq.getDataMap().putInt(getResources().getString(R.string.wear_action_key),
                    motionEvent.getAction());

            PutDataRequest dataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(m_googleApiClient, dataReq);
        }
    };

    WatchWriteInputView.OnTouchEventListener wwTouchEventListener =
            new WatchWriteInputView.OnTouchEventListener()
    {
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
    };
}
