package com.alanb.gesturetextinput;

import java.util.ArrayList;
import java.util.Collections;

public class EditDistCalculator
{
    public enum EditCommand
    {
        CORRECT, INSERT, DELETE, MODIFY
    }

    public static class EditInfo
    {
        public int num_correct;
        public int num_insert;
        public int num_delete;
        public int num_modify;
        public EditInfo(int c, int i, int d, int m)
        {
            this.num_correct = c;
            this.num_insert = i;
            this.num_delete = d;
            this.num_modify = m;
        }
    }

    public static EditInfo calc(String target_txt, String written_txt)
    {
        int[][] min_dist = new int[target_txt.length()+1][written_txt.length()+1];
        EditCommand[][] min_edit_cmd = new EditCommand[target_txt.length()+1][written_txt.length()+1];

        for (int ci=0; ci<=target_txt.length(); ci++)
        {
            min_dist[ci][0] = ci;
        }
        for (int cj=0; cj<=written_txt.length(); cj++)
        {
            min_dist[0][cj] = cj;
        }

        for (int ci=1; ci<=target_txt.length(); ci++)
        {
            for (int cj=1; cj<=written_txt.length(); cj++)
            {
                if (target_txt.charAt(ci-1) == written_txt.charAt(cj-1))
                {
                    min_dist[ci][cj] = min_dist[ci-1][cj-1];
                    min_edit_cmd[ci][cj] = EditCommand.CORRECT;
                }
                else
                {
                    ArrayList<Integer> choice = new ArrayList<>();
                    choice.add(min_dist[ci-1][cj] + 1);
                    choice.add(min_dist[ci][cj-1] + 1);
                    choice.add(min_dist[ci-1][cj-1] + 1);

                    switch (choice.indexOf(Collections.min(choice)))
                    {
                        case 0:
                            min_dist[ci][cj] = choice.get(0);
                            min_edit_cmd[ci][cj] = EditCommand.INSERT;
                            break;
                        case 1:
                            min_dist[ci][cj] = choice.get(1);
                            min_edit_cmd[ci][cj] = EditCommand.DELETE;
                            break;
                        case 2:
                            min_dist[ci][cj] = choice.get(2);
                            min_edit_cmd[ci][cj] = EditCommand.MODIFY;
                            break;
                    }
                }
            }
        }

        EditInfo info = new EditInfo(0, 0, 0, 0);
        int pos_t = target_txt.length();
        int pos_w = written_txt.length();
        while (pos_t > 0 && pos_w > 0)
        {
            switch (min_edit_cmd[pos_t][pos_w])
            {
                case CORRECT:
                    info.num_correct++;
                    pos_t--; pos_w--;
                    break;
                case INSERT:
                    info.num_insert++;
                    pos_t--;
                    break;
                case DELETE:
                    info.num_delete++;
                    pos_w--;
                    break;
                case MODIFY:
                    info.num_modify++;
                    pos_t--; pos_w--;
                    break;
            }
        }
        if (pos_w > 0)
        {
            info.num_delete += pos_w;
        }
        if (pos_t > 0)
        {
            info.num_insert += pos_t;
        }

        return info;
    }
}
