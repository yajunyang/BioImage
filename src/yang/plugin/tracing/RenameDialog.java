package yang.plugin.tracing;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;

final class RenameDialog extends Dialog implements ActionListener, FocusListener, KeyListener, WindowListener {
	
	private final Choice namesChoice;
	private final TextField nameField;
	
	private final Button okayButton;
	private final Button cancelButton;
	
	private static int left = -1;
	private static int top = -1;
	
	// Builds the dialog for renaming the types and clusters.
	RenameDialog() {
		
		super(IJ.getInstance(),NJ.NAME+": Rename",true);
		final GridBagConstraints c = new GridBagConstraints();
		final GridBagLayout grid = new GridBagLayout();
		setLayout(grid);
		
		// Add choice:
		namesChoice = new Choice();
		final int nrtypes = NJ.types.length;
		for (int i=1; i<nrtypes; ++i) namesChoice.addItem(NJ.types[i]);
		final int nrclusters = NJ.clusters.length;
		for (int i=1; i<nrclusters; ++i) namesChoice.addItem(NJ.clusters[i]);
		namesChoice.select(0);
		c.gridx = 0; c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(20,13,5,0);
		grid.setConstraints(namesChoice,c);
		add(namesChoice);
		
		// Add text field:
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(20,10,5,15);
		nameField = new TextField("",15);
		grid.setConstraints(nameField,c);
		nameField.setEditable(true);
		nameField.addFocusListener(this);
		add(nameField);
		
		// Add Okay and Cancel buttons:
		final Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));
		okayButton = new Button("   OK   ");
		okayButton.addActionListener(this);
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener(this);
		buttons.add(okayButton);
		buttons.add(cancelButton);
		c.gridx = 0; c.gridy++;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(15,10,12,10);
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
			final String name = nameField.getText();
			final int index = namesChoice.getSelectedIndex() + 1;
			if (index < NJ.types.length) NJ.types[index] = name;
			else NJ.clusters[index-NJ.types.length+1] = name;
			IJ.showStatus("Changed name");
			NJ.save = true;
		} else { NJ.copyright(); }
		
		close();
		
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

	private void close() {
		left = getX();
		top = getY();
		setVisible(false);
		dispose();
	}
	
	@Override
	public void keyTyped(final KeyEvent e) {}
	
	@Override
	public void keyPressed(final KeyEvent e) { try {
		
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			NJ.copyright();
			close();
		}
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyReleased(final KeyEvent e) {}
	
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