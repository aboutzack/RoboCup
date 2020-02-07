package adf.sample.extaction;
//
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.police.ActionClear;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import com.google.common.collect.Lists;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.stream.Collectors;

public class ActionExtClear extends ExtAction
{
    private PathPlanning pathPlanning;

  //清理距离
    private int clearDistance;	
  //被迫的行动
    private int forcedMove;		
  //需要休息时的阈值
    private int thresholdRest;	
  //需要休息时的阈值
    private int kernelTime;		
  //目标
    private EntityID target;	
    private Map<EntityID, Set<Point2D>> movePointCache;
  //清理点
    private int oldClearX;		
    private int oldClearY;
    private int count;

    public ActionExtClear(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)    //构造函数
    {
        super(ai, wi, si, moduleManager, developData);
        this.clearDistance = si.getClearRepairDistance();
        this.forcedMove = developData.getInteger("ActionExtClear.forcedMove", 3);
        this.thresholdRest = developData.getInteger("ActionExtClear.rest", 100);

        this.target = null;
        this.movePointCache = new HashMap<>();
        this.oldClearX = 0;
        this.oldClearY = 0;
        this.count = 0;

        switch (si.getMode())
        {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
    }

    @Override 
    //数据预处理
    public ExtAction precompute(PrecomputeData precomputeData)  
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        try
        {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override  
    //重新开始
    public ExtAction resume(PrecomputeData precomputeData)  
    {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        try
        {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override  
    //预备
    public ExtAction preparate()   
    {
        super.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        this.pathPlanning.preparate();
        try
        {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override 
    //这个time的更新
    public ExtAction updateInfo(MessageManager messageManager)     
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        return this;
    }

    @Override 
    //目标设定
    public ExtAction setTarget(EntityID target)    
    {
        this.target = null;
        StandardEntity entity = this.worldInfo.getEntity(target);
        if (entity != null)
        {
            if (entity instanceof Road)
            {
                this.target = target;
            }
            else if (entity.getStandardURN().equals(StandardEntityURN.BLOCKADE))
            {
                this.target = ((Blockade) entity).getPosition();
            }
            else if (entity instanceof Building)
            {
                this.target = target;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc()
    {
        this.result = null;
        PoliceForce policeForce = (PoliceForce) this.agentInfo.me();
        //需要休息
        if (this.needRest(policeForce))   
        {
            List<EntityID> list = new ArrayList<>();
            if (this.target != null)
            {
            	//将这个target加入list
                list.add(this.target);		
            }
          //调用calcrest去休息
            this.result = this.calcRest(policeForce, this.pathPlanning, list);		
            if (this.result != null)
            {
                return this;
            }
        }

        if (this.target == null)    
        {
            return this;
        }
        EntityID agentPosition = policeForce.getPosition();
        StandardEntity targetEntity = this.worldInfo.getEntity(this.target);
        StandardEntity positionEntity = Objects.requireNonNull(this.worldInfo.getEntity(agentPosition));
        if (targetEntity == null || !(targetEntity instanceof Area))	
        {
            return this;
        }
      //警察在broadcast的路上
        if (positionEntity instanceof Road)		
        {
            this.result = this.getRescueAction(policeForce, (Road) positionEntity);
            if (this.result != null)
            {
                return this;
            }
        }
        //到达target开始clear
        if (agentPosition.equals(this.target))
        {
            this.result = this.getAreaClearAction(policeForce, targetEntity);
        }
      //target在police旁
        else if (((Area) targetEntity).getEdgeTo(agentPosition) != null)	
        {
            this.result = this.getNeighbourPositionAction(policeForce, (Area) targetEntity);
        }
        else
        {
            List<EntityID> path = this.pathPlanning.getResult(agentPosition, this.target);
            if (path != null && path.size() > 0)
            {
                int index = path.indexOf(agentPosition);
                if (index == -1)	
                {
                    Area area = (Area) positionEntity;
                    for (int i = 0; i < path.size(); i++)
                    {
                        if (area.getEdgeTo(path.get(i)) != null)
                        {
                            index = i;
                            break;
                        }
                    }
                }
                else if (index >= 0)
                {
                    index++;
                }
                if (index >= 0 && index < (path.size()))
                {
                    StandardEntity entity = this.worldInfo.getEntity(path.get(index));
                    this.result = this.getNeighbourPositionAction(policeForce, (Area) entity);
                    if (this.result != null && this.result.getClass() == ActionMove.class)
                    {
                    	//没有可用的路线
                        if (!((ActionMove) this.result).getUsePosition())
                        {
                            this.result = null;
                        }
                    }
                }
                if (this.result == null)
                {
                    this.result = new ActionMove(path);
                }
            }
        }
        return this;
    }
    //救人
    private Action getRescueAction(PoliceForce police, Road road)  
    {//道路畅通
        if (!road.isBlockadesDefined())		
        {
            return null;
        }
      //道路畅通
        Collection<Blockade> blockades = this.worldInfo.getBlockades(road)		
                .stream()
                .filter(Blockade::isApexesDefined)
                .collect(Collectors.toSet());
      //agent
        Collection<StandardEntity> agents = this.worldInfo.getEntitiesOfType(	
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_BRIGADE
        );
        //警察位置XY
        double policeX = police.getX();
        double policeY = police.getY();
        double minDistance = Double.MAX_VALUE;
        Action moveAction = null;
        for (StandardEntity entity : agents)
        {
            Human human = (Human) entity;
          //平民位置不确定或者平民不在畅通的路上
            if (!human.isPositionDefined() || human.getPosition().getValue() != road.getID().getValue())	
            {
                continue;
            }
            //获取平民位置
            double humanX = human.getX();
            double humanY = human.getY();
            ActionClear actionClear = null;
            for (Blockade blockade : blockades)
            {
            	//human不在blockade中
                if (!this.isInside(humanX, humanY, blockade.getApexes()))		
                {
                    continue;
                }
              //警察到平民的距离
                double distance = this.getDistance(policeX, policeY, humanX, humanY);
              //human在路上
                if (this.intersect(policeX, policeY, humanX, humanY, road))
                {
                    Action action = this.getIntersectEdgeAction(policeX, policeY, humanX, humanY, road);
                    if (action == null)
                    {
                        continue;
                    }
                    if (action.getClass() == ActionClear.class)
                    {
                        if (actionClear == null)
                        {
                            actionClear = (ActionClear) action;
                            continue;
                        }
                        if (actionClear.getTarget() != null)
                        {
                            Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                        	//target存在且与地图中的blockade相交
                            if (another != null && this.intersect(blockade, another))	
                            {
                                return new ActionClear(another);
                            }
                            int anotherDistance = this.worldInfo.getDistance(police, another);
                            int blockadeDistance = this.worldInfo.getDistance(police, blockade);
                          //警察到目标blockade更远
                            if (anotherDistance > blockadeDistance)		
                            {
                                return action;
                            }
                        }
                        return actionClear;
                    }
                    else if (action.getClass() == ActionMove.class && distance < minDistance)
                    {
                        minDistance = distance;
                        moveAction = action;
                    }
                }
              //human与police间有blockade
                else if (this.intersect(policeX, policeY, humanX, humanY, blockade))	
                {
                    Vector2D vector = this.scaleClear(this.getVector(policeX, policeY, humanX, humanY));
                    int clearX = (int) (policeX + vector.getX());
                    int clearY = (int) (policeY + vector.getY());
                    vector = this.scaleBackClear(vector);
                    int startX = (int) (policeX + vector.getX());
                    int startY = (int) (policeY + vector.getY());
                    if (this.intersect(startX, startY, clearX, clearY, blockade))
                    {
                        if (actionClear == null)
                        {
                            actionClear = new ActionClear(clearX, clearY, blockade);
                        }
                        else
                        {
                        	//选择target与blockade中近的
                            if (actionClear.getTarget() != null)		
                            {
                                Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                                if (another != null && this.intersect(blockade, another))
                                {
                                    return new ActionClear(another);
                                }
                                int distance1 = this.worldInfo.getDistance(police, another);
                                int distance2 = this.worldInfo.getDistance(police, blockade);
                                if (distance1 > distance2)
                                {
                                    return new ActionClear(clearX, clearY, blockade);
                                }
                            }
                            return actionClear;
                        }
                    }
                  //试着往human那里走
                    else if (distance < minDistance)	
                    {
                        minDistance = distance;
                        moveAction = new ActionMove(Lists.newArrayList(road.getID()), (int) humanX, (int) humanY);
                    }
                }
            }
            if (actionClear != null)
            {
                return actionClear;
            }
        }
        return moveAction;
    }

    private Action getAreaClearAction(PoliceForce police, StandardEntity targetEntity)
    {
    	//目标是建筑则无行动
        if (targetEntity instanceof Building)	
        {
            return null;
        }
        Road road = (Road) targetEntity;
      //路上无障碍则不行动
        if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())	
        {
            return null;
        }
        Collection<Blockade> blockades = this.worldInfo.getBlockades(road)
                .stream()
                .filter(Blockade::isApexesDefined)
                .collect(Collectors.toSet());
        int minDistance = Integer.MAX_VALUE;
        Blockade clearBlockade = null;
        for (Blockade blockade : blockades)
        {
            for (Blockade another : blockades)
            {
            	//两块blockadeID不同且不相交
                if (!blockade.getID().equals(another.getID()) && this.intersect(blockade, another))		
                {
                    int distance1 = this.worldInfo.getDistance(police, blockade);
                    int distance2 = this.worldInfo.getDistance(police, another);
                  //选择近的blockade进行清理
                    if (distance1 <= distance2 && distance1 < minDistance)		
                    {
                        minDistance = distance1;
                        clearBlockade = blockade;
                    }
                    else if (distance2 < minDistance)
                    {
                        minDistance = distance2;
                        clearBlockade = another;
                    }
                }
            }
        }
        if (clearBlockade != null)		
        {
        	//离blockade距离小于清理距离
            if (minDistance < this.clearDistance)
            {
                return new ActionClear(clearBlockade);
            }
            else
            {
            	//到blockade旁边
                return new ActionMove(	
                        Lists.newArrayList(police.getPosition()),
                        clearBlockade.getX(),
                        clearBlockade.getY()
                );
            }
        }
        double agentX = police.getX();
        double agentY = police.getY();
        clearBlockade = null;
        Double minPointDistance = Double.MAX_VALUE;
        int clearX = 0;
        int clearY = 0;
      //选择离agent最近的blockade
        for (Blockade blockade : blockades)		
        {
            int[] apexes = blockade.getApexes();
            for (int i = 0; i < (apexes.length - 2); i += 2)
            {
                double distance = this.getDistance(agentX, agentY, apexes[i], apexes[i + 1]);
                if (distance < minPointDistance)		
                {
                    clearBlockade = blockade;
                    minPointDistance = distance;
                    clearX = apexes[i];
                    clearY = apexes[i + 1];
                }
            }
        }
        if (clearBlockade != null)
        {
        	//距离够就开始清理，否则再走一走
            if (minPointDistance < this.clearDistance)		
            {
                Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
                clearX = (int) (agentX + vector.getX());
                clearY = (int) (agentY + vector.getY());
                return new ActionClear(clearX, clearY, clearBlockade);
            }
            return new ActionMove(Lists.newArrayList(police.getPosition()), clearX, clearY);
        }
        return null;
    }

    private Action getNeighbourPositionAction(PoliceForce police, Area target)
    {
        double agentX = police.getX();
        double agentY = police.getY();
      //broadcast的position
        StandardEntity position = Objects.requireNonNull(this.worldInfo.getPosition(police));		
        Edge edge = target.getEdgeTo(position.getID());
        if (edge == null)
        {
            return null;
        }
    	//警察在广播的路上
        if (position instanceof Road)	
        {
            Road road = (Road) position;
          //有路障
            if (road.isBlockadesDefined() && road.getBlockades().size() > 0)	
            {
                double midX = (edge.getStartX() + edge.getEndX()) / 2;
                double midY = (edge.getStartY() + edge.getEndY()) / 2;
                if (this.intersect(agentX, agentY, midX, midY, road))
                {
                    return this.getIntersectEdgeAction(agentX, agentY, edge, road);
                }
                ActionClear actionClear = null;
                ActionMove actionMove = null;
                Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, midX, midY));
                int clearX = (int) (agentX + vector.getX());
                int clearY = (int) (agentY + vector.getY());
                vector = this.scaleBackClear(vector);
                int startX = (int) (agentX + vector.getX());
                int startY = (int) (agentY + vector.getY());
                for (Blockade blockade : this.worldInfo.getBlockades(road))
                {
                    if (blockade == null || !blockade.isApexesDefined())
                    {
                        continue;
                    }
                    if (this.intersect(startX, startY, midX, midY, blockade))
                    {
                        if (this.intersect(startX, startY, clearX, clearY, blockade))
                        {
                            if (actionClear == null)
                            {
                                actionClear = new ActionClear(clearX, clearY, blockade);
                              //原始点在target中心附近
                                if (this.equalsPoint(this.oldClearX, this.oldClearY, clearX, clearY))	
                                {
                                    if (this.count >= this.forcedMove)
                                    {
                                        this.count = 0;
                                      //去target中心
                                        return new ActionMove(Lists.newArrayList(road.getID()), clearX, clearY);	
                                    }
                                    this.count++;
                                }
                                this.oldClearX = clearX;
                                this.oldClearY = clearY;
                            }
                            else
                            {
                            	//比较target与现有blockade
                                if (actionClear.getTarget() != null)	
                                {
                                    Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                                    if (another != null && this.intersect(blockade, another))
                                    {
                                        return new ActionClear(another);
                                    }
                                }
                                return actionClear;
                            }
                        }
                      //去road中间
                        else if (actionMove == null)	
                        {
                            actionMove = new ActionMove(Lists.newArrayList(road.getID()), (int) midX, (int) midY);
                        }
                    }
                }
                if (actionClear != null)
                {
                    return actionClear;
                }
                else if (actionMove != null)
                {
                    return actionMove;
                }
            }
        }
      //target在广播的路上
        if (target instanceof Road)		
        {
            Road road = (Road) target;
            if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())
            {
                return new ActionMove(Lists.newArrayList(position.getID(), target.getID()));
            }
            Blockade clearBlockade = null;
            Double minPointDistance = Double.MAX_VALUE;
            int clearX = 0;
            int clearY = 0;
          //road上blockade的ID
            for (EntityID id : road.getBlockades())		
            {
                Blockade blockade = (Blockade) this.worldInfo.getEntity(id);
                if (blockade != null && blockade.isApexesDefined())
                {
                    int[] apexes = blockade.getApexes();
                  //找最近的blockade
                    for (int i = 0; i < (apexes.length - 2); i += 2)	
                    {
                        double distance = this.getDistance(agentX, agentY, apexes[i], apexes[i + 1]);
                        if (distance < minPointDistance)
                        {
                            clearBlockade = blockade;
                            minPointDistance = distance;
                            clearX = apexes[i];
                            clearY = apexes[i + 1];
                        }
                    }
                }
            }
          //清路
            if (clearBlockade != null && minPointDistance < this.clearDistance)		
            {
                Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
                clearX = (int) (agentX + vector.getX());
                clearY = (int) (agentY + vector.getY());
                if (this.equalsPoint(this.oldClearX, this.oldClearY, clearX, clearY))
                {
                    if (this.count >= this.forcedMove)
                    {
                        this.count = 0;
                        return new ActionMove(Lists.newArrayList(road.getID()), clearX, clearY);
                    }
                    this.count++;
                }
                this.oldClearX = clearX;
                this.oldClearY = clearY;
                return new ActionClear(clearX, clearY, clearBlockade);
            }
        }
      //去target
        return new ActionMove(Lists.newArrayList(position.getID(), target.getID()));		
    }

    private Action getIntersectEdgeAction(double agentX, double agentY, Edge edge, Road road)
    {
        double midX = (edge.getStartX() + edge.getEndX()) / 2;
        double midY = (edge.getStartY() + edge.getEndY()) / 2;
        return this.getIntersectEdgeAction(agentX, agentY, midX, midY, road);
    }
  //agent找路上最近的方式到达Point
    private Action getIntersectEdgeAction(double agentX, double agentY, double pointX, double pointY, Road road)	
    {
        Set<Point2D> movePoints = this.getMovePoints(road);
        Point2D bestPoint = null;
        double bastDistance = Double.MAX_VALUE;
      //找出离point最近的movePoint并计算距离
        for (Point2D p : movePoints)		
        {
            if (!this.intersect(agentX, agentY, p.getX(), p.getY(), road))
            {
                if (!this.intersect(pointX, pointY, p.getX(), p.getY(), road))
                {
                    double distance = this.getDistance(pointX, pointY, p.getX(), p.getY());
                    if (distance < bastDistance)
                    {
                        bestPoint = p;
                        bastDistance = distance;
                    }
                }
            }
        }
        if (bestPoint != null)
        {
            double pX = bestPoint.getX();
            double pY = bestPoint.getY();
        	//road可以走
            if (!road.isBlockadesDefined())	
            {
                return new ActionMove(Lists.newArrayList(road.getID()), (int) pX, (int) pY);
            }
            ActionClear actionClear = null;
            ActionMove actionMove = null;
            //获取agent点到P点的向量
            Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, pX, pY));
        	//结束clean位置
            int clearX = (int) (agentX + vector.getX());	
            int clearY = (int) (agentY + vector.getY());
            vector = this.scaleBackClear(vector);
        	//开始clean位置
            int startX = (int) (agentX + vector.getX());	
            int startY = (int) (agentY + vector.getY());
            for (Blockade blockade : this.worldInfo.getBlockades(road))
            {
            	//开始clean位置与P点间有blockade
                if (this.intersect(startX, startY, pX, pY, blockade))		
                {
                	//开始clean位置与结束clean位置间有blockade
                    if (this.intersect(startX, startY, clearX, clearY, blockade))	
                    {
                    	//没有target就从这里开始clean
                        if (actionClear == null)	
                        {
                            actionClear = new ActionClear(clearX, clearY, blockade);	
                        }
                        else
                        {
                        	//有target
                            if (actionClear.getTarget() != null)		
                            {
                                Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
                               //目标清理区域有blockade并且与眼前的blockade相交
                                if (another != null && this.intersect(blockade, another)) 	
                                {
                                    return new ActionClear(another);
                                }
                            }
                            return actionClear;
                        }
                    }
                  //开始clean位置与结束clean位置间没有blockade就move到P点
                    else if (actionMove == null)	
                    {
                        actionMove = new ActionMove(Lists.newArrayList(road.getID()), (int) pX, (int) pY);
                    }
                }
            }
            if (actionClear != null)
            {
                return actionClear;
            }
            else if (actionMove != null)
            {
                return actionMove;
            }
        }
        Action action = this.getAreaClearAction((PoliceForce) this.agentInfo.me(), road);
        if (action == null)
        {
        	//去Point所在位置
            action = new ActionMove(Lists.newArrayList(road.getID()), (int) pointX, (int) pointY);	
        }
        return action;
    }

    private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y)
    {
        return this.equalsPoint(p1X, p1Y, p2X, p2Y, 1000.0D);
    }
  //P1是否位于以P2为中心range*2为边长的正方形内
    private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y, double range)	
    {
        return (p2X - range < p1X && p1X < p2X + range) && (p2Y - range < p1Y && p1Y < p2Y + range);
    }
  //P是否在road内（apex[]为road各顶点坐标数据）
    private boolean isInside(double pX, double pY, int[] apex)		
    {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);

