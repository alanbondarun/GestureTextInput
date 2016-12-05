package com.alanb.gesturetextinput;

import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;

import java.util.List;

public class GlassSettingAdapter extends CardScrollAdapter
{
    private List<CardBuilder> mCards;

    public GlassSettingAdapter(List<CardBuilder> cards)
    {
        mCards = cards;
    }

    @Override
    public int getCount()
    {
        return mCards.size();
    }

    @Override
    public Object getItem(int pos)
    {
        return mCards.get(pos);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parentView)
    {
        return mCards.get(pos).getView(convertView, parentView);
    }

    @Override
    public int getPosition(Object item)
    {
        return mCards.indexOf(item);
    }
}
