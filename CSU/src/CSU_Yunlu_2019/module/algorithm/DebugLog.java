
package CSU_Yunlu_2019.module.algorithm;

import adf.agent.info.AgentInfo;

public class DebugLog {

    private final boolean DEBUGLOG = true;

    private AgentInfo agentInfo = null;

    public DebugLog() {

    }

    public DebugLog(AgentInfo ai) {
        agentInfo = ai;
    }

    public void log(String info) {
        if (agentInfo != null)
            info = this.agentInfo.getID() + ": " + info;
        // if (DEBUGLOG)
            System.out.println(info);
    }

    public void log(int info) {
        this.log(String.valueOf(info));
    }
    
}
