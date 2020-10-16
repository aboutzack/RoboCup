package mrl_2019.viewer;

import com.mrl.debugger.remote.VDClient;

public final class MrlPersonalData {

    public final static IViewerData VIEWER_DATA;

    public static final boolean DEBUG_MODE = true;

    static {
        if (DEBUG_MODE) {
            VIEWER_DATA = new FullViewerData();
            VDClient.getInstance().init();
        } else {
            VIEWER_DATA = new EmptyViewerData();
        }
    }

}
