package yang.plugin.tracing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import ij.gui.ImageCanvas;
import ij.process.ByteProcessor;

final class Tracing {
	
	private int iCapacity = 20;
	private final int iCapInc = 20;
	private int iSize = 0;
	private Segment[] sarray = null;
	
	private boolean hili = false;
	private boolean select = false;
	private boolean changed = false;
	
	private int type = 0;
	private int cluster = 0;
	private String label = "Default";
	
	private static int lastID = 0;
	private int ID;
	
	Tracing() {
		ID = ++lastID;
		sarray = new Segment[iCapacity];
	}
	
	Tracing(final int capacity) {
		ID = ++lastID;
		iCapacity = capacity;
		sarray = new Segment[iCapacity];
	}
	
	void id(final int id) { ID = id; if (ID > lastID) lastID = ID; NJ.save = true; }
	
	int id() { return ID; }
	
	static void resetID() { lastID = 0; }
	
	void add(final Segment segment) {
		if (iSize == iCapacity) inccap();
		sarray[iSize++] = segment;
		if (iSize > 1) sarray[iSize-1].first(sarray[iSize-2].last());
		changed = true;
		NJ.save = true;
	}
	
	private void inccap() {
		iCapacity += iCapInc;
		final Segment[] newarray = new Segment[iCapacity];
		for (int i=0; i<iSize; ++i) newarray[i] = sarray[i];
		sarray = newarray;
	}
	
	Segment get(final int index) { return sarray[index]; }
	
	int nrsegments() { return iSize; }
	
	double length() {
		double length = 0.0;
		for (int s=0; s<iSize; ++s)
		length += sarray[s].length();
		return length;
	}
	
	double distance2(final Point point) {
		double mindist2 = Double.MAX_VALUE;
		for (int s=0; s<iSize; ++s) {
			final double dist2 = sarray[s].distance2(point);
			if (dist2 < mindist2) mindist2 = dist2;
		}
		return mindist2;
	}
	
	void values(final ByteProcessor bp, final Values values) {
		for (int s=0; s<iSize; ++s)
			sarray[s].values(bp,values);
		final Point last = sarray[iSize-1].last();
		values.add(bp.getInterpolatedValue(last.x,last.y));
	}
	
	boolean changed() {	return changed; }
	
	void select(final boolean select) {
		if (this.select != select) {
			this.select = select;
			changed = true;
		}
	}
	
	boolean selected() { return select; }
	
	void highlight(final boolean hili) {
		if (this.hili != hili) {
			this.hili = hili;
			changed = true;
		}
	}
	
	boolean highlighted() { return hili; }
	
	void type(final int type) {
		if (this.type != type) {
			this.type = type;
			changed = true;
			NJ.save = true;
		}
	}
	
	int type() { return type; }
	
	void cluster(final int cluster) {
		if (this.cluster != cluster) {
			this.cluster = cluster;
			NJ.save = true;
		}
	}
	
	int cluster() { return cluster; }
	
	void label(final String label) {
		if (!this.label.equals(label)) {
			this.label = label;
			NJ.save = true;
		}
	}
	
	String label() { return label; }
	
	void draw(final Graphics g, final ImageCanvas imc) {
		final Color drawcolor = (hili || select) ? NJ.HIGHLIGHTCOLOR : NJ.typecolors[type];
		for (int s=0; s<iSize; ++s) sarray[s].draw(g,imc,drawcolor);
		changed = false;
	}
	
}