package com.alanb.gesturetextinput;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.alanb.gesturecommon.BreakTask;
import com.alanb.gesturecommon.EditDistCalculator;
import com.alanb.gesturecommon.KeyNode;
import com.alanb.gesturecommon.NanoTimer;
import com.alanb.gesturecommon.TaskPhraseLoader;
import com.alanb.gesturecommon.TaskRecordWriter;
import com.alanb.gesturecommon.TouchEvent;
import com.alanb.gesturecommon.TouchFeedbackFrameLayout;
import com.alanb.gesturecommon.WatchWriteCorneredView;
import com.alanb.gesturecommon.WatchWriteInputView;
import com.alanb.gesturecommon.WatchWriteSquareView;
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
    private ArrayList<TouchEvent> m_gestureTouchAreas;
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

    private boolean mEnableMultitouch = false;
    private boolean mUseCorneredShape = false;
    private boolean mBlockAutoBreak = true;

    public static String msgToSend="";
    public static final int STATE_CONNECTION_STARTED = 0;
    public static final int STATE_CONNECTION_LOST = 1;
    public static final int READY_TO_CONN = 2;
    public static final int MESSAGE_ARRIVED = 3;

    private ConnectedThread mConnectedThread;
    private BluetoothAdapter myBt;

    private final String NAME = "GoogleGlassss";
    private UUID bt_uuid;

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_pref_layout = prefs.getInt(getString(R.string.prefkey_watch_layout),
                getResources().getInteger(R.integer.pref_watch_layout_default));
        m_rootNode = KeyNode.keyTreeFromPref(this, KeyNode.KeyType.WATCHWRITE, m_pref_layout);
        if (prefs.getInt(getString(R.string.prefkey_multitouch_to_cancel),
                getResources().getInteger(R.integer.pref_multitouch_to_cancel_default)) == 0)
        {
            mEnableMultitouch = true;
        }
        else
        {
            mEnableMultitouch = false;
        }

        if (prefs.getInt(getString(R.string.prefkey_ww_shape),
            getResources().getInteger(R.integer.pref_ww_shape_default)) == 1)
        {
            mUseCorneredShape = true;
        }
        else
        {
            mUseCorneredShape = false;
        }
        mBlockAutoBreak = (prefs.getInt(getString(R.string.prefkey_block_auto_break),
                getResources().getInteger(R.integer.pref_block_auto_break_default)) == 0);

        m_feedbackFrameLayout = (TouchFeedbackFrameLayout)
                findViewById(R.id.w_touch_point_area);
        m_feedbackFrameLayout.attachFeedbackTo(m_feedbackFrameLayout);

        LayoutInflater inflater = LayoutInflater.from(this);
        WatchWriteInputView inputView = null;
        if (mUseCorneredShape)
        {
            inputView = (WatchWriteInputView) inflater.inflate(
                    R.layout.glass_watch_touch_area_cornered, m_feedbackFrameLayout, false);
        }
        else
        {
            inputView = (WatchWriteInputView) inflater.inflate(
                    R.layout.glass_watch_touch_area, m_feedbackFrameLayout, false);
        }
        m_feedbackFrameLayout.addView(inputView);
        m_feedbackFrameLayout.getFeedbackView().setPointColor(Color.argb(165, 255, 255, 255));
        m_feedbackFrameLayout.getFeedbackView().setRadius(20.0f);

        m_gestureTouchAreas = new ArrayList<TouchEvent>();

        m_inputTextView = (TextView) findViewById(R.id.w_input_text);

        m_viewTexts = new ArrayList<TextView>();
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_4));

        updateViews(m_rootNode, false);

        m_phraseTimer = new NanoTimer();
        initTask();

        bt_uuid = UUID.fromString(getResources().getString(R.string.bt_uuid_str));
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
                        byte[] buffer = (byte[]) msg.obj;
                        JSONObject jsonTouch = null;

                        try
                        {
                            String recv_str = new String(buffer, getResources().getString(R.string.default_json_charset));
                            Log.d(TAG + "Recv", "data: " + recv_str.trim());
                            String[] recv_strs = recv_str.trim().split(getString(R.string.bt_json_token));
                            Log.d(TAG, "WW data start");
                            if (recv_strs.length >= 1)
                            {
                                try
                                {
                                    String str = recv_strs[0];
                                    Log.d(TAG, "WW data: " + str);
                                    String[] str_comma_sep = str.split(",");
                                    if (str_comma_sep.length == 4)
                                    {
                                        double px = Double.valueOf(str_comma_sep[0]);
                                        double py = Double.valueOf(str_comma_sep[1]);
                                        int paction = Integer.valueOf(str_comma_sep[2]);
                                        int pmulti = Integer.valueOf(str_comma_sep[3]);
                                        if (mUseCorneredShape)
                                            processTouchEvent(WatchWriteCorneredView.getTouchEvent(px, py, paction, pmulti));
                                        else
                                            processTouchEvent(WatchWriteSquareView.getTouchEvent(px, py, paction, pmulti));
                                        processTouchMotion(px, py, paction);
                                    }
                                }
                                catch (NumberFormatException e1)
                                {
                                    e1.printStackTrace();
                                }
                            }
                        }
                        catch (IOException e) { e.printStackTrace(); }
                        break;
                    default:
                        break;
                }
            }
        };

        myBt = BluetoothAdapter.getDefaultAdapter();

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
        if (mConnectedThread != null && mConnectedThread.isAlive())
        {
            mConnectedThread.quit();
        }
        if (accThread != null && accThread.isAlive())
        {
            accThread.quit();
        }
        super.onDestroy();
    }

    public void startListening() {
        if(accThread!=null && accThread.isAlive()) {
            accThread.cancel();
        }else if (mConnectedThread!= null && mConnectedThread.isAlive()) {
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
        m_inputStr = "";
        m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));
        m_taskStr = m_taskLoader.next();
        m_taskTextView.setText(m_taskStr);
        m_inc_fixed_num = 0;
        m_fix_num = 0;
        m_canceled_num = 0;
        m_timedActions.clear();
    }

    private void processTouchMotion(double x, double y, int action)
    {
        m_feedbackFrameLayout.setCursorRatio((float)x, (float)y, action);
    }

    private TouchEvent prev_te = TouchEvent.AREA_OTHER;
    private void processTouchEvent(TouchEvent te)
    {
        if (prev_te == te)
            return;
        prev_te = te;

        Log.d(TAG, "event = " + te.toString());
        if (te == TouchEvent.END)
        {
            if (!m_phraseTimer.running())
                m_phraseTimer.begin();
            if (m_curNode == null || m_curNode == m_rootNode || m_gestureTouchAreas.size() <= 0 ||
                    m_gestureTouchAreas.get(m_gestureTouchAreas.size()-1) == TouchEvent.AREA_OTHER)
            {
                Log.d(TAG, "input canceled");
                m_phraseTimer.check();
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "cancel"));
                m_canceled_num++;
            }
            else if (m_curNode.getAct() == KeyNode.Act.DELETE)
            {
                m_phraseTimer.check();
                Log.d(TAG, "delete");
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
                Log.d(TAG, "input canceled");
                m_phraseTimer.check();
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "cancel"));
                m_canceled_num++;
            }

            // initialization for next touch(or gesture) input
            m_gestureTouchAreas.clear();
            updateViews(m_rootNode, false);
        }
        else if (te == TouchEvent.DROP)
        {
            updateViews(m_rootNode, false);
            m_gestureTouchAreas.clear();
            m_gestureTouchAreas.add(te);
        }
        else if (te == TouchEvent.MULTITOUCH && mEnableMultitouch)
        {
            updateViews(m_rootNode, false);
            m_gestureTouchAreas.clear();
            m_gestureTouchAreas.add(te);
        }
        else if (te == TouchEvent.AREA_OTHER)
        {
            updateViews(m_curNode, false);
            m_gestureTouchAreas.add(te);
        }
        else
        {
            if (m_gestureTouchAreas.size() > 0 && m_gestureTouchAreas.get(m_gestureTouchAreas.size()-1) == TouchEvent.AREA_OTHER)
            {
                m_gestureTouchAreas.remove(m_gestureTouchAreas.size()-1);
            }

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
                    updateViews(next_node, true);
                    m_gestureTouchAreas.add(te);
                }
                else if (sibling_node != null)
                {
                    updateViews(sibling_node, true);
                    if (m_gestureTouchAreas.size() >= 1)
                        m_gestureTouchAreas.remove(m_gestureTouchAreas.size()-1);
                    m_gestureTouchAreas.add(te);
                }
                else
                {
                    m_gestureTouchAreas.add(TouchEvent.DROP);
                    Log.d(TAG, "Touch drop: end reached");
                }
            }
        }
    }

    private boolean isValidTouchSequence(ArrayList<TouchEvent> events)
    {
        if (events.size() <= 0 ||
                (events.get(events.size()-1) != TouchEvent.DROP &&
                        events.get(events.size()-1) != TouchEvent.MULTITOUCH))
            return true;
        return false;
    }

    private void doneTask()
    {
        if (!m_taskMode)
            return;
        if (!m_taskStr.equals(""))
        {
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
                        .setNumInf(info.num_delete + info.num_insert + info.num_modify)
                        .setNumCancel(m_canceled_num)
                        .setTimedActions(m_timedActions));
            }

            m_inputStr = "";
            if (mBlockAutoBreak &&
                    m_taskRecordWriter.getNumTask() % getResources().getInteger(R.integer.block_test_num) == 0)
            {
                m_taskStr = "";
                m_taskTextView.setText(m_taskStr);
                BreakTask breakTask = new BreakTask();
                breakTask.setTaskEndListener(this.mBreakFinishListener);
                breakTask.execute(getResources().getInteger(R.integer.block_break_ms));
            }
            else
            {
                prepareTask();
            }
        }
    }

    private BreakTask.TaskEndListener mBreakFinishListener =
            new BreakTask.TaskEndListener()
    {
        @Override
        public void onFinish()
        {
            prepareTask();
        }
    };

    private void updateViews(KeyNode node, boolean selected)
    {
        m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));

        for (int ci = 0; ci < 4; ci++)
        {
            if (!node.isLeaf())
            {
                String raw_str = node.getNextNode(ci).getShowStr();
                m_viewTexts.get(ci).setText(raw_str);
            }
        }

        KeyNode np = node.getParent();
        for (int ci = 0; ci < 4; ci++)
        {
            if (ci == 0 || ci == 3)
            {
                if (selected && np != null && np.getNextNode(ci) == node)
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassBackground));
                else
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassBackgroundWeak));
            }
            else
            {
                if (selected && np != null && np.getNextNode(ci) == node)
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassPink));
                else
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassPinkWeak));
            }
        }

        this.m_curNode = node;
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        BluetoothServerSocket tmp;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = myBt.listenUsingInsecureRfcommWithServiceRecord(NAME, bt_uuid);

            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "AcceptThread start");
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

            Log.d(TAG, "AcceptThread finishing");
        }

        public void cancel()
        {
            quit();
            Message msg = handle.obtainMessage(READY_TO_CONN);
            handle.sendMessage(msg);
        }

        public void quit()
        {
            try
            {
                mmServerSocket.close();
            }
            catch (IOException e) { }
        }
    }


    private void manageConnectedSocket(BluetoothSocket socket) {
        // start our connection thread
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = handle.obtainMessage(STATE_CONNECTION_STARTED);
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

            Log.d(TAG, "ConnectedThread started");
            while (true) {
                try {
                    if (!mmSocket.isConnected())
                    {
                        Log.d(TAG, "disconnected from try");
                    }

                    bytes = mmInStream.read(buffer);

                    if (bytes > 0)
                        handle.obtainMessage(MESSAGE_ARRIVED, bytes, -1, buffer)
                                .sendToTarget();

                    Thread.sleep(20);
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }

            Log.d(TAG, "ConnectedThread finishing");
        }

        public void connectionLost() {
            Message msg = handle.obtainMessage(STATE_CONNECTION_LOST);
            handle.sendMessage(msg);
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
        }

        public void cancel()
        {
            quit();
            Message msg = handle.obtainMessage(READY_TO_CONN);
            handle.sendMessage(msg);
        }

        public void quit()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
