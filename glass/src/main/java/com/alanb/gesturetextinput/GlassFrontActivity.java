package com.alanb.gesturetextinput;

import com.google.android.glass.widget.CardBuilder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class GlassFrontActivity extends Activity
{
    private final String TAG = this.getClass().getSimpleName();
    private View mView;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        mView = buildView();
        setContentView(mView);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.front_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent = null;
        switch (item.getItemId())
        {
            case R.id.oned_menu_item:
                intent = new Intent(this, GlassOneDActivity.class);
                break;
            case R.id.watch_menu_item:
                intent = new Intent(this, GlassWatchWriteActivity.class);
                break;
            case R.id.settings_item:
                intent = new Intent(this, GlassSettingActivity.class);
                break;
        }
        if (intent != null)
        {
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
        {
            openOptionsMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private View buildView()
    {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE);
        card.setEmbeddedLayout(R.layout.glass_front_layout);
        return card.getView();
    }
}
