package mrl_2019.algorithm.clustering;

import java.util.ArrayList;
import java.util.Random;

/**
 * @author Selcuk Orhan DEMIREL
 */
public class Kmeans_Modified{

    private Double[][] data;         // data to cluster
    private int numClusters;    // number of clusters
    private Double[][] clusterCenters;   // cluster centers
    private int dataSize;               // size of the data
    private int dataDim;                // dimension of the data
    private ArrayList<Double[]>[] clusters;     // calculated clusters
    private Double[] clusterVars;        // cluster variances
    private int tryCount;

    private double epsilon;

    @SuppressWarnings({"unused", "unchecked"})
    public Kmeans_Modified(Double[][] data, int numClusters, Double[][] clusterCenters) {
        dataSize = data.length;
        dataDim = data[0].length;

        this.data = data;

        this.numClusters = numClusters;

        this.clusterCenters = clusterCenters;

        clusters = new ArrayList[numClusters];
        for (int i = 0; i < numClusters; i++) {
            clusters[i] = new ArrayList<Double[]>();
        }
        clusterVars = new Double[numClusters];

        epsilon = 0.01;
    }

    public Kmeans_Modified(Double[][] data, int numClusters, int tryCount) {
        this(data, numClusters, true, tryCount);
    }

    @SuppressWarnings("unchecked")
    public Kmeans_Modified(Double[][] data, int numClusters, boolean randomizeCenters, int tryCount) {
        this.tryCount = tryCount;
        dataSize = data.length;
        dataDim = data[0].length;

        this.data = data;

        this.numClusters = numClusters;

        this.clusterCenters = new Double[numClusters][dataDim];

        clusters = new ArrayList[numClusters];
        for (int i = 0; i < numClusters; i++) {
            clusters[i] = new ArrayList<Double[]>();
        }
        clusterVars = new Double[numClusters];

        epsilon = 0.01;

        if (randomizeCenters) {
            randomizeCenters(numClusters, data);
        }
    }

    private void randomizeCenters(int numClusters, Double[][] data) {
        Random r = new Random(numClusters);
        int[] check = new int[numClusters];
        for (int i = 0; i < numClusters; i++) {
            int rand = r.nextInt(dataSize);
            if (check[i] == 0) {
                this.clusterCenters[i] = data[rand].clone();
                check[i] = 1;
            } else {
                i--;
            }
        }
    }

    private void calculateClusterCenters() {
        for (int i = 0; i < numClusters; i++) {
            int clusterSize = clusters[i].size();

            for (int k = 0; k < dataDim; k++) {

                double sum = 0d;
                for (int j = 0; j < clusterSize; j++) {
                    Double[] elem = clusters[i].get(j);
                    sum += elem[k];
                }

                clusterCenters[i][k] = sum / clusterSize;
            }
        }
    }

    private void calculateClusterVars() {
        for (int i = 0; i < numClusters; i++) {
            int clusterSize = clusters[i].size();
            Double sum = 0d;

            for (int j = 0; j < clusterSize; j++) {

                Double[] elem = clusters[i].get(j);

                for (int k = 0; k < dataDim; k++) {
                    sum += Math.pow(elem[k] - getClusterCenters()[i][k], 2);
                }
            }

            if (clusterSize == 0) {
                clusterVars[i] = Double.MAX_VALUE;
            } else {
                clusterVars[i] = sum / clusterSize;
            }
        }
    }

    public Double getTotalVar() {
        double total = 0d;
        for (int i = 0; i < numClusters; i++) {
            total += clusterVars[i];
        }

        return total;
    }

    @SuppressWarnings("unused")
    public Double[] getClusterVars() {
        return clusterVars;
    }

    public ArrayList<Double[]>[] getClusters() {
        return clusters;
    }

    private void assignData(boolean justDistance) {
        if (numClusters <= 0) {
            return;
        }
        for (int k = 0; k < numClusters; k++) {
            clusters[k].clear();
        }

        int clusterIndex = 0;
        double distance = Double.MAX_VALUE;
        double newDistance;


        double tempDistance;
        for (int i = 0; i < dataSize; i++) {

            clusterIndex = 0;
            distance = Double.MAX_VALUE;

            for (int j = 0; j < numClusters; j++) {
//                newDistance = distanceFromCenter(data[i], j) / data[i][2] ;//distance over area
//                newDistance = distanceFromCenter(data[i], j) ;//distance over area
//                newDistance = distanceFromCenter(data[i], j)/data[i][2] ;//distance over area
                if (justDistance) {
                    newDistance = distanceFromCenter(data[i], j);//distance over area
                } else {
                    tempDistance = distanceFromCenter(data[i], j);
                    newDistance = tempDistance * (0.22 * clusters[j].size() + 1);//distance over area
                }
                if (newDistance <= distance) {
                    clusterIndex = j;
                    distance = newDistance;
                }
            }

            clusters[clusterIndex].add(data[i]);
        }

    }

    private double distanceFromCenter(Double[] datum, int j) {
        double sum = 0d;
        for (int i = 0; i < dataDim; i++) {
            sum += Math.pow((datum[i] - getClusterCenters()[j][i]), 2);
        }

        return Math.sqrt(sum);
    }

    public void calculateClusters() {

        double var1 = Double.MAX_VALUE;
        double var2;
        double delta;

        boolean justDistance = false;

        do {
            if (tryCount > 0) {
                calculateClusterCenters();
            }
            assignData(justDistance);
            calculateClusterVars();
            var2 = getTotalVar();
            if (Double.isNaN(var2))    // if this happens, there must be some empty clusters
            {
                delta = Double.MAX_VALUE;
                randomizeCenters(numClusters, data);
                assignData(justDistance);
                calculateClusterCenters();
                calculateClusterVars();
            } else {
                delta = Math.abs(var1 - var2);
                var1 = var2;
            }
            tryCount--;

        } while (delta > epsilon);

        assignData(true);
    }

    public void setEpsilon(Double epsilon) {
        this.epsilon = epsilon;
    }

    @SuppressWarnings("unused")
    public void setEpsilon(double epsilon) {
        if (epsilon > 0) {
            this.epsilon = epsilon;
        }
    }

    /**
     * @return the clusterCenters
     */
    public Double[][] getClusterCenters() {
        return clusterCenters;
    }
}