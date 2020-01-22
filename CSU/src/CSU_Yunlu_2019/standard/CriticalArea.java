package CSU_Yunlu_2019.standard;

import java.util.LinkedList;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import CSU_Yunlu_2019.module.route.pov.POVRouter;
import CSU_Yunlu_2019.util.BitUtil;
import adf.agent.info.WorldInfo;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * We divide roads into three different kinds. One is called the entrance, for
 * this kind roads are connected to the buildings, so if you'd like to go into
 * the buildings you must pass these roads. The second one is called avenue, for
 * they can be combined into a long street. And the rest is the cross, these
 * roads are connected to more than three roads.
 * <p>
 * The cross of two long street often plays important role in traffic system, so
 * we them as critical areas. Also, refuges are critical area, too.
 * <p>
 * Date: Mar 10, 2014 Time: 2:03 am improved by appreciation-csu
 * 
 * @author utisam
 */
public class CriticalArea implements Serializable{
	private WorldInfo world;
	private List<Area> criticalAreas = new LinkedList<Area>();

	private List<EntityID> criticalAreaIds = new LinkedList<>();

	public CriticalArea(WorldInfo world) {
		this.world = world;

		AREAMARK: for (StandardEntity se : world.getEntitiesOfType(StandardEntityURN.ROAD)) {
			Area area = (Area) se;
			List<Edge> edges = area.getEdges();
			if (edges.size() < 3)
				continue;
			for (Edge edge : edges) {
				if (!edge.isPassable())
					continue AREAMARK;
				if (world.getEntity(edge.getNeighbour()) instanceof Building)
					continue AREAMARK;
			}

			criticalAreas.add(area);
			criticalAreaIds.add(area.getID());
		}

		// remove neighbour of entrances
		for (Iterator<Area> itor = criticalAreas.iterator(); itor.hasNext();) {
			Area area = (Area) itor.next();
			List<EntityID> neighbours = area.getNeighbours();
			CRITICAL_MARK: for (EntityID entityId : neighbours) {
				Area neighbour = (Area) world.getEntity(entityId);
				List<EntityID> n_neighbours = neighbour.getNeighbours();
				if (n_neighbours.size() <= 2) {
					for (EntityID next : n_neighbours) {
						if (world.getEntity(next) instanceof Building) {
							criticalAreaIds.remove(area.getID());
							itor.remove();
							break CRITICAL_MARK;
						}
					}
				} else if (neighbour.getEdges().size() == n_neighbours.size()) {
					criticalAreaIds.remove(area.getID());
					itor.remove();
					break CRITICAL_MARK;
				}
			}
		}

	}

	private final int MAX_SEND_SIZE = 7;
	private int SEND_SIZE_BIT = BitUtil.needBitSize(MAX_SEND_SIZE);
	private List<Area> sendRemovedAreas = new LinkedList<Area>();

	public void update(POVRouter router) {
		sendRemovedAreas.clear();
		for (Iterator<Area> it = criticalAreas.iterator(); it.hasNext();) {
			Area area = (Area) it.next();

			if (router.isSureReachable(area)) {
				if (sendRemovedAreas.size() <= MAX_SEND_SIZE) {
					sendRemovedAreas.add(area);
				}
				criticalAreaIds.remove(area.getID());
				it.remove();
			}
		}
	}

	public int size() {
		return criticalAreas.size();
	}

	public Area get(int index) {
		return (Area) criticalAreas.get(index);
	}

	public List<Area> getAreas() {
		return criticalAreas;
	}

	public boolean isCriticalArea(Area area) {
		return this.isCriticalArea(area.getID());
	}

	public boolean isCriticalArea(EntityID area) {
		return this.criticalAreaIds.contains(area);
	}
}
