package yang.plugin.tracing;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;

final class RecolorDialog extends Dialog implements ActionListener, ItemListener, WindowListener {
	
	private final Choice typeChoice;
	private final Choice colorChoice;
	
	private final Button okayButton;
	private final Button cancelButton;
	
	private static int left = -1;
	private static int top = -1;
	
	// Builds the dialog for setting the type colors.
	RecolorDialog() {
		
		super(IJ.getInstance(),NJ.NAME+": Recolor",true);
		final GridBagConstraints c = new GridBagConstraints();
		final GridBagLayout grid = new GridBagLayout();
		setLayout(grid);
		
		// Add choices:
		typeChoice = new Choice();
		final int nrtypes = NJ.types.length;
		for (int i=0; i<nrtypes; ++i) typeChoice.addItem(NJ.types[i]);
		typeChoice.select(0);
		typeChoice.addItemListener(this);
		c.gridx = 0; c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(20,15,5,0);
		grid.setConstraints(typeChoice,c);
		add(typeChoice);
		
		colorChoice = new Choice();
		final int nrcolors = NJ.colors.length;
		for (int i=0; i<nrcolors; ++i) colorChoice.addItem(NJ.colornames[i]);
		colorChoice.select(NJ.colorIndex(NJ.typecolors[0]));
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(20,10,5,13);
		grid.setConstraints(colorChoice,c);
		add(colorChoice);
		
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
			NJ.typecolors[typeChoice.getSelectedIndex()] = NJ.colors[colorChoice.getSelectedIndex()];
			IJ.showStatus("Changed color");	    
			NJ.nhd.redraw();
			NJ.save = true;
		} else { NJ.copyright(); }
		
		close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void itemStateChanged(final ItemEvent e) { try {
		
		colorChoice.select(NJ.colorIndex(NJ.typecolors[typeChoice.getSelectedIndex()]));
		
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