package yang.plugin.tracing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import ij.gui.ImageCanvas;
import ij.process.ByteProcessor;

final class Segment {
	
	private int iCapacity = 500;
	private final int iCapInc = 500;
	private int iSize = 0;
	private Point[] parray = null;
	
	Segment() {
		parray = new Point[iCapacity];
	}
	
	Segment(final int capacity) {
		iCapacity = capacity;
		parray = new Point[iCapacity];
	}
	
	void add(final Point point) {
		if (iSize == iCapacity) inccap();
		parray[iSize++] = point;
	}
	
	private void inccap() {
		iCapacity += iCapInc;
		final Point[] newparray = new Point[iCapacity];
		for (int i=0; i<iSize; ++i) newparray[i] = parray[i];
		parray = newparray;
	}
	
	Point first() { return parray[0]; }
	
	void first(final Point point) { parray[0] = point; }
	
	Point last() { return parray[iSize-1]; }
	
	void last(final Point point) { parray[iSize-1] = point; }
	
	Point get(final int index) { return parray[index]; }
	
	void get(final int index, final Point point) {
		point.x = parray[index].x;
		point.y = parray[index].y;
	}
	
	int nrpoints() { return iSize; }
	
	void reset() { iSize = 0; }
	
	Segment duplicate() {
		final Segment segment = new Segment(iCapacity);
		segment.iSize = iSize;
		for (int i=0; i<iSize; ++i)
			segment.parray[i] = new Point(parray[i].x,parray[i].y);
		return segment;
	}
	
	double length() {
		double length = 0.0;
		final double pw = NJ.calibrate ? NJ.imageplus.getCalibration().pixelWidth : 1;
		final double ph = NJ.calibrate ? NJ.imageplus.getCalibration().pixelHeight : 1;
		if (iSize > 1) for (int i=1; i<iSize; ++i) {
			final double dx = (parray[i].x - parray[i-1].x)*pw;
			final double dy = (parray[i].y - parray[i-1].y)*ph;
			length += Math.sqrt(dx*dx + dy*dy);
		}
		return length;
	}
	
	double distance2(final Point point) {
		double mindist2 = Double.MAX_VALUE;
		// Minimum distance to vertices:
		for (int i=0; i<iSize; ++i) {
			final double dx = point.x - parray[i].x;
			final double dy = point.y - parray[i].y;
			final double dist2 = dx*dx + dy*dy;
			if (dist2 < mindist2) mindist2 = dist2;
		}
		// Minimum distance to edges:
		for (int i=1, im1=0; i<iSize; ++i, ++im1) {
			final double v12x = parray[i].x - parray[im1].x;
			final double v12y = parray[i].y - parray[im1].y;
			final double v13x = point.x - parray[im1].x;
			final double v13y = point.y - parray[im1].y;
			final double inprod = v12x*v13x + v12y*v13y;
			if (inprod >= 0.0f) {
				final double v12len2 = v12x*v12x + v12y*v12y;
				if (inprod <= v12len2) {
					final double v13len2 = v13x*v13x + v13y*v13y;
					final double dist2 = v13len2 - inprod*inprod/v12len2;
					if (dist2 < mindist2) mindist2 = dist2;
				}
			}
		}
		return mindist2;
	}
	
	void values(final ByteProcessor bp, final Values values) {
		final int ssfactor = NJ.interpolate ? NJ.subsamplefactor : 1;
		for (int i=1, im1=0; i<iSize; ++i, ++im1) {
			final double dx = (parray[i].x - parray[im1].x)/ssfactor;
			final double dy = (parray[i].y - parray[im1].y)/ssfactor;
			for (int j=0; j<ssfactor; ++j) {
				final double x = parray[im1].x + j*dx;
				final double y = parray[im1].y + j*dy;
				values.add(bp.getInterpolatedValue(x,y));
			}
		}
	}
	
	void reverse() {
		final int iHalf = iSize/2;
		for (int b=0, e=iSize-1; b<iHalf; ++b, --e) {
			final Point tmp = parray[b]; parray[b] = parray[e]; parray[e] = tmp;
		}
	}
	
	void draw(final Graphics g, final ImageCanvas imc, final Color color) {
		final Rectangle vof = imc.getSrcRect();
		final double mag = imc.getMagnification();
		final int dx = (int)(mag/2.0);
		final int dy = (int)(mag/2.0);
		g.setColor(color);
		if (iSize > 1) for (int i=1; i<iSize; ++i) {
			g.drawLine(
				dx + (int)((parray[i].x - vof.x)*mag),
				dy + (int)((parray[i].y - vof.y)*mag),
				dx + (int)((parray[i-1].x - vof.x)*mag),
				dy + (int)((parray[i-1].y - vof.y)*mag)
			);
		}
	}
	
}