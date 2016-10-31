package com.alanb.gesturetextinput;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

public class WatchCooperatingActivity extends AppCompatActivity
{
    private final String TAG = this.getClass().getName();
    private GoogleApiClient mGoogleApiClient;

    private GoogleApiClient.ConnectionCallbacks connectionCallbacks =
            new GoogleApiClient.ConnectionCallbacks()
    {
        @Override
        public void onConnected(@Nullable Bundle bundle)
        {
            Log.d(TAG, "connected");
            Wearable.DataApi.addListener(mGoogleApiClient, dataListener);
        }

        @Override
        public void onConnectionSuspended(int i)
        {
            Log.d(TAG, "connection suspended");
        }
    };

    private GoogleApiClient.OnConnectionFailedListener failedListener =
            new GoogleApiClient.OnConnectionFailedListener()
    {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
        {
            Log.d(TAG, "connection failed");
        }
    };

    private DataApi.DataListener dataListener = new DataApi.DataListener()
    {
        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer)
        {
            for (DataEvent event: dataEventBuffer)
            {
                if (event.getType() == DataEvent.TYPE_CHANGED)
                {
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo("/touchevent") == 0)
                    {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        Log.d(TAG, "Touch Received = " + dataMap.getString(
                                getResources().getString(R.string.wear_touch_key)
                        ));
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_coop);

        GoogleApiClient.Builder g_builder = new GoogleApiClient.Builder(this);
        g_builder.addApi(Wearable.API);
        g_builder.addConnectionCallbacks(connectionCallbacks);
        g_builder.addOnConnectionFailedListener(failedListener);
        mGoogleApiClient = g_builder.build();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause()
    {
        Wearable.DataApi.removeListener(mGoogleApiClient, dataListener);
        mGoogleApiClient.disconnect();
        super.onPause();
    }
}
