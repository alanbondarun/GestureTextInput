package com.alanb.gesturetextinput;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class SettingActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener
{
    private final String TAG = this.getClass().getName();
    private ArrayList<String> m_menu_str = new ArrayList<>();
    private ListView m_inputListView;
    private SettingListAdapter m_adapter;
    private ArrayList<String> m_watchlayout_str = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        m_menu_str.add(getResources().getString(R.string.watch_layout));
        String[] kind_str = getResources().getStringArray(R.array.watch_layout_kind);
        for (String kind: kind_str)
        {
            m_watchlayout_str.add(kind);
        }

        m_adapter = new SettingListAdapter();
        m_inputListView = (ListView) findViewById(R.id.s_inputListView);
        m_inputListView.setAdapter(m_adapter);
        m_inputListView.setOnItemClickListener(this);

        m_adapter.addItem(m_menu_str.get(0), "");
        updateSettingList();
    }

    public void updateSettingList()
    {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        int cur_layout_idx = prefs.getInt(getString(R.string.prefkey_layout),
                getResources().getInteger(R.integer.pref_layout_default));
        if (cur_layout_idx < 0 || cur_layout_idx >= m_watchlayout_str.size())
        {
            cur_layout_idx = getResources().getInteger(R.integer.pref_layout_default);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(getString(R.string.prefkey_layout), cur_layout_idx);
            editor.apply();
        }
        String cur_layout_str = m_watchlayout_str.get(cur_layout_idx);

        ((SettingItem)(m_adapter.getItem(0))).setValue(cur_layout_str);
        m_adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        switch (i)
        {
            case 0:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.watch_layout));
                builder.setItems(R.array.watch_layout_kind,
                        new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        SharedPreferences prefs = getSharedPreferences(
                                getString(R.string.app_pref_key),
                                MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(getString(R.string.prefkey_layout), i);
                        editor.apply();
                        updateSettingList();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
        }
    }
}
