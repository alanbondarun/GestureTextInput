package com.alanb.gesturetextinput;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class WatchWriteActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();
    private LinearLayout m_touchAreaAll;
    private ArrayList<RelativeLayout> m_touchAreas;
    private ArrayList<TextView> m_viewTexts;

    public enum TouchEvent
    {
        AREA1, AREA2, AREA3, AREA4, DROP
    }

    private KeyNode m_rootNode, m_curNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_write);

        m_rootNode = KeyNode.generateKeyTree(this);
        m_curNode = m_rootNode;

        m_touchAreaAll = (LinearLayout) findViewById(R.id.w_char_touch);

        m_touchAreas = new ArrayList<>();
        m_touchAreas.add((RelativeLayout) findViewById(R.id.touchArea1));
        m_touchAreas.add((RelativeLayout) findViewById(R.id.touchArea2));
        m_touchAreas.add((RelativeLayout) findViewById(R.id.touchArea3));
        m_touchAreas.add((RelativeLayout) findViewById(R.id.touchArea4));

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_4));

        for (int ci=0; ci<4; ci++)
        {
            m_viewTexts.get(ci).setText(m_rootNode.getShowStr(ci));
        }

        m_touchAreaAll.setOnTouchListener(new View.OnTouchListener() {
            private TouchEvent prev_e = TouchEvent.DROP;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                TouchEvent cur_e = TouchEvent.DROP;
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
                {
                    double xrel = motionEvent.getX() / view.getWidth();
                    double yrel = motionEvent.getY() / view.getHeight();
                    if (0 < xrel && xrel <= 0.5)
                    {
                        if (0 < yrel && yrel <= 0.5)
                        {
                            cur_e = TouchEvent.AREA1;
                        }
                        else if (0.5 < yrel && yrel < 1)
                        {
                            cur_e = TouchEvent.AREA3;
                        }
                        else
                        {
                            cur_e = TouchEvent.DROP;
                        }
                    }
                    else if (0.5 < xrel && xrel < 1)
                    {
                        if (0 < yrel && yrel <= 0.5)
                        {
                            cur_e = TouchEvent.AREA2;
                        }
                        else if (0.5 < yrel && yrel < 1)
                        {
                            cur_e = TouchEvent.AREA4;
                        }
                        else
                        {
                            cur_e = TouchEvent.DROP;
                        }
                    }
                    else
                    {
                        cur_e = TouchEvent.DROP;
                    }
                }
                else
                {
                    cur_e = TouchEvent.DROP;
                }
                if (cur_e != prev_e)
                {
                    WatchWriteActivity.this.processTouch(cur_e);
                    prev_e = cur_e;
                }

                return true;
            }
        });
    }

    public void processTouch(TouchEvent te)
    {
        switch (te)
        {
            // TODO cases
            case AREA1:
                Log.d(TAG, "1");
                break;
            case AREA2:
                Log.d(TAG, "2");
                break;
            case AREA3:
                Log.d(TAG, "3");
                break;
            case AREA4:
                Log.d(TAG, "4");
                break;
            case DROP:
                Log.d(TAG, "drop");
                break;
        }
    }
}
