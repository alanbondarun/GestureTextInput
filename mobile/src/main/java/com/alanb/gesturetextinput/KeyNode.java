package com.alanb.gesturetextinput;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;

public class KeyNode
{
    public enum Act
    {
        INPUT_CHAR, DEL, PARENT
    }
    private String[] showStr;
    private KeyNode[] nextNode;
    private Act act;
    private char charVal;

    public KeyNode(String[] str, KeyNode[] node, Act act)
    {
        this(str, node, act, '0');
    }

    public KeyNode(String[] str, KeyNode[] node, Act act, char cval)
    {
        this.showStr = str;
        this.nextNode = node;
        this.act = act;
        this.charVal = cval;
    }

    public String getShowStr(int idx)
    {
        if (idx >= 0 && idx < this.showStr.length)
            return this.showStr[idx];
        return null;
    }

    public KeyNode getNextNode(int idx)
    {
        if (idx >= 0 && idx < this.nextNode.length)
            return this.nextNode[idx];
        return null;
    }

    public char getCharVal()
    {
        return this.charVal;
    }

    public Act getAct()
    {
        return this.act;
    }

    private static String readJSONFile(Context context)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.key_value)));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                sb.append(line);
            }
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static KeyNode keyFromJSON(JSONObject jobj)
    {
        KeyNode node = null;
        try
        {
            if (!jobj.isNull("keys"))
            {
                JSONArray jarray = jobj.getJSONArray("keys");
                String[] a_show_str = new String[jarray.length()];
                KeyNode[] a_next_node = new KeyNode[jarray.length()];
                for (int ci=0; ci<jarray.length(); ci++)
                {
                    JSONObject sub_obj = jarray.getJSONObject(ci);

                    if (sub_obj.isNull("show_str"))
                        a_show_str[ci] = "";
                    else
                        a_show_str[ci] = sub_obj.getString("show_str");

                    if (sub_obj.isNull("input_char"))
                    {
                        a_next_node[ci] = keyFromJSON(sub_obj);
                    }
                    else
                    {
                        String input_char = sub_obj.getString("input_char");
                        a_next_node[ci] = new KeyNode(new String[4], new KeyNode[4], Act.INPUT_CHAR,
                                input_char.charAt(0));
                    }
                }
                node = new KeyNode(a_show_str, a_next_node, Act.PARENT);
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return node;
    }

    public static KeyNode generateKeyTree(Context context)
    {
        KeyNode rootNode = null;
        try
        {
            rootNode = keyFromJSON(new JSONObject(readJSONFile(context)));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return rootNode;
    }
}
