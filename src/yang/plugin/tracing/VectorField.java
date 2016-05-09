package yang.plugin.tracing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import ij.ImagePlus;
import ij.gui.Roi;

final class VectorField extends Roi implements KeyListener {
	
	private float[][][] vf;
	private static float maxveclen = 1;
	
	VectorField(final ImagePlus imp, final float[][][] vf) {
		
		super(0,0,imp.getWidth(),imp.getHeight());
		setImage(imp);
		this.vf = vf;
		imp.setRoi(this);
		ic.addKeyListener(this);
	}
	
	@Override
	public void draw(final Graphics g) {
		
		final float mag = (float)ic.getMagnification();
		if (mag > 4) {
			final int dx = (int)(mag/2.0);
			final int dy = (int)(mag/2.0);
			g.setColor(Color.red);
			final Rectangle vof = ic.getSrcRect();
			final int xmax = vof.x + vof.width;
			final int ymax = vof.y + vof.height;
			for (int y=vof.y; y<ymax; ++y)
				for (int x=vof.x; x<xmax; ++x) {
					final float scale = (255.0f - vf[0][y][x])*maxveclen*mag/255.0f;
					final int hvx = (int)(vf[1][y][x]*scale)/2;
					final int hvy = (int)(vf[2][y][x]*scale)/2;
					g.drawLine(ic.screenX(x)-hvx+dx,ic.screenY(y)-hvy+dy,ic.screenX(x)+hvx+dx,ic.screenY(y)+hvy+dy);
				}
		}
	}
	
	@Override
	public void keyPressed(KeyEvent e) { try {
		
		if (e.getKeyCode() == KeyEvent.VK_UP) maxveclen += 0.05f;
		else if (e.getKeyCode() == KeyEvent.VK_DOWN) maxveclen -= 0.05f;
		
		ic.repaint();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyReleased(KeyEvent e) {}
	
	@Override
	public void keyTyped(KeyEvent e) {}
	
}