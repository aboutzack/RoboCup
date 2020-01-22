package mrl_2019;


/**
 * User: mrl_2019
 * Date: Apr 28, 2010
 * Time: 11:38:50 PM
 */
public interface MRLConstants {
    //DEBUGGER
    public static final String LOG4J_PARAM = "none"; // "file" | "console" | "none";


    //    public static final boolean DEBUG_MODE= true;   #### this flag moved to MrlPersonalData and will be initialized in LaunchMRL. by default this is false until -vi argument found.
    public static final boolean DEBUG_MESSAGING = false;
    public static final boolean DEBUG_SAY = false;
    public static final boolean DEBUG_PLANNER = false;
    public static final boolean DEBUG_SEARCH = false;
    public static final boolean DEBUG_LA = false;
    public static final boolean DEBUG_RENDEZVOUS_ACTION = false;
    public static final boolean DEBUG_AMBULANCE_TEAM = false;
    public static final boolean DEBUG_FIRE_BRIGADE = false;
    public static final boolean DEBUG_POLICE_FORCE = false;
    public static final boolean DEBUG_POLICE_FORCE_CLEAR = false;
    public static final String PRECOMPUTE_DIRECTORY = "precompute/";

    //AGENT
    public static final int IN_TARGET = 0;
    public static final boolean HIGHWAY_STRATEGY = false;
    public static final boolean DO_NOT_MOVE_IN_BURNING_BUILDING = true;
    public static final boolean SEARCH_INSIDE_BUILDINGS = false;
    public static final int RANDOM_WALK_LENGTH = 50;
    public static final double MEAN_VELOCITY_OF_MOVING = 31445.392;
    public static final int MOVE_TIME_IN_PARTITION = 2;
    public static final String THINK_TIME_KEY = "kernel.agents.think-time";
    public static final String MAX_VIEW_DISTANCE_KEY = "perception.los.max-distance";
    public static final int ROAD_PASSABLY_RESET_TIME_IN_SMALL_MAP = 15;
    public static final int ROAD_PASSABLY_RESET_TIME_IN_MEDIUM_MAP = 20;
    public static final int ROAD_PASSABLY_RESET_TIME_IN_HUGE_MAP = 25;
    public static final int BUILDING_PASSABLY_RESET_TIME = 20;
    public static final int REFUGE_PASSABLY_RESET_TIME = 10;
    public static final int MAX_POLICE_FORCES = 40;
    public static final int MAX_FIRE_BRIGADES = 40;
    public static final int MAX_AMBULANCE_TEAMS = 40;
    public static final int MAX_SMALL_MAP_REFUGES = 10;
    public static final int MAX_MEDIUM_MAP_REFUGES = 20;
    public static final int MAX_BIG_MAP_REFUGES = 30;


    public static final String HP_PRECISION = "perception.los.precision.hp";
    public static final String DAMAGE_PRECISION = "perception.los.precision.damage";
    //    public static final int MAX_VIEW_DISTANCE = 10000;
    public static final String DEFAULT_NEW_ZONE_NAME = "MrlZone.file";
    public static final int AGENT_SIZE = 1000;
    public static final int AGENT_PASSING_THRESHOLD = 725;
    public static final int AGENT_MINIMUM_PASSING_THRESHOLD = 200;
    public static final int MIN_GRID_SIZE = 700;// 900
    public static final double SQ_MM_TO_SQ_M = 0.000001;
    public static final boolean NEW_SEARCH = false;


    //FIRE BRIGADE
    public static final int WATER_REFILL_RATE = 2000;
    public static final int WATER_REFILL_RATE_IN_HYDRANT = 150;
    public static final int AVAILABLE_HYDRANTS_UPDATE_TIME = 10;
    public static final boolean DEBUG_WATER_COOLING = false;

    public static final String MAX_WATER_KEY = "fire.tank.maximum";
    public static final String WATER_REFILL_RATE_KEY = "fire.tank.refill_rate";
    public static final String WATER_REFILL_HYDRANT_RATE_KEY = "fire.tank.refill_hydrant_rate";
    public static final String MAX_EXTINGUISH_DISTANCE_KEY = "fire.extinguish.max-distance";
    public static final String MAX_EXTINGUISH_POWER_KEY = "fire.extinguish.max-sum";
    //POLICE FORCE
    public static final boolean POLICE_STRATEGY = false;
    public static final String MAX_CLEAR_DISTANCE_KEY = "clear.repair.distance";
    public static final String CLEAR_RADIUS_KEY = "clear.repair.rad";
    //AMBULANCE TEAM

