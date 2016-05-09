package yang.plugin.tracing;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;
import ij.io.Opener;
import ij.plugin.BrowserLauncher;
import ij.process.ColorProcessor;

final class TracingToolbar extends Canvas implements MouseListener, MouseMotionListener, WindowListener {
	
	public static final int ERASE = 0;
	public static final int LOAD = 1;
	public static final int SAVE = 2;
	public static final int EXPORT = 3;
	public static final int SPACE1 = 4;
	public static final int ADD = 5;
	public static final int DELETE = 6;
	public static final int MOVE = 7;
	public static final int MEASURE = 8;
	public static final int ATTRIBS = 9;
	public static final int PARAMS = 10;
	public static final int SNAPSHOT = 11;
	public static final int MAGNIFY = 12;
	public static final int SCROLL = 13;
	public static final int SPACE2 = 14;
	public static final int HELP = 15;
	public static final int QUIT = 16;
	
	private static final int NUM_TOOLS = 17;
	private static final int LAST_TOOL = 16;
	private static final int SIZE = 22;
	private static final int OFFSET = 5;
	
	private int iPreviousTool = MAGNIFY;
	private int iCurrentTool = MAGNIFY;
	
	private static final Color gray = ImageJ.backgroundColor;
	private static final Color brighter = gray.brighter();
	private static final Color darker = gray.darker();
	private static final Color evenDarker = darker.darker();
	
	private final boolean[] down = new boolean[NUM_TOOLS];
	
	private Graphics g;
	private Toolbar previousToolbar;
	private ImagePlus imp;
	private ImageWindow imw;
	
	private int x;
	private int y;
	private int xOffset;
	private int yOffset;
	
	TracingToolbar() {
		
		// Set current ImageJ tool to magnifier so that all other images
		// will get magnified when clicked:
		IJ.setTool(Toolbar.MAGNIFIER);
		
		// Remove previous Toolbar and add present TracingToolbar:
		NJ.log("Removing current toolbar...");
		previousToolbar = Toolbar.getInstance();
		final Container container = previousToolbar.getParent();
		final Component[] component = container.getComponents();
		for (int i=0; i<component.length; ++i)
			if (component[i] == previousToolbar) {
				container.remove(previousToolbar);
				container.add(this, i);
				break;
			}
		
		// Reset tool buttons and set current tool:
		NJ.log("Installing "+NJ.NAME+" toolbar...");
		for (int i=0; i<NUM_TOOLS; ++i) down[i] = false;
		resetTool();
		
		// Other initializations:
		setForeground(gray);
		setBackground(gray);
		addMouseListener(this);
		addMouseMotionListener(this);
		container.validate();
	}
	
	int currentTool() { return iCurrentTool; }
	
	int previousTool() { return iPreviousTool; }
	
	@Override
	public void mouseClicked(final MouseEvent e) {}
	
	@Override
	public void mouseEntered(final MouseEvent e) {}
	
	@Override
	public void mouseExited(final MouseEvent e) { NJ.copyright(); }
	
