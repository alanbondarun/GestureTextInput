package com.alanb.gesturetextinput;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.alanb.gesturecommon.SettingItem;

import java.util.ArrayList;

public class SettingListAdapter extends BaseAdapter
{
    private ArrayList<SettingItem> m_items = new ArrayList<>();

    public SettingListAdapter()
    {
    }

    @Override
    public int getCount()
    {
        return m_items.size();
    }

    @Override
    public Object getItem(int i)
    {
        return m_items.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        final Context context = viewGroup.getContext();

        if (view == null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.setting_item, viewGroup, false);
        }

        SettingItem item = m_items.get(i);

        TextView txt_name = (TextView) view.findViewById(R.id.s_item_name);
        txt_name.setText(item.getTitle());

        TextView txt_value = (TextView) view.findViewById(R.id.s_item_value);
        txt_value.setText(item.getValue());

        return view;
    }

    public void addItem(String title, String val)
    {
        m_items.add(new SettingItem(title, val));
    }
}
