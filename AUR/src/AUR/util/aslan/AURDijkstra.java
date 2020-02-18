package AUR.util.aslan;

import java.util.ArrayList;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURDijkstra {

        private int V;
        private int src;
        public final int[] NULL_RESULT = new int[0];
        public int[] result = this.NULL_RESULT;
        public int[] parent = this.NULL_RESULT;

        private int minDistance(int dist[], Boolean sptSet[]) {
                int min = Integer.MAX_VALUE, min_index = -1;

                for (int v = 0; v < V; v++) {
                        if (sptSet[v] == false && dist[v] <= min) {
                                min = dist[v];
                                min_index = v;
                        }
                }

                return min_index;
        }
        
        public int getDistanceTo(int vertexNumber){
                return this.result[vertexNumber];
        }
        
        public ArrayList<Integer> getPathTo(int vertexNumber){
                ArrayList<Integer> pathTmp = new ArrayList<>();
                ArrayList<Integer> path = new ArrayList<>();
                do{
                        pathTmp.add(vertexNumber);
                        vertexNumber = parent[vertexNumber];
                }while(vertexNumber != src);
                pathTmp.add(vertexNumber);
                
                for(int i = pathTmp.size() - 1;i >= 0;i --)
                        path.add(pathTmp.get(i));
                
                return path;
        }

        public int[] dijkstra(int graph[][], int src, int V) {
                this.V = V;
                this.src = src;
                
                int dist[] = new int[V];
                int parent[] = new int[V]; 
                Boolean sptSet[] = new Boolean[V];

                for (int i = 0; i < V; i++) {
                        dist[i] = Integer.MAX_VALUE;
                        sptSet[i] = false;
                }

                dist[src] = 0;

                for (int count = 0; count < V - 1; count++) {
                        int u = minDistance(dist, sptSet);

                        sptSet[u] = true;

                        for (int v = 0; v < V; v++) 
                        {
                                if (!sptSet[v] && graph[u][v] != 0
                                        && dist[u] != Integer.MAX_VALUE
                                        && dist[u] + graph[u][v] < dist[v]) {
                                        dist[v] = dist[u] + graph[u][v];
                                        parent[v] = u;
                                }
                        }
                }
                
                this.parent = parent;
                this.result = dist;
                
                return dist;
        }
}
