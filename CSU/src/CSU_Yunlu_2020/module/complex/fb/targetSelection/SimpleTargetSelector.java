package CSU_Yunlu_2020.module.complex.fb.targetSelection;

import CSU_Yunlu_2020.module.algorithm.fb.Cluster;
import CSU_Yunlu_2020.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2020.world.object.CSUBuilding;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

@Deprecated
public class SimpleTargetSelector extends TargetSelector{
    private AgentInfo agentInfo;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private Clustering clustering;
    private List<Building> buildingsList = new ArrayList<>();
    private List<Building> convexBuildings;

    public SimpleTargetSelector(CSUFireBrigadeWorld world, Clustering clustering) {
        super(world);
        this.agentInfo = world.getAgentInfo();
        this.worldInfo = world.getWorldInfo();
        this.scenarioInfo = world.getScenarioInfo();
        this.clustering = clustering;
    }

    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {
        EntityID entityID;
        entityID = this.calcTargetInCluster();
        if (entityID == null) {
            entityID = this.calcTargetInWorld();
        }
        if (entityID != null) {
            CSUBuilding csuBuilding = new CSUBuilding(worldInfo.getEntity(entityID), null);
            return new FireBrigadeTarget(null, csuBuilding);
        } else {
            return null;
        }
    }

    public EntityID calc() {
        EntityID result;
        result = this.calcTargetInCluster();
        if (result == null) {
            result = this.calcTargetInWorld();
        }
        return result;
    }

    private EntityID calcTargetInCluster()
    {
        int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
        if (clusterIndex == -1) {
            return null;
        }
        Collection<StandardEntity> elements = this.clustering.getClusterEntities(clusterIndex);
        if (elements == null || elements.isEmpty())
        {
            return null;
        }
        StandardEntity me = this.agentInfo.me();
        List<StandardEntity> agents = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
        List<Building> fireBuildings = new ArrayList<Building>();
        for (StandardEntity entity : elements)
        {
            if (entity instanceof Building && ((Building) entity).isOnFire() && ((Building) entity).getFieryness() != 8)
            {
                fireBuildings.add((Building)entity);
            }
        }

        convexBuildings = GetConvexHull(fireBuildings);
        Random random = new Random();
        if(convexBuildings.size() != 0) {
            Building entity = convexBuildings.get(Math.abs(random.nextInt()) % convexBuildings.size());
            if (agents.isEmpty())
            {
                return null;
            }
            else if (agents.size() == 1)
            {
                if (agents.get(0).getID().getValue() == me.getID().getValue())
                {
                    return entity.getID();
                }
                return null;
            }
            agents.sort(new DistanceSorter(this.worldInfo, entity));
            StandardEntity a0 = agents.get(0);
            StandardEntity a1 = agents.get(1);

            if (me.getID().getValue() == a0.getID().getValue() || me.getID().getValue() == a1.getID().getValue())
            {
                return entity.getID();
            }
            else
            {
                agents.remove(a0);
                agents.remove(a1);
            }
        }
        return null;
    }

