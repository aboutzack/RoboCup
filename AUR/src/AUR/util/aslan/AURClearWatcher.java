package AUR.util.aslan;

import AUR.util.knd.AURConstants;
import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURRandomDirectSelector;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.police.ActionClear;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import com.google.common.collect.Lists;
import java.awt.Polygon;
import java.util.ArrayList;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Feb 2018
 */
public class AURClearWatcher extends AbstractModule {
        public final int MOVE = 1,
                         CLEAR = 2,
                         CLEAR_FROM_WATCHER = 3,
                         NULL = 4;
        
        private ArrayList<Integer> lastBlockadePList = null;
        private ArrayList<Integer> currentBlockadePList = null;
        private ArrayList<Blockade> currentBlockadeList = null;
        
        private double xCurrentPos = 0;
        private double yCurrentPos = 0;
        private double xLastPos = 0;
        private double yLastPos = 0;
        
        public double[] lastMoveVector = new double[2];
        public double[] lastMoveDirection = new double[2];
        
        public int lastAction;
        public int lastTime;
        public int currentTime;
        
        public int dontMoveCounter = 0;
        public int dontMoveWithMoveCounter = 0;
        
        AURRandomDirectSelector randomDirectSelector;

        public AURClearWatcher(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
                super(ai, wi, si, moduleManager, developData);
                randomDirectSelector = new AURRandomDirectSelector(ai, wi);
                
                this.lastAction = this.NULL;
                
                this.lastBlockadePList = new ArrayList<>();
                this.currentBlockadePList = new ArrayList<>();
                this.currentBlockadeList = new ArrayList<>();
        }
        
        public ArrayList<Integer> getBlockadeListPropertyList(ArrayList<Blockade> list){
                ArrayList<Integer> result = new ArrayList<>();
                for(Blockade b : list){
                        result.addAll(getBlockadePropertyList(b));
                }
                return result;
        }
        
        private ArrayList<Integer> getBlockadePropertyList(Blockade b){
                ArrayList<Integer> list = new ArrayList<>();
                for(int a : b.getApexes()){
                        list.add(a);
                }
                return list;
        }
        
        public void updateAgentInformations() {
                randomDirectSelector.update();
                
                this.xLastPos = this.xCurrentPos;
                this.yLastPos = this.yCurrentPos;
                this.xCurrentPos = this.agentInfo.getX();
                this.yCurrentPos = this.agentInfo.getY();
                this.lastTime = this.currentTime;
                this.currentTime = this.agentInfo.getTime();
                
                if(dontMoveCounter < 20 && isMoveLessThanAllowedValue()){
                        dontMoveCounter ++;
                }
                else{
                        dontMoveCounter = 0;
                }
                
                if(dontMoveWithMoveCounter < 10 && isMoveLessThanAllowedValue() &&
                   (lastAction == MOVE ||
                   lastAction == CLEAR_FROM_WATCHER)){
                        dontMoveWithMoveCounter ++;
                }
                else{
                        dontMoveWithMoveCounter = 0;
                }
        }
        
        public void setBlockadeList(ArrayList<Blockade> blockades){
                this.lastBlockadePList = (ArrayList<Integer>) currentBlockadePList.clone();
                this.currentBlockadePList = getBlockadeListPropertyList(blockades);
                this.currentBlockadeList = blockades;
        }
        
        public Action getAction(Action action){
                Action newAction;
                newAction = getNewAction(action);
                
                if(this.lastAction == CLEAR_FROM_WATCHER);
//                        System.out.println(" -> CLEAR_FROM_WATCHER");
                else if(newAction instanceof ActionClear){
//                        System.out.println(" -> CLEAR");
                        this.lastAction = this.CLEAR;
                }
                else if(newAction instanceof ActionMove){
                        ActionMove actionMove = (ActionMove)newAction;
                        
//                        System.out.println(" -> MOVE");
                        this.lastAction = this.MOVE;
                        if(((ActionMove)newAction).getUsePosition()){
                                this.lastMoveVector[0] = actionMove.getPosX() - agentInfo.getX();
                                this.lastMoveVector[1] = actionMove.getPosY() - agentInfo.getY();
                                if(! isMoveLessThanAllowedValue()){
                                        this.lastMoveDirection[0] = actionMove.getPosX() - agentInfo.getX();
                                        this.lastMoveDirection[1] = actionMove.getPosY() - agentInfo.getY();
                                }
                        }
                        else{
                                // Should Fill
                        }
                }
                
                return newAction;
        }
        
