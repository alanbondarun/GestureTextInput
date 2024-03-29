package com.alanb.gesturetextinput;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FrontActivity extends AppCompatActivity
{
    private String[] m_menu_str = {"PalmSwipe", "4-Key Watchwrite", "1D Input",
            "WatchWrite with Smartwatch", "Settings"};
    private ListView m_inputListView;
    private ArrayAdapter<String> m_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front);

        m_adapter = new ArrayAdapter<String>(this, R.layout.input_item, R.id.input_item_text);
        m_inputListView = (ListView) findViewById(R.id.inputListView);
        m_inputListView.setAdapter(m_adapter);
        m_inputListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Intent intent = null;
                switch (i)
                {
                    case 0:
                        intent = new Intent(getApplicationContext(), PalmActivity.class);
                        break;
                    case 1:
                        intent = new Intent(getApplicationContext(), WatchWriteActivity.class);
                        break;
                    case 2:
                        intent = new Intent(getApplicationContext(), OneDActivity.class);
                        break;
                    case 3:
                        intent = new Intent(getApplicationContext(), WatchCooperatingActivity.class);
                        break;
                    case 4:
                        intent = new Intent(getApplicationContext(), SettingActivity.class);
                        break;
                }
                if (intent != null)
                {
                    startActivity(intent);
                }
            }
        });

        for (String menu_str: m_menu_str)
        {
            m_adapter.add(menu_str);
        }
    }
}
