package AIT_2019.module.algorithm;

import rescuecore2.standard.entities.*;
import java.util.*;

public class KmeansPP {
    private StandardEntity[] targets;
    private Cluster[] result;

    private int num;
    private int rep;

    private static final int COMMON_SEED = 123456789;

    public KmeansPP(StandardEntity[] targets, int num, int rep) {
        this.targets = targets;
        this.num = num;
        this.rep = rep;
    }

    public KmeansPP(int num, double[] cxs, double[] cys, List<List<StandardEntity>> memberz) {
        this.targets = null;
        this.result = new Cluster[num];
        this.num = num;
        this.rep = 0;

        for (int i=0; i<num; ++i) {
            double cx = cxs[i];
            double cy = cys[i];
            List<StandardEntity> members = memberz.get(i);
            this.result[i] = new Cluster(cx, cy, members);
        }
    }

    public void execute() {
        //  # step 1:
        this.result = init(this.targets, this.num);

        //  # step 2:
        for (int i=0; i<this.rep; ++i) {
            Arrays.stream(this.result).forEach(Cluster::clearMembers);
            for (StandardEntity e : this.targets) assign(this.result, e);
            Arrays.stream(this.result).forEach(Cluster::updateCenter);
        }
    }

    public int getClusterNumber() {
        return this.result.length;
    }

    public double getClusterX(int i) {
        return this.result[i].getCX();
    }

    public double getClusterY(int i) {
        return this.result[i].getCY();
    }

    public StandardEntity[] getClusterMembers(int i) {
        return this.result[i].getMembers();
    }

    private static Cluster[] init(StandardEntity[] targets, int num) {
        Cluster[] ret = new Cluster[num];
        for (int i=0; i<num; ++i) ret[i] = new Cluster();

        Random random = new Random(COMMON_SEED);

        int rand = random.nextInt(targets.length);
        ret[0].addMember(targets[rand]);
        ret[0].updateCenter();

        boolean[] assigned = new boolean[targets.length];
        assigned[rand] = true;

        for (int i=0; i<num-1; ++i) {
            double[] ds = new double[targets.length];
            double sumd = 0.0;
            for (int j=0; j<targets.length; ++j) {
                if (assigned[j]) continue;

                double cx = ret[i].getCX();
                double cy = ret[i].getCY();
                double tx = Cluster.getEntityX(targets[j]);
                double ty = Cluster.getEntityY(targets[j]);
                ds[j] = Math.hypot(tx-cx, ty-cy);
                sumd += ds[j];
            }

            for(int j=0; j<targets.length; ++j) ds[j] /= sumd;

            double p = random.nextDouble();
            double cp = 0.0;

            for(int j=0; j<targets.length; ++j) {
                if (assigned[j]) continue;

                cp += ds[j];
                if (p <= cp) {
                    ret[i+1].addMember(targets[j]);
                    ret[i+1].updateCenter();
                    assigned[j] = true;
                    break;
                }
            }
        }

        return ret;
    }

    private static void assign(Cluster[] clusters, StandardEntity e) {
        double x = Cluster.getEntityX(e);
        double y = Cluster.getEntityY(e);
        Optional<Cluster> cluster = Arrays.stream(clusters)
            .min((c1, c2) -> {
                double cx1 = c1.getCX();
                double cy1 = c1.getCY();
                double cx2 = c2.getCX();
                double cy2 = c2.getCY();

                double d1 = Math.hypot(cx1-x, cy1-y);
                double d2 = Math.hypot(cx2-x, cy2-y);

                return Double.compare(d1, d2);
            });

        cluster.get().addMember(e);
    }

    private static class Cluster {
        private double cx;
        private double cy;

        private List<StandardEntity> members;

        public Cluster() {
            this.members = new LinkedList<>();
            this.cx = 0.0;
            this.cy = 0.0;
        }

        public Cluster(double cx, double cy, List<StandardEntity> members) {
            this.cx = cx;
            this.cy = cy;
            this.members = members;
        }

        public void addMember(StandardEntity e) {
            this.members.add(e);
        }

        public StandardEntity[] getMembers() {
            return this.members.toArray(new StandardEntity[0]);
        }

        public void clearMembers() {
            this.members.clear();
        }

        public void updateCenter() {
            if (this.members.isEmpty()) return;

            double sumx = 0;
            double sumy = 0;

            for (StandardEntity e : this.members) {
                sumx += Cluster.getEntityX(e);
                sumy += Cluster.getEntityY(e);
            }

            this.cx = sumx / this.members.size();
            this.cy = sumy / this.members.size();
        }

        public double getCX() {
            return this.cx;
        }

        public double getCY() {
            return this.cy;
        }

        public static double getEntityX(StandardEntity e) {
            int ret = -1;
            if (e instanceof Human) {
                Human h = (Human)e;
                if (h.isXDefined()) ret = h.getX();
            }
            else
            if (e instanceof Area) {
                Area a = (Area)e;
                if (a.isXDefined()) ret = a.getX();
            }
            else
            if (e instanceof Blockade) {
                Blockade b = (Blockade)e;
                if (b.isXDefined()) ret = b.getX();
            }
            return (double)ret;
        }

        public static double getEntityY(StandardEntity e) {
            int ret = -1;
            if (e instanceof Human) {
                Human h = (Human)e;
                if (h.isYDefined()) ret = h.getY();
            }
            else
            if (e instanceof Area) {
                Area a = (Area)e;
                if (a.isYDefined()) ret = a.getY();
            }
            else
            if (e instanceof Blockade) {
                Blockade b = (Blockade)e;
                if (b.isYDefined()) ret = b.getY();
            }
            return (double)ret;
        }
    }
}
