package com.alanb.gesturetextinput;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class WatchFrontActivity extends WearableActivity
{
    private final String TAG = this.getClass().getName();
    private GestureDetector m_gestureDetector;
    private String[] m_menu_strs = {"Input to Mobile", "Input to Glass"};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watch_front);

        ArrayAdapter<String> m_adapter = new ArrayAdapter<String>(this, R.layout.watch_front_item, R.id.wf_item_text);
        for (String menu_str: m_menu_strs)
        {
            m_adapter.add(menu_str);
        }

        ListView item_list = (ListView) findViewById(R.id.wf_act_menu);
        item_list.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                m_gestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });
        item_list.setAdapter(m_adapter);
        item_list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Intent intent = null;
                switch (i)
                {
                    case 0:
                        intent = new Intent(getApplicationContext(), WatchInputToMobileActivity.class);
                        break;
                    case 1:
                        intent = new Intent(getApplicationContext(), WatchInputToGlassActivity.class);
                        break;
                }
                if (intent != null)
                {
                    startActivity(intent);
                }
            }
        });

        final DismissOverlayView mDismissOverlay = (DismissOverlayView) findViewById(R.id.wf_dismiss_overlay);
        m_gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            public void onLongPress(MotionEvent event)
            {
                mDismissOverlay.show();
            }
        });
    }
}