    // COMMUNICATION
    public static final boolean TECHNICAL_CHALLENGE_COMMUNICATION = false;
    public static final int VOICE_CHANNEL_MESSAGE_REPEAT = 2;
    public static final int MESSAGE_VALIDATE_THRESHOLD = 5;
    public static final int SAY_PACKET_ALPHA = 4;
    public static final int SAY_PACKET_N = 10;
    public static final int SAY_PACKET_TTL_MAX = 64;
    public static final int SAY_PACKET_ALPHA1 = 7;
    public static final int SAY_VALIDATE_THRESHOLD = 45;
    public static final int DEFAULT_COMMUNICATION_CHANNEL = 1;
    public static final String SAY_COMMUNICATION_MODEL_KEY = "kernel.standard.StandardCommunicationModel";
    public static final String SPEAK_COMMUNICATION_MODEL_KEY = "kernel.standard.ChannelCommunicationModel";
    public static final String VOICE_RANGE_KEY = "comms.channels.0.range";

    public static final String MAX_PLATOON_CHANNELS_KEY = "comms.channels.max.platoon";

    public static final boolean RENDEZVOUS_ACTION = false;
    // edge
    public static final double BUILDING_EDGE_DEV_WEIGHT = 1.4d;


    public static final int MILLS_FOR_CREATING_RAYS = 10 * 1000;//3:00   //todo: check this time
    public static final float MAX_RAY_RATE = 0.0025f;

    public static final float MIN_RAY_RATE = 0.0000001f;

    public static final String GIS_KEY = "kernel.gis";
    public static final long KOBE = 2141239244L;
    public static final long VC = 4440103773L;
    public static final long FOLIGNO = 6907514034L;
    public static final long BERLIN = 17687985466L;
    public static final long PARIS = 14542274827L;
    public static final String POSITION_URN = "urn:rescuecore2.standard:property:position";
    public static final String HP_URN = "urn:rescuecore2.standard:property:hp";
    public static final String DAMAGE_URN = "urn:rescuecore2.standard:property:damage";
    public static final String BURIEDNESS_URN = "urn:rescuecore2.standard:property:buriedness";
    public static final String X_URN = "urn:rescuecore2.standard:property:x";
    public static final String Y_URN = "urn:rescuecore2.standard:property:y";
    public static final String FIERYNESS_URN = "urn:rescuecore2.standard:property:fieryness";
    public static final String STAMINA_URN = "urn:rescuecore2.standard:property:stamina";
    public static final String WATER_QUANTITY_URN = "urn:rescuecore2.standard:property:waterquantity";
    public static final String EDGES_URN = "urn:rescuecore2.standard:property:edges";
    public static final String TEMPERATURE_URN = "urn:rescuecore2.standard:property:temperature";
    public static final String KERNEL_TIMESTEPS = "kernel.timesteps";


    /**
     * The config key for perception implementations.
     */
    public static final String PERCEPTION_KEY = "perception.los.max-distance";

    /**
     * The config key for communication model implementations.
     */
    public static final String COMMUNICATION_MODEL_KEY = "kernel.communication";

    /**
     * The config key for agent implementations.
     */
    public static final String AGENTS_KEY = "kernel.agents";

    /**
     * The config key for simulator implementations.
     */
    public static final String SIMULATORS_KEY = "kernel.simulators";

    /**
     * The config key for viewer implementations.
     */
    public static final String VIEWERS_KEY = "kernel.viewers";

    /**
     * The config key for component implementations.
     */
    public static final String COMPONENTS_KEY = "kernel.components";

    /**
     * Whether to run the kernel in inline-only mode.
     */
    public static final String INLINE_ONLY_KEY = "kernel.inline-only";

    /**
     * The config key for ignoring agent commands at the start of the simulation.
     */
    public static final String IGNORE_AGENT_COMMANDS_KEY = "kernel.agents.ignoreuntil";
}
