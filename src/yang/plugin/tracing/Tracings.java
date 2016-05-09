package yang.plugin.tracing;

import java.awt.Graphics;

import ij.gui.ImageCanvas;

final class Tracings {
	
	private int iCapacity = 20;
	private final int iCapInc = 20;
	private int iSize = 0;
	private Tracing[] tarray = new Tracing[iCapacity];
	
	void add(final Tracing tracing) {
		if (iSize == iCapacity) inccap();
		tarray[iSize++] = tracing;
		NJ.save = true;
	}
	
	private void inccap() {
		iCapacity += iCapInc;
		final Tracing[] newarray = new Tracing[iCapacity];
		for (int i=0; i<iSize; ++i) newarray[i] = tarray[i];
		tarray = newarray;
	}
	
	Tracing get(final int index) { return tarray[index]; }
	
	void remove(final int index) {
		for (int i1=index, i2=index+1; i2<iSize; ++i1, ++i2)
			tarray[i1] = tarray[i2];
		--iSize;
		NJ.save = true;
	}
	
	void reset() { iSize = 0; NJ.save = true; }
	
	int nrtracings() { return iSize; }
	
	boolean changed() {
		for (int w=0; w<iSize; ++w)
			if (tarray[w].changed()) return true;
		return false;
	}
	
	void draw(final Graphics g, final ImageCanvas imc) {
		for (int w=0; w<iSize; ++w) tarray[w].draw(g,imc);
	}
	
}