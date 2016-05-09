package yang.plugin.tracing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.RoiEncoder;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import imagescience.utility.FMath;

final class TracingHandler extends Roi implements KeyListener, MouseListener, MouseMotionListener {
	
	private ImagePlus imp;
	private ImageCanvas imc;
	private ImageWindow imw;
	ByteProcessor ipgray;
	
	private final Dijkstra dijkstra = new Dijkstra();
	private float[][][] costs;
	private byte[][] dirsimage;
	
	private final Point clckPoint = new Point();
	private final Point currPoint = new Point();
	private final Point mousPoint = new Point();
	private final Point snapPoint = new Point();
	private final Point scrlPoint = new Point();
	private final Point movePoint = new Point();
	private final Point zoomPoint = new Point();
	
	private Tracings tracings = new Tracings();
	private Tracing currTracing;
	private Segment currSegment = new Segment();
	private Segment ssmpSegment = new Segment();
	private Point currVertex;
	
	private boolean bTracingActive;
	private boolean bManualTracing;
	private boolean bSnapCursor;
	private boolean bSmoothSegment;
	private boolean bComputedCosts;
	private boolean bDijkstra;
	private boolean bOnCanvas = false;
	
	private int iXSize, iYSize;
	
	private long lastClckTime = System.currentTimeMillis();
	
	TracingHandler() { super(0,0,1,1); }
	
	void attach(final ImagePlus impNew) {
		
		// Copy handles:
		imp = impNew;
		imw = imp.getWindow();
		imc = imw.getCanvas();
		
		// Create a copy that is surely a gray-scale image (the pixels
		// are already of type byte, but may represent color indices,
		// not actual gray-values):
		NJ.log("Creating gray-scale copy of new image...");
		iXSize = imp.getWidth(); iYSize = imp.getHeight();
		final ByteProcessor ipIn = (ByteProcessor)imp.getProcessor();
		final IndexColorModel icm = (IndexColorModel)ipIn.getColorModel();
		final int iMapSize = icm.getMapSize();
		final byte[] r = new byte[iMapSize]; icm.getReds(r);
		final byte[] g = new byte[iMapSize]; icm.getGreens(g);
		final byte[] b = new byte[iMapSize]; icm.getBlues(b);
		try {
			ipgray = new ByteProcessor(iXSize,iYSize);
			final byte[] g8pxs = (byte[])ipgray.getPixels();
			final byte[] inpxs = (byte[])ipIn.getPixels();
			final int nrpxs = inpxs.length;
			for (int i=0; i<nrpxs; ++i) {
				final int index = inpxs[i]&0xFF;
				g8pxs[i] = (byte)FMath.round((r[index]&0xFF)*0.3 + (g[index]&0xFF)*0.6 + (b[index]&0xFF)*0.1);
			}
		} catch (OutOfMemoryError e) {
			NJ.outOfMemory();
			ipgray = null;
		}
		
		// Remove and add listeners from and to canvas:
		NJ.log("Detaching ImageJ listeners...");
		imw.removeKeyListener(IJ.getInstance());
		imc.removeKeyListener(IJ.getInstance());
		imc.removeMouseListener(imc);
		imc.removeMouseMotionListener(imc);
		NJ.log("Attaching "+NJ.NAME+" listeners...");
		imw.addKeyListener(this);
		imc.addKeyListener(this);
		imc.addMouseListener(this);
		imc.addMouseMotionListener(this);
		NJ.log("Done");
		
		// Reset variables:
		costs = null;
		dirsimage = null;
		tracings.reset();
		Tracing.resetID();
		currSegment.reset();
		ssmpSegment.reset();
		currPoint.setLocation(-100,-100);
		zoomPoint.setLocation(0,0);
		bTracingActive = false;
		bManualTracing = false;
		bSnapCursor = true;
		bSmoothSegment = true;
		bComputedCosts = false;
		bDijkstra = false;
		
		// Enable displaying tracings:
		ic = null; // Work-around to prevent cloning in imp.setRoi()
		imp.setRoi(this);
	}
	
	void computeCosts() {
		final Costs ci = new Costs();
		final long lStartTime = System.currentTimeMillis();
		try {
			if (ipgray != null) costs = ci.run(ipgray,(NJ.appear==0),NJ.scale);
			else throw new OutOfMemoryError();
			NJ.log("Finished in "+(System.currentTimeMillis()-lStartTime)+" ms");
			bComputedCosts = true;
		} catch (OutOfMemoryError e) {
			NJ.outOfMemory();
			NJ.ntb.resetTool();
		}
		NJ.copyright();
	}
	
	boolean computedCosts() { return bComputedCosts; }
	
	void doDijkstra() {
		if (bTracingActive) bDijkstra = true;
	}
	
	Tracings tracings() { return tracings; }
	
	void redraw() { imc.repaint(); }
	
