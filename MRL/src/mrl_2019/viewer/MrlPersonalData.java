package mrl_2019.viewer;

public final class MrlPersonalData {

    public final static IViewerData VIEWER_DATA;

    public static final boolean DEBUG_MODE = false;

    static {
        if (DEBUG_MODE) {
            VIEWER_DATA = new FullViewerData();
        } else {
            VIEWER_DATA = new EmptyViewerData();
        }
    }

}
