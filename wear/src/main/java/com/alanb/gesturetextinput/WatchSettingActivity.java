package com.alanb.gesturetextinput;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.alanb.gesturecommon.SettingItem;
import com.alanb.gesturecommon.SettingItemInfo;

public class WatchSettingActivity extends WearableActivity
    implements AdapterView.OnItemClickListener
{
    private WatchSettingsAdapter mAdapter;
    private ListView mInputListView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watch_setting);

        mAdapter = new WatchSettingsAdapter();

        mInputListView = (ListView) findViewById(R.id.ws_settings);
        mInputListView.setAdapter(mAdapter);
        mInputListView.setOnItemClickListener(this);

        for (SettingItemInfo info: SettingItemInfo.getAllItems(this, SettingItemInfo.AppType.WEAR))
        {
            mAdapter.addItem(info.getLabel(), "");
        }
        updateSettingList();
    }

    private void updateSettingList()
    {
        SharedPreferences shared_pref = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        for (int ci = 0; ci < SettingItemInfo.getAllItems(this, SettingItemInfo.AppType.WEAR).size(); ci++)
        {
            SettingItemInfo item = SettingItemInfo.getAllItems(this, SettingItemInfo.AppType.WEAR).get(ci);
            int cur_pref = shared_pref.getInt(item.getPrefKey(), item.getDefaultItem());
            if (cur_pref < 0 || cur_pref >= item.getMenuLabels().length)
            {
                cur_pref = item.getDefaultItem();
                SharedPreferences.Editor editor = shared_pref.edit();
                editor.putInt(item.getPrefKey(), cur_pref);
                editor.apply();
            }

            ((SettingItem)(mAdapter.getItem(ci))).setValue(item.getMenuLabels()[cur_pref]);
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        final SettingItemInfo info = SettingItemInfo.getAllItems(this, SettingItemInfo.AppType.WEAR).get(i);
        if (i == 0)
        {
            SharedPreferences prefs = getSharedPreferences(
                    getString(R.string.app_pref_key),
                    MODE_PRIVATE);
            int prevLayout = prefs.getInt(info.getPrefKey(), info.getDefaultItem());

            /* click the list item to toggle WatchWrite input layout */
            if (prevLayout == 0)
                prevLayout = 1;
            else if (prevLayout == 1)
                prevLayout = 0;
            else
                prevLayout = info.getDefaultItem();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(info.getPrefKey(), prevLayout);
            editor.apply();
            updateSettingList();
        }
    }
}
