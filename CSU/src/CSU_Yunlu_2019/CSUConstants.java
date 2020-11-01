package CSU_Yunlu_2019;

/**
 * User: CSU_2019
 * Description:Constants Repository
 */
public interface CSUConstants {
    public static final boolean DEBUG_PATH_PLANNING = false;
    public static final boolean DEBUG_AT_SEARCH = false;
    public static final boolean DEBUG_DEFAULT_WORLD_HELPER = false;
    public static final boolean DEBUG_FB_WORLD_HELPER = false;
    public static final boolean DEBUG_DIRECTION_BASED_TARGET_SELECTOR = false;
    public static final boolean DEBUG_STUCK_HELPER = false;
    public static final boolean DEBUG_WATER_REFILL = false;
    public static final boolean DEBUG_MESSAGE_COUNT = false;
    public static final boolean DEBUG_CHANNEL_SUBSCRIBE = false;

    //防止target.cfg未正确配置
    public static final String WORLD_HELPER_DEFAULT = "CSU_Yunlu_2019.world.CSUWorldHelper";
    public static final String WORLD_HELPER_FIRE_BRIGADE = "CSU_Yunlu_2019.world.CSUFireBrigadeWorld";
    public static final String FIRE_CLUSTERING = "CSU_Yunlu_2019.module.algorithm.fb.CSUFireClustering";
    public static final String GRAPH_HELPER_DEFAULT = "CSU_Yunlu_2019.world.graph.GraphHelper";

    public static final double MEAN_VELOCITY_DISTANCE = 31445.392;
    public static final int AGENT_SIZE = 1000;
    public static final int AGENT_PASSING_THRESHOLD = 725;
    public static final int AGENT_PASSING_THRESHOLD_SMALL = 500;
    public static final int AGENT_MINIMUM_PASSING_THRESHOLD = 200;
    public static final int COLLINEAR_THRESHOLD = 10;
    public static final int TOO_SMALL_EDGE_THRESHOLD = 600;
    public static final int ROAD_PASSABLY_RESET_TIME_IN_SMALL_MAP = 15;
    public static final int ROAD_PASSABLY_RESET_TIME_IN_MEDIUM_MAP = 20;
    public static final int ROAD_PASSABLY_RESET_TIME_IN_HUGE_MAP = 25;
    public static final int MAX_SEARCH_INTERVAL_BETWEEN_LAST_SEEN = 4;
    public static final double MEAN_FB_MESSAGE_BYTE_SIZE = 36.733;
    public static final double MEAN_PF_MESSAGE_BYTE_SIZE = 40.8623;
    public static final double MEAN_AT_MESSAGE_BYTE_SIZE = 44.182;

    /** Config key for the number of fire brigades in the scenario. */
    public static final String FIRE_BRIGADE_COUNT_KEY = "scenario.agents.fb";

    /** Config key for the number of ambulance teams in the scenario. */
    public static final String AMBULANCE_TEAM_COUNT_KEY = "scenario.agents.at";

    /** Config key for the number of police forces in the scenario. */
    public static final String POLICE_FORCE_COUNT_KEY = "scenario.agents.pf";

    /** Config key for the number of fire stations in the scenario. */
    public static final String FIRE_STATION_COUNT_KEY = "scenario.agents.fs";

    /** Config key for the number of ambulance centres in the scenario. */
    public static final String AMBULANCE_CENTRE_COUNT_KEY = "scenario.agents.ac";

    /** Config key for the number of police offices in the scenario. */
    public static final String POLICE_OFFICE_COUNT_KEY = "scenario.agents.po";


    public static final String PRECOMPUTE_DIRECTORY = "precomp_data/";
}
