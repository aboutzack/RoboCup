package mrl_2019.complex.firebrigade;

import adf.agent.info.AgentInfo;
import rescuecore.objects.Building;

/**
 * @author ShivaZarei
 * 1/06/2019
 */

public class GreyPrediction {

    private int j=0;
    private double temperature;
    protected AgentInfo agentInfo;
    private int lastUpdateTime;
    private double t1=0;
    private double t;
    private double matrixTemperatue[][];
    private double u;
    private double matrixHulfTemperature[][];
    private double C[][];
    private double B[][];
    private double value;
    private double a;
    private double b;
    private double v;
    private double quantity;




    public double calc(int p){

        lastUpdateTime = getTime();
        Building building = new Building();

        for (int i = lastUpdateTime; i <=lastUpdateTime ; i--){

            if (lastUpdateTime==getTime()) {
                temperature=building.getTemperature();
                t = t1 + temperature;
                matrixTemperatue[j][1]=t;
                u = 0.5*t + 0.5*t1;
                matrixHulfTemperature[j][1]= -1*u;
                j++;
            }

            t1 = t;

        }


        int m = matrixHulfTemperature.length;
        int n = matrixHulfTemperature[0].length;
        int x=C.length;
        int y=C[0].length;
        int z=matrixTemperatue.length;
        int s=matrixTemperatue[0].length;
        double [][] C = new double[n][m];
        double [][] B = new double[m][y];
        double [][] Z = new double[j][j];

        for (int k=0; k < m ; k++ ){
            for (int l = 0 ; l < n ; l++){
                C[l][k]=matrixHulfTemperature[k][l]; // Transpose matrixHulfTemperature
            }
        }



        for (int i=0; i < m ;i++){
            for (int j=0 ; j< y ;j++){
                for (int k=0; k<n ; k++){

                    B[i][j] += matrixHulfTemperature[i][k]*C[k][j]; //multiple of matrixHulfTemperature & C
                }
            }
        }

        for (int i=0; i< z; i++){
            for (int j=0; j< y; j++){
                for (int k=0; k<s ; k++){
                    Z[i][j] += matrixTemperatue[i][k] * C[k][j];
                }
            }
        }


        a=Z[0][0];
        b=Z[1][0];

        v= ((-1*b)/a);
        value= (v * (Math.exp(a*(j+p-1)) - Math.exp(a* (j+p-2))));


        return value;


    }



//    public static double transpose(double A[][]){
//
//
//        int m = A.length;
//        int n = A[0].length;
//        double [][] C = new double[n][m];
//
//        for (int k=0; k < m ; k++ ){
//            for (int l = 0 ; l < n ; l++){
//                C[l][k]=A[k][l];
//            }
//        }
//    }


    public int getTime() {
        return agentInfo.getTime();
    }


}
