package com.alanb.gesturetextinput;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.alanb.gesturecommon.EditDistCalculator;
import com.alanb.gesturecommon.KeyNode;
import com.alanb.gesturecommon.NanoTimer;
import com.alanb.gesturecommon.OneDInputView;
import com.alanb.gesturecommon.TaskPhraseLoader;
import com.alanb.gesturecommon.TaskRecordWriter;
import com.alanb.gesturecommon.TouchFeedbackFrameLayout;
import com.alanb.gesturecommon.WatchWriteInputView;
import com.google.android.glass.widget.CardBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Math.max;

public class GlassWatchWriteActivity extends Activity
{
    private static final String TAG = GlassWatchWriteActivity.class.getName();

    private WatchWriteInputView m_touchInputView;
    private ArrayList<WatchWriteInputView.TouchEvent> m_gestureTouchAreas;
    private KeyNode m_rootNode;
    // DO NOT modify this directly; use updateCurNode() instead
    private KeyNode m_curNode;

    private TouchFeedbackFrameLayout m_feedbackFrameLayout;

    private String m_inputStr = "";
    private TextView m_inputTextView;
    private ArrayList<TextView> m_viewTexts;
    private final boolean upperTouchFeedback = true;
    private int m_pref_layout;

    private boolean m_taskMode;
    private TaskPhraseLoader m_taskLoader;
    private TextView m_taskTextView;
    private String m_taskStr = null;

    private int m_inc_fixed_num = 0;
    private int m_fix_num = 0;
    private int m_canceled_num = 0;

    private NanoTimer m_phraseTimer;
    private TaskRecordWriter m_taskRecordWriter = null;
    private ArrayList<TaskRecordWriter.TimedAction> m_timedActions;

    public static String msgToSend="";
    public static final int STATE_CONNECTION_STARTED = 0;
    public static final int STATE_CONNECTION_LOST = 1;
    public static final int READY_TO_CONN = 2;
    public static final int MESSAGE_ARRIVED = 3;

