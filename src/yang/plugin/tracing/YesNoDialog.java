package yang.plugin.tracing;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import ij.IJ;
import ij.gui.GUI;

final class YesNoDialog extends Dialog implements ActionListener, KeyListener, WindowListener {
	
	private Button yesButton;
	private Button noButton;
	
	private boolean yesPressed = false;
	private boolean noPressed = false;
	
	// Builds the dialog for specifying the parameters for derivative computing.
	YesNoDialog(final String title, final String question) {
		
		super(IJ.getInstance(),NJ.NAME+": "+title,true);
		setLayout(new BorderLayout());
		
		// Add question:
		final Panel questPanel = new Panel();
		questPanel.setLayout(new FlowLayout(FlowLayout.CENTER,15,15));
		final Label questLabel = new Label(question);
		questLabel.setFont(new Font("Dialog",Font.PLAIN,12));
		questPanel.add(questLabel);
		add("North",questPanel);
		
		// Add No and Yes buttons:
		final Panel yesnoPanel = new Panel();
		yesnoPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));
		yesButton = new Button("  Yes  ");
		yesButton.addActionListener(this);
		yesButton.addKeyListener(this);
		yesnoPanel.add(yesButton);
		noButton = new Button("   No   ");
		noButton.addActionListener(this);
		noButton.addKeyListener(this);
		yesnoPanel.add(noButton);
		add("Center",yesnoPanel);
		
		// Add spacing below buttons:
		final Panel spacePanel = new Panel();
		spacePanel.setLayout(new FlowLayout(FlowLayout.CENTER,0,5));
		add("South",spacePanel);
		
		// Pack and show:
		pack();
		GUI.center(this);
		addWindowListener(this);
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(final ActionEvent e) { try {
		
		if (e.getSource() == yesButton) yesPressed = true;
		else if (e.getSource() == noButton) noPressed = true;
		
		close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyTyped(KeyEvent e) {}
	
	@Override
	public void keyPressed(KeyEvent e) { try {
		
		final int keycode = e.getKeyCode();
		if (keycode == KeyEvent.VK_Y) yesPressed = true;
		else if (keycode == KeyEvent.VK_N || keycode == KeyEvent.VK_ESCAPE) noPressed = true;
		else if (keycode == KeyEvent.VK_ENTER)
			if (yesButton.hasFocus()) yesPressed = true;
			else if (noButton.hasFocus()) noPressed = true;
		
		if (yesPressed || noPressed) close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyReleased(KeyEvent e) {}
	
	boolean yesPressed() { return yesPressed; }
	
	boolean noPressed() { return noPressed; }
	
	private void close() {
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