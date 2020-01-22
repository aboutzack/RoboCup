
package CSU_Yunlu_2019.module.algorithm;

import java.util.*;

import adf.agent.info.AgentInfo;
import rescuecore2.worldmodel.EntityID;

public class StuckDetector {
    
    private final boolean DEBUGLOG = true;
    private final int OldMax = 5;
    private final int OldLine = 1;

    private LinkedList<EntityID> oldPath = new LinkedList<>();
    private boolean bStuck = false;

    private AgentInfo agentInfo;

    public StuckDetector(AgentInfo _ai) {
        this.agentInfo = _ai;
    }
    
    public void update(EntityID ne) {
        if (oldPath.size() > OldMax) oldPath.removeFirst();
        oldPath.add(ne);

        // Circling detection
        bStuck = false;
        Iterator itr=oldPath.iterator();
        int cntOld = 0;
        while (itr.hasNext() && cntOld < oldPath.size() - OldLine) {
            EntityID e = (EntityID)itr.next();
            if (e.getValue() == ne.getValue()) {
                bStuck = true;
                break;
            }
            ++cntOld;
        }
    }

    public LinkedList<EntityID> getOlds() {
        return oldPath;
    }
    
    private void debugLog(String info) {
        if (DEBUGLOG) System.out.println(this.agentInfo.getID() + ": " + info);
    }

    public void warnStuck() {
        if (bStuck) debugLog("Warning: Stucked !");
    }

    public boolean isStucked() {
        return bStuck;
    }
    
}
