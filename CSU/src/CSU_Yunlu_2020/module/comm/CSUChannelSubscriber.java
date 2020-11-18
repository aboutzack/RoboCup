package CSU_Yunlu_2020.module.comm;

import CSU_Yunlu_2020.CSUConstants;
import adf.agent.communication.MessageManager;
import adf.agent.config.ModuleConfig;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.platoon.PlatoonFire;
import adf.component.communication.ChannelSubscriber;
import adf.sample.tactics.SampleTacticsFireBrigade;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @see CSUMessageCoordinator
 *
 * 根据每个回合每个agent向其他所有种类的智能体发送的消息平均字节数{@link CSUConstants#MEAN_FB_MESSAGE_BYTE_SIZE}, {@link
 * CSUConstants#MEAN_PF_MESSAGE_BYTE_SIZE} and {@link CSUConstants#MEAN_AT_MESSAGE_BYTE_SIZE}计算每个回合所有
 * fireBrigadeMessages, policeMessages and ambulanceMessages的估计大小之和.
 * <p>
 * 使用{@link #getPriority(ScenarioInfo)}根据各类智能体数量和因子来计算每种智能体分配channels的优先级.
 * <p>
 * {@link #getChannels(StandardEntityURN, int, AgentInfo, WorldInfo, ScenarioInfo)}
 * 低优先级待高优先级的智能体的maxChannels数量的channels全部分配完毕后再分配. 保证所有种类智能体都分配到channels, 因此信道可能会有重叠及复用.
 *
 * @author CSU-zack
 */
public class CSUChannelSubscriber extends ChannelSubscriber {

    private static final double FB_RATIO = 2.0;
    private static final double PF_RATIO = 1.5;
    private static final double AT_RATIO = 1.0;
    //需要发送消息的agent的比例
    private double sendMessageAgentsRatio = 0;

    @Override
    public void subscribe(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
                          MessageManager messageManager) {
        if (sendMessageAgentsRatio == 0) {
            initSendMessageAgentsRatio(worldInfo, scenarioInfo);
        }
        // subscribe only once at the beginning of kernel agents ignore util
        if (agentInfo.getTime() == scenarioInfo.getKernelAgentsIgnoreuntil()) {
            int numChannels = scenarioInfo.getCommsChannelsCount() - 1; // 0th channel is the voice channel

            int maxChannelCount = 0;
            boolean isPlatoon = isPlatoonAgent(agentInfo, worldInfo);
            if (isPlatoon) {
                maxChannelCount = scenarioInfo.getCommsChannelsMaxPlatoon();
            } else {
                maxChannelCount = scenarioInfo.getCommsChannelsMaxOffice();
            }

            StandardEntityURN agentType = getAgentType(agentInfo, worldInfo);
            int[] channels = new int[maxChannelCount];
            for (int i = 0; i < maxChannelCount; i++) {
                channels[i] = getChannelNumber(agentType, i, numChannels, agentInfo, worldInfo, scenarioInfo);
            }

            if (CSUConstants.DEBUG_CHANNEL_SUBSCRIBE) {
                System.out.println(agentType + "subscribes to "+ Arrays.toString(channels));
            }

            messageManager.subscribeToChannels(channels);
        }
    }

    protected static boolean isPlatoonAgent(AgentInfo agentInfo, WorldInfo worldInfo) {
        StandardEntityURN agentType = getAgentType(agentInfo, worldInfo);
        return agentType == StandardEntityURN.FIRE_BRIGADE ||
                agentType == StandardEntityURN.POLICE_FORCE ||
                agentType == StandardEntityURN.AMBULANCE_TEAM;
    }

    protected static StandardEntityURN getAgentType(AgentInfo agentInfo, WorldInfo worldInfo) {
        return worldInfo.getEntity(agentInfo.getID()).getStandardURN();
    }

    public int getChannelNumber(StandardEntityURN agentType, int channelIndex, int numChannels, AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo) {
        int[] channels = getChannels(agentType, numChannels, agentInfo, worldInfo, scenarioInfo);
        return channels[channelIndex];
    }

    private int[] getChannels(StandardEntityURN agentType, int numChannels, AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo) {
        if (numChannels < 1) {
            return new int[1];
        }
        int scenarioAgents = CSUChannelSubscriber.getScenarioAgents(scenarioInfo);
        double fbRequiredBandwidth = CSUConstants.MEAN_FB_MESSAGE_BYTE_SIZE * scenarioAgents * getSendMessageAgentsRatio();
        double pfRequiredBandwidth = CSUConstants.MEAN_PF_MESSAGE_BYTE_SIZE * scenarioAgents * getSendMessageAgentsRatio();
        double atRequiredBandwidth = CSUConstants.MEAN_AT_MESSAGE_BYTE_SIZE * scenarioAgents * getSendMessageAgentsRatio();
        double[] requiredBandWidthRemain = new double[3];
        requiredBandWidthRemain[0] = fbRequiredBandwidth;
        requiredBandWidthRemain[1] = pfRequiredBandwidth;
        requiredBandWidthRemain[2] = atRequiredBandwidth;
        int maxChannels;
        if (CSUConstants.DEBUG_CHANNEL_SUBSCRIBE || isPlatoonAgent(agentInfo, worldInfo)) {
            maxChannels = scenarioInfo.getCommsChannelsMaxPlatoon();
        } else {
            maxChannels = scenarioInfo.getCommsChannelsMaxOffice();
        }

        int[] fbChannels = new int[maxChannels];
        int[] pfChannels = new int[maxChannels];
        int[] atChannels = new int[maxChannels];

        Map<Integer, Integer> radioBandWidthRemainMap = getRadioBandWidthMap(numChannels, scenarioInfo);
        ArrayList<Map.Entry<Integer, Integer>> sortedRadioBandWidthRemain = new ArrayList<>(radioBandWidthRemainMap.entrySet());
        //bandWidth从大到小排序
        sortedRadioBandWidthRemain.sort((t0, t1) -> (int) (t1.getValue() - t0.getValue()));
        List<StandardEntityURN> priority = getPriority(scenarioInfo);
        for (StandardEntityURN urn : priority) {
            for (int i = 0; i < maxChannels; i++) {
                Map.Entry<Integer, Integer> radioBandWidthRemain = sortedRadioBandWidthRemain.get(0);
                Integer BandWidthRemainValue = radioBandWidthRemain.getValue();
                if (urn == StandardEntityURN.FIRE_BRIGADE || urn == StandardEntityURN.FIRE_STATION) {
                    double tmp = requiredBandWidthRemain[0];
                    if (BandWidthRemainValue > requiredBandWidthRemain[0]) {
                        requiredBandWidthRemain[0] = 0;
                        radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                    } else if (i == maxChannels - 1) {//带宽不够且安排的是最后一个channel
                        requiredBandWidthRemain[0] = 0;
                        radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                    } else {//带宽不够且安排的不是最后一个channel
                        if (BandWidthRemainValue < 0) {//最大的剩余带宽都小于0了，直接分配完毕
                            requiredBandWidthRemain[0] = 0;
                            radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                        } else {//requiredBandWidthRemain扣除剩余带宽
                            requiredBandWidthRemain[0] = tmp - radioBandWidthRemain.getValue();
                            radioBandWidthRemain.setValue(0);
                        }
                    }
                    fbChannels[i] = radioBandWidthRemain.getKey();
                } else if (urn == StandardEntityURN.POLICE_FORCE || urn == StandardEntityURN.POLICE_OFFICE) {
                    double tmp = requiredBandWidthRemain[1];
                    if (BandWidthRemainValue > requiredBandWidthRemain[1]) {
                        requiredBandWidthRemain[1] = 0;
                        radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                    } else if (i == maxChannels - 1) {//带宽不够且安排的是最后一个channel
                        requiredBandWidthRemain[1] = 0;
                        radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                    } else {//带宽不够且安排的不是最后一个channel
                        if (BandWidthRemainValue < 0) {//最大的剩余带宽都小于0了，直接分配完毕
                            requiredBandWidthRemain[1] = 0;
                            radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                        } else {//requiredBandWidthRemain扣除剩余带宽
                            requiredBandWidthRemain[1] = tmp - radioBandWidthRemain.getValue();
                            radioBandWidthRemain.setValue(0);
                        }
                    }
                    pfChannels[i] = radioBandWidthRemain.getKey();
                } else if (urn == StandardEntityURN.AMBULANCE_TEAM || urn == StandardEntityURN.AMBULANCE_CENTRE) {
                    double tmp = requiredBandWidthRemain[2];
                    if (BandWidthRemainValue > requiredBandWidthRemain[2]) {
                        requiredBandWidthRemain[2] = 0;
                        radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                    } else if (i == maxChannels - 1) {//带宽不够且安排的是最后一个channel
                        requiredBandWidthRemain[2] = 0;
                        radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                    } else {//带宽不够且安排的不是最后一个channel
                        if (BandWidthRemainValue < 0) {//最大的剩余带宽都小于0了，直接分配完毕
                            requiredBandWidthRemain[2] = 0;
                            radioBandWidthRemain.setValue((int) (BandWidthRemainValue - tmp));
                        } else {//requiredBandWidthRemain扣除剩余带宽
                            requiredBandWidthRemain[2] = tmp - radioBandWidthRemain.getValue();
                            radioBandWidthRemain.setValue(0);
                        }
                    }
                    atChannels[i] = radioBandWidthRemain.getKey();
                }
                //重新将bandWidth从大到小排序
                sortedRadioBandWidthRemain.sort((t0, t1) -> (int) (t1.getValue() - t0.getValue()));
            }
        }
        if (agentType == StandardEntityURN.FIRE_BRIGADE || agentType == StandardEntityURN.FIRE_STATION) {
            return fbChannels;
        } else if (agentType == StandardEntityURN.POLICE_FORCE || agentType == StandardEntityURN.POLICE_OFFICE) {
            return pfChannels;
        } else if (agentType == StandardEntityURN.AMBULANCE_TEAM || agentType == StandardEntityURN.AMBULANCE_CENTRE) {
            return atChannels;
        }
        return new int[1];
    }

    public static int getScenarioAgents(ScenarioInfo scenarioInfo) {
        return scenarioInfo.getScenarioAgentsFb() + scenarioInfo.getScenarioAgentsFs() + scenarioInfo.getScenarioAgentsPf() +
                scenarioInfo.getScenarioAgentsPo() + scenarioInfo.getScenarioAgentsAt() + scenarioInfo.getScenarioAgentsAc();
    }

    private static Map<Integer, Integer> getRadioBandWidthMap(int numChannels, ScenarioInfo scenarioInfo) {
        HashMap<Integer, Integer> radioBandWidthMap = new HashMap<>();
        for (int i = 1; i <= numChannels; i++) {
            radioBandWidthMap.put(i, scenarioInfo.getCommsChannelBandwidth(i));
        }
        return radioBandWidthMap;
    }

    public void initSendMessageAgentsRatio(WorldInfo worldInfo, ScenarioInfo scenarioInfo) {
        if (isBandWidthSufficient(scenarioInfo) && CSUConstants.DEBUG_CHANNEL_SUBSCRIBE) {
            sendMessageAgentsRatio = 1.0;
            System.out.println("sendMessageAgentsRatio Sufficient: " + sendMessageAgentsRatio);
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        Pair<Integer, Integer> pos;
        for (StandardEntity standardEntity : worldInfo.getAllEntities()) {
            pos = worldInfo.getLocation(standardEntity);
            if (pos.first() < minX)
                minX = pos.first();
            if (pos.second() < minY)
                minY = pos.second();
            if (pos.first() > maxX)
                maxX = pos.first();
            if (pos.second() > maxY)
                maxY = pos.second();
        }
        double mapSize = ((maxX - minX) / 1000.0) * ((maxY - minY) / 1000.0);
        double agentCoverageSize = (scenarioInfo.getPerceptionLosMaxDistance() / 1000.0)
                * (scenarioInfo.getPerceptionLosMaxDistance() / 1000.0)
                * Math.PI
                * getScenarioAgents(scenarioInfo);
        //开方，防止太大
        sendMessageAgentsRatio = Math.min(1, Math.sqrt(mapSize / agentCoverageSize));
        if (CSUConstants.DEBUG_CHANNEL_SUBSCRIBE) {
            System.out.println("sendMessageAgentsRatio: " + sendMessageAgentsRatio);
        }
    }

    public static boolean isBandWidthSufficient(ScenarioInfo scenarioInfo) {
        double fbRequiredBandwidth = CSUConstants.MEAN_FB_MESSAGE_BYTE_SIZE * getScenarioAgents(scenarioInfo);
        double pfRequiredBandwidth = CSUConstants.MEAN_PF_MESSAGE_BYTE_SIZE * getScenarioAgents(scenarioInfo);
        double atRequiredBandwidth = CSUConstants.MEAN_AT_MESSAGE_BYTE_SIZE * getScenarioAgents(scenarioInfo);
        int totalDistributableRadioBandWith = getTotalDistributableRadioBandWith(scenarioInfo);
        return totalDistributableRadioBandWith > fbRequiredBandwidth + pfRequiredBandwidth + atRequiredBandwidth;
    }

    /**
     * eg.每种platoon可以分配2个频道，则返回前2*3个最大的频道带宽之和。即分配给智能体最多带宽之和
     */
    private static int getTotalDistributableRadioBandWith(ScenarioInfo scenarioInfo) {
        Map<Integer, Integer> radioBandWidthRemainMap = getRadioBandWidthMap(scenarioInfo.getCommsChannelsCount() - 1, scenarioInfo);
        ArrayList<Map.Entry<Integer, Integer>> sortedRadioBandWidthRemain = new ArrayList<>(radioBandWidthRemainMap.entrySet());
        int commsChannelsMaxPlatoon = scenarioInfo.getCommsChannelsMaxPlatoon();
        int result = 0;
        for (int i = 0; i < sortedRadioBandWidthRemain.size() && i < commsChannelsMaxPlatoon * 3; i++) {
            result += sortedRadioBandWidthRemain.get(i).getValue();
        }
        return result;
    }

    public double getSendMessageAgentsRatio() {
        return this.sendMessageAgentsRatio;
    }

    /**
     * 分配频道的优先级
     *
     */
    public static List<StandardEntityURN> getPriority(ScenarioInfo scenarioInfo) {
        double fb = (scenarioInfo.getScenarioAgentsFb() + scenarioInfo.getScenarioAgentsFs()) * FB_RATIO;
        double pf = (scenarioInfo.getScenarioAgentsPf() + scenarioInfo.getScenarioAgentsPo()) * PF_RATIO;
        double at = (scenarioInfo.getScenarioAgentsAt() + scenarioInfo.getScenarioAgentsAc()) * AT_RATIO;
        ArrayList<StandardEntityURN> result = new ArrayList<>();
        HashMap<StandardEntityURN, Double> map = new HashMap<>();
        map.put(StandardEntityURN.FIRE_BRIGADE, fb);
        map.put(StandardEntityURN.POLICE_FORCE, pf);
        map.put(StandardEntityURN.AMBULANCE_TEAM, at);
        ArrayList<Map.Entry<StandardEntityURN, Double>> entryArrayList = new ArrayList<>(map.entrySet());
        entryArrayList.sort((t0, t1) -> (int) (t1.getValue() - t0.getValue()));
        for (Map.Entry<StandardEntityURN, Double> entry : entryArrayList) {
            result.add(entry.getKey());
        }
        return result;
    }

    public static void main(String[] args) {
        Config config = new Config();
        config.setIntValue(CSUConstants.FIRE_BRIGADE_COUNT_KEY, 30);
        config.setIntValue(CSUConstants.FIRE_STATION_COUNT_KEY, 2);
        config.setIntValue(CSUConstants.POLICE_FORCE_COUNT_KEY, 30);
        config.setIntValue(CSUConstants.POLICE_OFFICE_COUNT_KEY, 2);
        config.setIntValue(CSUConstants.AMBULANCE_TEAM_COUNT_KEY, 30);
        config.setIntValue(CSUConstants.AMBULANCE_CENTRE_COUNT_KEY, 2);
        config.setIntValue("comms.channels." + "0" + ".bandwidth", 256);
        config.setIntValue("comms.channels." + "1" + ".bandwidth", 64);
        config.setIntValue("comms.channels." + "2" + ".bandwidth", 32);
        config.setIntValue("comms.channels." + "3" + ".bandwidth", 128);
        config.setIntValue("comms.channels." + "4" + ".bandwidth", 4000);
        config.setIntValue("comms.channels." + "5" + ".bandwidth", 4000);
        config.setIntValue("comms.channels." + "6" + ".bandwidth", 4000);
        config.setIntValue("comms.channels.count", 7);
        config.setIntValue("comms.channels.max.platoon", 2);
        FireBrigade fireBrigade = new FireBrigade(new EntityID(1111111));
        StandardWorldModel worldModel = new StandardWorldModel();
        PlatoonFire platoonFire = new PlatoonFire(new SampleTacticsFireBrigade(), false, false,
                new ModuleConfig("./config/module.cfg", new ArrayList<>()),
                new DevelopData(false, "./data/develop.json", new ArrayList<>()));
        ScenarioInfo scenarioInfo = new ScenarioInfo(config, ScenarioInfo.Mode.NON_PRECOMPUTE);
        WorldInfo worldInfo = new WorldInfo(worldModel);
        worldInfo.addEntity(fireBrigade);
        AgentInfo agentInfo = new AgentInfo(platoonFire, worldModel);

        int numChannels = scenarioInfo.getCommsChannelsCount() - 1;
        int maxChannels = scenarioInfo.getCommsChannelsMaxPlatoon();
        CSUChannelSubscriber subscriber = new CSUChannelSubscriber();
        for (int i = 0; i < maxChannels; i++) {
            System.out.println("FIREBRIGADE-" + i + ":" + subscriber.getChannelNumber(StandardEntityURN.FIRE_BRIGADE, i, numChannels, agentInfo, worldInfo, scenarioInfo));
        }
        for (int i = 0; i < maxChannels; i++) {
            System.out.println("POLICE-" + i + ":" + subscriber.getChannelNumber(StandardEntityURN.POLICE_OFFICE, i, numChannels, agentInfo, worldInfo, scenarioInfo));
        }
        for (int i = 0; i < maxChannels; i++) {
            System.out.println("AMB-" + i + ":" + subscriber.getChannelNumber(StandardEntityURN.AMBULANCE_CENTRE, i, numChannels, agentInfo, worldInfo, scenarioInfo));
        }
    }
}