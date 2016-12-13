package com.alanb.gesturetextinput;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.alanb.gesturecommon.SettingItemInfo;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

public class GlassSettingActivity extends Activity
    implements AdapterView.OnItemClickListener
{
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private GlassSettingAdapter mAdapter;
    private int mSelectedMenu = 0;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        mCards = buildCards();

        mCardScrollView = new CardScrollView(this);
        mAdapter = new GlassSettingAdapter(mCards);
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        mCardScrollView.setOnItemClickListener(this);
        setContentView(mCardScrollView);
    }

    private ArrayList<CardBuilder> buildCards()
    {
        ArrayList<SettingItemInfo> infos = SettingItemInfo.getAllItems(this, SettingItemInfo.AppType.GLASS);
        ArrayList<CardBuilder> cards = new ArrayList<CardBuilder>();
        SharedPreferences shared_pref = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);

        for (SettingItemInfo info: infos)
        {
            int cur_pref = shared_pref.getInt(info.getPrefKey(), info.getDefaultItem());

            cards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                    .setText(info.getLabel())
                    .setFootnote(info.getMenuLabels()[cur_pref]));
        }

        return cards;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        mSelectedMenu = position;
        invalidateOptionsMenu();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.clear();

        SettingItemInfo info = SettingItemInfo.getAllItems(this, SettingItemInfo.AppType.GLASS).get(mSelectedMenu);
        for (int ci=0; ci<info.getMenuLabels().length; ci++)
        {
            menu.add(Menu.NONE, ci, Menu.NONE, info.getMenuLabels()[ci]);
        }

        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        SettingItemInfo info = SettingItemInfo.getAllItems(this, SettingItemInfo.AppType.GLASS).get(mSelectedMenu);
        SharedPreferences prefs = getSharedPreferences(
                getString(R.string.app_pref_key),
                MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(info.getPrefKey(), item.getItemId());
        editor.apply();

        mCards.get(mSelectedMenu).setFootnote(info.getMenuLabels()[item.getItemId()]);
        mAdapter.notifyDataSetChanged();
        return true;
    }
}
