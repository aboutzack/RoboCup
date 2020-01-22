/*
package mrl_2019.complex;

import javolution.util.FastSet;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

*/
/**
 * @author Pooya Deldar Gohardani
 *//*



*/
/**
 * in this class check the agent is stuck or not, if agent try move to 50% of targets and failed them, agent is in stuck
 *//*

public class StuckChecker {
    private MrlWorldHelper world;
    private int stuckThreshold;
    private int randomCount = 10;
    Random random;

    private Blockade coveringBlockade;
    private StuckState stuckState = StuckState.FREE;
    private boolean isStuck = true; // consider every agent stuck otherwise it shown false
    private int idleTimes = 0;


    public StuckChecker(MrlWorldHelper world, int stuckThreshold, int randomCount) {
        this.stuckThreshold = stuckThreshold;
        this.world = world;
        this.random = new Random(System.currentTimeMillis());

    }

    */
/**
     * this function check agent is Stuck or not, and if agent is on blockage return true
     *
     * @return true if the agent is stuck false otherwise
     *//*

    public boolean amIStuck() {

        if (!(world.getSelfHuman() instanceof PoliceForce)) {


            coveringBlockade = Util.findCoveringBlockade(world, world.getSelfHuman());
            if (coveringBlockade != null) {
                stuckState = StuckState.TOTALLY_BLOCKED;

            } else if (isBlockedInARegion()) {
                stuckState = StuckState.BLOCKED_IN_REGION;
            }

            if (stuckState.equals(StuckState.TOTALLY_BLOCKED) || stuckState.equals(StuckState.BLOCKED_IN_REGION)) {
                isStuck = true;
                System.out.println("Time: " + world.getTime() + " id: " + world.getSelf().getID() + " I am blocked!");
            } else {
                isStuck = false;
            }
        }

        return isStuck;
    }

    public boolean isBlockedInARegion() {
        boolean isStuck = false;
        if (!(world.getSelfHuman() instanceof PoliceForce)) {
            Pair<Integer, Integer> location = world.getSelfLocation();
            List<EntityID> possibleTargets;
            possibleTargets = findPossibleTargets(location.first(), location.second());
            if (possibleTargets.size() > 0) {
                isStuck = checkStatus(chooseRandomTargets(possibleTargets));
            }

        }
        return isStuck;
    }


    public void updateStuckState(State agentState, boolean isWorking) {
        if (!(world.getSelfHuman() instanceof PoliceForce)) {

            if (stuckState.equals(StuckState.TOTALLY_BLOCKED)) {
                if (Util.isOnBlockade(world, world.getSelfHuman(), coveringBlockade)) {
                    //TODO: maybe, needs stuck agent message for more emphasis
                    stuckState = StuckState.TOTALLY_BLOCKED;
                    isStuck = true;
                } else {
                    stuckState = StuckState.FREE;
                    isStuck = false;
                }
            }

            if (stuckState.equals(StuckState.FREE)) {

                // Agent who can do nothing is probably blocked!
                if (!isWorking && !agentState.equals(State.THINKING) && !agentState.equals(State.CRASH) && !agentState.equals(State.BURIED)) {
                    idleTimes++;
                    System.out.println("Time: " + world.getTime() + " id: " + world.getSelf().getID() + "IDLE times:" + idleTimes);
                    if (idleTimes > 2) {
                        world.getPlatoonAgent().setAgentState(State.STUCK);
                        stuckState = StuckState.BLOCKED_IN_REGION;
//                world.getHelper(HumanHelper.class).setLockedByBlockade(world.getSelf().getID(), true);
                        System.out.println("Time: " + world.getTime() + " id: " + world.getSelf().getID() + " I can do nothing, I think I am blocked!");
                        idleTimes = 0;

                    }
                }
            }


        }
    }

    private Set<EntityID> chooseRandomTargets(List<EntityID> targets) {
        Set<EntityID> randomTargets = new FastSet<EntityID>();
        int rand;
        int count = 0;
        int tryCount = 1000;
        int minCount = Math.min(randomCount, targets.size());
        List<EntityID> tempTargetList = new ArrayList<EntityID>(targets);


        while (count < minCount) {
            rand = random.nextInt(tempTargetList.size());

//            if (randomTargets.contains(targets.get(rand))) {
            tryCount--;
            if (tryCount < 0) {
                System.err.println(" RIDIIIII " + world.getSelf() + " " + world.getTime());
            }
//                continue;
//            }

            randomTargets.add(tempTargetList.get(rand));
            tempTargetList.remove(rand);

            count++;
        }

        return randomTargets;
    }


    */
