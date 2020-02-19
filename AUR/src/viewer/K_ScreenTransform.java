package viewer;

import java.awt.Graphics2D;
import viewer.fromMisc.ScreenTransform;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class K_ScreenTransform extends ScreenTransform {

	public K_ScreenTransform(double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
	}

	public Polygon getTransformedPolygon(Shape shape) {
		Polygon polygon = (Polygon) shape;
		Polygon result = new Polygon();
		for(int i = 0; i < polygon.npoints; i++) {
			result.addPoint(this.xToScreen(polygon.xpoints[i]), this.yToScreen(polygon.ypoints[i]));
		}
		return result;
	}

	public Rectangle2D getTransformedRectangle(Rectangle2D rect) {
		return new Rectangle(
			xToScreen(rect.getMinX()),
			yToScreen(rect.getMinY() + rect.getHeight()),
			(int) (rect.getWidth() * zoom),
			(int) (rect.getHeight() * zoom)
		);
	}

	public Rectangle2D getTransformedRectangle(double x0, double y0, double w, double h) {
		return new Rectangle(
			xToScreen(x0),
			yToScreen(y0 + h),
			(int) (w * zoom),
			(int) (h * zoom)
		);
	}

	public void drawTransformedLine(Graphics2D g2, double x0, double y0, double x1, double y1) {
		g2.drawLine(
			this.xToScreen(x0),
			this.yToScreen(y0),
			this.xToScreen(x1),
			this.yToScreen(y1)
		);
	}
	
	public void fillTransformedOvalFixedRadius(Graphics2D g2, double x0, double y0, double r) {
		g2.fillOval(
			(int) (this.xToScreen(x0) - r),
			(int) (this.yToScreen(y0) - r),
			(int) (2 * r),
			(int) (2 * r)
		);
	}
    
}
