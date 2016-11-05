package com.alanb.gesturetextinput;

public class SettingItem
{
    private String m_title;
    private String m_value;

    public SettingItem(String title, String value)
    {
        this.m_title = title;
        this.m_value = value;
    }

    public void setTitle(String s)
    {
        this.m_title = s;
    }
    public String getTitle()
    {
        return this.m_title;
    }

    public void setValue(String s)
    {
        this.m_value = s;
    }
    public String getValue()
    {
        return this.m_value;
    }
}
