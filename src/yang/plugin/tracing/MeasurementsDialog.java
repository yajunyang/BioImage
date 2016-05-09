package yang.plugin.tracing;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.text.TextPanel;
import ij.text.TextWindow;
import imagescience.utility.Formatter;

final class MeasurementsDialog extends Dialog implements ActionListener, WindowListener {
	
	private final Choice typeChoice;
	private final Choice clusterChoice;
	
	private final Checkbox groupCheckbox;
	private final Checkbox traceCheckbox;
	private final Checkbox vertiCheckbox;
	
	private final Checkbox calibCheckbox;
	private final Checkbox interCheckbox;
	private final Checkbox clearCheckbox;
	
	private final Choice decsChoice;

	private static boolean group = true;
	private static boolean trace = true;
	private static boolean verti = true;
	private static boolean calib = true;
	private static boolean inter = true;
	private static boolean clear = true;

	private static int decs = 3;
	
	private static String pgh = null;
	private static String pth = null;
	private static String pvh = null;
	
	private final Button runButton;
	private final Button closeButton;
	
	private final GridBagConstraints c = new GridBagConstraints();
	private final GridBagLayout grid = new GridBagLayout();
	
	private boolean bFirstChoice = true;
	private boolean bFirstReset = true;
	
	private static int left = -1;
	private static int top = -1;
	
