package yang.plugin.tracing;

import java.awt.Button;
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;

final class AttributesDialog extends Dialog implements ActionListener, FocusListener, ItemListener, WindowListener {
	
	private final Choice idChoice;
	private final Choice typeChoice;
	private final Choice clusterChoice;
	
	private final TextField labelField;
	
	private final Button namesButton;
	private final Button colorsButton;
	private final Button okayButton;
	private final Button closeButton;
	
	private final GridBagConstraints c = new GridBagConstraints();
	private final GridBagLayout grid = new GridBagLayout();
	
	private boolean bFirstChoice = true;
	
	private static int left = -1;
	private static int top = -1;
	
	// Builds the dialog for setting the tracing attributes.
	AttributesDialog() {
		
		super(IJ.getInstance(),NJ.NAME+": Attributes",false);
		setLayout(grid);
		
		// Add ID, type, and cluster choices:
		idChoice = addChoice("Tracing ID:");
		idChoice.addItemListener(this);
		typeChoice = addChoice("Type:");
		clusterChoice = addChoice("Cluster:");
		
		// Add label text field:
		final Label labelLabel = makeLabel("Label:");
		c.gridwidth = 1;
		c.gridx = 0; c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(2,12,5,0);
		grid.setConstraints(labelLabel,c);
		add(labelLabel);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2,0,5,15);
		labelField = new TextField("Default",15);
		grid.setConstraints(labelField,c);
		labelField.setEditable(true);
		labelField.addFocusListener(this);
		add(labelField);
		
		// Add buttons:
		final Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));
		namesButton = new Button("Rename");
		namesButton.addActionListener(this);
		colorsButton = new Button("Recolor");
		colorsButton.addActionListener(this);
		okayButton = new Button("   OK   ");
		okayButton.addActionListener(this);
		closeButton = new Button(" Close ");
		closeButton.addActionListener(this);
		buttons.add(namesButton);
		buttons.add(colorsButton);
		buttons.add(okayButton);
		buttons.add(closeButton);
		c.gridx = 0; c.gridy++;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(20,11,12,10);
		grid.setConstraints(buttons,c);
		add(buttons);
		
		// Pack and show:
		reset();
		select(0);
		pack();
		if (left >= 0 && top >= 0) setLocation(left,top);
		else GUI.center(this);
		addWindowListener(this);
		setVisible(true);
	}
	
	private Choice addChoice(final String label) {
		
		final Label newLabel = makeLabel(label);
		c.gridwidth = 1;
		c.gridx = 0; c.gridy++;
		c.anchor = GridBagConstraints.EAST;
		if (bFirstChoice) c.insets = new Insets(20,12,5,0);
		else c.insets = new Insets(0,12,5,0);
		grid.setConstraints(newLabel,c);
		add(newLabel);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		if (bFirstChoice) c.insets = new Insets(20,0,5,13);
		else c.insets = new Insets(0,0,5,13);
		final Choice newChoice = new Choice();
		grid.setConstraints(newChoice,c);
		add(newChoice);
		
		bFirstChoice = false;
		return newChoice;
	}
	
	void reset() {
		
		idChoice.removeAll();
		idChoice.addItem("Unknown");
		final Tracings tracings = NJ.nhd.tracings();
		final int nrtracings = tracings.nrtracings();
		for (int i=0; i<nrtracings; ++i) idChoice.addItem("N"+tracings.get(i).id());
		
		typeChoice.removeAll();
		final int nrtypes = NJ.types.length;
		for (int i=0; i<nrtypes; ++i) typeChoice.addItem(NJ.types[i]);
		
		clusterChoice.removeAll();
		final int nrclusters = NJ.clusters.length;
		for (int i=0; i<nrclusters; ++i) clusterChoice.addItem(NJ.clusters[i]);
		
		select(0);
	}
	
	void select(final int index) {
		
		final int nritems = idChoice.getItemCount();
		
		if (index > 0 && index < nritems) {
			idChoice.select(index);
			final Tracing tracing = NJ.nhd.tracings().get(index-1);
			typeChoice.select(tracing.type());
			clusterChoice.select(tracing.cluster());
			labelField.setText(tracing.label());
		} else {
			idChoice.select(0);
			typeChoice.select(0);
			clusterChoice.select(0);
			labelField.setText("Default");
		}
	}
	
	private Label makeLabel(String label) {
		if (IJ.isMacintosh()) label += "  ";
		return new Label(label);
	}
	
	@Override
	public void actionPerformed(final ActionEvent e) { try {
		
		if (e.getSource() == okayButton) {
			final int type = typeChoice.getSelectedIndex();
			final int cluster = clusterChoice.getSelectedIndex();
			final String label = labelField.getText();
			final Tracings tracings = NJ.nhd.tracings();
			final int nrtracings = tracings.nrtracings();
			int iCount = 0;
			for (int i=0; i<nrtracings; ++i) {
				final Tracing tracing = tracings.get(i);
				if (tracing.selected() || tracing.highlighted()) {
					NJ.log("Labeling tracing N"+tracing.id());
					tracing.type(type);
					tracing.cluster(cluster);
					tracing.label(label);
					tracing.select(false);
					tracing.highlight(false);
					++iCount;
				}
			}
			if (iCount == 0) IJ.showStatus("Labeled no tracings");
			else if (iCount == 1) IJ.showStatus("Labeled tracing");
			else IJ.showStatus("Labeled tracings");
			select(0);
			if (tracings.changed()) NJ.nhd.redraw();
			
		} else if (e.getSource() == colorsButton) {
			final RecolorDialog cd = new RecolorDialog();
			
		} else if (e.getSource() == namesButton) {
			final RenameDialog nd = new RenameDialog();
			final int index = idChoice.getSelectedIndex();
			reset(); select(index);
			if (NJ.mdg != null) NJ.mdg.reset();
			
		} else {
			close();
			NJ.ntb.resetTool();
			NJ.copyright();
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
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
	
	@Override
	public void itemStateChanged(final ItemEvent e) { try {
		
		if (e.getSource() == idChoice) {
			final int index = idChoice.getSelectedIndex();
			select(index);
			final Tracings tracings = NJ.nhd.tracings();
			final int nrtracings = tracings.nrtracings();
			for (int i=0; i<nrtracings; ++i) {
				tracings.get(i).highlight(false);
				tracings.get(i).select(false);
			}
			if (index > 0) tracings.get(index-1).highlight(true);
			NJ.nhd.redraw();
		}
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	void close() {
		final Tracings tracings = NJ.nhd.tracings();
		final int nrtracings = tracings.nrtracings();
		for (int i=0; i<nrtracings; ++i) {
			final Tracing tracing = tracings.get(i);
			tracing.select(false);
			tracing.highlight(false);
		}
		NJ.nhd.redraw();
		left = getX();
		top = getY();
		setVisible(false);
		dispose();
		NJ.adg = null;
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