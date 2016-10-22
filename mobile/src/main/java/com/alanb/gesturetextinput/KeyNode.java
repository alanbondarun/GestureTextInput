package com.alanb.gesturetextinput;

import android.content.Context;
import android.util.Log;

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
        return null;
    }

    public static KeyNode generateKeyTree(Context context)
    {
        try
        {
            JSONObject jobj = new JSONObject(readJSONFile(context));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
