package com.alanb.gesturetextinput;

import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
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

class ItemInfo
{
    static class Builder
    {
        private String label;
        private int menu_label_id;
        private String pref_key;
        private int default_item;

        public Builder setLabel(String label) { this.label = label; return this; }
        public Builder setMenuLabelId(int id) { this.menu_label_id = id; return this; }
        public Builder setPrefKey(String key) { this.pref_key = key; return this; }
        public Builder setDefaultItem(int item) { this.default_item = item; return this; }
        public ItemInfo build(Context context) { return new ItemInfo(this, context); }
    }

    private Builder m_prefs;
    private String[] m_menu_labels;
    private ItemInfo(Builder bld, Context context)
    {
        m_prefs = bld;
        m_menu_labels = context.getResources().getStringArray(m_prefs.menu_label_id);
    }
    public String getLabel() { return m_prefs.label; }
    public int getMenuLabelId() { return m_prefs.menu_label_id; }
    public String getPrefKey() { return m_prefs.pref_key; }
    public int getDefaultItem() { return m_prefs.default_item; }
    public String[] getMenuLabels() { return m_menu_labels; }
}

public class SettingActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener
{

    private final String TAG = this.getClass().getName();
    private ListView m_inputListView;
    private SettingListAdapter m_adapter;
    private ArrayList<ItemInfo> m_items;

    private void insertItems()
    {
        m_items.add(new ItemInfo.Builder().setLabel(getResources().getString(R.string.watch_layout))
                .setMenuLabelId(R.array.watch_layout_kind)
                .setPrefKey(getString(R.string.prefkey_watch_layout))
                .setDefaultItem(getResources().getInteger(R.integer.pref_watch_layout_default))
                .build(this));
        m_items.add(new ItemInfo.Builder().setLabel(getResources().getString(R.string.oned_layout))
                .setMenuLabelId(R.array.oned_layout_kind)
                .setPrefKey(getString(R.string.prefkey_oned_layout))
                .setDefaultItem(getResources().getInteger(R.integer.pref_oned_layout_default))
                .build(this));
        m_items.add(new ItemInfo.Builder().setLabel(getResources().getString(R.string.pref_task_mode))
                .setMenuLabelId(R.array.pref_task_mode_item)
                .setPrefKey(getString(R.string.prefkey_task_mode))
                .setDefaultItem(getResources().getInteger(R.integer.pref_task_mode_default))
                .build(this));
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        m_items = new ArrayList<>();
        insertItems();

        m_adapter = new SettingListAdapter();
        m_inputListView = (ListView) findViewById(R.id.s_inputListView);
        m_inputListView.setAdapter(m_adapter);
        m_inputListView.setOnItemClickListener(this);

        for (ItemInfo info: m_items)
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
            ItemInfo item = m_items.get(ci);
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
        final ItemInfo info = m_items.get(i);
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