        private Action getNewAction(Action action){
                Action result = action;
//                System.out.println("Dont Move With Move Counter : " + dontMoveWithMoveCounter);
                
                if(dontMoveWithMoveCounter >= AURConstants.ClearWatcher.DONT_MOVE_COUNTER_LIMIT){
                        if(isAgentTrapedInBlockade() != null){
                                this.lastAction = CLEAR_FROM_WATCHER;
                                return new ActionClear(isAgentTrapedInBlockade());
                        }
                        else{
//                                System.out.println("Get Random Directed Move . . . ");
                                randomDirectSelector.generate();
                                return new ActionMove(
                                        Lists.newArrayList(agentInfo.getPosition()),
                                        (int) (randomDirectSelector.generatedPoint.getX()),
                                        (int) (randomDirectSelector.generatedPoint.getY())
                                );
                        }
                }
                else if(isMoveLessThanAllowedValue() &&
                        this.lastAction != CLEAR_FROM_WATCHER &&
                        currentBlockadeList != null &&
                        currentBlockadeList.size() > 0 &&
                        this.lastAction != this.NULL &&
                        lastBlockadePList.equals(currentBlockadePList)
                ){
                        this.lastAction = CLEAR_FROM_WATCHER;
                        return new ActionClear(AURPoliceUtil.getNearestBlockadeToAgentFromList(agentInfo, currentBlockadeList));
                }
                else if(dontMoveCounter > AURConstants.ClearWatcher.OLD_FUNCTION_CLEAR_COUNTER_LIMIT &&
                        isMoveLessThanAllowedValue() &&
                        lastAction == MOVE &&
                        agentInfo.getPositionArea().isBlockadesDefined() &&
                        ! agentInfo.getPositionArea().getBlockades().isEmpty()
                ){
                        this.lastAction = CLEAR_FROM_WATCHER;
                        return new ActionClear(AURPoliceUtil.getNearestBlockadeToAgentFromList(agentInfo,worldInfo, agentInfo.getPositionArea().getBlockades()));
                }
                else{
                        this.lastAction = this.NULL;
                }
                return result;
        }
        
        private boolean isMoveLessThanAllowedValue(){
                return AURGeoUtil.dist(xLastPos, yLastPos, xCurrentPos, yCurrentPos) < AURConstants.ClearWatcher.ALLOWED_MOVE_VALUE ;
        }
        
        private Blockade isAgentTrapedInBlockade(){
                Area positionArea = agentInfo.getPositionArea();
                if(!positionArea.isBlockadesDefined())
                        return null;
                
                Polygon circle = AURGeoTools.getCircle(new int[]{(int) agentInfo.getX(),(int) agentInfo.getY()}, AURConstants.Agent.RADIUS + 10);
                for(EntityID b : positionArea.getBlockades()){
                        Blockade blockade = (Blockade) worldInfo.getEntity(b);
                        if(AURGeoTools.intersect(blockade, circle) || circle.contains(blockade.getX(), blockade.getY()))
                                return blockade;
                }
                return null;
        }

//        private Action getDontMoveAction() {
//                Shape shape = agentInfo.getPositionArea().getShape();
//                double[] p, agent = new double[]{agentInfo.getX(), agentInfo.getY()};
//                do{
//                        p = AURGeoMetrics.getVectorScaled(AURGeoMetrics.getPointsPlus(
//                                agent,
//                                AURGeoTools.getNormalVectorWithRadian(2 * Math.PI * Math.random())
//                        ),AURConstants.Agent.RADIUS * 2);
//                }while(! shape.contains(p[0], p[1]));
//                
//                return new ActionMove(Lists.newArrayList(agentInfo.getPosition()), (int) p[0], (int) p[1]);
//        }

        @Override
        public AbstractModule calc() {
                return this;
        }
}