package mrl_2019.complex.firebrigade;

import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.MRLConstants;
import mrl_2019.util.MrlLineOfSightPerception;
import mrl_2019.util.Util;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.entity.MrlRoad;
import rescuecore2.worldmodel.EntityID;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/12/13
 * Time: 4:29 PM
 * Author: Mostafa Movahedi
 */

/**
 * A preprocessor to determine each area is visible from which areas and which areas are observable from specific area
 */
public class ProcessAreaVisibility {

    public static void process(MrlFireBrigadeWorld world, WorldInfo worldInfo, ScenarioInfo scenarioInfo, boolean isPrecompute) {
        long startTime = System.currentTimeMillis();

        String visibleFromFileName = MRLConstants.PRECOMPUTE_DIRECTORY + world.getMapName() + ".vif";
        String observableAreasFileName = MRLConstants.PRECOMPUTE_DIRECTORY + world.getMapName() + ".oba";
        String lineOfSightsNameFileName = MRLConstants.PRECOMPUTE_DIRECTORY + world.getMapName() + ".los";

        if (new File(visibleFromFileName).exists() && new File(observableAreasFileName).exists() && new File(lineOfSightsNameFileName).exists()) {
            Thread loader = new Thread() {
                @Override
                public void run() {
                    loadData(world, visibleFromFileName, observableAreasFileName, lineOfSightsNameFileName);
                }
            };
            loader.start();
        } else {
            FileEntityIDMap visibleFrom = new FileEntityIDMap();
            FileEntityIDMap observableAreas = new FileEntityIDMap();
            FileLineOfSight lineOfSights = new FileLineOfSight();

            MrlLineOfSightPerception lineOfSightPerception = new MrlLineOfSightPerception(worldInfo, scenarioInfo);

            Set<MrlRoad> allRoads = world.getMrlRoads();
            Set<MrlBuilding> allBuildings = world.getMrlBuildings();

            for (MrlRoad road : allRoads) {
                road.setObservableAreas(lineOfSightPerception.getVisibleAreas(road.getID()));
                road.setLineOfSight(lineOfSightPerception.getRays());
                observableAreas.put(road.getID().getValue(), Util.EIDListToIntegerList(road.getObservableAreas()));
                lineOfSights.put(road.getID().getValue(), road.getLineOfSight());
            }
            for (MrlBuilding building : allBuildings) {
                building.setObservableAreas(lineOfSightPerception.getVisibleAreas(building.getID()));
                building.setLineOfSight(lineOfSightPerception.getRays());
                observableAreas.put(building.getID().getValue(), Util.EIDListToIntegerList(building.getObservableAreas()));
                lineOfSights.put(building.getID().getValue(), building.getLineOfSight());
            }

            for (MrlRoad road1 : allRoads) {
                for (MrlRoad road2 : allRoads) {
                    if (road1.equals(road2)) continue;
                    if (road2.getObservableAreas().contains(road1.getID())) {
                        road1.getVisibleFrom().add(road2.getID());

                    }
                }
                for (MrlBuilding building : allBuildings) {
                    if (building.getObservableAreas().contains(road1.getID())) {
                        road1.getVisibleFrom().add(building.getID());
                    }
                }
                visibleFrom.put(road1.getID().getValue(), Util.EIDListToIntegerList(road1.getVisibleFrom()));
            }
            for (MrlBuilding building1 : allBuildings) {
                for (MrlBuilding building2 : allBuildings) {
                    if (building1.equals(building2)) continue;
                    if (building2.getObservableAreas().contains(building1.getID())) {
                        building1.getVisibleFrom().add(building2.getID());

                    }
                }
                for (MrlRoad road : allRoads) {
                    if (road.getObservableAreas().contains(building1.getID())) {
                        building1.getVisibleFrom().add(road.getID());
                    }
                }
                visibleFrom.put(building1.getID().getValue(), Util.EIDListToIntegerList(building1.getVisibleFrom()));
            }

            try {
                if (isPrecompute) {
                    Util.writeObject(observableAreas, observableAreasFileName);
                    Util.writeObject(visibleFrom, visibleFromFileName);
                    Util.writeObject(lineOfSights, lineOfSightsNameFileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        world.setProcessVisibilityDataLoaded(true);
//        MrlPersonalData.VIEWER_DATA.setAreaVisibility(world.getMrlRoads(), world.getMrlBuildings());

        long endTime = System.currentTimeMillis();
//        System.out.print(" ProcessAreaVisibility(" + (endTime - startTime) + ")");
    }

    private static void loadData(MrlFireBrigadeWorld world, String visibleFromFileName, String observableAreasFileName, String lineOfSightsNameFileName) {
        try {
            world.setProcessVisibilityDataLoaded(false);
            FileEntityIDMap visibleFrom = (FileEntityIDMap) Util.readObject(visibleFromFileName);
            FileEntityIDMap observableArea = (FileEntityIDMap) Util.readObject(observableAreasFileName);
            FileLineOfSight lineOfSight = (FileLineOfSight) Util.readObject(lineOfSightsNameFileName);
            for (Integer next : visibleFrom.keySet()) {
                MrlRoad road = world.getMrlRoad(new EntityID(next));
                if (road != null) {
                    road.setVisibleFrom(Util.IntegerListToEIDSet(visibleFrom.get(next)));
                } else {
                    MrlBuilding building = world.getMrlBuilding(new EntityID(next));
                    building.setVisibleFrom(Util.IntegerListToEIDSet(visibleFrom.get(next)));
                }
            }
            for (Integer next : observableArea.keySet()) {
                MrlRoad road = world.getMrlRoad(new EntityID(next));
                if (road != null) {
                    road.setObservableAreas(Util.IntegerListToEIDList(observableArea.get(next)));
                } else {
                    MrlBuilding building = world.getMrlBuilding(new EntityID(next));
                    building.setObservableAreas(Util.IntegerListToEIDList(observableArea.get(next)));
                }
            }
            for (Integer next : lineOfSight.keySet()) {
                MrlRoad road = world.getMrlRoad(new EntityID(next));
                if (road != null) {
                    road.setLineOfSight(lineOfSight.get(next));
                } else {
                    MrlBuilding building = world.getMrlBuilding(new EntityID(next));
                    building.setLineOfSight(lineOfSight.get(next));
                }
            }
            world.setProcessVisibilityDataLoaded(true);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
