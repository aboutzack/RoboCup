package mrl_2019.complex.search;


import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Siavash
 */
public abstract class SearchStrategy {

    private static Log logger = LogFactory.getLog(SearchStrategy.class);

    private List<EntityID> visitedBuildings;
    protected List<Area> visitedAreas;
    protected Set<Area> blackList;
    protected MrlWorldHelper world;
    protected Path path;
    protected Building building;
    protected boolean searchInside;
    protected WorldInfo worldInfo;
    protected AgentInfo agentInfo;

    SearchStrategy(MrlWorldHelper world, WorldInfo worldInfo, AgentInfo agentInfo) {
        this.world = world;
        this.agentInfo = agentInfo;
        this.worldInfo = worldInfo;

        visitedBuildings = new ArrayList<>();
        visitedAreas = new ArrayList<>();
        blackList = new HashSet<>();
        searchInside = false;
        this.worldInfo = worldInfo;
    }

//    @Override
//    public SearchStatus manualMoveToArea(Area targetArea) throws CommandException {
//        if (targetArea != null && !world.getSelfPosition().equals(targetArea)) {
//            agent.move(targetArea, MRLConstants.IN_TARGET, false);
//            blackList.add(targetArea);
//        } else if (targetArea != null && world.getSelfPosition().equals(targetArea)) {
//            return SearchStatus.FINISHED;
//        }
//        return SearchStatus.CANCELED;
//    }

//    @Override
//    public SearchStatus manualMoveToRoad(Road targetRoad) throws CommandException {
//        if (targetRoad != null && !world.getSelfPosition().equals(targetRoad)) {
//            agent.move(targetRoad, MRLConstants.IN_TARGET, false);
//            blackList.add(targetRoad);
//        } else if (targetRoad != null && world.getSelfPosition().equals(targetRoad)) {
//            return SearchStatus.FINISHED;
//        }
//        return SearchStatus.CANCELED;
//    }


    /**
     * creates visited building message and adds visited buildings to {@code visitedBuildings}.
     */
    public void updateVisitedBuildings() {
//        if (world.getSelfPosition() instanceof Building) {
//
//            Building visitedBuild = (Building) world.getSelfPosition();
//            if (!visitedBuildings.contains(visitedBuild.getID())) {
//                visitedBuildings.add(visitedBuild.getID());
//            }
//            if (world.getUnvisitedBuildings().contains(visitedBuild.getID())) {
//                world.getUnvisitedBuildings().remove(visitedBuild.getID());
//            }
//        }
    }

    /**
     * adds {@code visited} to visited buildings and removes it from unvisited buildings and sends the proper message.
     *
     * @param visited visited building
     */
    public void updateVisitedBuildings(Building visited) {
//        if (!visitedBuildings.contains(visited.getID())) {
//            visitedBuildings.add(visited.getID());
//        }
//
//        if(world.getUnvisitedBuildings() != null && visited != null){
//            world.getUnvisitedBuildings().remove(visited.getID());
//            world.getVisitedBuildings().add(visited.getID());
//            MrlBuilding mrlBuilding = world.getMrlBuilding(visited.getID());
//            mrlBuilding.setVisited();
//        }

    }

    public void addVisitedArea(Area visitedArea) {
        if (!visitedAreas.contains(visitedArea)) {
            visitedAreas.add(visitedArea);
        }
    }

    /**
     * Search inside buildings
     *
     * @param searchInside true if you want to search inside the buildings.
     */
    public void setSearchInside(boolean searchInside) {
        this.searchInside = searchInside;
    }

    public void setVisitedBuildings(List<EntityID> visitedBuildings) {
        this.visitedBuildings = visitedBuildings;
    }
}
