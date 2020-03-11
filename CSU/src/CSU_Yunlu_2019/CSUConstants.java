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
    //防止target.cfg未正确配置
    public static final String WORLD_HELPER_DEFAULT = "CSU_Yunlu_2019.world.CSUWorldHelper";
    public static final String WORLD_HELPER_FIRE_BRIGADE = "CSU_Yunlu_2019.world.CSUFireBrigadeWorld";
    public static final String FIRE_CLUSTERING = "CSU_Yunlu_2019.module.algorithm.fb.CSUFireClustering";

    public static final double MEAN_VELOCITY_DISTANCE = 31445.392;
}
