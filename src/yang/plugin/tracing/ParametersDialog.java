package yang.plugin.tracing;

import java.awt.BasicStroke;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;

final class ParametersDialog extends Dialog implements ActionListener, FocusListener, WindowListener {
	
	private final TextField scaleField;
	private final TextField gammaField;
	
	private final Choice appearChoice;
	private final Choice snapChoice;
	private final Choice dijkChoice;
	private final Choice smoothChoice;
	private final Choice sampleChoice;
	private final Choice lineChoice;
	
	private final Checkbox activateCheckbox;
	private final Checkbox usenameCheckbox;
	private final Checkbox autosaveCheckbox;
	private final Checkbox logCheckbox;
	
	private final Button saveButton;
	private final Button okayButton;
	private final Button cancelButton;
	
	private final GridBagConstraints c = new GridBagConstraints();
	private final GridBagLayout grid = new GridBagLayout();
	
	private boolean bFirstParam = true;
	private boolean bAppearChanged = false;
	private boolean bScaleChanged = false;
	private boolean bGammaChanged = false;
	
	private static int left = -1;
	private static int top = -1;
	
	// Builds the dialog for setting the parameters.
	ParametersDialog() {
		
		super(IJ.getInstance(),NJ.NAME+": Parameters",true);
		setLayout(grid);
		
		// Add parameters:
		appearChoice = addChoice("Neurite appearance:");
		appearChoice.addItem("Bright");
		appearChoice.addItem("Dark");
		appearChoice.select(NJ.appear);
		
		scaleField = addTextField("Hessian smoothing scale:",String.valueOf(NJ.scale));
		gammaField = addTextField("Cost weight factor:",String.valueOf(NJ.gamma));
		
		snapChoice = addChoice("Snap window size:");
		final int maxsnapsize = 19;
		for (int i=1; i<=maxsnapsize; i+=2) snapChoice.addItem(i+" x "+i);
		snapChoice.select(NJ.snaprange);
		
		dijkChoice = addChoice("Path-search window size:");
		final int maxdijksize = 2500;
		for (int i=100; i<=maxdijksize; i+=100) dijkChoice.addItem(i+" x "+i);
		dijkChoice.select(NJ.dijkrange/100 - 1);
		
		smoothChoice = addChoice("Tracing smoothing range:");
		final int maxsmoothrange = 10;
		for (int i=0; i<=maxsmoothrange; ++i) smoothChoice.addItem(String.valueOf(i));
		smoothChoice.select(NJ.halfsmoothrange);
		
		sampleChoice = addChoice("Tracing subsampling factor:");
		final int maxsubsample = 10;
		for (int i=1; i<=maxsubsample; ++i) sampleChoice.addItem(String.valueOf(i));
		sampleChoice.select(NJ.subsamplefactor-1);
		
		lineChoice = addChoice("Line width:");
		final int maxlinewidth = 10;
		for (int i=1; i<=maxlinewidth; ++i) lineChoice.addItem(String.valueOf(i));
		lineChoice.select(NJ.linewidth-1);
		
		c.insets = new Insets(22,18,0,18);
		c.gridx = 0; c.gridy++; c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		activateCheckbox = new Checkbox(" Activate image window when mouse enters");
		grid.setConstraints(activateCheckbox,c);
		activateCheckbox.setState(NJ.activate);
		add(activateCheckbox);
		
		c.gridy++;
		c.insets = new Insets(0,18,0,18);
		usenameCheckbox = new Checkbox(" Use image name in result window titles");
		grid.setConstraints(usenameCheckbox,c);
		usenameCheckbox.setState(NJ.usename);
		add(usenameCheckbox);
		
		c.gridy++;
		autosaveCheckbox = new Checkbox(" Automatically save tracings");
		grid.setConstraints(autosaveCheckbox,c);
		autosaveCheckbox.setState(NJ.autosave);
		add(autosaveCheckbox);
		
		c.gridy++;
		logCheckbox = new Checkbox(" Show log messages");
		grid.setConstraints(logCheckbox,c);
		logCheckbox.setState(NJ.log);
		add(logCheckbox);
		
		// Add Save, Okay, and Cancel buttons:
		final Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));
		saveButton = new Button("  Save  ");
		saveButton.addActionListener(this);
		okayButton = new Button("   OK   ");
		okayButton.addActionListener(this);
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener(this);
		buttons.add(saveButton);
		buttons.add(okayButton);
		buttons.add(cancelButton);
		c.gridx = 0; c.gridy++; c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(22,10,12,10);
		grid.setConstraints(buttons, c);
		add(buttons);
		
		// Pack and show:
		pack();
		if (left >= 0 && top >= 0) setLocation(left,top);
		else GUI.center(this);
		addWindowListener(this);
		setVisible(true);
	}
	
	private Label makeLabel(String label) {
		if (IJ.isMacintosh()) label += "  ";
		return new Label(label);
	}
	
	private float stringToFloat(final String s, final float defval) {
		
		try {
			final Float f = new Float(s);
			return f.floatValue();
		}
		catch(NumberFormatException e) {}
		
		return defval;
	}
	
	private Choice addChoice(final String label) {
		
		final Label newLabel = makeLabel(label);
		c.gridx = 0; c.gridy++; c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		if (bFirstParam) c.insets = new Insets(25,15,3,0);
		else c.insets = new Insets(0,15,3,0);
		grid.setConstraints(newLabel,c);
		add(newLabel);
		
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		if (bFirstParam) c.insets = new Insets(25,0,3,13);
		else c.insets = new Insets(0,0,3,13);
		final Choice newChoice = new Choice();
		grid.setConstraints(newChoice,c);
		add(newChoice);
		
		bFirstParam = false;
		return newChoice;
	}
	
	private TextField addTextField(final String label, final String value) {
		
		c.gridx = 0; c.gridy++; c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		if (bFirstParam) c.insets = new Insets(25,15,3,0);
		else c.insets = new Insets(0,15,3,0);
		final Label newLabel = makeLabel(label);
		grid.setConstraints(newLabel,c);
		add(newLabel);
		
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		if (bFirstParam) c.insets = new Insets(25,0,3,15);
		else c.insets = new Insets(0,0,3,15);
		final TextField newTextField = new TextField(value, 6);
		grid.setConstraints(newTextField,c);
		newTextField.setEditable(true);
		newTextField.addFocusListener(this);
		add(newTextField);
		
		bFirstParam = false;
		return newTextField;
	}
	
	@Override
	public void actionPerformed(final ActionEvent e) { try {
		
		if (e.getSource() == okayButton) {
			setParams(); IJ.showStatus("Set parameters");
		} else if (e.getSource() == saveButton) {
			setParams(); NJ.saveParams(); IJ.showStatus("Saved parameters");
		} else { NJ.copyright(); }
		
		close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	private void setParams() {
		
		final boolean log = logCheckbox.getState();
		if (log) NJ.log = true;
		
		NJ.log("Setting parameters...");
		
		int appear = appearChoice.getSelectedIndex();
		if (appear == NJ.appear) bAppearChanged = false;
		else bAppearChanged = true;
		NJ.appear = appear;
		if (NJ.appear == 0) NJ.log("   Neurite appearance = Bright");
		else NJ.log("   Neurite appearance = Dark");
		
		float scale = stringToFloat(scaleField.getText(),1.0f);
		if (scale < 1.0f) scale = 1.0f;
		if (scale == NJ.scale) bScaleChanged = false;
		else bScaleChanged = true;
		NJ.scale = scale;
		NJ.log("   Hessian smoothing scale = "+NJ.scale+" pixels");
		
		float gamma = stringToFloat(gammaField.getText(),0.5f);
		if (gamma < 0.0f) gamma = 0.0f;
		else if (gamma > 1.0f) gamma = 1.0f;
		if (gamma == NJ.gamma) bGammaChanged = false;
		else bGammaChanged = true;
		NJ.gamma = gamma;
		NJ.log("   Cost weight factor = "+NJ.gamma);
		
		NJ.snaprange = snapChoice.getSelectedIndex();
		final int snapwinsize = 2*NJ.snaprange + 1;
		NJ.log("   Snap window size = "+snapwinsize+" x "+snapwinsize+" pixels");
		
		NJ.dijkrange = 100*(dijkChoice.getSelectedIndex() + 1);
		NJ.log("   Path-search window size = "+NJ.dijkrange+" x "+NJ.dijkrange+" pixels");
		
		NJ.halfsmoothrange = smoothChoice.getSelectedIndex();
		NJ.log("   Tracing smoothing range = "+NJ.halfsmoothrange+" pixels on both sides");
		
		NJ.subsamplefactor = sampleChoice.getSelectedIndex() + 1;
		NJ.log("   Tracing subsampling factor = "+NJ.subsamplefactor);
		
		NJ.linewidth = lineChoice.getSelectedIndex() + 1;
		NJ.log("   Line width = "+NJ.linewidth+" pixels");
		NJ.tracestroke = new BasicStroke(NJ.linewidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
		
		NJ.activate = activateCheckbox.getState();
		if (NJ.activate) NJ.log("   Activating image window when mouse enters");
		else NJ.log("   Not activating image window when mouse enters");
		
		NJ.usename = usenameCheckbox.getState();
		if (NJ.usename) NJ.log("   Using image name in result window titles");
		else NJ.log("   Using default result window titles");
		
		NJ.autosave = autosaveCheckbox.getState();
		if (NJ.autosave) NJ.log("   Automatically saving tracings");
		else NJ.log("   Asking user to save tracings");
		
		if (log) NJ.log("   Showing log messages");
		else NJ.log("   Stop showing log messages");
		
		NJ.log("Done");
		
		NJ.log = log;
		if (!log) NJ.closelog();
		NJ.save = true;
		
		if (NJ.image) NJ.nhd.redraw();
	}
	
	boolean appearChanged() { return bAppearChanged; }
	
	boolean scaleChanged() { return bScaleChanged; }
	
	boolean gammaChanged() { return bGammaChanged; }
	
	@Override
	public void focusGained(final FocusEvent e) { try {
		
		Component c = e.getComponent();
		if (c instanceof TextField) ((TextField)c).selectAll();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void focusLost(final FocusEvent e) { try {
		
		Component c = e.getComponent();
		if (c instanceof TextField) ((TextField)c).select(0,0);
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	private void close() {
		left = getX();
		top = getY();
		setVisible(false);
		dispose();
	}
	
	@Override
	public void windowActivated(final WindowEvent e) { }
	
	@Override
	public void windowClosed(final WindowEvent e) { }
	
	@Override
	public void windowClosing(final WindowEvent e) { try {
		
		close();
		
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