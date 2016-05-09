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

final class ErrorDialog extends Dialog implements ActionListener, KeyListener, WindowListener {
	
	private Button button;
	private Label label;
	
	private final static Font font = new Font("Dialog",Font.PLAIN,12);
	
	ErrorDialog(String title, String message) {
		
		super(IJ.getInstance(),title,true);
		if (message == null) return;
		setLayout(new BorderLayout(0,0));
		
		label = new Label(message);
		label.setFont(font);
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER,15,15));
		panel.add(label);
		add("North", panel);
		
		button = new Button("  OK  ");
		button.setFont(font);
		button.addActionListener(this);
		button.addKeyListener(this);
		panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER,0,0));
		panel.add(button);
		add("Center", panel);
		
		panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER,0,5));
		add("South", panel);
		
		pack();
		GUI.center(this);
		addWindowListener(this);
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) { try {
		
		close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyPressed(KeyEvent e) { try {
		
		if (e.getKeyCode() == KeyEvent.VK_ENTER) close();
		
	} catch (Throwable x) { NJ.catcher.uncaughtException(Thread.currentThread(),x); } }
	
	@Override
	public void keyReleased(KeyEvent e) { }
	
	@Override
	public void keyTyped(KeyEvent e) { }
	
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