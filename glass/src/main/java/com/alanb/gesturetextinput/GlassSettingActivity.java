package com.alanb.gesturetextinput;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;

public class GlassSettingActivity extends Activity
{

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        View mView = buildView();
        setContentView(mView);
    }

    private View buildView()
    {
        return new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("MENU layout")
                .setFootnote("Optional menu description")
                .getView();
    }
}
