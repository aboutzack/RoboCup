package AIT_2019.module.algorithm;

import java.util.*;

public class Hungarian{
    public static boolean[][] execute(int[][] mat){
        int n = mat.length;
        int m = mat[0].length;
        boolean[][] ret = new boolean[n][m];
        int[] toRight  = new int[n];
        int[] toLeft   = new int[m];
        int[] ofsLeft  = new int[n];
        int[] ofsRight = new int[m];

        for (int i=0;i<n;++i) {
            toRight[i] = -1;
            ofsLeft[i] =  0;
        }
        for (int i=0;i<m;++i) {
            ofsRight[i] =  0;
            toLeft[i]   = -1;
        }
        for (int r=0;r<n;++r) {
            boolean[] left  = new boolean[n];
            boolean[] right = new boolean[m];
            int[] trace = new int[m];
            int[] ptr   = new int[m];
            left[r] = true;
            for (int i=0;i<n;++i) left[i] = false;
            for (int i=0;i<m;++i) {
                right[i] = false;
                trace[i] = -1;
                ptr[i]   =  r;
            }
            left[r] = true;
            for (;;) {
                int d = Integer.MAX_VALUE;
                for (int j=0;j<m;++j) if (!right[j]) d = Math.min(d, residue(mat, ofsLeft, ofsRight, ptr[j], j));
                for (int i=0;i<n;++i) if (left[i])   ofsLeft[i]  -= d;
                for (int j=0;j<m;++j) if (right[j])  ofsRight[j] += d;
                int b = -1;
                for (int j=0;j<m;++j) if (!right[j] && residue(mat, ofsLeft, ofsRight, ptr[j], j)==0) b = j;
                trace[b] = ptr[b];
                int c = toLeft[b];
                if (c < 0) {
                    while (b >= 0) {
                        int a = trace[b];
                        int z = toRight[a];
                        toLeft[b]  = a;
                        toRight[a] = b;
                        b = z;
                    }
                    break;
                }
                right[b] = left[c] = true;
                for (int j=0;j<m;++j) if(residue(mat, ofsLeft, ofsRight, c, j) < residue(mat, ofsLeft, ofsRight, ptr[j], j)) ptr[j] = c; 
            }
        }
        for (int i=0;i<n;++i) ret[i][toRight[i]] = true;
        return ret;
    }
    public static int residue(int[][] mat, int[] ofsL, int[] ofsR, int i, int j){
        return mat[i][j] + ofsL[i] + ofsR[j];
    }
}
