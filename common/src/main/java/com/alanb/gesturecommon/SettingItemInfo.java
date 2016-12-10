package com.alanb.gesturecommon;

import android.content.Context;

import java.util.ArrayList;

public class SettingItemInfo
{
    public static class Builder
    {
        private String label;
        private int menu_label_id;
        private String pref_key;
        private int default_item;

        public Builder setLabel(String label) { this.label = label; return this; }
        public Builder setMenuLabelId(int id) { this.menu_label_id = id; return this; }
        public Builder setPrefKey(String key) { this.pref_key = key; return this; }
        public Builder setDefaultItem(int item) { this.default_item = item; return this; }
        public SettingItemInfo build(Context context) { return new SettingItemInfo(this, context); }
    }

    private Builder m_prefs;
    private String[] m_menu_labels;
    private SettingItemInfo(Builder bld, Context context)
    {
        m_prefs = bld;
        m_menu_labels = context.getResources().getStringArray(m_prefs.menu_label_id);
    }
    public String getLabel() { return m_prefs.label; }
    public int getMenuLabelId() { return m_prefs.menu_label_id; }
    public String getPrefKey() { return m_prefs.pref_key; }
    public int getDefaultItem() { return m_prefs.default_item; }
    public String[] getMenuLabels() { return m_menu_labels; }

    private static ArrayList<SettingItemInfo> mAllItems = null;

    public static ArrayList<SettingItemInfo> getAllItems(Context context)
    {
        if (mAllItems != null)
            return mAllItems;

        mAllItems = new ArrayList<SettingItemInfo>();
        mAllItems.add(new SettingItemInfo.Builder().setLabel(context.getResources().getString(R.string.watch_layout))
                .setMenuLabelId(R.array.watch_layout_kind)
                .setPrefKey(context.getString(R.string.prefkey_watch_layout))
                .setDefaultItem(context.getResources().getInteger(R.integer.pref_watch_layout_default))
                .build(context));
        mAllItems.add(new SettingItemInfo.Builder().setLabel(context.getResources().getString(R.string.oned_layout))
                .setMenuLabelId(R.array.oned_layout_kind)
                .setPrefKey(context.getString(R.string.prefkey_oned_layout))
                .setDefaultItem(context.getResources().getInteger(R.integer.pref_oned_layout_default))
                .build(context));
        mAllItems.add(new SettingItemInfo.Builder().setLabel(context.getResources().getString(R.string.pref_task_mode))
                .setMenuLabelId(R.array.pref_task_mode_item)
                .setPrefKey(context.getString(R.string.prefkey_task_mode))
                .setDefaultItem(context.getResources().getInteger(R.integer.pref_task_mode_default))
                .build(context));
        mAllItems.add(new SettingItemInfo.Builder().setLabel(context.getResources().getString(R.string.pref_multitouch_to_cancel))
                .setMenuLabelId(R.array.pref_multitouch_to_cancel_item)
                .setPrefKey(context.getString(R.string.prefkey_multitouch_to_cancel))
                .setDefaultItem(context.getResources().getInteger(R.integer.pref_multitouch_to_cancel_default))
                .build(context));
        return mAllItems;
    }
}