/**
     * this function create two circles, first is inner circle that centre is human(x,y) and radius viewDistance
     * second is outer circle that centre is human(x,y) and radius is viewDistance * 1.5
     *
     * @param x integer representing the x position of the agent in the map
     * @param y integer representing the x position of the agent in the map
     * @return
     *//*

    private List<EntityID> findPossibleTargets(int x, int y) {
        List<EntityID> targets = new ArrayList<EntityID>();
        int radius = world.getViewDistance();
        double minorCircle;
        double majorCircle;
        if (world.isMapHuge()) {
            radius *= 2;
        }
        minorCircle = radius * 2;
        majorCircle = radius * 2.5;
        Circle2D innerCircle = new Circle2D(new Point2D(x, y), minorCircle);
        Circle2D outerCircle = new Circle2D(new Point2D(x, y), majorCircle);

        targets = findPoints(innerCircle, outerCircle, majorCircle);
        return targets;
    }

    */
/**
     * findPoints functions find all of entities that in area between innerCircle and outerCircle
     *
     * @param innerCircle the Circle with agent(x, y) and viewDistance radius
     * @param outerCircle the Circle with agent(x, y) and 1.5 * viewDistance radius
     * @param majorCircle
     *//*

    private List<EntityID> findPoints(Circle2D innerCircle, Circle2D outerCircle, double majorCircle) {
        List<EntityID> targets = new ArrayList<EntityID>();
        Pair<Integer, Integer> location;

        for (StandardEntity se : world.getObjectsInRange(world.getSelfPosition(), (int) Math.ceil(majorCircle))) {
            location = se.getLocation(world);
            if (contains(outerCircle, location.first(), location.second())
                    && !contains(innerCircle, location.first(), location.second())) {
                targets.add(se.getID());
            }
        }
        return targets;
    }


    */
/**
     * In this function we send move act to all of @targets and
     * if more than stuckThreshold move act is failed, this human is in stuck else agent is free
     *
     * @param entityIDs , roads and buildings entityIds that choice random from targets
     * @return boolean true if the agent is stuck
     *//*

    private boolean checkStatus(Set<EntityID> entityIDs) {

        if (entityIDs == null || entityIDs.isEmpty()) {
            return false;
        }

        int unSuccessfulMove = 0;

        StandardEntity se;
        Area source = (Area) world.getSelfPosition();
        for (EntityID id : entityIDs) {
            se = world.getEntity(id);
            if (se instanceof Area) {
                if (world.getPlatoonAgent().getPathPlanner().planMove(source, (Area) se, 0, false).isEmpty()) {
                    unSuccessfulMove++;
                }
            }
        }


        if (((unSuccessfulMove / entityIDs.size()) * 100) > stuckThreshold) {
            return true;
        }
        return false;
    }

    private boolean contains(Circle2D circle2D, double x, double y) {
        double dx = circle2D.getCenter().getX() - x;
        double dy = circle2D.getCenter().getY() - y;

        return ((dx * dx) + (dy * dy)) < circle2D.getRadius() * circle2D.getRadius();
    }


    public StuckState getStuckState() {
        return stuckState;
    }

    public boolean isStuck() {
        return isStuck;
    }

    public void setStuckState(StuckState stuckState) {
        this.stuckState = stuckState;
    }

    public Blockade getCoveringBlockade() {
        return coveringBlockade;
    }


    public boolean amITotallyStuck() {

        if (!(world.getSelfHuman() instanceof PoliceForce)) {
            if (isStuck) {
                if (coveringBlockade == null) {
                    coveringBlockade = Util.findCoveringBlockade(world, world.getSelfHuman());
                    if (coveringBlockade != null) {
                        isStuck = true;
                    } else {
                        isStuck = false;
                    }
                } else {
                    isStuck = Util.isOnBlockade(world, world.getSelfHuman(), coveringBlockade);
                }

            }

        }

        return isStuck;

    }


    */
/**
     * This method determines weather the specified agent is blocked or not. For investigating the situation,<br/>
     * it first considers the first position of the agent and its current position, so if these two where the same,<br/>
     * the agent might be blocked yet otherwise this method will check the distance between current position of the<br/>
     * agent throw its fist position and if the agent is moved more than threshold T  then it is not blocked,<br/>
     * otherwise it might be blocked yet!<br/>
     * <br/>
     * <b>Note: </b> This method should be used, just when the agent is blocked!
     *
     * @param agentEntity Entity of the agent to check its blocking situation
     * @return True if the agent is blocked and False otherwise.
     *//*

    public boolean isBlocked(StandardEntity agentEntity) {

        Human human = (Human) agentEntity;
        boolean isBlocked;

        if (human instanceof PoliceForce) {
            isBlocked = false;
            if (world.getEntity(human.getPosition()) instanceof Road) {
                isBlocked = false;
            }
        } else if (human.getPosition().equals(world.getAgentFirstPositionMap().get(human.getID()))) {
            isBlocked = true;
        } else {
            //checking second condition
            int thresholdT = world.getViewDistance() * 2;
            if (world.isMapHuge()) {
                thresholdT += thresholdT;
            }
            int distance = world.getDistance(human.getPosition(), world.getAgentPositionMap().get(human.getID()));
            if (distance > thresholdT) {
                isBlocked = false;
            } else {
                isBlocked = true;
            }
        }

        return isBlocked;
    }
}
*/
