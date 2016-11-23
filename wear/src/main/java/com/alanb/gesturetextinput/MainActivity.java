package com.alanb.gesturetextinput;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alanb.gesturecommon.CommonUtils;
import com.alanb.gesturecommon.WatchWriteInputView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends WearableActivity
{
    private final String TAG = this.getClass().getName();
    public static final String AVOID_DEVICE = "Galaxy";

    private BoxInsetLayout mContainerView;
    private GoogleApiClient m_googleApiClient = null;
    private RelativeLayout m_charTouchLayout;

    private float m_touchX;
    private float m_touchY;

    private DismissOverlayView mDismissOverlay;
    private GestureDetector mDetector;

    public static final int READY_TO_CONN =0;
    public static final int CANCEL_CONN =1;
    public static final int MESSAGE_READ =2;

    private BluetoothAdapter myBt;
    private String m_btDeviceName, m_btDeviceMac;

    UUID[] uuids = new UUID[2];
    // some uuid's we like to use..
    String uuid1 = "05f2934c-1e81-4554-bb08-44aa761afbfb";
    String uuid2 = "c2911cd0-5c3c-11e3-949a-0800200c9a66";
    //  DateFormat df = new DateFormat("ddyyyy")
    ConnectThread mConnThread;
    ConnectedThread m_connectedThread;
    Handler handle;
    // constant we define and pass to startActForResult (must be >0), that the system passes back to you in your onActivityResult()
    // implementation as the requestCode parameter.
    int REQUEST_ENABLE_BT = 1;
    // bc for discovery mode for BT...
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                if (!device.getName().contains(AVOID_DEVICE))
                {
                    m_btDeviceName = device.getName();
                    m_btDeviceMac = device.getAddress();
                    beginConnect();
                }
            }
        }
    };

    Context ctx;

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

        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            public void onLongPress(MotionEvent event)
            {
                mDismissOverlay.show();
            }
        });

        ctx = this;
        handle = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case READY_TO_CONN:
                        mConnThread=null;
                        beginConnect();
                        break;
                    case CANCEL_CONN:
                        break;
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;

                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        Log.e(TAG,"received: "+readMessage);
                        if (readMessage.length() > 0)
                        {
                            // do soemthing...
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        uuids[0] = UUID.fromString(uuid1);
        uuids[1] = UUID.fromString(uuid2);

        // use the same UUID across an installation
        // should allow clients to find us repeatedly
        myBt = BluetoothAdapter.getDefaultAdapter();
        if (myBt == null) {
            Toast.makeText(this, "Device Does not Support Bluetooth", Toast.LENGTH_LONG).show();
        }
        else if (!myBt.isEnabled()) {
            // we need to wait until bt is enabled before set up, so that's done either in the following else, or
            // in the onActivityResult for our code ...
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            detectAndSetUp();
        }
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
    public void onDestroy()
    {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    WatchWriteInputView.OnTouchListener wwTouchListener =
            new WatchWriteInputView.OnTouchListener()
    {
        @Override
        public void onTouch(MotionEvent motionEvent)
        {


            if (m_connectedThread != null && m_connectedThread.isAlive())
            {
                String data = "na mid god mid";
                m_connectedThread.write(data.getBytes());
            }
            mDetector.onTouchEvent(motionEvent);
            /*PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/touchpos");
            putDataMapReq.getDataMap().putFloat(getResources().getString(R.string.wear_xpos_key),
                    motionEvent.getX() / m_charTouchLayout.getWidth());
            putDataMapReq.getDataMap().putFloat(getResources().getString(R.string.wear_ypos_key),
                    motionEvent.getY() / m_charTouchLayout.getHeight());
            putDataMapReq.getDataMap().putInt(getResources().getString(R.string.wear_action_key),
                    motionEvent.getAction());

            PutDataRequest dataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(m_googleApiClient, dataReq);*/
        }
    };

    WatchWriteInputView.OnTouchEventListener wwTouchEventListener =
            new WatchWriteInputView.OnTouchEventListener()
    {
        @Override
        public void onTouchEvent(WatchWriteInputView.TouchEvent te)
        {
            /*PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/touchevent");
            putDataMapReq.getDataMap().putString(getResources().getString(R.string.wear_touch_key),
                    te.name());
            putDataMapReq.setUrgent();

            PutDataRequest dataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(m_googleApiClient, dataReq);*/
        }
    };

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if(requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode != RESULT_OK)
            {
                Toast.makeText(this, "Failed to enable Bluetooth", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_LONG).show();
                detectAndSetUp();
            }
        }
    }

    private void detectAndSetUp() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        Set<BluetoothDevice> pairedDevices = myBt.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "device: " + device.getName());
                if (!device.getName().contains(AVOID_DEVICE))
                {
                    this.m_btDeviceName = device.getName();
                    this.m_btDeviceMac = device.getAddress();
                    beginConnect();
                    break;
                }
            }
        }
        myBt.startDiscovery();
    }

    private void beginConnect()
    {
        if (mConnThread != null)
        {
            Log.e(TAG, "Canceling old connection, and starting new one.");
            mConnThread.cancel();
        }
        else
        {
            Log.e(TAG, "got a thing...");
            Log.e(TAG, "device: " + m_btDeviceName);
            Log.e(TAG, "mac: " + m_btDeviceMac);
            BluetoothDevice dev = myBt.getRemoteDevice(m_btDeviceMac);
            mConnThread = new ConnectThread(dev);
            mConnThread.run();
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            Log.e(TAG,"ConnectThread start....");
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {

                // this seems to work on the note3...
                // you can remove the Insecure if you want to...
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuids[0]);
                //                  Method m;
                // this is an approach I've seen others use, it wasn't nescesary for me,
                // but your results may vary...

                //                  m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
                //                  tmp = (BluetoothSocket) m.invoke(device, 1);
                //              } catch (NoSuchMethodException e1) {
                //                  // TODO Auto-generated catch block
                //                  e1.printStackTrace();
                //              } catch (IllegalArgumentException e2) {
                //                  // TODO Auto-generated catch block
                //                  e2.printStackTrace();
                //              } catch (IllegalAccessException e3) {
                //                  // TODO Auto-generated catch block
                //                  e3.printStackTrace();
                //              } catch (InvocationTargetException e4) {
                //                  // TODO Auto-generated catch block
                //                  e4.printStackTrace();
                //              }
                //                  if(tmp.isConnected()) {
                //                      break
                //                  }



            } catch (Exception e) {
                Log.e(TAG,"Danger Will Robinson");
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            myBt.cancelDiscovery();
            Log.e(TAG,"stopping discovery");

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.e(TAG,"connecting!");

                mmSocket.connect();
            } catch (IOException connectException) {

                Log.e(TAG,"failed to connect");

                // Unable to connect; close the socket and get out
                try {
                    Log.e(TAG,"close-ah-da-socket");

                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG,"failed to close hte socket");

                }
                Log.e(TAG,"returning..");

                return;
            }

            Log.e(TAG,"we can now manage our connection!");

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);

            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket)
    {
        m_connectedThread = new ConnectedThread(mmSocket);
        m_connectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    handle.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
        public void connectionLost()
        {
            Message msg = handle.obtainMessage(CANCEL_CONN);
            handle.sendMessage(msg);
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //              mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                //              .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