	@Override
	public void draw(final Graphics g) { try {
		
		// Set stroke:
		if (g instanceof Graphics2D) ((Graphics2D)g).setStroke(NJ.tracestroke);
		
		// Draw finished tracings:
		tracings.draw(g,imc);
		
		// Draw currently active tracing and segment:
		if (bTracingActive) {
			currTracing.draw(g,imc);
			currSegment.draw(g,imc,NJ.ACTIVECOLOR);
		}
		
		final double mag = imc.getMagnification();
		final int ihalfmag = (int)(mag/2.0);
		
		if (currVertex != null) {
			g.setColor(NJ.HIGHLIGHTCOLOR);
			final int csx = imc.screenX(currVertex.x) + ihalfmag;
			final int csy = imc.screenY(currVertex.y) + ihalfmag;
			final int width = 3*NJ.linewidth;
			g.fillOval(csx-width/2,csy-width/2,width,width);
		}
		
		// Draw currPoint cursor:
		if (NJ.ntb.currentTool() == TracingToolbar.ADD && bOnCanvas) {
			if (g instanceof Graphics2D) ((Graphics2D)g).setStroke(NJ.crossstroke);
			g.setColor(Color.red);
			final int csx = imc.screenX(currPoint.x) + ihalfmag;
			final int csy = imc.screenY(currPoint.y) + ihalfmag;
			g.drawLine(csx,csy-5,csx,csy+5);
			g.drawLine(csx-5,csy,csx+5,csy);
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	// Note that this method is called only if imp is not null.
	public ColorProcessor makeSnapshot(final boolean snapshotimage, final boolean snapshottracings) {
		
		ColorProcessor cp = null;
		
		if (snapshotimage || snapshottracings) try {
			NJ.log("Creating snapshot image");
			iXSize = imp.getWidth(); iYSize = imp.getHeight();
			cp = new ColorProcessor(iXSize,iYSize);
			cp.setLineWidth(NJ.linewidth);
			if (snapshotimage) {
				final ByteProcessor bp = (ByteProcessor)imp.getProcessor();
				final IndexColorModel icm = (IndexColorModel)bp.getColorModel();
				final int iMapSize = icm.getMapSize();
				final byte[] r = new byte[iMapSize]; icm.getReds(r);
				final byte[] g = new byte[iMapSize]; icm.getGreens(g);
				final byte[] b = new byte[iMapSize]; icm.getBlues(b);
				final byte[] bpxs = (byte[])bp.getPixels();
				final int[] cpxs = (int[])cp.getPixels();
				final int nrpxs = bpxs.length;
				for (int i=0; i<nrpxs; ++i) {
					final int index = bpxs[i]&0xFF;
					cpxs[i] = ((r[index]&0xFF)<<16) | ((g[index]&0xFF)<<8) | (b[index]&0xFF);
				}
			}
			if (snapshottracings) {
				// Draw finished tracings:
				final Point spnt = new Point();
				final Point epnt = new Point();
				final int nrt = tracings.nrtracings();
				for (int t=0; t<nrt; ++t) {
					final Tracing tracing = tracings.get(t);
					final int nrs = tracing.nrsegments();
					cp.setColor(NJ.typecolors[tracing.type()]);
					for (int s=0; s<nrs; ++s) {
						final Segment segment = tracing.get(s);
						final int nrp = segment.nrpoints();
						if (nrp > 1) for (int p=1; p<nrp; ++p) {
							segment.get(p-1,spnt);
							segment.get(p,epnt);
							cp.drawLine(spnt.x,spnt.y,epnt.x,epnt.y);
						}
					}
				}
				// Draw currently active tracing and segment:
				if (bTracingActive) {
					// Draw current tracing:
					final Tracing tracing = currTracing;
					final int nrs = tracing.nrsegments();
					cp.setColor(NJ.typecolors[tracing.type()]);
					for (int s=0; s<nrs; ++s) {
						final Segment segment = tracing.get(s);
						final int nrp = segment.nrpoints();
						if (nrp > 1) for (int p=1; p<nrp; ++p) {
							segment.get(p-1,spnt);
							segment.get(p,epnt);
							cp.drawLine(spnt.x,spnt.y,epnt.x,epnt.y);
						}
					}
					// Draw current segment:
					cp.setColor(NJ.ACTIVECOLOR);
					final Segment segment = currSegment;
					final int nrp = segment.nrpoints();
					if (nrp > 1) for (int p=1; p<nrp; ++p) {
						segment.get(p-1,spnt);
						segment.get(p,epnt);
						cp.drawLine(spnt.x,spnt.y,epnt.x,epnt.y);
					}
				}
			}
			
		} catch (OutOfMemoryError e) {
			NJ.outOfMemory();
		}
		
		return cp;
	}
	
	@Override
	public void keyPressed(final KeyEvent e) { try {
		
		final int iKeyCode = e.getKeyCode();
		
		if (iKeyCode == KeyEvent.VK_C && costs != null && NJ.hkeys) {
			try {
				NJ.log("Showing tracing cost image");
				final ByteProcessor ip = new ByteProcessor(iXSize,iYSize);
				final byte[] pixels = (byte[])ip.getPixels();
				for (int y=0, i=0; y<iYSize; ++y)
					for (int x=0; x<iXSize; ++x, ++i)
						pixels[i] = (byte)costs[0][y][x];
				final String title = NJ.usename ? (NJ.imagename+"-costs") : (NJ.NAME+": Costs");
				final ImagePlus tmp = new ImagePlus(title,ip);
				tmp.show(); tmp.updateAndRepaintWindow();
			} catch (OutOfMemoryError error) {
				NJ.outOfMemory();
			}
		} else if (iKeyCode == KeyEvent.VK_D && dirsimage != null && NJ.hkeys) {
			try {
				NJ.log("Showing local directions image");
				final ByteProcessor ip = new ByteProcessor(iXSize,iYSize);
				final byte[] pixels = (byte[])ip.getPixels();
				for (int y=0, i=0; y<iYSize; ++y)
					for (int x=0; x<iXSize; ++x, ++i)
						pixels[i] = (byte)(31*(0xFF & dirsimage[y][x]));
				final String title = NJ.usename ? (NJ.imagename+"-directions") : (NJ.NAME+": Directions");
				final ImagePlus tmp = new ImagePlus(title,ip);
				tmp.show(); tmp.updateAndRepaintWindow();
			} catch (OutOfMemoryError error) {
				NJ.outOfMemory();
			}
		} else if (iKeyCode == KeyEvent.VK_V && costs != null && NJ.hkeys) {
			try {
				NJ.log("Showing local vectors image");
				final ByteProcessor ip = new ByteProcessor(iXSize,iYSize);
				final byte[] pixels = (byte[])ip.getPixels();
				for (int y=0, i=0; y<iYSize; ++y)
					for (int x=0; x<iXSize; ++x, ++i)
						pixels[i] = (byte)(255.0f - costs[0][y][x]);
				final String title = NJ.usename ? (NJ.imagename+"-vectors") : (NJ.NAME+": Vectors");
				final ImagePlus tmp = new ImagePlus(title,ip);
				tmp.show(); tmp.updateAndRepaintWindow();
				final VectorField vf = new VectorField(tmp,costs);
			} catch (OutOfMemoryError error) {
				NJ.outOfMemory();
			}
		} else if (iKeyCode == KeyEvent.VK_CONTROL && bSnapCursor) {
			NJ.log("Switching off local snapping");
			currPoint.x = mousPoint.x;
			currPoint.y = mousPoint.y;
			bSnapCursor = false;
			if (bTracingActive) updateCurrSegment();
			redraw();
		} else if (iKeyCode == KeyEvent.VK_SHIFT && !bManualTracing) {
			NJ.log("Switching to manual tracing mode");
			bManualTracing = true;
			if (bTracingActive) updateCurrSegment();
			redraw();
		} else if (iKeyCode == KeyEvent.VK_S && bSmoothSegment && NJ.hkeys) {
			NJ.log("Disabling segment smoothing");
			bSmoothSegment = false;
			if (bTracingActive) updateCurrSegment();
			redraw();
		} else if ((iKeyCode == KeyEvent.VK_TAB || iKeyCode == KeyEvent.VK_SPACE) && bTracingActive) {
			NJ.log("Finishing current tracing");
			finishCurrSegment();
			finishCurrTracing();
			redraw();
		} else if (iKeyCode == KeyEvent.VK_LEFT && NJ.workimages != null) {
			if (NJ.workimagenr <= 0)
			NJ.notify("The current image is the first image");
			else {
				--NJ.workimagenr;
				NJ.log("Request to load image "+NJ.workdir+NJ.workimages[NJ.workimagenr]);
				NJ.ntb.loadImage(NJ.workdir,NJ.workimages[NJ.workimagenr]);
				NJ.ntb.resetTool();
			}
		} else if (iKeyCode == KeyEvent.VK_RIGHT && NJ.workimages != null) {
			if (NJ.workimagenr >= NJ.workimages.length-1)
			NJ.notify("The current image is the last image");
			else {
				++NJ.workimagenr;
				NJ.log("Request to load image "+NJ.workdir+NJ.workimages[NJ.workimagenr]);
				NJ.ntb.loadImage(NJ.workdir,NJ.workimages[NJ.workimagenr]);
				NJ.ntb.resetTool();
			}
		} else if (iKeyCode == KeyEvent.VK_ADD || iKeyCode == KeyEvent.VK_EQUALS) {
			imc.zoomIn(zoomPoint.x,zoomPoint.y);
			showValue(imc.offScreenX(zoomPoint.x),imc.offScreenY(zoomPoint.y));
		} else if (iKeyCode == KeyEvent.VK_MINUS || iKeyCode == KeyEvent.VK_SUBTRACT) {
			imc.zoomOut(zoomPoint.x,zoomPoint.y);
			showValue(imc.offScreenX(zoomPoint.x),imc.offScreenY(zoomPoint.y));
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyReleased(final KeyEvent e) { try {
		
		final int iKeyCode = e.getKeyCode();
		
		if (iKeyCode == KeyEvent.VK_CONTROL) {
			NJ.log("Switching on local snapping");
			currPoint.x = snapPoint.x;
			currPoint.y = snapPoint.y;
			bSnapCursor = true;
			if (bTracingActive) updateCurrSegment();
			redraw();
		} else if (iKeyCode == KeyEvent.VK_SHIFT) {
			NJ.log("Back to automatic tracing mode");
			bManualTracing = false;
			if (bTracingActive) updateCurrSegment();
			redraw();
		} else if (iKeyCode == KeyEvent.VK_S) {
			NJ.log("Enabling segment smoothing");
			bSmoothSegment = true;
			if (bTracingActive) updateCurrSegment();
			redraw();
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyTyped(final KeyEvent e) {}
	
	@Override
	public void mouseClicked(final MouseEvent e) {}
	
	@Override
	public void mouseDragged(final MouseEvent e) { try {
		
		final int x = e.getX();
		final int y = e.getY();
		final int osx = imc.offScreenX(x);
		final int osy = imc.offScreenY(y);
		
		showValue(osx,osy);
		
		switch (NJ.ntb.currentTool()) {
			case TracingToolbar.MOVE: {
				if (currVertex != null) {
					final int dx = osx - movePoint.x;
					final int dy = osy - movePoint.y;
					if (dx != 0 || dy != 0) {
						NJ.save = true;
						currVertex.x += dx;
						currVertex.y += dy;
						movePoint.x += dx;
						movePoint.y += dy;
						redraw();
					}
				}
				break;
			}
			case TracingToolbar.SCROLL: {
				scroll(x,y);
				break;
			}
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void mouseEntered(final MouseEvent e) { try {
		
		if (NJ.activate) {
			imw.toFront();
			imc.requestFocusInWindow();
		}
		
		zoomPoint.x = e.getX();
		zoomPoint.y = e.getY();
		
		bOnCanvas = true;
		redraw();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void mouseExited(final MouseEvent e) { try {
		
		NJ.copyright();
		bOnCanvas = false;
		redraw();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	private Point mouseMovedPoint = new Point();
	
	@Override
	public void mouseMoved(final MouseEvent e) { try {
		
		zoomPoint.x = e.getX();
		zoomPoint.y = e.getY();
		
		final int x = imc.offScreenX(e.getX());
		final int y = imc.offScreenY(e.getY());
		
		showValue(x,y);
		
		switch (NJ.ntb.currentTool()) {
			case TracingToolbar.ADD: {
				final int prevMouseX = mousPoint.x;
				final int prevMouseY = mousPoint.y;
				mousPoint.x = x;
				mousPoint.y = y;
				
				// Move away from the border (the Dijkstra algorithm does
				// not allow the starting point to be on the border):
				if (mousPoint.x == 0) ++mousPoint.x;
				else if (mousPoint.x == iXSize-1) --mousPoint.x;
				if (mousPoint.y == 0) ++mousPoint.y;
				else if (mousPoint.y == iYSize-1) --mousPoint.y;
				
				// If the mouse point is still on the same pixel, there is
				// no need to do anything (this prevents superfluous screen
				// refreshments at zoom levels > 100%):
				if (prevMouseX != mousPoint.x || prevMouseY != mousPoint.y) {
					
					snapPoint.x = currPoint.x = mousPoint.x;
					snapPoint.y = currPoint.y = mousPoint.y;
					
					// Update directions map if necessary:
					if (bDijkstra) {
						bDijkstra = false;
						NJ.log("Computing shortest paths to clicked point...");
						IJ.showStatus("Computing optimal paths");
						final long lStartTime = System.currentTimeMillis();
						dirsimage = dijkstra.run(costs,clckPoint);
						NJ.log("Finished in "+(System.currentTimeMillis()-lStartTime)+" ms");
						NJ.copyright();
					}
					
					// Compute locally lowest cost point for snapping:
					int startx = mousPoint.x - NJ.snaprange; if (startx < 1) startx = 1;
					int starty = mousPoint.y - NJ.snaprange; if (starty < 1) starty = 1;
					int stopx = mousPoint.x + NJ.snaprange; if (stopx > iXSize-2) stopx = iXSize-2;
					int stopy = mousPoint.y + NJ.snaprange; if (stopy > iYSize-2) stopy = iYSize-2;
					for (int sy=starty; sy<=stopy; ++sy)
						for (int sx=startx; sx<=stopx; ++sx)
							if (costs[0][sy][sx] < costs[0][snapPoint.y][snapPoint.x]) {
								snapPoint.x = sx;
								snapPoint.y = sy;
							}
					
					// Snap if requested:
					if (bSnapCursor) {
						currPoint.x = snapPoint.x;
						currPoint.y = snapPoint.y;
					}
					
					if (bTracingActive) updateCurrSegment();
					
					// Draw:
					redraw();
				}
				break;
			}
			case TracingToolbar.MOVE: {
				final Point prevVertex = currVertex;
				currVertex = null;
				mouseMovedPoint.x = x;
				mouseMovedPoint.y = y;
				double mindist2 = Double.MAX_VALUE;
				final double NBR2 = 4*NJ.NEARBYRANGE*NJ.NEARBYRANGE;
				final int nrt = tracings.nrtracings();
				for (int t=0; t<nrt; ++t) {
					final Tracing tracing = tracings.get(t);
					final int nrs = tracing.nrsegments();
					for (int s=0; s<nrs; ++s) {
						final Segment segment = tracing.get(s);
						final int nrp = segment.nrpoints();
						for (int p=0; p<nrp; ++p) {
							final Point mpnt = segment.get(p);
							final double dx = mpnt.x - mouseMovedPoint.x;
							final double dy = mpnt.y - mouseMovedPoint.y;
							final double dist2 = dx*dx + dy*dy;
							if (dist2 < NBR2 && dist2 < mindist2) {
								currVertex = mpnt;
								mindist2 = dist2;
							}
						}
					}
				}
				if (currVertex != prevVertex) redraw();
				break;
			}
			case TracingToolbar.DELETE:
			case TracingToolbar.ATTRIBS: {
				mouseMovedPoint.x = x;
				mouseMovedPoint.y = y;
				final int nrt = tracings.nrtracings();
				int tmin = 0; double mindist2 = Double.MAX_VALUE;
				for (int t=0; t<nrt; ++t) {
					final Tracing tracing = tracings.get(t);
					tracing.highlight(false);
					final double dist2 = tracing.distance2(mouseMovedPoint);
					if (dist2 < mindist2) { mindist2 = dist2; tmin = t; }
				}
				final double NBR2 = NJ.NEARBYRANGE*NJ.NEARBYRANGE;
				if (mindist2 <= NBR2) tracings.get(tmin).highlight(true);
				else tmin = -1;
				if (tracings.changed()) {
					if (NJ.adg != null) NJ.adg.select(tmin+1);
					redraw();
				}
				break;
			}
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void mousePressed(final MouseEvent e) { try {
		
		final int x = e.getX();
		final int y = e.getY();
		
		switch (NJ.ntb.currentTool()) {
			case TracingToolbar.SCROLL: {
				scrlPoint.x = imc.offScreenX(x);
				scrlPoint.y = imc.offScreenY(y);
				break;
			}
			case TracingToolbar.MAGNIFY: {
				final int flags = e.getModifiers();
				if ((flags & (Event.ALT_MASK | Event.META_MASK | Event.CTRL_MASK)) != 0) imc.zoomOut(x,y);
				else imc.zoomIn(x,y);
				showValue(imc.offScreenX(x),imc.offScreenY(y));
				break;
			}
			case TracingToolbar.ADD: {
				final long currClckTime = System.currentTimeMillis();
				final int prevClckX = clckPoint.x;
				final int prevClckY = clckPoint.y;
				clckPoint.x = currPoint.x;
				clckPoint.y = currPoint.y;
				NJ.log("Clicked point ("+clckPoint.x+","+clckPoint.y+")");
				
				if (!bTracingActive) {
					currTracing = new Tracing();
					bTracingActive = true;
					bDijkstra = true;
				} else {
					finishCurrSegment();
					if ((currClckTime - lastClckTime < 500) && (prevClckX == clckPoint.x && prevClckY == clckPoint.y))
						finishCurrTracing();
				}
				
				lastClckTime = currClckTime;
				break;
			}
			case TracingToolbar.DELETE: {
				final int nrtracings = tracings.nrtracings();
				for (int w=0; w<nrtracings; ++w) {
					final Tracing tracing = tracings.get(w);
					if (tracing.highlighted()) {
						final YesNoDialog ynd =
						new YesNoDialog("Delete","Do you really want to delete this tracing?");
						if (ynd.yesPressed()) {
							NJ.log("Deleting tracing N"+tracing.id());
							tracings.remove(w);
							IJ.showStatus("Deleted tracing");
							if (NJ.adg != null) NJ.adg.reset();
						} else {
							tracing.highlight(false);
							NJ.copyright();
							if (NJ.adg != null) NJ.adg.select(0);
						}
						break;
					}
				}
				break;
			}
			case TracingToolbar.MOVE: {
				movePoint.x = imc.offScreenX(x);
				movePoint.y = imc.offScreenY(y);
				break;
			}
			case TracingToolbar.ATTRIBS: {
				final int nrtracings = tracings.nrtracings();
				for (int i=0; i<nrtracings; ++i) {
					final Tracing tracing = tracings.get(i);
					if (tracing.highlighted()) {
						if (!tracing.selected()) {
							NJ.log("Selecting tracing N"+tracing.id());
							tracing.select(true);
							IJ.showStatus("Selected tracing");
						} else {
							NJ.log("Deselecting tracing N"+tracing.id());
							tracing.select(false);
							tracing.highlight(false);
							if (NJ.adg != null) NJ.adg.select(0);
							IJ.showStatus("Deselected tracing");
						}
						break;
					}
				}
				break;
			}
		}
		
		redraw();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	private void finishCurrTracing() {
		
		if (currTracing.nrsegments() == 0) {
			NJ.log("Dumping tracing of zero units length");
			IJ.showStatus("Dumped tracing");
		} else {
			NJ.log("Adding tracing of length "+IJ.d2s(currTracing.length(),3)+" "+NJ.imageplus.getCalibration().getUnit());
			tracings.add(currTracing);
			if (NJ.adg != null) NJ.adg.reset();
			IJ.showStatus("Added tracing");
		}
		
		bTracingActive = false;
		bDijkstra = false;
		dirsimage = null;
	}
	
	private void finishCurrSegment() {
		
		if (currSegment.nrpoints() < 2) {
			NJ.log("Dumping segment of zero units length");
			IJ.showStatus("Dumped segment");
		} else {
			NJ.log("Adding segment of length "+IJ.d2s(currSegment.length(),3)+" "+NJ.imageplus.getCalibration().getUnit());
			currTracing.add(currSegment.duplicate());
			IJ.showStatus("Added segment");
		}
		
		currSegment.reset();
		bDijkstra = true;
	}
	
	private void updateCurrSegment() {
		
		currSegment.reset();
		
		if (currPoint.x != clckPoint.x || currPoint.y != clckPoint.y) {
			
			// Extract current segment from direction map:
			currSegment.add(new Point(currPoint));
			if (bManualTracing || dirsimage == null) {
				currSegment.add(new Point(clckPoint));
				currSegment.reverse();
			} else {
				final Point pnt = new Point(currPoint);
				while (pnt.x != clckPoint.x || pnt.y != clckPoint.y) {
					switch (dirsimage[pnt.y][pnt.x]) {
						case 0: { pnt.x = clckPoint.x; pnt.y = clckPoint.y; break; }
						case 1: { --pnt.x; --pnt.y; break; }
						case 2: { --pnt.y; break; }
						case 3: { ++pnt.x; --pnt.y; break; }
						case 4: { --pnt.x; break; }
						case 5: { ++pnt.x; break; }
						case 6: { --pnt.x; ++pnt.y; break; }
						case 7: { ++pnt.y; break; }
						case 8: { ++pnt.x; ++pnt.y; break; }
					}
					currSegment.add(new Point(pnt));
				}
				currSegment.reverse();
				// Smooth and subsample current segment:
				if (bSmoothSegment) smoothsample();
			}
		}
	}
	
	private void smoothsample() {
		
		// Copy current segment with borders:
		final Point pnt = new Point();
		currSegment.get(0,pnt);
		ssmpSegment.reset();
		for (int i=0; i<NJ.halfsmoothrange; ++i) { ssmpSegment.add(new Point(pnt)); }
		final int clckPoint = currSegment.nrpoints() - 1;
		for (int i=0; i<=clckPoint; ++i) { currSegment.get(i,pnt); ssmpSegment.add(new Point(pnt)); }
		for (int i=0; i<NJ.halfsmoothrange; ++i) { ssmpSegment.add(new Point(pnt)); }
		
		// Smooth and subsample except first and last point:
		int smppos = NJ.halfsmoothrange;
		ssmpSegment.get(smppos,pnt);
		currSegment.reset();
		currSegment.add(new Point(pnt));
		
		final float kernval = 1.0f/(2*NJ.halfsmoothrange + 1);
		final int lastsmp = clckPoint + NJ.halfsmoothrange;
		smppos += NJ.subsamplefactor;
		while (smppos < lastsmp) {
			ssmpSegment.get(smppos,pnt);
			float xpos = kernval*pnt.x;
			float ypos = kernval*pnt.y;
			for (int i=1; i<=NJ.halfsmoothrange; ++i) {
				ssmpSegment.get(smppos+i,pnt);
				xpos += kernval*pnt.x;
				ypos += kernval*pnt.y;
				ssmpSegment.get(smppos-i,pnt);
				xpos += kernval*pnt.x;
				ypos += kernval*pnt.y;
			}
			pnt.x = FMath.round(xpos);
			pnt.y = FMath.round(ypos);
			currSegment.add(new Point(pnt));
			smppos += NJ.subsamplefactor;
		}
		
		ssmpSegment.get(lastsmp,pnt);
		currSegment.add(new Point(pnt));
	}
	
	@Override
	public void mouseReleased(final MouseEvent e) {}
	
	private void showValue(final int xp, final int yp) {
		final Calibration cal = NJ.imageplus.getCalibration();
		ipgray.setCalibrationTable(cal.getCTable());
		IJ.showStatus(
			"x="+IJ.d2s(xp*cal.pixelWidth,2)+" ("+xp+"), "+
			"y="+IJ.d2s(yp*cal.pixelHeight,2)+" ("+yp+"), "+
			"value="+IJ.d2s(ipgray.getPixelValue(xp,yp),2)+" ("+ipgray.getPixel(xp,yp)+")"
		);
	}
	
	private void scroll(final int x, final int y) {
		
		final Rectangle vofRect = imc.getSrcRect();
		final double mag = imc.getMagnification();
		int newx = scrlPoint.x - (int)(x/mag);
		int newy = scrlPoint.y - (int)(y/mag);
		if (newx < 0) newx = 0;
		if (newy < 0) newy = 0;
		if ((newx + vofRect.width) > iXSize) newx = iXSize - vofRect.width;
		if ((newy + vofRect.height) > iYSize) newy = iYSize - vofRect.height;
		vofRect.x = newx;
		vofRect.y = newy;
		imp.draw();
		Thread.yield();
	}
	
	void eraseTracings() { tracings.reset(); redraw(); }
	
	void resetTracings() { tracings.reset(); }
	
	void setCursor(final Cursor c) { imc.setCursor(c); }
	
	void loadTracings(final String dir, final String file) {
		
		final String path = (dir.endsWith(File.separator) ? dir : dir+File.separator) + file;
		NJ.log("Loading tracings from "+path);
		
		try {
			final BufferedReader br = new BufferedReader(new FileReader(path));
			if (!br.readLine().startsWith("// "+NJ.NAME+" Data File")) throw new IOException();
			final String version = br.readLine();
			
			int brappear = NJ.appear;
			float brscale = NJ.scale;
			float brgamma = NJ.gamma;
			int brsnaprange = NJ.snaprange;
			int brdijkrange = NJ.dijkrange;
			int brhalfsmoothrange = NJ.halfsmoothrange;
			int brsubsamplefactor = NJ.subsamplefactor;
			int brlinewidth = NJ.linewidth;
			final String[] brtypes = new String[11];
			final Color[] brtypecolors = new Color[11];
			final String[] brclusters = new String[11];
			final Tracings brtracings = new Tracings();
			
			if (version.compareTo(NJ.VERSION) <= 0) {
				NJ.log("   Opened "+NJ.NAME+" version "+version+" data file");
				
				br.readLine(); // Parameters
				if (version.compareTo("1.4.0") >= 0) brappear = Integer.valueOf(br.readLine()).intValue();
				else brappear = 0; // Bright neurites by default for older file versions
				brscale = Float.valueOf(br.readLine()).floatValue();
				brgamma = Float.valueOf(br.readLine()).floatValue();
				brsnaprange = Integer.valueOf(br.readLine()).intValue();
				brdijkrange = Integer.valueOf(br.readLine()).intValue();
				brhalfsmoothrange = Integer.valueOf(br.readLine()).intValue();
				brsubsamplefactor = Integer.valueOf(br.readLine()).intValue();
				if (version.compareTo("1.1.0") >= 0) brlinewidth = Integer.valueOf(br.readLine()).intValue();
				if (version.compareTo("1.1.0") < 0) {
					br.readLine(); // Skip pixel x-size
					br.readLine(); // Skip pixel y-size
					br.readLine(); // Skip pixel units
					br.readLine(); // Skip auto-save option
					br.readLine(); // Skip log option
				}
				NJ.log("   Read parameters");
				
				br.readLine(); // Type names and colors
				for (int i=0; i<=10; ++i) {
					brtypes[i] = br.readLine();
					brtypecolors[i] = NJ.colors[Integer.valueOf(br.readLine()).intValue()]; }
					NJ.log("   Read type names and colors");
					
				br.readLine(); // Cluster names
				for (int i=0; i<=10; ++i) brclusters[i] = br.readLine();
				NJ.log("   Read cluster names");
				
				// Tracings
				String line = br.readLine();
				while (line.startsWith("// Tracing")) {
					final Tracing tracing = new Tracing();
					tracing.id(Integer.valueOf(br.readLine()).intValue());
					tracing.type(Integer.valueOf(br.readLine()).intValue());
					tracing.cluster(Integer.valueOf(br.readLine()).intValue());
					tracing.label(br.readLine());
					line = br.readLine();
					while (line.startsWith("// Segment")) {
						final Segment segment = new Segment();
						line = br.readLine();
						while (!line.startsWith("//")) {
							final Point pnt = new Point();
							pnt.x = Integer.valueOf(line).intValue();
							pnt.y = Integer.valueOf(br.readLine()).intValue();
							segment.add(pnt);
							line = br.readLine();
						}
						if (segment.length() > 0.0) tracing.add(segment);
					}
					if (tracing.length() > 0.0) brtracings.add(tracing);
				}
				NJ.log("   Read tracings");
				
			} else throw new IllegalStateException("Data file version "+version+" while running version "+NJ.VERSION);
			
			br.close();
			
			boolean bAppearChanged = false;
			if (NJ.appear != brappear) {
				bAppearChanged = true;
				NJ.appear = brappear;
			}
			boolean bScaleChanged = false;
			if (NJ.scale != brscale) {
				bScaleChanged = true;
				NJ.scale = brscale;
			}
			boolean bGammaChanged = false;
			if (NJ.gamma != brgamma) {
				bGammaChanged = true;
				NJ.gamma = brgamma;
			}
			NJ.snaprange = brsnaprange;
			NJ.dijkrange = brdijkrange;
			NJ.halfsmoothrange = brhalfsmoothrange;
			NJ.subsamplefactor = brsubsamplefactor;
			NJ.linewidth = brlinewidth;
			NJ.tracestroke = new BasicStroke(NJ.linewidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
			NJ.types = brtypes;
			NJ.typecolors = brtypecolors;
			NJ.clusters = brclusters;
			tracings = brtracings;
			NJ.log("   Effectuated read data");
			
			NJ.log("Done");
			NJ.save = false;
			
			IJ.showStatus("Loaded tracings from "+path);
			
			if (bComputedCosts) {
				if (bScaleChanged || bAppearChanged) { computeCosts(); doDijkstra(); }
				else if (bGammaChanged) { doDijkstra(); }
			}
			
		} catch (NumberFormatException e) {
			NJ.log("Error reading from file");
			NJ.error("Error reading from file");
			NJ.copyright();
			
		} catch (IllegalStateException e) {
			NJ.log(e.getMessage());
			NJ.error(e.getMessage());
			NJ.copyright();
			
		} catch (Throwable e) {
			NJ.log("Unable to read from file");
			NJ.error("Unable to read from file");
			NJ.copyright();
		}
		
		if (NJ.mdg != null) NJ.mdg.reset();
		if (NJ.adg != null) NJ.adg.reset();
		
		redraw();
	}
	
	void closeTracings() {
		
		String status = "Dumped image";
		if (NJ.save) {
			if (NJ.autosave) {
				NJ.log("Automatically saving tracings");
				saveTracings(NJ.workdir,NJ.imagename+".ndf");
				status += " but saved tracings";
			} else {
				NJ.log("Asking user to save tracings");
				final YesNoDialog ynd = new YesNoDialog("Save","Do you want to save the tracings?");
				if (ynd.yesPressed()) {
					final FileDialog fdg = new FileDialog(IJ.getInstance(),NJ.NAME+": Save",FileDialog.SAVE);
					fdg.setFilenameFilter(new ImageDataFilter());
					fdg.setFile(NJ.imagename+".ndf");
					fdg.setVisible(true);
					final String dir = fdg.getDirectory();
					final String file = fdg.getFile();
					fdg.dispose();
					if (dir != null && file != null) {
						saveTracings(dir,file);
						status += " but saved tracings";
					} else {
						NJ.log("Dumping tracings");
						status += " and tracings";
					}
				} else {
					NJ.log("Dumping tracings");
					status += " and tracings";
				}
			}
		} else NJ.log("No need to save current tracings");
		
		costs = null; // To free more memory
		IJ.showStatus(status);
	}
	
	void saveTracings(final String dir, final String file) {
		
		final String path = (dir.endsWith(File.separator) ? dir : dir+File.separator) + file;
		NJ.log("Saving tracings to "+path);
		
		try {
			final FileWriter fw = new FileWriter(path);
			fw.write("// "+NJ.NAME+" Data File - DO NOT CHANGE\n");
			fw.write(NJ.VERSION+"\n");
			NJ.log("   Opened "+NJ.NAME+" version "+NJ.VERSION+" data file");
			
			fw.write("// Parameters\n");
			fw.write(NJ.appear+"\n");
			fw.write(NJ.scale+"\n");
			fw.write(NJ.gamma+"\n");
			fw.write(NJ.snaprange+"\n");
			fw.write(NJ.dijkrange+"\n");
			fw.write(NJ.halfsmoothrange+"\n");
			fw.write(NJ.subsamplefactor+"\n");
			fw.write(NJ.linewidth+"\n");
			NJ.log("   Wrote parameters");
			
			fw.write("// Type names and colors\n");
			final int nrtypes = NJ.types.length;
			for (int i=0; i<nrtypes; ++i) fw.write(NJ.types[i]+"\n"+NJ.colorIndex(NJ.typecolors[i])+"\n");
			NJ.log("   Wrote type names and colors");
			
			fw.write("// Cluster names\n");
			final int nrclusters = NJ.clusters.length;
			for (int i=0; i<nrclusters; ++i) fw.write(NJ.clusters[i]+"\n");
			NJ.log("   Wrote cluster names");
			
			final int nrtracings = tracings.nrtracings();
			for (int n=0; n<nrtracings; ++n) {
				final Tracing tracing = tracings.get(n);
				fw.write("// Tracing N"+tracing.id()+"\n");
				fw.write(tracing.id()+"\n");
				fw.write(tracing.type()+"\n");
				fw.write(tracing.cluster()+"\n");
				fw.write(tracing.label()+"\n");
				final int nrsegments = tracing.nrsegments();
				for (int s=0; s<nrsegments; ++s) {
					fw.write("// Segment "+(s+1)+" of Tracing N"+tracing.id()+"\n");
					final Segment segment = tracing.get(s);
					final int nrpoints = segment.nrpoints();
					for (int p=0; p<nrpoints; ++p) {
						final Point pnt = segment.get(p);
						fw.write(pnt.x+"\n"+pnt.y+"\n");
					}
				}
			}
			NJ.log("   Wrote tracings");
			
			fw.write("// End of "+NJ.NAME+" Data File\n");
			fw.close();
			NJ.log("Done");
			IJ.showStatus("Saved tracings to "+path);
			NJ.save = false;
			
		} catch (IOException ioe) {
			NJ.log("Unable to write to file");
			NJ.error("Unable to write to file");
			NJ.copyright();
		}
	}
	
	void exportTracings(final String dir, final String file, final int type) {
		
		final String path = (dir.endsWith(File.separator) ? dir : dir+File.separator) + file;
		
		try {
			boolean separate = false;
			String delim = "\t";
			switch (type) {
				case 0: // Tab-del single
					separate = false;
					delim = "\t";
					break;
				case 1: // Tab-del separate
					separate = true;
					delim = "\t";
					break;
				case 2: // Comma-del single
					separate = false;
					delim = ",";
					break;
				case 3: // Comma-del separate
					separate = true;
					delim = ",";
					break;
				case 4: // Segmented line selections
					separate = true;
					delim = "";
					break;
			}
			if (separate) {
				final FileWriter fw = new FileWriter(path);
				String pathbase, pathext;
				final int lastdot = path.lastIndexOf('.');
				if (lastdot < 0) {
					pathbase = path.substring(0,path.length());
					pathext = "";
				} else {
					pathbase = path.substring(0,lastdot);
					pathext = path.substring(lastdot,path.length());
				}
				if (type == 4) pathext = ".roi";
				final int nrt = tracings.nrtracings();
				for (int t=0; t<nrt; ++t) {
					final Tracing tracing = tracings.get(t);
					final String tpath = pathbase+".N"+tracing.id()+pathext;
					NJ.log("Exporting tracing to "+tpath);
					fw.write("Tracing N"+tracing.id()+": "+tpath+"\n");
					if (type == 4) {
						final int nrs = tracing.nrsegments();
						// First determine number of points:
						int nrptotal = 0;
						for (int s=0, p0=0; s<nrs; ++s, p0=1)
							nrptotal += tracing.get(s).nrpoints() - p0;
						// Extract points into arrays:
						final int[] xcoords = new int[nrptotal];
						final int[] ycoords = new int[nrptotal];
						for (int s=0, p=0, p0=0; s<nrs; ++s, p0=1) {
							final Segment segment = tracing.get(s);
							final int nrp = segment.nrpoints();
							for (int pi=p0; pi<nrp; ++pi, ++p) {
								final Point pnt = segment.get(pi);
								xcoords[p] = pnt.x;
								ycoords[p] = pnt.y;
							}
						}
						// Convert arrays to ROI and save:
						final PolygonRoi roi = new PolygonRoi(xcoords,ycoords,nrptotal,Roi.POLYLINE);
						final RoiEncoder roienc = new RoiEncoder(tpath);
						roienc.write(roi);
					} else {
						final FileWriter tfw = new FileWriter(tpath);
						final int nrs = tracing.nrsegments();
						for (int s=0, p0=0; s<nrs; ++s, p0=1) {
							final Segment segment = tracing.get(s);
							final int nrp = segment.nrpoints();
							for (int p=p0; p<nrp; ++p) {
								final Point pnt = segment.get(p);
								tfw.write(pnt.x+delim+pnt.y+"\n");
							}
						}
						tfw.close();
					}
				}
				fw.close();
				NJ.log("Done");
				IJ.showStatus("Exported tracings to "+path);
			} else {
				NJ.log("Exporting tracings to "+path);
				final FileWriter fw = new FileWriter(path);
				final int nrt = tracings.nrtracings();
				for (int t=0; t<nrt; ++t) {
					final Tracing tracing = tracings.get(t);
					fw.write("Tracing N"+tracing.id()+":\n");
					final int nrs = tracing.nrsegments();
					for (int s=0, p0=0; s<nrs; ++s, p0=1) {
						final Segment segment = tracing.get(s);
						final int nrp = segment.nrpoints();
						for (int p=p0; p<nrp; ++p) {
							final Point pnt = segment.get(p);
							fw.write(pnt.x+delim+pnt.y+"\n");
						}
					}
				}
				fw.close();
				NJ.log("Done");
				IJ.showStatus("Exported tracings to "+path);
			}
		} catch (Throwable e) {
			NJ.log("Unable to write to file");
			NJ.error("Unable to write to file");
			NJ.copyright();
		}
	}
	
}