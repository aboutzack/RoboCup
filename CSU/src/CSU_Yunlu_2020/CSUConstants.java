package CSU_Yunlu_2020;

/**
 * User: CSU_2020
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
    public static final boolean DEBUG_DISTANCE_BASED_CLUSTER_SELECTOR = false;
    public static final boolean DEBUG_NEED_CLEAR_COMMON = false;
    public static final boolean DEBUG_CANNOT_FIND_POSITION_TO_EXTINGUISH = false;
    public static final boolean DEBUG_CHANGE_STRATEGY = false;
    public static final boolean DEBUG_BACK_TO_MY_CLUSTER = false;
    public static final boolean DEBUG_INIT_CND = false;

    //防止target.cfg未正确配置
    public static final String WORLD_HELPER_DEFAULT = "CSU_Yunlu_2020.world.CSUWorldHelper";
    public static final String WORLD_HELPER_FIRE_BRIGADE = "CSU_Yunlu_2020.world.CSUFireBrigadeWorld";
    public static final String FIRE_CLUSTERING = "CSU_Yunlu_2020.module.algorithm.fb.CSUFireClustering";
    public static final String GRAPH_HELPER_DEFAULT = "CSU_Yunlu_2020.world.graph.GraphHelper";
    public static final String GUIDE_LINE_CREATOR = "CSU_Yunlu_2020.module.complex.pf.GuidelineCreator";
    public static final String SEARCH_HELPER = "CSU_Yunlu_2020.module.complex.fb.search.SearchHelper";
    public static final String A_STAR_PATH_PLANNING = "CSU_Yunlu_2020.module.algorithm.AStarPathPlanning";

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
    public static final double MEAN_FB_MESSAGE_BYTE_SIZE = 40;
    public static final double MEAN_PF_MESSAGE_BYTE_SIZE = 44;
    public static final double MEAN_AT_MESSAGE_BYTE_SIZE = 44;

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
