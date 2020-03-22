package CSU_Yunlu_2019.debugger;

import com.mrl.debugger.remote.VDClient;

/**
 * @author: Guanyu-Cai
 * @Date: 03/21/2020
 */
public class DebugHelper {
    public static final boolean DEBUG_MODE = false;
    public static final VDClient VD_CLIENT = DEBUG_MODE ? VDClient.getInstance() : null;

    static {
        if (DEBUG_MODE) {
            VD_CLIENT.init();
        }
    }
}