        for (int i = 0; i < apex.length - 2; i += 2)
        {
        	//apex[i]-p.x  apex[i+1]-p.y
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);		
          //apex[i+2]-p.x  apex[i+3]-p.y
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);	
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }
  //判断是否有agent或者point位于area内
    private boolean intersect(double agentX, double agentY, double pointX, double pointY, Area area)	
    {
        for (Edge edge : area.getEdges())
        {
            double startX = edge.getStartX();
            double startY = edge.getStartY();
            double endX = edge.getEndX();
            double endY = edge.getEndY();
          //agent到point的连线是否被area截断
            if (java.awt.geom.Line2D.linesIntersect(	
                    agentX, agentY, pointX, pointY,
                    startX, startY, endX, endY
            ))
            {	//获取area的中心位置
                double midX = (edge.getStartX() + edge.getEndX()) / 2;	
                double midY = (edge.getStartY() + edge.getEndY()) / 2;
                if (!equalsPoint(pointX, pointY, midX, midY) && !equalsPoint(agentX, agentY, midX, midY))
                {
                    return true;
                }
            }
        }
        return false;
    }
  //判断blockade是否有相交
    private boolean intersect(Blockade blockade, Blockade another)		
    {
        if (blockade.isApexesDefined() && another.isApexesDefined())
        {
            int[] apexes0 = blockade.getApexes();
            int[] apexes1 = another.getApexes();
            for (int i = 0; i < (apexes0.length - 2); i += 2)
            {
                for (int j = 0; j < (apexes1.length - 2); j += 2)
                {
                    if (java.awt.geom.Line2D.linesIntersect(
                    		//x1,y1,x2,y2
                            apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],		
                            //x3,y3,x4,y4
                            apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3]		
                    ))
                    {
                        return true;
                    }
                }
            }
            for (int i = 0; i < (apexes0.length - 2); i += 2)
            {
                if (java.awt.geom.Line2D.linesIntersect(
                        apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                        apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1]
                ))
                {
                    return true;
                }
            }
            for (int j = 0; j < (apexes1.length - 2); j += 2)
            {
                if (java.awt.geom.Line2D.linesIntersect(
                        apexes0[apexes0.length - 2], apexes0[apexes0.length - 1], apexes0[0], apexes0[1],
                        apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3]
                ))
                {
                    return true;
                }
            }
        }
        return false;
    }
  //判断是否有agent和Point间是否有blockade  
    private boolean intersect(double agentX, double agentY, double pointX, double pointY, Blockade blockade)	
    {
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(blockade.getApexes()), true);
        for (Line2D line : lines)
        {
            Point2D start = line.getOrigin();
            Point2D end = line.getEndPoint();
            double startX = start.getX();
            double startY = start.getY();
            double endX = end.getX();
            double endY = end.getY();
            if (java.awt.geom.Line2D.linesIntersect(
                    agentX, agentY, pointX, pointY,
                    startX, startY, endX, endY
            ))
            {
                return true;
            }
        }
        return false;
    }
  //获取X到Y的距离
    private double getDistance(double fromX, double fromY, double toX, double toY)		
    {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return Math.hypot(dx, dy);
    }
  //v1和v2两向量的夹角余弦值（起点在原点）
    private double getAngle(Vector2D v1, Vector2D v2)	
    {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if (flag > 0)
        {
            return angle;
        }
        if (flag < 0)
        {
            return -1 * angle;
        }
        return 0.0D;
    }
  //返回from到to的△X与△Y
    private Vector2D getVector(double fromX, double fromY, double toX, double toY)		
    {
        return (new Point2D(toX, toY)).minus(new Point2D(fromX, fromY));
    }
  //要清理多大一块路
    private Vector2D scaleClear(Vector2D vector)		
    {
        return vector.normalised().scale(this.clearDistance);
    }

    private Vector2D scaleBackClear(Vector2D vector)
    {
        return vector.normalised().scale(-510);
    }

    private Set<Point2D> getMovePoints(Road road)
    {
        Set<Point2D> points = this.movePointCache.get(road.getID());		
        if (points == null)
        {
            points = new HashSet<>();
            int[] apex = road.getApexList();
          //点加入set中
            for (int i = 0; i < apex.length; i += 2)		
            {
                for (int j = i + 2; j < apex.length; j += 2)
                {
                    double midX = (apex[i] + apex[j]) / 2;
                    double midY = (apex[i + 1] + apex[j + 1]) / 2;
                    if (this.isInside(midX, midY, apex))
                    {
                        points.add(new Point2D(midX, midY));
                    }
                }
            }
          //删除边缘中心的点
            for (Edge edge : road.getEdges())		
            {
                double midX = (edge.getStartX() + edge.getEndX()) / 2;
                double midY = (edge.getStartY() + edge.getEndY()) / 2;
                points.remove(new Point2D(midX, midY));
            }
            this.movePointCache.put(road.getID(), points);
        }
        return points;
    }
  //累了吗？
    private boolean needRest(Human agent)		
    {
        int hp = agent.getHP();
        int damage = agent.getDamage();
      //还阔以坚持
        if (damage == 0 || hp == 0)
        {
            return false;		
        }
      //但只能坚持这么久了
        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);		
        if (this.kernelTime == -1)
        {
            try
            {
                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
            }
            catch (NoSuchConfigOptionException e)
            {
                this.kernelTime = -1;
            }
        }
      //要休息吗
        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;	
    }
  //要去refuge休息了
    private Action calcRest(Human human, PathPlanning pathPlanning, Collection<EntityID> targets)		
    {
        EntityID position = human.getPosition();
        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
        int currentSize = refuges.size();
        if (refuges.contains(position))
        {
            return new ActionRest();
        }
        List<EntityID> firstResult = null;
        while (refuges.size() > 0)
        {
            pathPlanning.setFrom(position);
            pathPlanning.setDestination(refuges);
            List<EntityID> path = pathPlanning.calc().getResult();
            if (path != null && path.size() > 0)
            {
                if (firstResult == null)
                {
                    firstResult = new ArrayList<>(path);
                  //没有target并且找到了refuge
                    if (targets == null || targets.isEmpty())		
                    {
                        break;
                    }
                }
                EntityID refugeID = path.get(path.size() - 1);
                pathPlanning.setFrom(refugeID);		
              //有target就去target
                pathPlanning.setDestination(targets);
                List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
              //有一条从position经过refuge到target的路
                if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0)		
                {
                    return new ActionMove(path);
                }
                //不去这个refuge了，换一个
                refuges.remove(refugeID);
                //remove failed
                if (currentSize == refuges.size())
                {
                    break;
                }
                currentSize = refuges.size();
            }	
          //找不到路，原地等死
            else
            {
                break;
            }
        }
        return firstResult != null ? new ActionMove(firstResult) : null;
    }
}
