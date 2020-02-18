package AUR.util.aslan;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import java.util.Collection;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURPoliceScenarioAnalyzer extends AbstractModule {
        public final int YES = 1;
        public final int NO = 0;
        public final int UNDEFINED = 2;
        
        public AURPoliceScenarioAnalyzer(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
                super(ai, wi, si, moduleManager, developData);
        }

        @Override
        public AbstractModule calc() {
                return this;
        }

        public int blockadeStatusInMap = this.UNDEFINED;
        
        public int isThereBlockadesInMap(){
                return blockadeStatusInMap;
        }
        
        @Override
        public AbstractModule updateInfo(MessageManager messageManager) {
                super.updateInfo(messageManager);
                
                if(blockadeStatusInMap == this.UNDEFINED || blockadeStatusInMap == this.NO){
                        Collection<EntityID> blockadesCollection = worldInfo.getEntityIDsOfType(StandardEntityURN.BLOCKADE);
                        if(! blockadesCollection.isEmpty()){
                                this.blockadeStatusInMap = this.YES;
                        }
                        else if(agentInfo.getTime() > 20){
                                this.blockadeStatusInMap = this.NO;
                        }
                }
                
                return this;
        }
        
}
