package ij.plugin.myplugin;

import ij.process.ImageProcessor;

public class Corner_My implements Comparable {
	int u;
	int v;
	float q;
	
	Corner_My(int u, int v, float q) {
		this.u = u;
		this.v = v;
		this.q = q;
	}
	
	public int compareTo (Object obj) {
		Corner_My c2 = (Corner_My) obj;
		if(this.q > c2.q) return -1;
		if(this.q < c2.q) return 1;
		else return 0;
	}
	
	double dist2(Corner_My c2) {
		int dx = this.u - c2.u;
		int dy = this.v - c2.v;
		return (dx * dx) + (dy * dy);
	}
	
	void draw(ImageProcessor ip) {
		int paintvalue = 0;
		int size = 2;
		ip.setValue(paintvalue);
		ip.drawLine(u - size, v, u + size, v);
		ip.drawLine(u, v - size, u, v + size);
	}
}
