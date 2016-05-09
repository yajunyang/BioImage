package yang.plugin.tracing;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;

final class ExportDialog extends Dialog implements ActionListener, WindowListener {
	
	private final CheckboxGroup checkboxgroup = new CheckboxGroup();
	private final Checkbox[] checkboxes = new Checkbox[5];
	private static final boolean[] states = { true, false, false, false, false };
	
	private final Button okayButton;
	private final Button cancelButton;
	
	private final GridBagConstraints c = new GridBagConstraints();
	private final GridBagLayout grid = new GridBagLayout();
	
	private boolean canceled = true;
	
	private static int left = -1;
	private static int top = -1;
	
	// Builds the dialog for choosing the export type.
	ExportDialog() {
		
		super(IJ.getInstance(),NJ.NAME+": Export",true);
		setLayout(grid);
		
		// Add message:
		final Label message = new Label("Export tracing vertex coordinates with one vertex per line to:");
		c.gridx = c.gridy = 0; c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(20,18,0,18);
		grid.setConstraints(message,c);
		add(message);
		
		// Add image and tracings checkboxes:
		c.gridy++; c.insets = new Insets(10,18,0,18);
		checkboxes[0] = new Checkbox(" Tab-delimited text file: single file for all tracings",states[0],checkboxgroup);
		grid.setConstraints(checkboxes[0],c);
		add(checkboxes[0]);
		
		c.gridy++; c.insets = new Insets(0,18,0,18);
		checkboxes[1] = new Checkbox(" Tab-delimited text files: separate file for each tracing",states[1],checkboxgroup);
		grid.setConstraints(checkboxes[1],c);
		add(checkboxes[1]);
		
		c.gridy++;
		checkboxes[2] = new Checkbox(" Comma-delimited text file: single file for all tracings",states[2],checkboxgroup);
		grid.setConstraints(checkboxes[2],c);
		add(checkboxes[2]);
		
		c.gridy++;
		checkboxes[3] = new Checkbox(" Comma-delimited text files: separate file for each tracing",states[3],checkboxgroup);
		grid.setConstraints(checkboxes[3],c);
		add(checkboxes[3]);
		
		c.gridy++;
		checkboxes[4] = new Checkbox(" Segmented line selection files: separate file for each tracing",states[4],checkboxgroup);
		grid.setConstraints(checkboxes[4],c);
		add(checkboxes[4]);
		
		// Add Okay, and Cancel buttons:
		final Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));
		okayButton = new Button("   OK   ");
		okayButton.addActionListener(this);
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener(this);
		buttons.add(okayButton);
		buttons.add(cancelButton);
		c.gridx = 0; c.gridy++; c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(20,10,12,10);
		grid.setConstraints(buttons,c);
		add(buttons);
		
		// Pack and show:
		pack();
		if (left >= 0 && top >= 0) setLocation(left,top);
		else GUI.center(this);
		addWindowListener(this);
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(final ActionEvent e) { try {
		
		if (e.getSource() == okayButton) {
			for (int i=0; i<checkboxes.length; ++i)
			states[i] = checkboxes[i].getState();
			canceled = false;
		}
		
		close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }

	public int lastChoice() {
		
		for (int i=0; i<states.length; ++i)
			if (states[i] == true) return i;
		
		return -1;
	}
	
	public boolean wasCanceled() { return canceled; }
	
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