	// Builds the dialog for doing length measurements.
	MeasurementsDialog() {
		
		super(IJ.getInstance(),NJ.NAME+": Measurements",false);
		setLayout(grid);
		
		// Add choices:
		typeChoice = addChoice("Tracing type:");
		clusterChoice = addChoice("Cluster:");
		
		// Add check boxes:
		c.gridx = 0;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		
		c.gridy++; c.insets = new Insets(15,18,0,18);
		groupCheckbox = new Checkbox(" Display group measurements");
		grid.setConstraints(groupCheckbox,c);
		groupCheckbox.setState(group);
		add(groupCheckbox);
		
		c.gridy++; c.insets = new Insets(0,18,0,18);
		traceCheckbox = new Checkbox(" Display tracing measurements");
		grid.setConstraints(traceCheckbox,c);
		traceCheckbox.setState(trace);
		add(traceCheckbox);
		
		c.gridy++;
		vertiCheckbox = new Checkbox(" Display vertex measurements");
		grid.setConstraints(vertiCheckbox,c);
		vertiCheckbox.setState(verti);
		add(vertiCheckbox);
		
		c.gridy++; c.insets = new Insets(15,18,0,18);
		calibCheckbox = new Checkbox(" Calibrate measurements");
		grid.setConstraints(calibCheckbox,c);
		calibCheckbox.setState(calib);
		add(calibCheckbox);
		
		c.gridy++; c.insets = new Insets(0,18,0,18);
		interCheckbox = new Checkbox(" Interpolate value measurements");
		grid.setConstraints(interCheckbox,c);
		interCheckbox.setState(inter);
		add(interCheckbox);
		
		c.gridy++;
		clearCheckbox = new Checkbox(" Clear previous measurements");
		grid.setConstraints(clearCheckbox,c);
		clearCheckbox.setState(clear);
		add(clearCheckbox);
		
		// Add decimals choice:
		final Panel decsPanel = new Panel();
		decsPanel.setLayout(new FlowLayout(FlowLayout.CENTER,0,0));
		final Label decsLabel = makeLabel("Maximum decimal places:");
		decsPanel.add(decsLabel);
		decsChoice = new Choice();
		decsPanel.add(decsChoice);
		c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(15,10,5,15);
		grid.setConstraints(decsPanel,c);
		add(decsPanel);
		for (int i=0; i<=10; ++i) decsChoice.addItem(String.valueOf(i));
		decsChoice.select(decs);
		
		// Add Run and Close buttons:
		final Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));
		runButton = new Button("  Run  ");
		runButton.addActionListener(this);
		closeButton = new Button("Close");
		closeButton.addActionListener(this);
		buttons.add(runButton);
		buttons.add(closeButton);
		c.gridy++; c.insets = new Insets(20,10,12,10);
		grid.setConstraints(buttons,c);
		add(buttons);
		
		// Pack and show:
		reset();
		pack();
		if (left >= 0 && top >= 0) setLocation(left,top);
		else GUI.center(this);
		addWindowListener(this);
		setVisible(true);
	}
	
	private Choice addChoice(final String label) {
		
		final Label newLabel = makeLabel(label);
		c.gridx = 0; c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		if (bFirstChoice) c.insets = new Insets(20,13,5,0);
		else c.insets = new Insets(0,13,5,0);
		grid.setConstraints(newLabel,c);
		add(newLabel);
		final Choice newChoice = new Choice();
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		if (bFirstChoice) c.insets = new Insets(20,0,5,13);
		else c.insets = new Insets(0,0,5,13);
		grid.setConstraints(newChoice,c);
		add(newChoice);
		
		bFirstChoice = false;
		return newChoice;
	}
	
	void reset() {
		
		final int nrtypes = NJ.types.length;
		final int tindex = bFirstReset ? nrtypes : typeChoice.getSelectedIndex();
		typeChoice.removeAll();
		for (int i=0; i<nrtypes; ++i) typeChoice.addItem(NJ.types[i]);
		typeChoice.addItem("All");
		typeChoice.select(tindex);
		
		final int nrclusters = NJ.clusters.length;
		final int cindex = bFirstReset ? nrclusters : clusterChoice.getSelectedIndex();
		clusterChoice.removeAll();
		for (int i=0; i<nrclusters; ++i) clusterChoice.addItem(NJ.clusters[i]);
		clusterChoice.addItem("All");
		clusterChoice.select(cindex);
		
		bFirstReset = false;
	}
	
	private Label makeLabel(String label) {
		
		if (IJ.isMacintosh()) label += "  ";
		return new Label(label);
	}
	
	@Override
	public void actionPerformed(final ActionEvent e) { try {
		
		group = groupCheckbox.getState();
		trace = traceCheckbox.getState();
		verti = vertiCheckbox.getState();
		calib = NJ.calibrate = calibCheckbox.getState();
		inter = NJ.interpolate = interCheckbox.getState();
		clear = clearCheckbox.getState();
		decs = decsChoice.getSelectedIndex();
		
		if (e.getSource() == runButton) {
			final Tracings tracings = NJ.nhd.tracings();
			final int nrtracings = tracings.nrtracings();
			final int type = typeChoice.getSelectedIndex();
			final int cluster = clusterChoice.getSelectedIndex();
			final ByteProcessor bp = NJ.nhd.ipgray;
			final String cstring = calib ? "calibrated " : "uncalibrated ";
			final Calibration cal = NJ.imageplus.getCalibration();
			String su = calib ? new String(cal.getUnit()) : "pixel";
			if (su.equals("pixel")) su = "pix";
			String vu = calib ? new String(cal.getValueUnit()) : "Gray Value";
			if (vu.equals("Gray Value")) vu = "a.u.";
			if (calib) bp.setCalibrationTable(cal.getCTable());
			else bp.setCalibrationTable(null);
			final Formatter fm = new Formatter();
			fm.decs(decs);
			
			if (group == true) {
				final Values lengths = new Values();
				final Values values = new Values();
				for (int n=0; n<nrtracings; ++n) {
					final Tracing tracing = tracings.get(n);
					if ((tracing.type() == type || type == NJ.types.length) && (tracing.cluster() == cluster || cluster == NJ.clusters.length)) {
						lengths.add(tracing.length());
						tracing.values(bp,values);
					}
				}
				final String gh = new String(
					"Image\t"+
					"Cluster\t"+
					"Type\t"+
					"Count\t"+
					"Sum Len ["+su+"]\t"+
					"Mean Len ["+su+"]\t"+
					"SD Len ["+su+"]\t"+
					"Min Len ["+su+"]\t"+
					"Max Len ["+su+"]\t"+
					"Mean Val ["+vu+"]\t"+
					"SD Val ["+vu+"]\t"+
					"Min Val ["+vu+"]\t"+
					"Max Val ["+vu+"]"
				);
				final StringBuffer measures = new StringBuffer();
				measures.append(NJ.imagename);
				measures.append("\t" + (cluster==NJ.clusters.length ? "All" : NJ.clusters[cluster]));
				measures.append("\t" + (type==NJ.types.length ? "All" : NJ.types[type]));
				if (lengths.count() > 0) {
					lengths.stats();
					measures.append("\t" + String.valueOf(lengths.count()));
					measures.append("\t" + fm.d2s(lengths.sum()));
					measures.append("\t" + fm.d2s(lengths.mean()));
					measures.append("\t" + fm.d2s(lengths.sd()));
					measures.append("\t" + fm.d2s(lengths.min()));
					measures.append("\t" + fm.d2s(lengths.max()));
					values.stats();
					measures.append("\t" + fm.d2s(values.mean()));
					measures.append("\t" + fm.d2s(values.sd()));
					measures.append("\t" + fm.d2s(values.min()));
					measures.append("\t" + fm.d2s(values.max()));
					measures.append("\n");
				} else {
					measures.append("\t0\n");
				}
				if (NJ.grw == null || !NJ.grw.isShowing()) {
					NJ.log("Writing "+cstring+"measurements to new group results window");
					final String title = NJ.usename ? (NJ.imagename+"-groups") : (NJ.NAME+": Groups");
					NJ.grw = new TextWindow(title,gh,measures.toString(),820,300);
				} else {
					NJ.log("Writing "+cstring+"measurements to group results window");
					final TextPanel tp = NJ.grw.getTextPanel();
					if (clear == true || !gh.equals(pgh)) tp.setColumnHeadings(gh);
					tp.append(measures.toString());
				}
				pgh = gh;
			}
			if (trace == true) {
				int iCount = 0;
				final String th = new String(
					"Image\t"+
					"Tracing\t"+
					"Cluster\t"+
					"Type\t"+
					"Label\t"+
					"Length ["+su+"]\t"+
					"Mean Val ["+vu+"]\t"+
					"SD Val ["+vu+"]\t"+
					"Min Val ["+vu+"]\t"+
					"Max Val ["+vu+"]"
				);
				final StringBuffer measures = new StringBuffer();
				final Values values = new Values();
				for (int n=0; n<nrtracings; ++n) {
					final Tracing tracing = tracings.get(n);
					if ((tracing.type() == type || type == NJ.types.length) && (tracing.cluster() == cluster || cluster == NJ.clusters.length)) {
						measures.append(NJ.imagename);
						measures.append("\tN" + tracing.id());
						measures.append("\t" + NJ.clusters[tracing.cluster()]);
						measures.append("\t" + NJ.types[tracing.type()]);
						measures.append("\t" + tracing.label());
						measures.append("\t" + fm.d2s(tracing.length()));
						values.reset();
						tracing.values(bp,values);
						values.stats();
						measures.append("\t" + fm.d2s(values.mean()));
						measures.append("\t" + fm.d2s(values.sd()));
						measures.append("\t" + fm.d2s(values.min()));
						measures.append("\t" + fm.d2s(values.max()));
						measures.append("\n");
						++iCount;
					}
				}
				if (iCount == 0) {
					measures.append(NJ.imagename);
					measures.append("\tNone\n");
				}
				if (NJ.trw == null || !NJ.trw.isShowing()) {
					NJ.log("Writing "+cstring+"measurements to new tracing results window");
					final String title = NJ.usename ? (NJ.imagename+"-tracings") : (NJ.NAME+": Tracings");
					NJ.trw = new TextWindow(title,th,measures.toString(),820,300);
					final Point loc = NJ.trw.getLocation();
					loc.x += 20; loc.y += 20;
					NJ.trw.setLocation(loc.x,loc.y);
				} else {
					NJ.log("Writing "+cstring+"measurements to tracing results window");
					final TextPanel tp = NJ.trw.getTextPanel();
					if (clear == true || !th.equals(pth)) tp.setColumnHeadings(th);
					tp.append(measures.toString());
				}
				pth = th;
			}
			if (verti == true) {
				int iCount = 0;
				final String vh = new String(
					"Image\t"+
					"Tracing\t"+
					"Segment\t"+
					"Vertex\t"+
					"X ["+su+"]\t"+
					"Y ["+su+"]\t"+
					"Val ["+vu+"]"
				);
				final StringBuffer measures = new StringBuffer();
				final double pw = NJ.calibrate ? NJ.imageplus.getCalibration().pixelWidth : 1;
				final double ph = NJ.calibrate ? NJ.imageplus.getCalibration().pixelHeight : 1;
				for (int n=0; n<nrtracings; ++n) {
					final Tracing tracing = tracings.get(n);
					if ((tracing.type() == type || type == NJ.types.length) && (tracing.cluster() == cluster || cluster == NJ.clusters.length)) {
						final int nrsegments = tracing.nrsegments();
						for (int s=0, p0=0; s<nrsegments; ++s, p0=1) {
							final Segment segment = tracing.get(s);
							final int nrpoints = segment.nrpoints();
							for (int p=p0, v=1; p<nrpoints; ++p, ++v) {
								final Point point = segment.get(p);
								measures.append(NJ.imagename);
								measures.append("\tN"+tracing.id());
								measures.append("\t"+(s+1));
								measures.append("\t"+v);
								measures.append("\t"+fm.d2s(point.x*pw));
								measures.append("\t"+fm.d2s(point.y*ph));
								measures.append("\t"+fm.d2s(bp.getPixelValue(point.x,point.y)));
								measures.append("\n");
								++iCount;
							}
						}
					}
				}
				if (iCount == 0) {
					measures.append(NJ.imagename);
					measures.append("\tNone\n");
				}
				if (NJ.vrw == null || !NJ.vrw.isShowing()) {
					NJ.log("Writing "+cstring+"measurements to new vertex results window");
					final String title = NJ.usename ? (NJ.imagename+"-vertices") : (NJ.NAME+": Vertices");
					NJ.vrw = new TextWindow(title,vh,measures.toString(),820,300);
					final Point loc = NJ.vrw.getLocation();
					loc.x += 40; loc.y += 40;
					NJ.vrw.setLocation(loc.x,loc.y);
				} else {
					NJ.log("Writing "+cstring+"measurements to vertex results window");
					final TextPanel tp = NJ.vrw.getTextPanel();
					if (clear == true || !vh.equals(pvh)) tp.setColumnHeadings(vh);
					tp.append(measures.toString());
				}
				pvh = vh;
			}
		} else if (e.getSource() == closeButton) {
			close();
			NJ.ntb.resetTool();
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	void close() {
		
		left = getX();
		top = getY();
		setVisible(false);
		dispose();
		NJ.mdg = null;
		NJ.copyright();
	}
	
	@Override
	public void windowActivated(final WindowEvent e) { }
	
	@Override
	public void windowClosed(final WindowEvent e) { }
	
	@Override
	public void windowClosing(final WindowEvent e) { try {
		
		close();
		NJ.ntb.resetTool();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void windowDeactivated(final WindowEvent e) { }
	
	@Override
	public void windowDeiconified(final WindowEvent e) { }
	
	@Override
	public void windowIconified(final WindowEvent e) { }
	
	@Override
	public void windowOpened(final WindowEvent e) { }
	
}