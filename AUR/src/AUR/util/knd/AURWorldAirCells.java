package AUR.util.knd;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import viewer.K_ScreenTransform;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURWorldAirCells {
	
	private AURWorldGraph wsg = null;
	private Rectangle2D worldBounds = null;
	private float cells[][][] = null;

	public float[][][] getCells() {
		return cells;
	}

	public int getCellSize() {
		return AURConstants.FireSim.WORLD_AIR_CELL_SIZE;
	}

	public AURWorldAirCells(AURWorldGraph wsg) {
		this.wsg = wsg;
		this.worldBounds = wsg.wi.getBounds();
		int rows = (int) Math.ceil(worldBounds.getHeight() / AURConstants.FireSim.WORLD_AIR_CELL_SIZE) + 1;
		int cols = (int) Math.ceil(worldBounds.getWidth() / AURConstants.FireSim.WORLD_AIR_CELL_SIZE) + 1;
		this.worldBounds.setRect(
			worldBounds.getMinX(),
			worldBounds.getMinY(),
			cols * AURConstants.FireSim.WORLD_AIR_CELL_SIZE,
			rows * AURConstants.FireSim.WORLD_AIR_CELL_SIZE
		);

		cells = new float[rows][cols][2];

	}

	private int[] res = new int[2];

	public int[] getCell_ij(double x, double y) {
		if (worldBounds.contains(x, y) == false) {
			return null;
		}
		x -= worldBounds.getMinX();
		y -= worldBounds.getMinY();

		res[0] = (int) Math.floor(y / AURConstants.FireSim.WORLD_AIR_CELL_SIZE);
		res[1] = (int) Math.floor(x / AURConstants.FireSim.WORLD_AIR_CELL_SIZE);

		return res;
	}

	private int[] res2 = new int[2];

	public int[] getCell_xy(int i, int j) {
		res2[0] = (int) (j * getCellSize() + worldBounds.getMinX());
		res2[1] = (int) (i * getCellSize() + worldBounds.getMinY());
		return res2;
	}

	public void paintJustCells(Graphics2D g2, K_ScreenTransform kst) {
		int a = AURConstants.FireSim.WORLD_AIR_CELL_SIZE;
		int mx = (int) (worldBounds.getMinX());
		int my = (int) (worldBounds.getMinY());
		for (int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				Rectangle2D cell = kst.getTransformedRectangle(j * a + mx, i * a + my, a, a);
				g2.draw(cell);
			}
		}
	}
	
	public void paintTemperatures(Graphics2D g2, K_ScreenTransform kst) {
		int a = AURConstants.FireSim.WORLD_AIR_CELL_SIZE;
		int mx = (int) (worldBounds.getMinX());
		int my = (int) (worldBounds.getMinY());
		for (int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				g2.setColor(new Color(255, 0, 0, Math.min(255, (int) (cells[i][j][0] / 1500 * 255))));
				Rectangle2D cell = kst.getTransformedRectangle(j * a + mx, i * a + my, a, a);
				g2.fill(cell);
			}
		}
	}

}
