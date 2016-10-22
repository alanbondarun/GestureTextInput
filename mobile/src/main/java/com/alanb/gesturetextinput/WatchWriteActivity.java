package com.alanb.gesturetextinput;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class WatchWriteActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();
    private LinearLayout m_touchAreaAll;
    private ArrayList<RelativeLayout> m_touchAreas;

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

        m_touchAreaAll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                double xrel = motionEvent.getX() / view.getWidth();
                double yrel = motionEvent.getY() / view.getHeight();
                if (0 < xrel && xrel <= 0.5)
                {
                    if (0 < yrel && yrel <= 0.5)
                    {
                        WatchWriteActivity.this.processTouch(TouchEvent.AREA1);
                    }
                    else if (0.5 < yrel && yrel < 1)
                    {
                        WatchWriteActivity.this.processTouch(TouchEvent.AREA3);
                    }
                    else
                    {
                        WatchWriteActivity.this.processTouch(TouchEvent.DROP);
                    }
                }
                else if (0.5 < xrel && xrel < 1)
                {
                    if (0 < yrel && yrel <= 0.5)
                    {
                        WatchWriteActivity.this.processTouch(TouchEvent.AREA2);
                    }
                    else if (0.5 < yrel && yrel < 1)
                    {
                        WatchWriteActivity.this.processTouch(TouchEvent.AREA4);
                    }
                    else
                    {
                        WatchWriteActivity.this.processTouch(TouchEvent.DROP);
                    }
                }
                else
                {
                    WatchWriteActivity.this.processTouch(TouchEvent.DROP);
                }
                return true;
            }
        });
    }

    public void processTouch(TouchEvent te)
    {
        switch (te)
        {
            // TODO
        }
    }
}