    // our last connection
    ConnectedThread mConnectedThread;// = new ConnectedThread(socket);
    // track our connections
    ArrayList<ConnectedThread> mConnThreads;
    // bt adapter for all your bt needs (where we get all our bluetooth powers)
    BluetoothAdapter myBt;
    // list of sockets we have running (for multiple connections)
    ArrayList<BluetoothSocket> mSockets = new ArrayList<BluetoothSocket>();
    // list of addresses for devices we've connected to
    ArrayList<String> mDeviceAddresses = new ArrayList<String>();
    // just a name, nothing more...
    String NAME="G6BITCHES";
    // We can handle up to 7 connections... or something...
    UUID[] uuids = new UUID[2];
    // some uuid's we like to use..
    String uuid1 = "05f2934c-1e81-4554-bb08-44aa761afbfb";
    String uuid2 = "c2911cd0-5c3c-11e3-949a-0800200c9a66";
    // constant we define and pass to startActForResult (must be >0), that the system passes back to you in your onActivityResult()
    // implementation as the requestCode parameter.
    int REQUEST_ENABLE_BT = 1;
    AcceptThread accThread;
    Handler handle;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        View mView = buildView();
        setContentView(mView);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_pref_layout = prefs.getInt(getString(R.string.prefkey_watch_layout),
                getResources().getInteger(R.integer.pref_watch_layout_default));
        switch (m_pref_layout)
        {
            case 0:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_2area);
                break;
            case 1:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_3area);
                break;
            case 2:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_3area_opt);
                break;
            case 3:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_3area_opt_2);
                break;
        }

        m_feedbackFrameLayout = (TouchFeedbackFrameLayout)
                findViewById(R.id.w_touch_point_area);
        m_feedbackFrameLayout.attachFeedbackTo(m_feedbackFrameLayout);

        LayoutInflater inflater = LayoutInflater.from(this);
        WatchWriteInputView inputView = (WatchWriteInputView) inflater.inflate(
                R.layout.glass_watch_touch_area, m_feedbackFrameLayout, false);
        m_feedbackFrameLayout.addView(inputView);
        m_feedbackFrameLayout.getFeedbackView().setPointColor(Color.argb(80, 255, 255, 255));
        m_feedbackFrameLayout.getFeedbackView().setRadius(20.0f);

        m_gestureTouchAreas = new ArrayList<WatchWriteInputView.TouchEvent>();

        m_inputTextView = (TextView) findViewById(R.id.w_input_text);

        m_viewTexts = new ArrayList<TextView>();
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_4));

        updateViews(m_rootNode);

        m_phraseTimer = new NanoTimer();
        initTask();

        uuids[0] = UUID.fromString(uuid1);
        uuids[1] = UUID.fromString(uuid2);
        handle = new Handler(Looper.getMainLooper()) {
            private long prevTimeStamp = 0;
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.what) {
                    case STATE_CONNECTION_STARTED:
                        break;
                    case STATE_CONNECTION_LOST:
                        startListening();
                        break;
                    case READY_TO_CONN:
                        startListening();
                        break;
                    case MESSAGE_ARRIVED:
                        JSONObject jsonTouch = null;
                        try
                        {
                            jsonTouch = new JSONObject(msg.getData().getString("Message"));

                            long timeStamp = jsonTouch.getLong("timestamp");
                            Log.d(TAG, "ts: " + timeStamp + ", prev-ts:" + prevTimeStamp);
                            if (timeStamp < prevTimeStamp)
                                break;
                            prevTimeStamp = timeStamp;

                            if (jsonTouch.has("touchevent"))
                            {
                                Log.d(TAG, "has touchevent");
                                JSONObject jsonTE = jsonTouch.getJSONObject("touchevent");
                                String teString = jsonTE.getString(getResources().getString(R.string.wear_touch_key));
                                Log.d(TAG, "te str: " + teString);
                                processTouchEvent(WatchWriteInputView.TouchEvent.valueOf(teString));
                            }
                            else if (jsonTouch.has("touchpos"))
                            {
                                // TODO
                            }
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        // ....
        myBt = BluetoothAdapter.getDefaultAdapter();
        // run the "go get em" thread..
        accThread = new AcceptThread();
        accThread.start();
    }

    @Override
    protected void onDestroy()
    {
        if (m_taskRecordWriter != null)
        {
            m_taskRecordWriter.close();
        }
        if (mConnectedThread != null)
            mConnectedThread.cancel();
        if (accThread != null)
            accThread.cancel();
        super.onDestroy();
    }

    public void startListening() {
        if(accThread!=null) {
            accThread.cancel();
        }else if (mConnectedThread!= null) {
            mConnectedThread.cancel();
        } else {
            accThread = new AcceptThread();
            accThread.start();
        }
    }

    private View buildView()
    {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE);
        card.setEmbeddedLayout(R.layout.glass_watch_layout);
        return card.getView();
    }

    private void initTask()
    {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_taskMode = prefs.getInt(getString(R.string.prefkey_task_mode),
                getResources().getInteger(R.integer.pref_task_mode_default)) == 0;
        m_timedActions = new ArrayList<TaskRecordWriter.TimedAction>();

        m_taskTextView = (TextView) findViewById(R.id.w_task_text);
        if (m_taskMode)
        {
            m_taskLoader = new TaskPhraseLoader(this);
            try
            {
                m_taskRecordWriter = new TaskRecordWriter(this, this.getClass());
            }
            catch (java.io.IOException e)
            {
                e.printStackTrace();
            }
            prepareTask();
        }
        else
        {
            m_taskTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void prepareTask()
    {
        if (!m_taskMode)
            return;
        m_taskStr = m_taskLoader.next();
        m_taskTextView.setText(m_taskStr);
        m_inc_fixed_num = 0;
        m_fix_num = 0;
        m_canceled_num = 0;
        m_timedActions.clear();
    }

    private void processTouchEvent(WatchWriteInputView.TouchEvent te)
    {
        Log.d(TAG, "event = " + te.toString());
        if (te == WatchWriteInputView.TouchEvent.END)
        {
            if (!m_phraseTimer.running())
                m_phraseTimer.begin();
            if (m_curNode.getAct() == KeyNode.Act.DELETE)
            {
                m_phraseTimer.check();
                Log.d(TAG, "Delete one character");
                m_inputStr = m_inputStr.substring(0, max(0, m_inputStr.length()-1));
                m_inc_fixed_num++;
                m_fix_num++;
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "del"));
            }
            else if (m_curNode.getAct() == KeyNode.Act.DONE)
            {
                Log.d(TAG, "Input Done");
                doneTask();
            }
            else if (m_curNode.getCharVal() != null)
            {
                m_phraseTimer.check();
                Log.d(TAG, "Input Result: " + m_curNode.getCharVal());
                m_inputStr += String.valueOf(m_curNode.getCharVal());
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), m_curNode.getCharVal().toString()));
            }
            else
            {
                m_phraseTimer.check();
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "cancel"));
                m_canceled_num++;
            }

            // initialization for next touch(or gesture) input
            m_gestureTouchAreas.clear();
            updateViews(m_rootNode);
        }
        else if (te == WatchWriteInputView.TouchEvent.DROP)
        {
            m_gestureTouchAreas.add(te);
        }
        else if (te == WatchWriteInputView.TouchEvent.MULTITOUCH)
        {
            updateViews(m_rootNode);
            m_gestureTouchAreas.clear();
            m_gestureTouchAreas.add(te);
        }
        else if (te != WatchWriteInputView.TouchEvent.AREA_OTHER)
        {
            if (isValidTouchSequence(m_gestureTouchAreas))
            {
                KeyNode next_node = null;
                KeyNode sibling_node = m_curNode.getParent();
                switch (te)
                {
                    case AREA1:
                        next_node = m_curNode.getNextNode(0);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(0);
                        break;
                    case AREA2:
                        next_node = m_curNode.getNextNode(1);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(1);
                        break;
                    case AREA3:
                        next_node = m_curNode.getNextNode(2);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(2);
                        break;
                    case AREA4:
                        next_node = m_curNode.getNextNode(3);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(3);
                        break;
                }
                if (next_node != null)
                {
                    updateViews(next_node);
                    m_gestureTouchAreas.add(te);
                }
                else if (sibling_node != null)
                {
                    updateViews(sibling_node);
                    if (m_gestureTouchAreas.size() >= 1)
                        m_gestureTouchAreas.remove(m_gestureTouchAreas.size()-1);
                    m_gestureTouchAreas.add(te);
                }
                else
                {
                    m_gestureTouchAreas.add(WatchWriteInputView.TouchEvent.DROP);
                    Log.d(TAG, "Touch drop: end reached");
                }
            }
        }
    }

    private boolean isValidTouchSequence(ArrayList<WatchWriteInputView.TouchEvent> events)
    {
        if (events.size() <= 0 ||
                (events.get(events.size()-1) != WatchWriteInputView.TouchEvent.DROP &&
                        events.get(events.size()-1) != WatchWriteInputView.TouchEvent.MULTITOUCH))
            return true;
        return (events.size() == 1 && events.get(0) == WatchWriteInputView.TouchEvent.DROP);
    }

    private void doneTask()
    {
        if (!m_taskMode)
            return;

        EditDistCalculator.EditInfo info = EditDistCalculator.calc(m_taskStr, m_inputStr);

        double time_before_last = m_phraseTimer.getDiffInSeconds();
        m_phraseTimer.check();
        m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "done"));
        m_phraseTimer.end();

        if (m_taskRecordWriter != null)
        {
            m_taskRecordWriter.write(m_taskRecordWriter.new InfoBuilder()
                    .setInputTime(time_before_last)
                    .setInputStr(m_inputStr)
                    .setPresentedStr(m_taskStr)
                    .setLayoutNum(m_pref_layout)
                    .setNumC(info.num_correct)
                    .setNumIf(m_inc_fixed_num)
                    .setNumF(m_fix_num)
                    .setNumInf(info.num_delete + info.num_insert+ info.num_modify)
                    .setNumCancel(m_canceled_num)
                    .setTimedActions(m_timedActions));
        }

        m_inputStr = "";
        prepareTask();
    }

    private void updateViews(KeyNode node)
    {
        m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));
        if (node.isLeaf())
        {
            KeyNode np = node.getParent();
            if (np != null)
            {
                for (int ci=0; ci < np.getNextNodeNum(); ci++)
                {
                    if (np.getNextNode(ci) == node)
                    {
                        m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassBackground));
                    }
                    else
                    {
                        m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }
        }
        else
        {
            for (int ci = 0; ci < 4; ci++)
            {
                String raw_str = node.getNextNode(ci).getShowStr();
                m_viewTexts.get(ci).setText(raw_str);
                m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
            }
        }
        this.m_curNode = node;
    }

    private WatchWriteInputView.OnTouchListener m_wwTouchListener =
            new WatchWriteInputView.OnTouchListener()
    {
        @Override
        public void onTouch(MotionEvent motionEvent)
        {

        }
    };

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        BluetoothServerSocket tmp;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = myBt.listenUsingInsecureRfcommWithServiceRecord(NAME, uuids[0]);

            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.e(TAG,"Running?");
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {

                try {

                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                // If a connection was accepted

                if (socket != null) {
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);

                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);

            } catch (IOException e) { }
        }
    }


    private void manageConnectedSocket(BluetoothSocket socket) {
        // start our connection thread
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        // so the HH can show you it's working and stuff...
        String devs="";
        for(BluetoothSocket sock: mSockets) {
            devs+=sock.getRemoteDevice().getName()+"\n";
        }
        // pass it to the UI....
        Message msg = handle.obtainMessage(STATE_CONNECTION_STARTED);
        Bundle bundle = new Bundle();
        bundle.putString("NAMES", devs);
        msg.setData(bundle);

        handle.sendMessage(msg);
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

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0)
                    {
                        String recv_str = new String(buffer, getResources().getString(R.string.default_json_charset));
                        String[] recv_strs = recv_str.trim().split(getString(R.string.bt_json_token));
                        for (String str: recv_strs)
                        {
                            Log.d(TAG, "message received: " + str);
                            Message msg = handle.obtainMessage(MESSAGE_ARRIVED);
                            Bundle data = new Bundle();
                            data.putString("Message", str);
                            msg.setData(data);
                            handle.sendMessage(msg);
                        }
                    }
                    if(!msgToSend.equals("")) {
                        Log.e(TAG,"writing!");
                        write(msgToSend.getBytes());
                        setMsg("");
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
        public void connectionLost() {
            Message msg = handle.obtainMessage(STATE_CONNECTION_LOST);
            handle.sendMessage(msg);
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                Message msg = handle.obtainMessage(READY_TO_CONN);
                handle.sendMessage(msg);
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public synchronized void setMsg(String newMsg)
    {
        msgToSend = newMsg;
        Log.d(TAG, "msg = " + msgToSend);
    }
}
