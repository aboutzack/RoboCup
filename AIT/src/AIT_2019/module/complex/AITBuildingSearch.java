package AIT_2019.module.complex;

import static rescuecore2.standard.entities.StandardEntityURN.AMBULANCE_TEAM;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_FORCE;

import java.awt.Shape;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.mrl.debugger.remote.VDClient;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.StandardMessagePriority;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import AIT_2019.module.algorithm.ConvexHull;
import AIT_2019.module.algorithm.StuckedHumans;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class AITBuildingSearch extends Search
{
    private PathPlanning pathPlanning;
    private Clustering clustering;
    private Clustering stuckedHumans;

    private int avoidTimeSendingReceived = -1;
    private int avoidTimeSendingSent = -1;
    private Map<EntityID, Integer> sentTimeMap = new HashMap<>();//"建筑id-上次发送这个建筑消息的时间"

    private Random random = new Random();

    //有正在处理的cluster聚类
    private boolean hasFocusedAssignedCluster = false;
    //存正在处理的聚类上的buildingid
    private List<EntityID> buildingIDsOfFocusedCluster = new ArrayList<>();
    //存已经处理的聚类的下标
    private List<Integer> indexOfEverFocusedClusters = new ArrayList<>();

    private EntityID targetID = null;

    private EntityID result = null;

    // Debug
    // private VDClient vdclient = VDClient.getInstance();
    // /Debug

    public AITBuildingSearch(AgentInfo ai, WorldInfo wi, ScenarioInfo si,
            ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.pathPlanning = moduleManager.getModule(
                "SampleSearch.PathPlaning.Fire",
                "adf.sample.module.algorithm.SamplePathPlanning");
        this.clustering = moduleManager.getModule(
                "SampleSearch.Clustering.Fire",
                "adf.sample.module.algorithm.SampleKMeans");
        this.stuckedHumans = moduleManager.getModule(
                "AITActionExtClear.StuckedHumans",
                "AIT_2019.module.algorithm.StuckedHumans");
        this.registerModule(this.pathPlanning);
        this.registerModule(this.clustering);
        this.registerModule(this.stuckedHumans);
        this.avoidTimeSendingReceived = 4;
        this.avoidTimeSendingSent = 3;
        this.random.setSeed(ai.getID().getValue());

        // Debug
        // this.vdclient.init("localhost", 1099);
        // /Debug
    }

    @Override
    public Search precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() > 1)
        {
            return this;
        }
        return this;
    }

    @Override
    public Search resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() > 1)
        {
            return this;
        }
        this.preparate();
        return this;
    }

    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public Search preparate()
    {
        super.preparate();
        if (this.getCountPreparate() > 1)
        {
            return this;
        }

        this.pathPlanning.preparate();
        this.clustering.preparate();
        this.stuckedHumans.preparate();
        return this;
    }

    @Override
    public Search updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() > 1)
        {
            return this;
        }

        this.pathPlanning.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);
        this.stuckedHumans.updateInfo(messageManager);
        this.sendChangedEntityInfo(messageManager);
        this.reflectOtherEntityInfo(messageManager);

        if (this.agentInfo.getTime() < 1)
        {
            return this;
        }
        if (this.isStuckedInBlockade())
        {
            messageManager.addMessage(new CommandPolice(
                    true, StandardMessagePriority.HIGH, null,
                    this.agentInfo.getPosition(), CommandPolice.ACTION_CLEAR));
            messageManager.addMessage(new CommandPolice(
                    false, StandardMessagePriority.HIGH, null,
                    this.agentInfo.getPosition(), CommandPolice.ACTION_CLEAR));
        }
        if(this.buildingIDsOfFocusedCluster.isEmpty())
        {
            this.setFocusedCluster();
        }

        List<EntityID> changedEntityIDs = this.worldInfo.getChanged().getChangedEntities().stream()
                .map(id -> this.worldInfo.getEntity(id))
                .filter(Building.class::isInstance)
                .filter(se -> !(se instanceof Refuge))
                .map(StandardEntity::getID)
                .collect(Collectors.toList());
        this.buildingIDsOfFocusedCluster.removeAll(changedEntityIDs);

        if(this.buildingIDsOfFocusedCluster.isEmpty())
        {
            this.setFocusedCluster();
        }

        // Debug
        // if (this.agentInfo.me().getStandardURN() != FIRE_BRIGADE) { return this; }
        // List<Shape> datas = new ArrayList<>();
        // for (EntityID id : this.buildingIDsOfFocusedCluster)
        // {
            // StandardEntity entity = this.worldInfo.getEntity(id);
            // if (!(entity instanceof Area)) { continue; }
            // Area area = (Area) entity;
            // datas.add(area.getShape());
        // }
        // this.vdclient.drawAsync(
            // this.agentInfo.getID().getValue(),
            // "SamplePolygon",
            // (Serializable) datas);
        // /Debug

        return this;
    }

    @Override
    public Search calc()
    {
        if (this.agentInfo.getTime() < 1)
        {
            this.clustering.calc();
            return this;
        }
        if (this.isStuckedInBlockade())
        {
            return this;
        }

        // Debug
        // if (this.result != null)
        // {
            // Area area = (Area) this.worldInfo.getEntity(this.result);
            // this.vdclient.drawAsync(
                    // this.agentInfo.getID().getValue(),
                    // "ClusterConvexhull",
                    // (Serializable) Arrays.asList(area.getShape()));
        // }
        // /Debug

        if (this.targetID != null
                && this.buildingIDsOfFocusedCluster.contains(this.targetID))
        {
            return this;
        }

        this.result = null;
        this.targetID = null;
        int size = this.buildingIDsOfFocusedCluster.size();
        int index = this.random.nextInt(size);
        this.targetID = this.buildingIDsOfFocusedCluster.get(index);
        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        this.pathPlanning.setDestination(targetID);
        List<EntityID> path = this.pathPlanning.calc().getResult();

        if (path != null && path.size() > 0)
        {
            StandardEntity entity = this.worldInfo.getEntity(path.get(path.size() - 1));
            if (entity instanceof Building) { path.remove(path.size() - 1); }
            this.result = path.get(path.size() - 1);
        }
        this.out("SEARCH #" +  this.result);
        return this;
    }

    //usage:updateInfo
    //选择一个新的聚类，并将其buildingid存进buildingIDsOfFocusedCluster
    private void setFocusedCluster()
    {
        int clusterIndex = -1;
        //这个clustering是SampleKMeans的一个实例，其存储了所有的聚类
        this.clustering.calc();
        //没有正在处理的聚类，处理当前agent所在的聚类
        if (!this.hasFocusedAssignedCluster)
        {
            //根据当前的agent取出其所在的聚类对应的下标
            clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
            //将其下标加入到"之前处理过的"队列中
            this.indexOfEverFocusedClusters.add(clusterIndex);
            //有正在处理的聚类
            this.hasFocusedAssignedCluster = true;
        }
        else
        {//有正在处理的类，再次进入这个方法时，他已经处理完了，所以随机取一个新的聚类
            //获得存在的聚类个数
            int clusterNumber = this.clustering.getClusterNumber();
            //如果"之前处理过的"队列的小等于总共聚类的个数，也就是说对聚类完成了一轮遍历，重新开始一轮新的遍历
            if (this.indexOfEverFocusedClusters.size() == clusterNumber) {
                this.indexOfEverFocusedClusters.clear();
                this.hasFocusedAssignedCluster = false;
            }
            //随机抽取一个新的聚类进行处理
            clusterIndex = this.random.nextInt(clusterNumber);
            while (this.indexOfEverFocusedClusters.contains(clusterIndex))
            {
                //随机抽取直到抽到没处理过的聚类
                clusterIndex = this.random.nextInt(clusterNumber);
            }
        }
        //获取对应聚类中的建筑id列表
        this.buildingIDsOfFocusedCluster = this.clustering.getClusterEntities(clusterIndex).stream()
                .filter(Building.class::isInstance)
                .filter(se -> !(se instanceof Refuge))
                .map(StandardEntity::getID)
                .collect(Collectors.toList());

        // Debug
        // if (this.agentInfo.me().getStandardURN() != FIRE_BRIGADE) { return; }
        // List<Shape> datas = new ArrayList<>();
        // for (StandardEntity entity : this.clustering.getClusterEntities(clusterIndex))
        // {
            // if (!(entity instanceof Area)) { continue; }
            // Area area = (Area) entity;
            // datas.add(area.getShape());
        // }
        // this.vdclient.drawAsync(
            // this.agentInfo.getID().getValue(),
            // "ClusterConvexhull",
            // (Serializable) datas);
        // /Debug
    }

    //usage:sendChangedEntityInfo
    //是否应该发送消息
    private Boolean checkShouldSend()
    {
        //发送消息的开关
        boolean shouldSendMessage = true;
        StandardEntity agentMe = this.agentInfo.me();
        Human me = (Human) agentMe;
        Collection<StandardEntity> agents = this.worldInfo.getEntitiesOfType(
                AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE);
        agents.remove(agentMe);
        for (StandardEntity agent : agents)
        {
            if (!shouldSendMessage) { break; }
            //agent不是human,跳过
            if (!(agent instanceof Human)) { continue; }
            Human other = (Human) agent;
            if (other.getPosition() != me.getPosition()) { continue; }
            //getID获取到的是EntityID，getValue才能获得int指
            //System.out.println("human的ID:"+other.getID().getValue() +"我的ID:"+ me.getID().getValue());
            if (other.getID().getValue() > me.getID().getValue())
            {
                //为什么id大于当前agentid就不发消息
                shouldSendMessage = false;
                //System.out.println("发送消息");
            }
        }
        return shouldSendMessage;
    }

    //usage:sendChangedEntityInfo
    //从两栋建筑中选择一栋情况更紧急的
    private Building selectPreferred(Building bld1, Building bld2)
    {
        if (bld1 == null && bld2 == null) { return null; }
        else if (bld1 != null && bld2 == null) { return bld1; }
        else if (bld1 == null && bld2 != null) { return bld2; }
        if (bld1.isOnFire() && bld2.isOnFire())
        {
            //燃烧情况（优先）
            if (bld1.isFierynessDefined() && bld2.isFierynessDefined())
            {
                return (bld1.getFieryness() > bld2.getFieryness()) ? bld1 : bld2;
            }
            //温度
            if (bld1.isTemperatureDefined() && bld2.isTemperatureDefined())
            {
                return (bld1.getTemperature() > bld2.getTemperature()) ? bld1 : bld2;
            }
        }
        else if (bld1.isOnFire() && !bld2.isOnFire())
        {
            return bld1;
        }

        return bld2;
    }

    //usage:updateInfo
    //从所有发生了改变的entity中选取building，从这些building中选择一个最紧急的建筑添加到MessageManager里
    private void sendChangedEntityInfo(MessageManager messageManager)
    {
        //System.out.println("频道:"+messageManager.getChannels().toString()+"个,"+"接受消息列表大小:"+messageManager.getReceivedMessageList().size()+",发送消息列表大小"+messageManager.getSendMessageList().size()+",时间："+this.agentInfo.getTime());
        if (!this.checkShouldSend()) { return; }

        Building building = null;
        //获取地图时间
        int currTime = this.agentInfo.getTime();
        Human me = (Human) this.agentInfo.me();
        //获取at，fb，pf所在地形
        List<EntityID> agentPositions = this.worldInfo.getEntitiesOfType(
                AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE).stream()
            .map(Human.class::cast)
            .map(Human::getPosition)
            .collect(Collectors.toList());
        for (EntityID id : this.worldInfo.getChanged().getChangedEntities())
        {
            Integer time = this.sentTimeMap.get(id);
            if (time != null && time > currTime) { continue; }
            StandardEntity entity = this.worldInfo.getEntity(id);
            //如果不是建筑，跳过这个entity
            if (!(entity instanceof Building)) { continue; }
            Building bld = (Building) entity;
            //没有agent在这个building或者这个building的和me在同一位置
            if (!agentPositions.contains(bld.getID())
                    || bld.getID().equals(me.getPosition()))
            {
                building = this.selectPreferred(building, bld);
            }
        }

        if (building != null)
        {
            messageManager.addMessage(new MessageBuilding(true, building));
            this.sentTimeMap.put(building.getID(), currTime + this.avoidTimeSendingSent);
            //System.out.println(messageManager.getReceivedMessageList());
            //System.out.println(this.sentTimeMap);
            //System.out.println("我是"+me.getURN()+"我的位置:"+me.getPosition().getValue()+"选择建筑id:"+building.getID().getValue()+"时间:"+currTime);
            this.out("SEND #" + building.getID());
        }
    }

    //usage:updateInfo
    //将messge列表中的messagebuilding的信息更新到worldInfo中
    private void reflectOtherEntityInfo(MessageManager messageManager)
    {
        Set<EntityID> changedEntityIDs =
                this.worldInfo.getChanged().getChangedEntities();
        int time = this.agentInfo.getTime();
        //对消息列表进行遍历
        for (CommunicationMessage message
                : messageManager.getReceivedMessageList(MessageBuilding.class))
        {
            MessageBuilding msg = (MessageBuilding) message;
            if (!changedEntityIDs.contains(msg.getBuildingID()))
            {
                MessageUtil.reflectMessage(this.worldInfo, msg);
            }
            this.sentTimeMap.put(msg.getBuildingID(), time + this.avoidTimeSendingReceived);
        }
    }

    //usage:updateInfo,calc
    //判断自己是否被卡住
    private boolean isStuckedInBlockade()
    {
        return this.stuckedHumans.calc().getClusterIndex(this.agentInfo.getID()) == 0;
    }

    private void out(String str)
    {
        String ret;
        ret  = "🚒  [" + String.format("%10d", this.agentInfo.getID().getValue())+ "]";
        ret += " BUILDING-SEARCH ";
        ret += "@" + String.format("%3d", this.agentInfo.getTime());
        ret += " -> ";
//        System.out.println(ret + str);
    }
}
