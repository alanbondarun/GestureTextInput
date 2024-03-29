package com.alanb.gesturetextinput;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.alanb.gesturecommon.MotionEventRecorder;
import com.alanb.gesturecommon.TouchEvent;
import com.alanb.gesturecommon.WatchWriteCorneredView;
import com.alanb.gesturecommon.WatchWriteInputView;
import com.alanb.gesturecommon.WatchWriteSquareView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;

public class WatchInputToMobileActivity extends WearableActivity
{
    private final String TAG = this.getClass().getName();
    private GoogleApiClient m_googleApiClient = null;
    private RelativeLayout m_charTouchLayout;
    private WatchWriteInputView m_touchInputView;

    private DismissOverlayView mDismissOverlayView;

    private MotionEventRecorder m_motionRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watch_to_mobile);
        setAmbientEnabled();

        SharedPreferences pref = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        int ww_shape_pref = pref.getInt(getString(R.string.prefkey_ww_shape),
                getResources().getInteger(R.integer.pref_ww_shape_default));
        Log.d(TAG, Integer.toString(ww_shape_pref));

        m_charTouchLayout = (RelativeLayout) findViewById(R.id.wm_touch_frame);
        LayoutInflater inflater = LayoutInflater.from(this);
        if (ww_shape_pref == 0)
        {
            m_touchInputView = (WatchWriteSquareView) inflater.inflate(R.layout.watch_touch_area,
                    m_charTouchLayout, false);
        }
        else
        {
            m_touchInputView = (WatchWriteCorneredView) inflater.inflate(R.layout.watch_touch_area_cornered,
                    m_charTouchLayout, false);
        }
        m_charTouchLayout.addView(m_touchInputView);

        m_touchInputView.setOnTouchListener(wwTouchListener);
        m_touchInputView.setOnTouchEventListener(wwTouchEventListener);

        mDismissOverlayView = (DismissOverlayView) findViewById(R.id.wm_dismiss_overlay);

        try
        {
            m_motionRecorder = new MotionEventRecorder(this, this.getClass());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

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
    protected void onDestroy()
    {
        if (m_motionRecorder != null)
        {
            m_motionRecorder.close();
        }
        super.onDestroy();
    }

    WatchWriteCorneredView.OnTouchListener wwTouchListener =
            new WatchWriteCorneredView.OnTouchListener()
    {
        private LongPressNotifyTask mmLongPress = null;
        @Override
        public void onTouch(MotionEvent motionEvent)
        {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            {
                mmLongPress = new LongPressNotifyTask();
                mmLongPress.execute(new LongPressNotifyTaskData(
                        getResources().getInteger(R.integer.watch_exit_touch_msec), mDismissOverlayView));
            }
            else
            {
                if (mmLongPress != null)
                {
                    mmLongPress.cancel(true);
                    mmLongPress = null;
                }
            }

            if (m_motionRecorder != null)
                m_motionRecorder.write(motionEvent);

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/touchpos");
            putDataMapReq.getDataMap().putFloat(getResources().getString(R.string.wear_xpos_key),
                    motionEvent.getX() / m_touchInputView.getWidth());
            putDataMapReq.getDataMap().putFloat(getResources().getString(R.string.wear_ypos_key),
                    motionEvent.getY() / m_touchInputView.getHeight());
            putDataMapReq.getDataMap().putInt(getResources().getString(R.string.wear_action_key),
                    motionEvent.getAction());

            PutDataRequest dataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(m_googleApiClient, dataReq);
        }
    };

    WatchWriteCorneredView.OnTouchEventListener wwTouchEventListener =
            new WatchWriteCorneredView.OnTouchEventListener()
    {
        @Override
        public void onTouchEvent(TouchEvent te)
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