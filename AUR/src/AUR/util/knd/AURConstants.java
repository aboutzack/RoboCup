package AUR.util.knd;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURConstants {

	public static class Misc {
		public final static int CLOSE_BUILDING_THRESHOLD = 25 * 1000;
		public final static int FIRE_ZONE_BORDER_INTERSECT_THRESHOLD = 10 * 1000;
		public final static int MIN_NUMBER_OF_NEIGHBOUR_CLUSTERS = 6;
		public final static int AGENT_CLUSTER_BOUNDS_OFFSET = 1000 * 10;
	}
	
	public static class PathPlanning {
		public final static int DEFAULT_BLOCKADE_FORGET_TIME = 40;
		public final static int POLICE_BLOCKADE_FORGET_TIME = 40;
		public final static int AMBULANCE_BLOCKADE_FORGET_TIME = 40;
		public final static int FIREBRIGADE_BLOCKADE_FORGET_TIME = 50;
	}
	
	public static class Agent {
		public final static int RADIUS = 500;
		public final static int VELOCITY = 40000;
	}
	
	public static class Math {
		public final static double DOUBLE_INF = Double.MAX_VALUE;
		public final static int INT_INF = Integer.MAX_VALUE;
		public final static int INT_NEGATIVE_INF = Integer.MIN_VALUE;
		public final static double sqr2 = 1.41421;
	}
	
	static class Viewer {
		public final static boolean LAUNCH = false;
	}
	
	static class FireSim {
		
		public final static double RADIATION_COEFFICENT = 0.011;
		public final static double STEFAN_BOLTZMANN_CONSTANT = 0.000000056704;
		public final static double GAMMA = 0.2;
		public final static double WATER_COEFFICIENT = 20;
		public final static float TIME_STEP_LENGTH = 1;
		public final static float ENERGY_LOSS = 0.86f;
		public final static float AIR_TO_AIR_COEFFICIENT = 1f;
		public final static float AIR_TO_BUILDING_COEFFICIENT = 0.0015f;
		public final static float WEIGHT_GRID = 0.2f;
		public final static float AIR_CELL_HEAT_CAPACITY = 0.004f;

		
		public final static int WORLD_AIR_CELL_SIZE = 5000;
		public final static int MAX_RADIATION_DISTANCE = 200000;
		public final static double RADIATION_RAY_RATE = 0.0025;
	
		public final static int FLOOR_HEIGHT = 3;
		
		public final static double STEEL_CAPACITY = 1.0;
		public final static double WOODEN_CAPACITY = 1.1;
		public final static double CONCRETE_CAPACITY = 1.5;
		
		public final static double STEEL_ENERGY = 800.0;
		public final static double WOODEN_ENERGY = 2400.0;
		public final static double CONCRETE_ENERGY = 350.0;
		
		public final static double STEEL_IGNITION = 47.0;
		public final static double WOODEN_IGNITION = 47.0;
		public final static double CONCRETE_IGNITION = 47.0;
		
		public final static boolean POLICE_OFFICE_INFLAMMABLE = false;
		public final static boolean AMBULANCE_CENTRE_INFLAMMABLE = false;
		public final static boolean FIRE_STATION_INFLAMMABLE = false;
		public final static boolean REFUGE_INFLAMMABLE = false;
		
	}
        
        /**
         * 
         * @author Amir Aslan Aslani - Mar 2018
         */
        public static class PoliceExtClear {
                public final static int CLEAR_POLYGON_HEIGHT = AURConstants.Agent.RADIUS * 10 / 6;
                public final static int GUID_POINT_CLEAR_POLYGON_HEIGHT = AURConstants.Agent.RADIUS * 10 / 6;
                public final static int MOVE_LENGTH_CALCULATE_ERROR = 500;
                
                public final static boolean USE_BUILDINGS_ENTRANCE_PERPENDICULAR_LINE = true;
                public final static boolean USE_STRAIGHT_ROAD_DETECTION = true;
                public final static boolean IGNORE_POLICES_RESCUE = true;
                public final static boolean OPEN_NEAR_BUILDINGS_ENTRANCES = false;
        }
        
        /**
         * 
         * @author Amir Aslan Aslani - Mar 2018
         */
        public static class ClearWatcher {
                public final static int DONT_MOVE_COUNTER_LIMIT = 3;
                public final static int OLD_FUNCTION_CLEAR_COUNTER_LIMIT = 3;
                public final static double ALLOWED_MOVE_VALUE = AURConstants.Agent.RADIUS;
        }
        
        /**
         * 
         * @author Amir Aslan Aslani - Mar 2018
         */
        public static class CollapseEstimator{
                
                // Building Collapse Estimator
                public static final double FLOOR_HEIGHT = 7;
                public static final double MAX_COLLAPSE = 100;
                public static final double WALL_COLLAPSE_EXTENT_MIN = 0.6;
                public static final double WALL_COLLAPSE_EXTENT_MAX = 1;
                
                // Amount of damage for each collapse degree class
                public final static double DESTROYED_MEAN = 100;
                public final static double DESTROYED_SD = 5;
                
                public final static double SEVERE_MEAN = 75;
                public final static double SEVERE_SD = 5;
                
                public final static double MODERATE_MEAN = 50;
                public final static double MODERATE_SD = 5;
                
                public final static double SLIGHT_MEAN = 25;
                public final static double SLIGHT_SD = 5;
                
                // Damage distribution for wooden buildings
                public final static double WOOD_DESTROYED = 0.15;
                public final static double WOOD_SEVERE = 0.15;
                public final static double WOOD_MODERATE = 0.15;
                public final static double WOOD_SLIGHT = 0.15;
                public final static double WOOD_NONE = 0.4;
                
                // Damage distribution for steel buildings
                public final static double STEEL_DESTROYED = 0.1;
                public final static double STEEL_SEVERE = 0.15;
                public final static double STEEL_MODERATE = 0.2;
                public final static double STEEL_SLIGHT = 0.25;
                public final static double STEEL_NONE = 0.3;

                // Damage distribution for concrete buildings
                public final static double CONCRETE_DESTROYED = 0.3;
                public final static double CONCRETE_SEVERE = 0.25;
                public final static double CONCRETE_MODERATE = 0.2;
                public final static double CONCRETE_SLIGHT = 0.15;
                public final static double CONCRETE_NONE = 0.1;
                
        }
        
        /**
         * 
         * @author Amir Aslan Aslani - Mar 2018
         */
        public static class RoadDetector {
                
                public final static double DIST_SCORE_COEFFICIENT = 25000;
                public final static double DECREASE_POLICE_AREA_SCORE = 0.01;
                public final static double GAS_STATION_EXPLODE_DISTANCE = 20000;
                
                public static class BaseScore {
                        public final static double DISTANCE = 0.4;
                        
                        public final static double REFUGE = 0.4;
                        public final static double GAS_STATION = 0.0;
                        public final static double HYDRANT = 0.05;
                        
                        public final static double WSG_ROAD = 0.15;
                        public final static double CLUSTER = 0.7;
                        
                        public final static double POLICE_FORCE = 0.1;
                        public final static double AMBULANCE_TEAM = 0.125;
                        public final static double FIRE_BRIGADE = 0.125;
                        
                        public final static double ENTRANCES_NUMBER = 0.175;
                }
                
                public static class SecondaryScore {
                        public final static double DEAD_POLICE_CLUSTER = 0.3;
                        public final static double BLOCKED_HUMAN = 0.2;
                        public final static double DISTANCE = 0.3;
                        public final static double SELECTED_TARGET = 0.2;
                        public final static double ROADS_WITHOUT_BLOCKADES = 0.0;
                        public final static double RELEASED_AGENTS_START_POSITION_SCORE = 0.0;
//                        public final static double BUILDINGS_THAT_CONTAINS_CIVILANS = 0.3;
//                        public final static double BUILDINGS_DONT_CONTAINS_CIVILIAN = 0.05;
                        public final static double BLOCKED_BUILDINGS = 0.25;
                        public final static double BUILDINGS_THAT_I_KNOW_WHAT_IN_THAT = 0.3;
                }
                
        }
	
}