    private EntityID calcTargetInWorld()
    {
        Collection<StandardEntity> entities = this.worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.GAS_STATION,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE
        );
        // TODO: 3/1/20 根据着火建筑的聚类和凸包,更精确的选择目标
        ArrayList<StandardEntity> fireBuildings = new ArrayList<>();
        for (StandardEntity entity : entities) {
            if (((Building) entity).isOnFire()) {
                fireBuildings.add(entity);
            }
        }
        fireBuildings.sort(new DistanceSorter(worldInfo, agentInfo.me()));
        return fireBuildings.isEmpty() ? null : fireBuildings.get(0).getID();
    }

    @SuppressWarnings("unused")
    private double getAngle(Vector2D v1, Vector2D v2)
    {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if (flag > 0)
        {
            return angle;
        }
        if (flag < 0)
        {
            return -1 * angle;
        }
        return 0.0D;
    }

    /*
     * 计算凸包
     */
    public List<Building> GetConvexHull (List<Building> fireBuildings)
    {
        List<Building> resultBuildings = new ArrayList<Building>();
        //数量过少，无需计算凸包
        if (fireBuildings.size() <= 3) {
            for(Building building : fireBuildings) {
                resultBuildings.add(building);
            }
            return resultBuildings;
        }

        if (!fireBuildings.isEmpty()) {
            //获取极点
            Building extremePoint1 = getLowestBuilding(fireBuildings);
            //获取极点连着的一条极边
            Building extremePoint2 = getAnotherBuilding(extremePoint1, fireBuildings);

            Collections.sort(buildingsList,new Comparator<Building>() {
                public int compare(Building a,Building b)
                {
                    //按夹角的余弦值从小到大排序
                    double judge =  getCosAngel(a,extremePoint1,extremePoint2) - getCosAngel(b,extremePoint1,extremePoint2);
                    if (judge > 0.000001) {
                        return 1;
                    } else if (judge < -0.000001) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });

            Stack<Building> extremePoint = new Stack<Building>();
            Stack<Building> temp = new Stack<Building>();
            extremePoint.push(extremePoint1);
            extremePoint.push(extremePoint2);
            for (int i = 0;i < buildingsList.size();i++) {
                temp.push(buildingsList.get(i));
            }
            while (!temp.empty()) {
                Building a = extremePoint.pop();
                Building b = extremePoint.peek();
                extremePoint.push(a);
                if (toLeft(b,a,temp.peek())){
                    extremePoint.push(temp.peek());
                    temp.pop();
                } else {
                    if(extremePoint.size() > 2)
                        extremePoint.pop();
                    if(extremePoint.size() == 2) {
                        extremePoint.push(temp.peek());
                        temp.pop();
                    }
                }
            }

            while(!extremePoint.isEmpty()) {
                resultBuildings.add(extremePoint.pop());
            }

            return resultBuildings;
        }else {
            return null;
        }
    }

    /*
     * 判断点pp是否在边(ea,eb)左边
     */
    public boolean toLeft (Building ea,Building eb,Building pp)
    {
        return ((eb.getX() - ea.getX()) * (pp.getY() - ea.getY()) - (eb.getY() - ea.getY()) * (pp.getX() - ea.getX())) > 0;
    }

    /**
     * 获取y值最小,最下面最左边的建筑
     * @param fireBuildings
     * @return
     */
    public Building getLowestBuilding(List<Building> fireBuildings) {

        Building res = null;
        for(Building building : fireBuildings) {
            if(res == null || (res.getY() > building.getY()) || (res.getY() == building.getY() && res.getX() > building.getX())) {
                res = building;
            }
        }
        return res;
    }

    /**
     * 求角abc的余弦角度
     */
    public double getCosAngel(Building a,Building b,Building c)
    {
        if (a == null || b == null || c == null) return 0.0D;
        double flag = (a.getX()-b.getX()) * (c.getY()-b.getY()) - (a.getY()-b.getY()) * (c.getX()-b.getX());
        double ba = Math.sqrt((double)((a.getX()-b.getX())*(a.getX()-b.getX()) + (a.getY()-b.getY())*(a.getY()-b.getY())));
        double bc = Math.sqrt((double)((c.getX()-b.getX())*(c.getX()-b.getX()) + (c.getY()-b.getY())*(c.getY()-b.getY())));
        double cosB = (a.getX()-b.getX())*(c.getX()-b.getX()) + (a.getY()-b.getY())*(c.getY()-b.getY());
        cosB /= ba * bc;

        if (flag > 0.000001) {
            return Math.acos(cosB);
        }
        if(flag < -0.000001) {
            return (Math.acos(cosB) + Math.PI);
        }

        return 0.0D;
    }

    /**
     * 求余弦值
     */
    public double getCos(Building a,Building b)
    {
        double ba = Math.sqrt((double)((a.getX()-b.getX())*(a.getX()-b.getX()) + (a.getY()-b.getY())*(a.getY()-b.getY())));
        double bc = 1;
        double cosB = (a.getY()-b.getY())*(-1);
        cosB /= ba * bc;

        return cosB;
    }

    /**
     * 根据一个极点找到另一个极点构成极边
     * @param a
     * @param fireBuildings
     * @return
     */
    public Building getAnotherBuilding(Building a, List<Building> fireBuildings) {
        boolean flag = false;
        Building resultBuilding = null;
        for (Building fireBuilding : fireBuildings) {
            if (a != fireBuilding && fireBuilding.getX() > a.getX()) {
                if(!flag) {
                    resultBuilding = fireBuilding;
                    flag = true;
                    continue;
                }
                if(getCos(a, fireBuilding) > getCos(a, resultBuilding)) {
                    resultBuilding = fireBuilding;
                }
            }
            if (fireBuilding != a)
                buildingsList.add(fireBuilding);
        }

        buildingsList.remove(resultBuilding);

        return resultBuilding;
    }

    private class DistanceSorter implements Comparator<StandardEntity>
    {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference)
        {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b)
        {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }
}
