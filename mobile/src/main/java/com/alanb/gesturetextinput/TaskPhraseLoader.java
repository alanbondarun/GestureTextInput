package com.alanb.gesturetextinput;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class TaskPhraseLoader
{
    private ArrayList<String> m_phrases;
    private int m_pos = 0;

    public TaskPhraseLoader(Context context)
    {
        m_phrases = new ArrayList<>();
        BufferedReader phrase_rdr = new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(R.raw.phrases2)));

        try
        {
            String line = phrase_rdr.readLine();
            while (line != null)
            {
                m_phrases.add(line.toLowerCase());
                line = phrase_rdr.readLine();
            }
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }

        Collections.shuffle(m_phrases);
    }

    public String next()
    {
        String phrase = m_phrases.get(m_pos);
        m_pos = (m_pos + 1) % m_phrases.size();
        return phrase;
    }
}
