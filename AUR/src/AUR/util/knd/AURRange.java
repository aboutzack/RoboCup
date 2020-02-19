package AUR.util.knd;

import java.awt.Color;
import java.awt.Graphics2D;

public class AURRange {

	double rcx = 0;
	double rcy = 0;
	double r = 0;

	public AURRange(double rcx, double rcy, double r) {
		this.rcx = rcx;
		this.rcy = rcy;
		this.r = r;
	}

	public boolean contains(double x, double y) {
		double dx = rcx - x;
		double dy = rcy - y;
		return (dx * dx + dy * dy < r * r);
	}

	public void draw(Graphics2D g) {
		g.setColor(Color.orange);
		g.drawOval((int) (rcx - r), (int) (rcy - r), (int) (r * 2), (int) (r * 2));
	}
}
