package com.alanb.gesturetextinput;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.alanb.gesturecommon.MathUtils;
import com.alanb.gesturecommon.MotionEventRecorder;
import com.alanb.gesturecommon.TouchEvent;
import com.alanb.gesturecommon.WatchWriteCorneredView;
import com.alanb.gesturecommon.WatchWriteInputView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class WatchInputToGlassActivity extends WearableActivity
{
    private final String TAG = this.getClass().getName();
    public static final String CONNECT_DEVICE = "Glass";

    private WatchWriteCorneredView m_charTouchArea;

    private DismissOverlayView mDismissOverlay;

    private MotionEventRecorder m_motionRecorder = null;

    public static final int READY_TO_CONN =0;
    public static final int CANCEL_CONN =1;
    public static final int MESSAGE_READ =2;

    private BluetoothAdapter myBt;
    private String m_btDeviceName, m_btDeviceMac;

    private UUID bt_uuid;
    private long beginTime;

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
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName().contains(CONNECT_DEVICE))
                {
                    m_btDeviceName = device.getName();
                    m_btDeviceMac = device.getAddress();
                    beginConnect();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watch_to_glass);
        setAmbientEnabled();

        RelativeLayout m_charTouchLayout = (RelativeLayout) findViewById(R.id.w_touch_frame);
        LayoutInflater inflater = LayoutInflater.from(this);
        m_charTouchArea = (WatchWriteCorneredView) inflater.inflate(R.layout.watch_touch_area_cornered,
                m_charTouchLayout, false);
        m_charTouchLayout.addView(m_charTouchArea);

        m_charTouchArea.setOnTouchListener(wwTouchListener);
        m_charTouchArea.setOnTouchEventListener(wwTouchEventListener);

        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);

        try
        {
            m_motionRecorder = new MotionEventRecorder(this, this.getClass());
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }

        beginTime = System.currentTimeMillis();

        bt_uuid = UUID.fromString(getResources().getString(R.string.bt_uuid_str));
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
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        Log.e(TAG,"received: "+readMessage);
                        break;
                    default:
                        break;
                }
            }
        };

        myBt = BluetoothAdapter.getDefaultAdapter();
        if (myBt == null)
        {
            Toast.makeText(this, "Device Does not Support Bluetooth", Toast.LENGTH_LONG).show();
        }
        else if (!myBt.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            detectAndSetUp();
        }
    }


    @Override
    public void onDestroy()
    {
        unregisterReceiver(mReceiver);
        if (m_motionRecorder != null)
        {
            m_motionRecorder.close();
        }
        super.onDestroy();
    }

    WatchWriteCorneredView.OnTouchListener wwTouchListener =
            new WatchWriteCorneredView.OnTouchListener()
            {
                private LongPressNotifyTask mmLongPressTask = null;
                private int mmEventSendTurn = 0;
                @Override
                public void onTouch(MotionEvent motionEvent)
                {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                    {
                        mmLongPressTask = new LongPressNotifyTask();
                        mmLongPressTask.execute(new LongPressNotifyTaskData(
                                getResources().getInteger(R.integer.watch_exit_touch_msec), mDismissOverlay));
                    }
                    else
                    {
                        if (mmLongPressTask != null)
                        {
                            mmLongPressTask.cancel(true);
                            mmLongPressTask = null;
                        }
                    }

                    if (m_motionRecorder != null)
                    {
                        m_motionRecorder.write(motionEvent);
                    }
                    if (m_connectedThread != null && m_connectedThread.isAlive())
                    {
                        if (motionEvent.getAction() != MotionEvent.ACTION_MOVE ||
                                isNewPeriod(mmEventSendTurn,
                                        Double.valueOf(getResources().getString(R.string.watch_bt_motion_send_rate))))
                        {
                            String builder = "";
                            builder = builder.concat(String.format(Locale.getDefault(), "%.3f", motionEvent.getX() / m_charTouchArea.getWidth()));
                            builder = builder.concat(",");
                            builder = builder.concat(String.format(Locale.getDefault(), "%.3f", motionEvent.getY() / m_charTouchArea.getHeight()));
                            builder = builder.concat(",");
                            builder = builder.concat(String.valueOf(motionEvent.getAction()));
                            builder = builder.concat(",");
                            builder = builder.concat(String.valueOf(motionEvent.getPointerCount()));
                            builder = builder.concat(getResources().getString(R.string.bt_json_token));
                            byte[] data_bytes = builder.getBytes();
                            m_connectedThread.write(data_bytes);
                        }
                        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE)
                        {
                            mmEventSendTurn++;
                        }
                    }
                }
            };

    private boolean isNewPeriod(int turn, double rate)
    {
        return !MathUtils.fequal(Math.floor((turn + 1)/rate) - Math.floor(turn/rate), 0);
    }

    WatchWriteCorneredView.OnTouchEventListener wwTouchEventListener =
            new WatchWriteCorneredView.OnTouchEventListener()
            {
                @Override
                public void onTouchEvent(TouchEvent te)
                {

            /*if (m_connectedThread != null && m_connectedThread.isAlive())
            {
                JSONObject sendObject = new JSONObject();
                try
                {
                    sendObject.put("ev", te.name());
                    sendObject.put("ts", System.currentTimeMillis() - beginTime);
                    byte[] json_bytes = sendObject.toString().concat(getString(R.string.bt_json_token)).
                            getBytes(getResources().getString(R.string.default_json_charset));
                    m_connectedThread.write(json_bytes);
                }
                catch (JSONException e1)
                {
                    e1.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }*/
                }
            };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
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

    private void detectAndSetUp()
    {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        Set<BluetoothDevice> pairedDevices = myBt.getBondedDevices();
        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                Log.d(TAG, "device: " + device.getName());
                if (device.getName().contains(CONNECT_DEVICE))
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

        ConnectThread(BluetoothDevice device) {
            Log.e(TAG, "ConnectThread start");

            BluetoothSocket tmp = null;
            mmDevice = device;

            try
            {
                tmp = device.createRfcommSocketToServiceRecord(bt_uuid);
            }
            catch (Exception e)
            {
                Log.e(TAG,"Danger Will Robinson");
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run()
        {
            // Cancel discovery because it will slow down the connection
            myBt.cancelDiscovery();
            Log.d(TAG,"stopping discovery");

            try
            {
                Log.d(TAG,"connecting!");
                mmSocket.connect();
            }
            catch (IOException connectException)
            {
                Log.e(TAG,"failed to connect");
                try
                {
                    Log.e(TAG,"close-ah-da-socket");
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG,"failed to close hte socket");
                    closeException.printStackTrace();
                }
                Log.e(TAG,"returning..");

                return;
            }

            Log.d(TAG, "connection established");
            manageConnectedSocket(mmSocket);
        }

        void cancel()
        {
            try
            {
                mmSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);
            }
            catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(BluetoothSocket mmSocket)
    {
        m_connectedThread = new ConnectedThread(mmSocket);
        m_connectedThread.start();
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket)
        {
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

        void connectionLost()
        {
            Message msg = handle.obtainMessage(CANCEL_CONN);
            handle.sendMessage(msg);
        }

        void write(byte[] bytes) {
            try {
                //Log.d(TAG, "send data: " + new String(bytes, getResources().getString(R.string.default_json_charset)).trim());
                mmOutStream.write(bytes);
                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
