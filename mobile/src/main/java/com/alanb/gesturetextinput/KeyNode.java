package com.alanb.gesturetextinput;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class KeyNode
{
    public enum Act
    {
        OTHER, DELETE, DONE
    }
    private String showStr;
    private KeyNode[] nextNode;
    private Character charVal;
    private KeyNode parentNode;
    private Act act;

    public KeyNode(String str, KeyNode[] node, KeyNode parent)
    {
        this(str, node, parent, null, Act.OTHER);
    }

    public KeyNode(String str, KeyNode[] node, KeyNode parent, Character cval)
    {
        this(str, node, parent, cval, Act.OTHER);
    }

    public KeyNode(String str, KeyNode[] node, KeyNode parent, Act act)
    {
        this(str, node, parent, null, act);
    }

    public KeyNode(String str, KeyNode[] node, KeyNode parent, Character cval, Act act)
    {
        this.showStr = str;
        this.nextNode = node;
        this.parentNode = parent;
        this.charVal = cval;
        if (act == null)
            this.act = Act.OTHER;
        else
            this.act = act;
    }

    public int getNextNodeNum()
    {
        if (this.nextNode == null)
            return 0;
        return this.nextNode.length;
    }

    public KeyNode getNextNode(int idx)
    {
        if (this.nextNode != null && idx >= 0 && idx < this.nextNode.length)
            return this.nextNode[idx];
        return null;
    }

    public boolean isLeaf()
    {
        return this.nextNode == null;
    }

    public Act getAct() { return this.act; }
    public String getShowStr() { return this.showStr; }
    public Character getCharVal()
    {
        return this.charVal;
    }
    public KeyNode getParent() { return this.parentNode; }

    private static String readJSONFile(Context context, int rid)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rid)));
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
            String n_show_str = null;
            if (!jobj.isNull("show_str"))
                n_show_str = jobj.getString("show_str");
            Character n_input_char = null;
            if (!jobj.isNull("input_char"))
                n_input_char = jobj.getString("input_char").charAt(0);
            KeyNode[] narray = null;
            if (!jobj.isNull("keys"))
            {
                JSONArray jarray = jobj.getJSONArray("keys");
                narray = new KeyNode[jarray.length()];
                for (int ci=0; ci<jarray.length(); ci++)
                {
                    narray[ci] = keyFromJSON(jarray.getJSONObject(ci));
                }
            }
            Act act = null;
            if (!jobj.isNull("act"))
            {
                if (jobj.getString("act").equals("delete"))
                {
                    act = Act.DELETE;
                }
                else if (jobj.getString("act").equals("done"))
                {
                    act = Act.DONE;
                }
            }

            node = new KeyNode(n_show_str, narray, null, n_input_char, act);
            if (narray != null)
            {
                for (int ci=0; ci<narray.length; ci++)
                {
                    narray[ci].parentNode = node;
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return node;
    }

    public static KeyNode generateKeyTree(Context context, int rid)
    {
        KeyNode rootNode = null;
        try
        {
            rootNode = keyFromJSON(new JSONObject(readJSONFile(context, rid)));
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        return rootNode;
    }
}
