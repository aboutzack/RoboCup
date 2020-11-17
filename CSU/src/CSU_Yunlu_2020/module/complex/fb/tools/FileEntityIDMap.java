package CSU_Yunlu_2020.module.complex.fb.tools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * A structure for save and restore ObservableAreas and VisibleFrom to/from file
 */
public class FileEntityIDMap extends HashMap<Integer, List<Integer>> implements Serializable {
    static final long serialVersionUID = -28728787457456789L;
}
