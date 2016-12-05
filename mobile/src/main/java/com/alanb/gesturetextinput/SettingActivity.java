package com.alanb.gesturetextinput;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.alanb.gesturecommon.SettingItem;
import com.alanb.gesturecommon.SettingItemInfo;

import java.util.ArrayList;

public class SettingActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener
{
    private final String TAG = this.getClass().getName();
    private ListView m_inputListView;
    private SettingListAdapter m_adapter;
    private ArrayList<SettingItemInfo> m_items;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        m_items = SettingItemInfo.getAllItems(this);

        m_adapter = new SettingListAdapter();
        m_inputListView = (ListView) findViewById(R.id.s_inputListView);
        m_inputListView.setAdapter(m_adapter);
        m_inputListView.setOnItemClickListener(this);

        for (SettingItemInfo info: m_items)
        {
            m_adapter.addItem(info.getLabel(), "");
        }
        updateSettingList();
    }

    public void updateSettingList()
    {
        SharedPreferences shared_pref = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        for (int ci = 0; ci < m_items.size(); ci++)
        {
            SettingItemInfo item = m_items.get(ci);
            int cur_pref = shared_pref.getInt(item.getPrefKey(), item.getDefaultItem());
            if (cur_pref < 0 || cur_pref >= item.getMenuLabels().length)
            {
                cur_pref = item.getDefaultItem();
                SharedPreferences.Editor editor = shared_pref.edit();
                editor.putInt(item.getPrefKey(), cur_pref);
                editor.apply();
            }

            ((SettingItem)(m_adapter.getItem(ci))).setValue(item.getMenuLabels()[cur_pref]);
        }

        m_adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        final SettingItemInfo info = m_items.get(i);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(info.getLabel());
        builder.setItems(info.getMenuLabelId(),
            new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    SharedPreferences prefs = getSharedPreferences(
                            getString(R.string.app_pref_key),
                            MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(info.getPrefKey(), i);
                    editor.apply();
                    updateSettingList();
                }
            });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
