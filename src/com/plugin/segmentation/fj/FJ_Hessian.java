package com.plugin.segmentation.fj;

/*
[1]	Y. Sato, S. Nakajima, N. Shiraga, H. Atsumi, S. Yoshida, T. Koller, G. Gerig, R. Kikinis. 
	Three-Dimensional Multi-Scale Line Filter for Segmentation and Visualization of Curvilinear Structures in Medical Images. 
	Medical Image Analysis, vol. 2, no. 2, 1998, pp. 143-168.
[2]	A. F. Frangi, W. J. Niessen, R. M. Hoogeveen, T. van Walsum, M. A. Viergever.
 	Model-Based Quantitation of 3D Magnetic Resonance Angiographic Images. 
 	IEEE Transactions on Medical Imaging, vol. 18, no. 10, 1999, pp. 946-956.
[3]	K. Rohr.
 	Landmark-Based Image Analysis using Geometric and Intensity Models.
 	 Kluwer Academic Publishers, Dordrecht, 2001.
 */

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

public class FJ_Hessian implements PlugIn, WindowListener {

	private static boolean largest = true;
	private static boolean middle = false;
	private static boolean smallest = true;

	private static boolean absolute = true;

	private static String scale = "1.0";

	private ImagePlus imp = null;

	private static Point pos = new Point(-1,-1);

	public void run(String arg) {

		if (!FJ.libcheck()) return;
		final ImagePlus imp = FJ.imageplus();
		if (imp == null) return;

		FJ.log(FJ.name()+" "+FJ.version()+": Hessian");

		GenericDialog gd = new GenericDialog(FJ.name()+": Hessian");
		gd.addCheckbox(" Largest eigenvalue of Hessian tensor     ",largest);
		gd.addCheckbox(" Middle eigenvalue of Hessian tensor     ",middle);
		gd.addCheckbox(" Smallest eigenvalue of Hessian tensor     ",smallest);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(5,0,0,0));
		gd.addCheckbox(" Absolute eigenvalue comparison     ",absolute);
		gd.addPanel(new Panel(),GridBagConstraints.EAST,new Insets(5,0,0,0));
		gd.addStringField("                Smoothing scale:",scale);

		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else gd.centerDialog(true); 
		gd.addWindowListener(this);
		
		// Displays this dialog box. and stop to wait cancel or Ok button
		gd.showDialog();

		if (gd.wasCanceled()) return;
		
		largest = gd.getNextBoolean();
		middle = gd.getNextBoolean();
		smallest = gd.getNextBoolean();
		absolute = gd.getNextBoolean();
		scale = gd.getNextString();
	
		(new FJHessian()).run(imp,largest,middle,smallest,absolute,scale);
	}

	public void windowActivated(final WindowEvent e) { }

	public void windowClosed(final WindowEvent e) {

		pos.x = e.getWindow().getX();
		pos.y = e.getWindow().getY();
	}
	
	public void windowClosing(final WindowEvent e) { }

	public void windowDeactivated(final WindowEvent e) { }

	public void windowDeiconified(final WindowEvent e) { }

	public void windowIconified(final WindowEvent e) { }

	public void windowOpened(final WindowEvent e) { }

}

class FJHessian {

	void run(
		final ImagePlus imp,
		final boolean largest,
		final boolean middle,
		final boolean smallest,
		final boolean absolute,
		final String scale
	) {
		try {
			double scaleval;
			try 
			{ scaleval = Double.parseDouble(scale); }
			catch (Exception e) { throw new IllegalArgumentException("Invalid smoothing scale value"); }

			/**
			 * Creates a wrapper Image of the given <ImagePlus> object. 
			 * This is done by checking the element type and calling 
			 * the wrapper constructor of the corresponding Image <subclass> 
			 */
			final Image img = Image.wrap(imp);
			
			final Aspects aspects = img.aspects(); 				// Returns the aspect sizes of the image elements
			if (!FJ_Options.isotropic) img.aspects(new Aspects());
			
			final Hessian hess = new Hessian();
			hess.messenger.log(FJ_Options.log);
			hess.messenger.status(FJ_Options.pgs);
			hess.progressor.display(FJ_Options.pgs);

			final Vector<Image> eigenimages = hess.run(new FloatImage(img),scaleval,absolute);
					
			final int nrimgs = eigenimages.size();
			for (int i=0; i<nrimgs; ++i)
				eigenimages.get(i).aspects(aspects);
			if (nrimgs == 2) {
				if (largest) FJ.show(eigenimages.get(0),imp);
				if (smallest) FJ.show(eigenimages.get(1),imp);
			} else if (nrimgs == 3) {
				if (largest) FJ.show(eigenimages.get(0),imp);
				if (middle) FJ.show(eigenimages.get(1),imp);
				if (smallest) FJ.show(eigenimages.get(2),imp);
			}

			FJ.close(imp);

		} catch (OutOfMemoryError e) {
			FJ.error("Not enough memory for this operation");

		} catch (IllegalArgumentException e) {
			FJ.error(e.getMessage());

		} catch (IllegalStateException e) {
			FJ.error(e.getMessage());

		} catch (Throwable e) {
			FJ.error("An unidentified error occurred while running the plugin");

		}
	}
}
