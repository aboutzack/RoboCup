package mrl_2019.complex.search;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;
import java.util.Set;

/**
 * <p> Find minimum cost for search buildings with Maximal Covering algorithm.</p>
 * <p>
 * raveshe ejraye algorithm: <br/>
 * dar ebteda yek matrix az building ha v area ha ijad mikonim ke dar aan ghable didan budane building az har area moshakhas shode ast. <br/>
 * marhale aval: tamamie builsind haye khas ra moshakhas mikonim va building haei ke in building zir majmueye anhast ra hazf mikonim
 * (<i>b1 subset b2 = remove b2 and b1 is special building</i>)* va yek matrix-e jadid ba building-haye baghimande va hameye roas-ha ijad mikonim. <br/>
 * *<i>dalile entakhab building-haye khas in ast ke motmaen shavim ke hatman anha ra mibinim.</i> <br/>
 * dar edame va dar matrix-e jadid tamamie area-haei ke zir majmueye area-haye digar hastand ra hazf mikonim. be in dalil ke mikhahim kamtarin te'dade area ra daghte bashim. <br/>
 * dar enteha tamamie area-haye mande makanhaei hastand ke baraye didane tamamie building-ha niaz darim be anha beravim. <br/>
 * ma baraye inke be natijeye behtari beresim in kar ra chandin bar tekrar mikonim ta zamani ke be hade aghale momken beresad(<i>ta zamani ke meghdari baraye hazf vojud nadashte bashad</i>). <br/>
 * </p>
 * <p>
 * Created by Mostafa Shabani.
 * User: MRL
 * Date: 11/28/13
 * Time: 11:47 AM
 * </p>
 *
 * @Author: Mostafa Shabani
 */
public class MaximalCovering {
    public MaximalCovering(MrlWorldHelper world) {
        this.world = world;
    }

    MrlWorldHelper world;

    /**
     * main method for find maximal covering.
     *
     * @param buildings buildings for find covering.
     * @return a set of target areas Id
     */
    public Set<EntityID> findMaximalCovering(Set<MrlBuilding> buildings) {
        Map<EntityID, Set<MrlBuilding>> areasMap = new FastMap<EntityID, Set<MrlBuilding>>();
        Map<MrlBuilding, Set<EntityID>> buildingsMap = new FastMap<MrlBuilding, Set<EntityID>>();

//        MrlPersonalData.VIEWER_DATA.setExploreBuildings(world.getSelf().getID(), new FastSet<MrlBuilding>(buildings));

        // fill buildings and possible areas map
        for (MrlBuilding building : buildings) {
            buildingsMap.put(building, new FastSet<EntityID>(building.getVisibleFrom()));
            for (EntityID id : building.getVisibleFrom()) {
                Set<MrlBuilding> bs = areasMap.get(id);
                if (bs == null) {
                    bs = new FastSet<MrlBuilding>();
                }
                bs.add(building);
                areasMap.put(id, bs);
            }
        }

        // call maximal covering method
        Set<EntityID> areas = processMatrix(buildingsMap, areasMap);

//        MrlPersonalData.VIEWER_DATA.setExplorePositions(world.getSelf().getID(), areas, world);

        return areas;
    }

    private Set<EntityID> processMatrix(Map<MrlBuilding, Set<EntityID>> buildingsMap, Map<EntityID, Set<MrlBuilding>> areasMap) {

        //step one
        Set<MrlBuilding> buildingsToRemove = new FastSet<MrlBuilding>();
        int i = 0, j;
        for (MrlBuilding building1 : buildingsMap.keySet()) {
            j = 0;
            if (!buildingsToRemove.contains(building1)) {
                for (MrlBuilding building2 : buildingsMap.keySet()) {
                    if (i > j++ || building1.equals(building2) || buildingsToRemove.contains(building2)) { //continue;
                    } else if (buildingsMap.get(building1).containsAll(buildingsMap.get(building2))) {
                        buildingsToRemove.add(building1);
                    } else if (buildingsMap.get(building2).containsAll(buildingsMap.get(building1))) {
                        buildingsToRemove.add(building2);
                    }
                }
            }
            i++;
        }
        for (MrlBuilding b : buildingsToRemove) {
            buildingsMap.remove(b);
            for (Set<MrlBuilding> bs : areasMap.values()) {
                bs.remove(b);
            }
        }

        // step two
        i = 0;
        Set<EntityID> areasToRemove = new FastSet<EntityID>();
        for (EntityID area1 : areasMap.keySet()) {
            j = 0;
            if (!areasToRemove.contains(area1)) {
                for (EntityID area2 : areasMap.keySet()) {
                    if (i > j++ || area1.equals(area2) || areasToRemove.contains(area2)) { //continue;
                    } else if (areasMap.get(area1).containsAll(areasMap.get(area2))) {
                        areasToRemove.add(area2);
                    } else if (areasMap.get(area2).containsAll(areasMap.get(area1))) {
                        areasToRemove.add(area1);
                    }
                }
            }
            i++;
        }
        for (EntityID id : areasToRemove) {
            areasMap.remove(id);
            for (Set<EntityID> ids : buildingsMap.values()) {
                ids.remove(id);
            }
        }

        // call again
        if (!areasToRemove.isEmpty() || !buildingsToRemove.isEmpty()) {
            return processMatrix(buildingsMap, areasMap);
        }
        return areasMap.keySet();
    }
}
