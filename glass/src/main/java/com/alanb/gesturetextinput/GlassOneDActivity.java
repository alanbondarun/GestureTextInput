package com.alanb.gesturetextinput;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;

public class GlassOneDActivity extends Activity
{
    private View mView;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        mView = buildView();
        setContentView(mView);
    }

    private View buildView()
    {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE);
        card.setEmbeddedLayout(R.layout.glass_oned_layout);
        return card.getView();
    }
}
