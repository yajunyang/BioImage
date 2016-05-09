package yang.plugin.tracing;

import java.awt.BasicStroke;
import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Toolbar;
import ij.text.TextPanel;
import ij.text.TextWindow;
import imagescience.ImageScience;

final class NJ {
	
	// NeuronJ name and version number:
	static final String NAME = "NeuronJ";
	static final String VERSION = "1.4.3";
	
	// Initialization operations:
	static void init() {
		
		final long lStartTime = System.currentTimeMillis();
		
		// Activate uncaught exception catcher:
		catcher = new Catcher();
		try { Thread.currentThread().setUncaughtExceptionHandler(catcher); }
		catch (final Throwable e) { }
		
		// Before doing anything, load the parameter settings, which also determine whether or not to show log messages:
		loadParams();
		
		// Show version numbers:
		log("Running on "+System.getProperty("os.name")+" version "+System.getProperty("os.version"));
		log("Running on Java version "+System.getProperty("java.version"));
		log("Running on ImageJ version "+IJ.getVersion());
		log("Running on ImageScience version "+ImageScience.version());
		
		Toolbar.getInstance();
		// Store relevant last settings:
		lasttool = Toolbar.getToolId();
		lastdoublebuffering = Prefs.doubleBuffer;
		Prefs.doubleBuffer = true;
		
		// Install NeuronJ toolbar and handler:
		ntb = new TracingToolbar();
		nhd = new TracingHandler();
		
		NJ.copyright();
		log("Initialization completed in "+(System.currentTimeMillis()-lStartTime)+" ms");
	}
	
	// Uncaught exception catcher:
	static Catcher catcher = null;
	
	// Flag for hidden keys:
	static final boolean hkeys = true;
	
	// Handles for shared objects:
	static TracingToolbar ntb = null;
	static TracingHandler nhd = null;
	static MeasurementsDialog mdg = null;
	static AttributesDialog adg = null;
	static TextWindow grw = null;
	static TextWindow trw = null;
	static TextWindow vrw = null;
	
	// Range for cursor-tracing 'nearby' determination
	static final float NEARBYRANGE = 2;
	
	// Standard colors:
	static final Color ACTIVECOLOR = Color.red;
	static final Color HIGHLIGHTCOLOR = Color.white;
	
	// Regarding images:
	static ImagePlus imageplus = null;
	static boolean image = false;
	static boolean calibrate = true;
	static boolean interpolate = true;
	static String imagename = "";
	static String workdir = "";
	static String[] workimages = null;
	static int workimagenr = 0;
	
	static void image(final ImagePlus imp) {
		imageplus = imp;
		if (imageplus == null) {
			image = false;
			imagename = "";
		} else {
			image = true;
			final String title = imageplus.getTitle();
			final int dotIndex = title.lastIndexOf(".");
			if (dotIndex >= 0) imagename = title.substring(0,dotIndex);
			else imagename = title;
		}
	}
	
	// Method for showing no-image error message:
	static void noImage() {
		notify("Please load an image first using "+NAME);
		NJ.copyright();
	}
	
	// Colors supported by NeuronJ:
	static final Color[] colors = {
		Color.black, Color.blue, Color.cyan, Color.green, Color.magenta, Color.orange, Color.pink, Color.red, Color.yellow
	};
	
	static final String[] colornames = {
		"Black", "Blue", "Cyan", "Green", "Magenta", "Orange", "Pink", "Red", "Yellow"
	};
	
	static int colorIndex(final Color color) {
		final int nrcolors = colors.length;
		for (int i=0; i<nrcolors; ++i) if (color == colors[i]) return i;
		return -1;
	}
	
	// Tracing types, type colors, clusters:
	static String[] types = {
		"Default", "Axon", "Dendrite", "Primary", "Secondary", "Tertiary", "Type 06", "Type 07", "Type 08", "Type 09", "Type 10"
	};
	
	static Color[] typecolors = {
		Color.magenta, Color.red, Color.blue, Color.red, Color.blue, Color.yellow,
		Color.magenta, Color.magenta, Color.magenta, Color.magenta, Color.magenta
	};
	
	static String[] clusters = {
		"Default", "Cluster 01", "Cluster 02", "Cluster 03", "Cluster 04", "Cluster 05",
		"Cluster 06", "Cluster 07", "Cluster 08", "Cluster 09", "Cluster 10"
	};
	
	// Switch for enabling or disabling auto image window activation:
	static boolean activate = true;
	
	// Switch for enabling or disabling using the image name in result window titles:
	static boolean usename = false;
	
	// Switch for enabling or disabling (automatic) saving of tracings:
	static boolean autosave = false;
	static boolean save = false;
	
	// Switch for enabling or disabling log messaging:
	static boolean log = false;
	
	// To remember several last ImageJ settings:
	static boolean lastdoublebuffering = false;
	static int lasttool = 0;
	
	// Neurite appearance (bright = 0 and dark = 1):
	static int appear = 0;
	
	// Scale at which eigenvalues are computed:
	static float scale = 2.0f;
	
