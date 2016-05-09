package yang.plugin.tracing;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;

final class SnapshotDialog extends Dialog implements ActionListener, WindowListener {
	
	private final Checkbox imageCheckbox;
	private final Checkbox tracingsCheckbox;
	
	private static boolean drawimage = true;
	private static boolean drawtracings = true;
	
	private final Button okayButton;
	private final Button cancelButton;
	
	private final GridBagConstraints c = new GridBagConstraints();
	private final GridBagLayout grid = new GridBagLayout();
	
	private boolean canceled = true;
	
	private static int left = -1;
	private static int top = -1;
	
	// Builds the dialog for setting the snapshot parameters.
	SnapshotDialog() {
		
		super(IJ.getInstance(),NJ.NAME+": Snapshot",true);
		setLayout(grid);
		
		// Add image and tracings checkboxes:
		c.insets = new Insets(20,18,0,18);
		c.gridx = c.gridy = 0; c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		imageCheckbox = new Checkbox(" Draw image                ");
		grid.setConstraints(imageCheckbox, c);
		imageCheckbox.setState(drawimage);
		add(imageCheckbox);
		c.gridy++; c.insets = new Insets(0,18,0,18);
		tracingsCheckbox = new Checkbox(" Draw tracings");
		grid.setConstraints(tracingsCheckbox,c);
		tracingsCheckbox.setState(drawtracings);
		add(tracingsCheckbox);
		
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
			drawimage = imageCheckbox.getState();
			drawtracings = tracingsCheckbox.getState();
			canceled = false;
		}
		
		close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	public boolean drawImage() { return drawimage; }
	
	public boolean drawTracings() { return drawtracings; }
	
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