	@Override
	public void mousePressed(final MouseEvent e) { try {
		
		// Determine which tool button was pressed:
		final int x = e.getX(); int iNewTool;
		for (iNewTool=0; iNewTool<NUM_TOOLS; ++iNewTool)
			if (x > iNewTool*SIZE && x < (iNewTool+1)*SIZE) break;
		setTool(iNewTool);
		
		// Carry out actions for selected tool:
		switch (iNewTool) {
			case ADD: {
				if (!NJ.image) {
					NJ.noImage();
					setPreviousTool();
				} else if (!NJ.nhd.computedCosts())
				NJ.nhd.computeCosts();
				break;
			}
			case DELETE:
			case MOVE: {
				if (!NJ.image) {
					NJ.noImage();
					setPreviousTool();
				}
				break;
			}
			case ATTRIBS: {
				if (NJ.adg == null)
				if (!NJ.image) { NJ.noImage(); setPreviousTool(); }
				else NJ.adg = new AttributesDialog();
				break;
			}
			case MEASURE: {
				if (NJ.mdg == null)
				if (!NJ.image) { NJ.noImage(); setPreviousTool(); }
				else NJ.mdg = new MeasurementsDialog();
				break;
			}
			case ERASE: {
				if (!NJ.image) NJ.noImage();
				else {
					final YesNoDialog ynd = new YesNoDialog("Erase","Do you really want to erase all tracings?");
					if (ynd.yesPressed()) {
						NJ.log("Erasing all tracings");
						NJ.nhd.eraseTracings();
						if (NJ.adg != null) NJ.adg.reset();
						IJ.showStatus("Erased all tracings");
					} else { NJ.copyright(); }
				}
				setPreviousTool();
				break;
			}
			case PARAMS: {
				final ParametersDialog pd = new ParametersDialog();
				if (NJ.image && NJ.nhd.computedCosts()) {
					if (pd.scaleChanged() || pd.appearChanged()) { NJ.nhd.computeCosts(); NJ.nhd.doDijkstra(); }
					else if (pd.gammaChanged()) { NJ.nhd.doDijkstra(); }
				}
				setPreviousTool();
				break;
			}
			case SNAPSHOT: {
				if (!NJ.image) NJ.noImage();
				else {
					final SnapshotDialog sdg = new SnapshotDialog();
					if (sdg.wasCanceled()) {
						NJ.copyright();
					} else {
						final ColorProcessor cp = NJ.nhd.makeSnapshot(sdg.drawImage(),sdg.drawTracings());
						if (cp != null) {
							final String title = NJ.usename ? (NJ.imagename+"-snapshot") : (NJ.NAME+": Snapshot");
							final ImagePlus ssimp = new ImagePlus(title,cp);
							ssimp.show(); ssimp.updateAndRepaintWindow();
							IJ.showStatus("Generated snapshot image");
						} else NJ.copyright();
					}
				}
				setPreviousTool();
				break;
			}
			case HELP: {
				try {
					NJ.log("Opening default browser showing online "+NJ.NAME+" manual");
					BrowserLauncher.openURL("http://www.imagescience.org/meijering/software/neuronj/manual/");
				} catch (Throwable throwable) {
					NJ.error("Could not open default internet browser");
				}
				setPreviousTool();
				break;
			}
			case QUIT: {
				final YesNoDialog ynd = new YesNoDialog("Quit","Do you really want to quit "+NJ.NAME+"?");
				if (ynd.yesPressed()) NJ.quit();
				else setPreviousTool();
				break;
			}
			case LOAD: {
				final FileDialog fdg = new FileDialog(IJ.getInstance(),NJ.NAME+": Load",FileDialog.LOAD);
				fdg.setFilenameFilter(new ImageDataFilter());
				fdg.setVisible(true);
				final String dir = fdg.getDirectory();
				final String file = fdg.getFile();
				fdg.dispose();
				if (dir != null && file != null) {
					final String ext = file.substring(file.lastIndexOf(".")+1);
					if (ext.equalsIgnoreCase("ndf")) {
						if (!NJ.image) NJ.noImage();
						else NJ.nhd.loadTracings(dir,file);
					} else {
						final boolean bLoaded = loadImage(dir,file);
						if (bLoaded) {
							final File images = new File(NJ.workdir);
							NJ.workimages = images.list(new ImageFilter());
							if (NJ.workimages != null && NJ.workimages.length > 0) {
								NJ.log("Found "+NJ.workimages.length+" images in "+NJ.workdir);
								for (int i=0; i<NJ.workimages.length; ++i)
									if (NJ.workimages[i].equals(file)) {
										NJ.workimagenr = i;
										NJ.log("Loaded image is number "+NJ.workimagenr+" on the list");
										break;
									}
							}
						}
					}
				} else NJ.copyright();
				setPreviousTool();
				break;
			}
			case SAVE: {
				if (!NJ.image) NJ.noImage();
				else {
					final FileDialog fdg = new FileDialog(IJ.getInstance(),NJ.NAME+": Save",FileDialog.SAVE);
					fdg.setFilenameFilter(new ImageDataFilter());
					fdg.setFile(NJ.imagename+".ndf");
					fdg.setVisible(true);
					final String dir = fdg.getDirectory();
					final String file = fdg.getFile();
					fdg.dispose();
					if (dir != null && file != null) NJ.nhd.saveTracings(dir,file);
					else NJ.copyright();
				}
				setPreviousTool();
			}
			case EXPORT: {
				if (!NJ.image) NJ.noImage();
				else {
					final ExportDialog edg = new ExportDialog();
					if (!edg.wasCanceled()) {
						final FileDialog fdg = new FileDialog(IJ.getInstance(),NJ.NAME+": Export",FileDialog.SAVE);
						fdg.setFilenameFilter(new ImageDataFilter());
						fdg.setFile(NJ.imagename+".txt");
						fdg.setVisible(true);
						final String dir = fdg.getDirectory();
						final String file = fdg.getFile();
						fdg.dispose();
						if (dir != null && file != null) NJ.nhd.exportTracings(dir,file,edg.lastChoice());
						else NJ.copyright();
					} else {
						NJ.copyright();
					}
				}
				setPreviousTool();
				break;
			}
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	boolean loadImage(final String dir, final String file) {
		
		final String directory = dir.endsWith(File.separator) ? dir : dir+File.separator;
		final ImagePlus newImp = (new Opener()).openImage(directory,file);
		boolean bAccept = false;
		
		if (newImp != null) {
			
			NJ.log("Checking image "+directory+file);
			final int type = newImp.getType();
			if (type != ImagePlus.GRAY8 && type != ImagePlus.COLOR_256)
			NJ.error("Only 8-bit images are supported");
			else if (newImp.getStackSize() != 1)
			NJ.error("Image stacks are not supported");
			else if (newImp.getWidth() < 3)
			NJ.error("Image too small in x-dimension");
			else if (newImp.getHeight() < 3)
			NJ.error("Image too small in y-dimension");
			else bAccept = true;
			
			if (bAccept) {
				NJ.log("Image accepted");
				if (NJ.image) {
					NJ.nhd.closeTracings();
					// Close image but first restore listeners to avoid
					// a call to windowClosed():
					restoreListeners();
					NJ.log("Closing current image...");
					imp.hide();
				}
				NJ.workdir = directory;
				NJ.image(newImp);
				imp = newImp; imp.show();
				imw = imp.getWindow();
				imw.addWindowListener(this);
				NJ.nhd.attach(imp);
				IJ.showStatus("Loaded image from "+directory+file);
				iPreviousTool = MAGNIFY;
				final String ndfile = NJ.imagename + ".ndf";
				final File ndf = new File(directory + ndfile);
				if (ndf.exists()) {
					NJ.log("Data file exists for loaded image");
					NJ.nhd.loadTracings(directory,ndfile);
				} else {
					NJ.log("Found no data file for loaded image");
					if (NJ.adg != null) NJ.adg.reset();
				}
				NJ.save = false;
			} else {
				NJ.log("Image not accepted");
				NJ.copyright();
			}
		} else {
			NJ.error("Unable to load image");
			NJ.copyright();
		}
		
		return bAccept;
	}
	
	void restoreListeners() { imw.removeWindowListener(this); }
	
	void restoreToolbar() {
		
		NJ.log("Restoring toolbar");
		final Container container = this.getParent();
		final Component component[] = container.getComponents();
		for (int i=0; i<component.length; ++i) {
			if (component[i] == this) {
				container.remove(this);
				container.add(previousToolbar, i);
				container.validate();
				break;
			}
		}
		previousToolbar.repaint();
	}
	
	@Override
	public void mouseReleased(final MouseEvent e) {}
	
	@Override
	public void mouseDragged(final MouseEvent e) {}
	
	@Override
	public void mouseMoved(final MouseEvent e) { try {
		
		final int x = e.getX();
		for (int i=0; i<NUM_TOOLS; ++i)
			if (x > i*SIZE && x < (i+1)*SIZE) {
				showMessage(i);
				break;
			}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void paint(final Graphics g) {
		
		for (int i=0; i<NUM_TOOLS; ++i) drawButton(g,i);
	}
	
	private void setPreviousTool() {
		
		// To avoid having to reopen the attributes or measurements dialogs:
		if (iPreviousTool == ATTRIBS || iPreviousTool == MEASURE) setTool(MAGNIFY);
		else setTool(iPreviousTool);
	}
	
	void resetTool() { setTool(MAGNIFY); }
	
	void setTool(int tool) {
		
		// Check validity:
		if (tool < 0 ||
			tool == SPACE1 ||
			tool == SPACE2 ||
			tool > LAST_TOOL ||
			tool == iCurrentTool)
			return;
		
		// Reset current tool:
		down[iCurrentTool] = false;
		down[tool] = true;
		final Graphics g = this.getGraphics();
		drawButton(g,iCurrentTool);
		drawButton(g,tool);
		g.dispose();
		iPreviousTool = iCurrentTool;
		iCurrentTool = tool;
		if (iCurrentTool != ATTRIBS && NJ.adg != null) NJ.adg.close();
		if (iCurrentTool != MEASURE && NJ.mdg != null) NJ.mdg.close();
		
		// Adapt cursor to current tool:
		if (NJ.image) switch (iCurrentTool) {
			case ADD: NJ.nhd.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR)); break;
			case DELETE:
			case MOVE:
			case MEASURE: NJ.nhd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); break;
			case MAGNIFY: NJ.nhd.setCursor(new Cursor(Cursor.MOVE_CURSOR)); break;
			case SCROLL: NJ.nhd.setCursor(new Cursor(Cursor.HAND_CURSOR)); break;
			default: NJ.nhd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); break;
		}
	}
	
	private void drawButton(final Graphics g, final int tool) {
		
		if (tool==SPACE1 || tool==SPACE2) return;
		
		fill3DRect(g, tool*SIZE + 1, 1, SIZE, SIZE - 1, !down[tool]);
		int x = tool*SIZE + OFFSET;
		int y = OFFSET;
		this.g = g;
		
		switch (tool) {
			case ERASE:
				xOffset = x + 1;
				yOffset = y;
				g.setColor(Color.black);
				m(0,0); d(7,0); d(10,3); d(10,12); d(0,12); d(0,0);
				m(7,0); d(7,3); d(10,3);
				g.setColor(Color.white);
				m(8,2); d(8,2);
				for (int i=1; i<=3; ++i) { m(1,i); d(6,i); }
				for (int i=4; i<=11; ++i) { m(1,i); d(9,i); }
				break;
			case LOAD:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.black);
				m(7,1); d(8,0); d(10,0); d(13,3); d(13,1); m(13,3); d(11,3);
				m(0,4); d(1,3); d(3,3); d(4,4); d(9,4); d(9,7); d(5,7); d(0,12); d(0,4);
				m(9,7); d(13,7); d(8,12); d(0,12);
				g.setColor(Color.yellow.darker());
				m(1,4); d(3,4);
				m(1,5); d(8,5);
				m(1,6); d(8,6);
				m(1,7); d(4,7);
				m(1,8); d(3,8);
				m(1,9); d(2,9);
				m(1,10); d(1,10);
				g.setColor(evenDarker);
				m(5,8); d(11,8);
				m(4,9); d(10,9);
				m(3,10); d(9,10);
				m(2,11); d(8,11);
				break;
			case SAVE:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.black);
				m(0,0); d(13,0); d(13,12); d(1,12); d(0,11); d(0,0);
				m(2,0); d(2,5); d(3,6); d(10,6); d(11,5); d(11,0);
				m(11,2); d(13,2);
				m(3,12); d(3,8); d(11,8); d(11,12);
				m(3,9); d(8,9);
				m(3,10); d(8,10);
				m(3,11); d(8,11);
				g.setColor(Color.red.darker());
				m(1,1); d(1,11);
				m(2,6); d(2,11);
				m(2,7); d(11,7);
				m(11,6); d(11,7);
				m(12,3); d(12,11);
				break;
			case EXPORT:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.black);
				m(7,0); d(13,0); d(13,12); d(1,12); d(0,11); d(0,6); d(3,6);
				m(5,6); d(10,6); d(11,5); d(11,0);
				m(11,2); d(13,2);
				m(3,12); d(3,8); d(11,8); d(11,12);
				m(3,9); d(8,9);
				m(3,10); d(8,10);
				m(3,11); d(8,11);
				m(0,0); d(0,4); m(1,0); d(1,4); m(2,0); d(2,4); m(3,0); d(3,4);
				m(4,-1); d(4,5); m(5,0); d(5,4); m(6,1); d(6,3); d(7,2);
				g.setColor(Color.green.darker());
				m(1,7); d(1,11);
				m(2,7); d(2,11);
				m(2,7); d(11,7);
				m(11,6); d(11,7);
				m(12,3); d(12,11);
				break;
			case ADD:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.green.darker().darker().darker());
				m(0,4); d(2,4); d(2,3); d(4,3); d(4,2); d(6,2); d(6,1); d(8,1); d(8,0); d(9,0);
				m(3,12); d(3,10); d(4,10); d(4,9); d(5,9); d(5,8); d(6,8); d(6,7); d(7,7); d(7,6);
				d(8,6); d(8,5); d(9,5); d(9,4); d(11,4); d(11,3); d(13,3);
				g.setColor(Color.black);
				m(9,10); d(13,10); m(11,8); d(11,12);
				break;
			case DELETE:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.green.darker().darker().darker());
				m(0,4); d(2,4); d(2,3); d(4,3); d(4,2); d(6,2); d(6,1); d(8,1); d(8,0); d(9,0);
				m(3,12); d(3,10); d(4,10); d(4,9); d(5,9); d(5,8); d(6,8); d(6,7); d(7,7); d(7,6);
				d(8,6); d(8,5); d(9,5); d(9,4); d(11,4); d(11,3); d(13,3);
				g.setColor(Color.black);
				m(9,10); d(13,10);
				break;
			case MOVE:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.green.darker().darker().darker());
				m(13,0); d(10,0); d(10,1); d(8,1); d(8,2); d(4,2); d(3,3); d(7,3); d(7,4);
				d(3,4); d(3,5); d(7,5); d(6,6); d(2,6); d(2,7); d(1,7); d(1,9); d(0,9); d(0,12);
				g.setColor(Color.black);
				m(9,7); d(9,12); d(10,11); d(10,8); d(11,9); d(11,10); d(12,10);
				break;
			case MEASURE:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.green.darker().darker().darker());
				m(0,0); d(1,0); d(1,1); d(2,1); m(2,2); d(7,2); d(7,1); d(8,1); d(8,0); d(9,0);
				m(0,4); d(1,4); d(1,5); d(3,5); m(3,6); d(8,6); d(8,5); d(10,5); d(10,4); d(11,4); d(11,3);
				d(12,3); d(12,2); d(13,2); m(13,1);
				g.setColor(Color.black);
				m(0,10); d(2,8); d(2,12); d(0,10); d(13,10); d(11,8); d(11,12); d(13,10);
				break;
			case ATTRIBS:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.red);
				for (int i=0; i<=5; ++i) { m(0,i); d(5,i); }
				g.setColor(Color.yellow);
				for (int i=0; i<=5; ++i) { m(7,i); d(12,i); }
				g.setColor(Color.green);
				for (int i=7; i<=12; ++i) { m(0,i); d(5,i); }
				g.setColor(Color.blue);
				for (int i=7; i<=12; ++i) { m(7,i); d(12,i); }
				g.setColor(Color.black);
				m(0,0); d(5,0); d(5,5); d(0,5); d(0,0);
				m(7,0); d(12,0); d(12,5); d(7,5); d(7,0);
				m(0,7); d(5,7); d(5,12); d(0,12); d(0,7);
				m(7,7); d(12,7); d(12,12); d(7,12); d(7,7);
				break;
			case PARAMS:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.black);
				m(0,0); d(5,0); d(5,5); d(0,5); d(0,0);
				m(0,7); d(5,7); d(5,12); d(0,12); d(0,7);
				m(7,2); d(13,2); m(7,4); d(13,4);
				m(7,9); d(13,9); m(7,11); d(13,11);
				g.setColor(Color.white);
				m(1,1); d(4,1); m(1,2); d(4,2); m(1,3); d(4,3); m(1,4); d(4,4);
				m(1,8); d(4,8); m(1,9); d(4,9); m(1,10); d(4,10); m(1,11); d(4,11);
				break;
			case SNAPSHOT:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.black);
				m(1,1); d(12,1); d(13,2); d(13,11); d(12,12); d(1,12); d(0,11); d(0,2);
				m(7,0); d(12,0); d(11,-1); d(8,-1);
				m(5,4); d(8,4); d(8,5); d(9,5); d(9,8); d(8,8); d(8,9); d(5,9); d(5,8); d(4,8); d(4,5); d(5,5);
				g.setColor(Color.white);
				m(9,0); d(10,0);
				m(6,5); d(6,8); m(7,5); d(7,8); m(5,6); d(5,7); m(8,6); d(8,7);
				g.setColor(evenDarker);
				m(1,2); d(12,2); d(12,11); d(1,11); d(1,3); d(11,3); d(11,10); d(2,10); d(2,4);
				m(4,4); d(3,4); d(3,9); d(4,9); m(9,4); d(10,4); d(10,9); d(9,9);
				break;
			case MAGNIFY:
				xOffset = x;
				yOffset = y;
				g.setColor(Color.black);
				m(0,3); d(3,0); d(5,0); d(8,3); d(8,5); d(5,8); d(3,8); d(0,5); d(0,3);
				m(1,1); d(1,1); m(7,1); d(7,1); m(1,7); d(1,7);
				m(8,7); d(12,11); m(7,8); d(11,12); m(7,7); d(12,12);
				g.setColor(Color.white);
				m(3,1); d(5,1);
				m(2,2); d(6,2);
				m(1,3); d(7,3);
				m(1,4); d(7,4);
				m(1,5); d(7,5);
				m(2,6); d(6,6);
				m(3,7); d(5,7);
				g.setColor(Color.black);
				m(4,6); d(5,6); d(6,5); d(6,4);
				break;
			case SCROLL:
				xOffset = x-1;
				yOffset = y-1;
				g.setColor(Color.black);
				m(2,1); d(3,1); d(4,2); d(4,3); d(5,3); d(5,5); d(5,1); d(6,0); d(7,0); d(8,1); d(8,5);
				m(9,1); d(10,1); d(11,2); d(11,6); m(12,4); d(13,3); d(14,4); d(14,7); d(13,8); d(13,10); d(12,11); d(12,12); d(11,13);
				m(4,13); d(3,12); d(2,11); d(1,10); d(0,9); d(0,8); d(1,7); d(2,7); d(3,8); d(3,6); d(2,5); d(2,4); d(1,3); d(1,2);
				g.setColor(Color.white);
				m(2,2); d(3,2); d(3,3); d(2,3); d(3,4); d(4,4); d(4,5); d(3,5);
				m(6,1); d(6,5); d(7,5); d(7,1);
				m(9,2); d(9,5); d(10,5); d(10,2);
				m(12,6); d(12,5); d(13,4); d(13,6);
				m(4,6); d(10,6); m(4,7); d(13,7); m(4,8); d(12,8);
				m(2,8); d(1,8); d(1,9); d(12,9); d(12,10); d(2,10); d(3,11); d(11,11); d(11,12); d(4,12); d(5,13); d(10,13);
				break;
			case HELP:
				xOffset = x;
				yOffset = y-1;
				g.setColor(Color.black);
				m(2,3); d(5,0); d(8,0); d(11,3); d(11,5); d(7,9); d(7,10); d(6,11); d(7,12); d(6,13); d(5,13);
				d(4,12); d(5,11); d(4,10); d(4,9); d(8,5); d(8,4); d(7,3); d(6,3); d(4,5); d(3,5); d(2,4);
				g.setColor(Color.white);
				m(5,1); d(8,1); m(4,2); d(9,2); m(3,3); d(5,3); m(3,4); d(4,4);
				m(8,3); d(10,3); m(9,4); d(10,4); m(9,5); d(5,9); d(5,10); d(6,10); d(6,9); d(10,5);
				m(5,12); d(6,12);
				break;
			case QUIT:
				xOffset = x;
				yOffset = y-1;
				g.setColor(Color.black);
				m(0,0); d(8,0); d(8,9); d(5,9); d(5,5); d(0,0); d(0,8); d(5,13); d(5,9);
				m(11,5); d(11,5); m(10,6); d(12,6); m(9,7); d(13,7);
				m(10,8); d(12,8); m(10,9); d(12,9); m(10,10); d(12,10);
				m(9,11); d(11,11); m(8,12); d(10,12); m(7,13); d(8,13);
				g.setColor(Color.white);
				m(2,1); d(7,1); m(3,2); d(7,2); m(4,3); d(7,3); m(5,4); d(7,4);
				m(6,5); d(6,8); m(7,5); d(7,8);
				g.setColor(evenDarker);
				m(1,2); d(1,8); m(2,3); d(2,9); m(3,4); d(3,10); m(4,5); d(4,11);
				break;
		}
	}
	
	private void fill3DRect(
		final Graphics g,
		final int x,
		final int y,
		final int width,
		final int height,
		final boolean raised
	) {
		
		if (raised) g.setColor(gray);
		else g.setColor(darker);
		
		g.fillRect(x + 1, y + 1, width - 2, height - 2);
		g.setColor(raised ? brighter : evenDarker);
		g.drawLine(x, y, x, y + height - 1);
		g.drawLine(x + 1, y, x + width - 2, y);
		g.setColor((raised) ? (evenDarker) : (brighter));
		g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
		g.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
	}
	
	private void m(final int x, final int y) {
		
		this.x = xOffset + x;
		this.y = yOffset + y;
	}
	
	private void d(int x, int y) {
		
		x += xOffset;
		y += yOffset;
		g.drawLine(this.x, this.y, x, y);
		this.x = x;
		this.y = y;
	}
	
	private void showMessage (final int tool) {
		
		switch (tool) {
			case ERASE: IJ.showStatus("Erase tracings"); break;
			case LOAD: IJ.showStatus("Load image/tracings"); break;
			case SAVE: IJ.showStatus("Save tracings"); break;
			case EXPORT: IJ.showStatus("Export tracings"); break;
			case ADD: IJ.showStatus("Add tracings"); break;
			case DELETE: IJ.showStatus("Delete tracings"); break;
			case MOVE: IJ.showStatus("Move vertices"); break;
			case MEASURE: IJ.showStatus("Measure tracings"); break;
			case ATTRIBS: IJ.showStatus("Label tracings"); break;
			case PARAMS: IJ.showStatus("Set parameters"); break;
			case SNAPSHOT: IJ.showStatus("Make snapshot"); break;
			case MAGNIFY: IJ.showStatus("Zoom in/out"); break;
			case SCROLL: IJ.showStatus("Scroll canvas"); break;
			case HELP: IJ.showStatus("Open online manual"); break;
			case QUIT: IJ.showStatus("Quit "+NJ.NAME); break;
			default: NJ.copyright(); break;
		}
	}
	
	@Override
	public void windowActivated(WindowEvent e) { }
	
	@Override
	public void windowClosed(WindowEvent e) { try {
		
		NJ.log("Image window closed by user");
		NJ.nhd.closeTracings();
		NJ.image(null);
		NJ.nhd.resetTracings();
		if (NJ.adg != null) NJ.adg.reset();
		resetTool();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void windowClosing(WindowEvent e) { }
	
	@Override
	public void windowDeactivated(WindowEvent e) { }
	
	@Override
	public void windowDeiconified(WindowEvent e) { }
	
	@Override
	public void windowIconified(WindowEvent e) { }
	
	@Override
	public void windowOpened(WindowEvent e) { }
	
}
