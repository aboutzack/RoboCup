package CSU_Yunlu_2019.standard;

import java.awt.Polygon;
import java.util.LinkedList;
import java.util.List;

import adf.agent.info.WorldInfo;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class CSUBlockadeHelper {

	private Polygon polygon;

	private Blockade underlyingBlockade;

	private EntityID blockadeId;

	// private AdvancedWorldModel world;

	private List<Pair<Integer, Integer>> vertexes = new LinkedList<>();

	public CSUBlockadeHelper(EntityID blockadeId, WorldInfo world) {
		// this.world = world;
		this.underlyingBlockade = getEntity(world, blockadeId, Blockade.class);
		this.blockadeId = blockadeId;

		this.polygon = createPolygon(underlyingBlockade.getApexes());
	}

	/**
	 * Only for test
	 */
	public CSUBlockadeHelper(EntityID id, int[] apexes, int x, int y) {
		this.underlyingBlockade = new Blockade(blockadeId);

		this.underlyingBlockade.setX(x);
		this.underlyingBlockade.setY(y);
		this.underlyingBlockade.setApexes(apexes);

		this.blockadeId = id;
		this.polygon = createPolygon(apexes);
	}

	private Polygon createPolygon(int[] apexes) {
		int vertexCount = apexes.length / 2;
		int[] xCoordinates = new int[vertexCount];
		int[] yCOordinates = new int[vertexCount];

		for (int i = 0; i < vertexCount; i++) {
			xCoordinates[i] = apexes[2 * i];
			yCOordinates[i] = apexes[2 * i + 1];

			vertexes.add(new Pair<Integer, Integer>(apexes[2 * i], apexes[2 * i + 1]));
		}

		return new Polygon(xCoordinates, yCOordinates, vertexCount);
	}

	public Polygon getPolygon() {
		return this.polygon;
	}

	public Blockade getSelfBlockade() {
		return this.underlyingBlockade;
	}

	public EntityID getBlockadeId() {
		return this.blockadeId;
	}

	public List<Pair<Integer, Integer>> getVertexesList() {
		return this.vertexes;
	}

	/**
	 * Get an object of Entity according to its ID and cast this object to
	 * <b>&lt;T extends StandardEntity&gt;</b>.
	 */
	public <T extends StandardEntity> T getEntity(WorldInfo world, EntityID id, Class<T> c) {
		StandardEntity entity;
		entity = world.getEntity(id);
		if (c.isInstance(entity)) {
			T castedEntity = c.cast(entity);

			return castedEntity;
		} else {
			return null;
		}
	}
}
