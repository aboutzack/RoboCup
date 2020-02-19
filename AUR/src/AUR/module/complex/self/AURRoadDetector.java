package AUR.module.complex.self;

import AUR.util.aslan.AURClearWatcher;
import AUR.util.aslan.AURPoliceScoreGraph;
import AUR.util.aslan.AURPoliceScenarioAnalyzer;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURWorldGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.RoadDetector;
import com.google.common.collect.Lists;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class AURRoadDetector extends RoadDetector {

        private Set<Area> openedAreas = new HashSet<>();
                
        private Clustering clustering;
        private PathPlanning pathPlanning;

        private EntityID result;
        private final AURWorldGraph wsg;
        private final AURPoliceScoreGraph psg;
        private final AURPoliceScenarioAnalyzer psa;
        private final AURClearWatcher cw;

        public AURRoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
                super(ai, wi, si, moduleManager, developData);
                this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning", "AUR.module.algorithm.AuraPathPlanning");
                this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering", "AUR.module.algorithm.AURWorldClusterer");
                
                this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
                this.wsg.calc();
                
                this.psg = moduleManager.getModule("aslan.PoliceScoreGraph","AUR.util.aslan.AURPoliceScoreGraph");
                this.psa = moduleManager.getModule("aslan.PoliceScenarioAnalyzer","AUR.util.aslan.AURPoliceScenarioAnalyzer");
                this.cw = moduleManager.getModule("aslan.PoliceClearWatcher","AUR.util.aslan.AURClearWatcher");
                registerModule(this.clustering);
                registerModule(this.psa);
                registerModule(this.cw);
                registerModule(this.psg);
                
                this.result = null;
        }

        @Override
        public RoadDetector updateInfo(MessageManager messageManager) {
                super.updateInfo(messageManager);
                wsg.updateInfo(messageManager);
                return this;
        }

        @Override
        public RoadDetector precompute(PrecomputeData precomputeData) {
                super.precompute(precomputeData);
                wsg.precompute(precomputeData);
                return this;
        }

        @Override
        public RoadDetector resume(PrecomputeData precomputeData) {
                super.resume(precomputeData);
                wsg.resume(precomputeData);
                return this;
        }

        EntityID lastTarget = null;
        @Override
        public RoadDetector calc() {
                wsg.calc();
                if(psa.isThereBlockadesInMap() == psa.NO){
                        lastTarget = null;
                        this.result = null;
                        return this;
                }
                        
                EntityID positionID = this.agentInfo.getPosition();
                StandardEntity currentPosition = worldInfo.getEntity(positionID);
                openedAreas.add((Area) currentPosition);
                
                EntityID targetID = psg.getAreaWithMaximumScore();

                if (wsg.getAreaGraph(targetID).getFinalScore() == 0 || targetID == null) {
                        return this;
                }
                List<EntityID> path = this.wsg.getNoBlockadePathToClosest(positionID, Lists.newArrayList(targetID));
                if (path != null && path.size() > 0) {
                        this.result = path.get(path.size() - 1);
                        
                        if(lastTarget == null){
                                psg.setTargetScore(result, AURConstants.RoadDetector.SecondaryScore.SELECTED_TARGET);
                        }
                        else if(! lastTarget.equals(result)){
                                psg.setTargetScore(lastTarget, - AURConstants.RoadDetector.SecondaryScore.SELECTED_TARGET);
                                psg.setTargetScore(result, AURConstants.RoadDetector.SecondaryScore.SELECTED_TARGET);
                        }
                        
                        lastTarget = this.result;
                }
                return this;
        }
        
        @Override
        public EntityID getTarget() {
                return this.result;
        }
}