	// Cost component weight factor:
	static float gamma = 0.7f;
	
	// Half-window size for snapping cursor to locally lowest cost:
	static int snaprange = 4;
	
	// Window size for shortest-path searching. Must be less than about
	// 2900 in order to avoid cumulative costs exceeding the range
	// spanned by the integers.
	static int dijkrange = 2500;
	
	// For smoothing and subsampling tracing segments:
	static int halfsmoothrange = 5;
	static int subsamplefactor = 5;
	
	// Line width for drawing tracings:
	static int linewidth = 1;
	static BasicStroke tracestroke = new BasicStroke(linewidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
	static final BasicStroke crossstroke = new BasicStroke();
	
	// Central method for writing log messages:
	static void log(final String message) {
		if (log) {
			checklog();
			logpan.append(message);
		}
	}
	
	static void checklog() {
		if (logwin == null || !logwin.isShowing()) {
			final String startupmsg = NAME+" version "+VERSION+"\nCopyright (C) Erik Meijering";
			logwin = new TextWindow(NAME+": Log",startupmsg,500,500);
			logpan = logwin.getTextPanel();
		}
	}
	
	static void closelog() {
		if (logwin != null) logwin.setVisible(false);
	}
	
	static TextWindow logwin = null;
	static TextPanel logpan = null;
	
	// Central method for error messages:
	static void error(final String message) {
		if (IJ.getInstance() == null) IJ.showMessage(NJ.NAME+": Error",message+".");
		else new ErrorDialog(NJ.NAME+": Error",message+".");
		IJ.showProgress(1.0);
		IJ.showStatus("");
	}
	
	// Central method for showing copyright notice:
	static final String COPYRIGHT = NAME+" "+VERSION+" (C) Erik Meijering";
	static void copyright() { IJ.showStatus(COPYRIGHT); }
	
	// Central method for notifications:
	static void notify(final String message) {
		if (IJ.getInstance() == null) IJ.showMessage(NJ.NAME+": Note",message+".");
		else new ErrorDialog(NJ.NAME+": Note",message+".");
		IJ.showProgress(1.0);
		IJ.showStatus("");
	}
	
	// Central method for out-of-memory notifications:
	static void outOfMemory() {
		log("Not enough memory for the computations");
		error("Not enough memory for the computations");
		IJ.showProgress(1.0);
		IJ.showStatus("");
	}
	
	// Method for saving the parameters:
	static void saveParams() {
		
		Prefs.set("nj.appear",appear);
		Prefs.set("nj.scale",scale);
		Prefs.set("nj.gamma",gamma);
		Prefs.set("nj.snaprange",snaprange);
		Prefs.set("nj.dijkrange",dijkrange);
		Prefs.set("nj.halfsmoothrange",halfsmoothrange);
		Prefs.set("nj.subsamplefactor",subsamplefactor);
		Prefs.set("nj.linewidth",linewidth);
		Prefs.set("nj.activate",activate);
		Prefs.set("nj.usename",usename);
		Prefs.set("nj.autosave",autosave);
		Prefs.set("nj.log",log);
	}
	
	// Method for loading the parameters:
	static void loadParams() {
		
		appear = (int)Prefs.get("nj.appear",appear);
		scale = (float)Prefs.get("nj.scale",scale);
		gamma = (float)Prefs.get("nj.gamma",gamma);
		snaprange = (int)Prefs.get("nj.snaprange",snaprange);
		dijkrange = (int)Prefs.get("nj.dijkrange",dijkrange);
		halfsmoothrange = (int)Prefs.get("nj.halfsmoothrange",halfsmoothrange);
		subsamplefactor = (int)Prefs.get("nj.subsamplefactor",subsamplefactor);
		linewidth = (int)Prefs.get("nj.linewidth",linewidth);
		tracestroke = new BasicStroke(linewidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		activate = Prefs.get("nj.activate",activate);
		usename = Prefs.get("nj.usename",usename);
		autosave = Prefs.get("nj.autosave",autosave);
		log = Prefs.get("nj.log",log);
	}
	
	static void quit() {
		
		if (mdg != null) { mdg.close(); }
		if (adg != null) { adg.close(); }
		if (grw != null) { grw.setVisible(false); grw.dispose(); grw = null; }
		if (trw != null) { trw.setVisible(false); trw.dispose(); trw = null; }
		if (vrw != null) { vrw.setVisible(false); vrw.dispose(); vrw = null; }
		
		if (image) {
			nhd.closeTracings();
			// Close image but first restore listeners to avoid a call to ntb.windowClosed():
			ntb.restoreListeners();
			NJ.log("Closing current image...");
			imageplus.hide();
		}
		
		ntb.restoreToolbar();
		
		ntb = null;
		nhd = null;
		image(null);
		
		IJ.showStatus("");
		IJ.showProgress(1.0);
		IJ.setTool(lasttool);
		Prefs.doubleBuffer = lastdoublebuffering;
		log("Quitting "+NAME);
		closelog();
	}
	
}