/*
 * Dialog Description
Largest/Middle/Smallest eigenvalue of structure tensor. 
After computation of the structure tensor the resulting eigenvalues are ordered for each image element (pixel/voxel). 
All largest eigenvalues are put in a separate image, as are all smallest (and middle, in 3D). 
These options determine which of these so-called eigenimages will be displayed by the plugin.
Due to the positive-semidefiniteness of the structure tensor, the eigenvalues are always larger than or equal to zero. 
If the size of the image is one in the z-dimension (a single slice), the plugin computes 2D-tensor eigenvalues, 
otherwise it computes 3D-tensor eigenvalues (for every time frame and channel in a 5D image).

 * Smoothing scale. 
The smoothing scale is equal to the standard deviation of the Gaussian derivative kernels used for computing the elements of the structure tensor and must be larger than zero. 
See the algorithmic details in the description of the Derivatives dialog for boundary conditions in setting this parameter. 
If physically isotropic Gaussian image smoothing is to be applied (which can be specified in the Options dialog), 
then in each dimension the scale is divided by the sampling interval in that dimension (the pixel width/height/depth as specified in ImageJ > Image > Properties).

 * Integration scale. 
The integration scale is equal to the standard deviation of the Gaussian kernel used for smoothing the elements of the structure tensor and must be larger than zero.
See the algorithmic details in the description of the Derivatives dialog for boundary conditions in setting this parameter. 
If physically isotropic Gaussian image smoothing is to be applied (which can be specified in the Options dialog), 
then in each dimension the scale is divided by the sampling interval in that dimension (the pixel width/height/depth as specified in ImageJ > Image > Properties).

References

[1]	A. R. Rao, B. G. Schunck. Computing Oriented Texture Fields. 
	CVGIP: Graphical Models and Image Processing, vol. 53, no. 2, 1991, pp. 157-185.
[2]	J. Weickert. 
	Coherence-Enhancing Diffusion Filtering. International Journal of Computer Vision, vol. 31, no. 2, 1999, pp. 111-127.
 */

package com.plugin.segmentation.fj;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagescience.feature.Structure;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

public class FJ_Structure implements PlugIn, WindowListener {

	private static boolean largest = true;
	private static boolean middle = false;
	private static boolean smallest = true;

	private static String sscale = "1.0";
	private static String iscale = "3.0";

	private static Point pos = new Point(-1, -1);

	public void run(String arg) {

		if (!FJ.libcheck())
			return;
		final ImagePlus imp = FJ.imageplus();
		if (imp == null)
			return;

		FJ.log(FJ.name() + " " + FJ.version() + ": Structure");

		GenericDialog gd = new GenericDialog(FJ.name() + ": Structure");
		gd.addCheckbox(" Largest eigenvalue of structure tensor    ", largest);
		gd.addCheckbox(" Middle eigenvalue of structure tensor    ", middle);
		gd.addCheckbox(" Smallest eigenvalue of structure tensor    ", smallest);
		gd.addPanel(new Panel(), GridBagConstraints.EAST,
				new Insets(5, 0, 0, 0));
		gd.addStringField("                Smoothing scale:", sscale);
		gd.addStringField("                Integration scale:", iscale);

		if (pos.x >= 0 && pos.y >= 0) {
			gd.centerDialog(false);
			gd.setLocation(pos);
		} else
			gd.centerDialog(true);
		gd.addWindowListener(this);
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		largest = gd.getNextBoolean();
		middle = gd.getNextBoolean();
		smallest = gd.getNextBoolean();
		sscale = gd.getNextString();
		iscale = gd.getNextString();

		(new FJStructure()).run(imp, largest, middle, smallest, sscale, iscale);
	}

	public void windowActivated(final WindowEvent e) {
	}

	public void windowClosed(final WindowEvent e) {

		pos.x = e.getWindow().getX();
		pos.y = e.getWindow().getY();
	}

	public void windowClosing(final WindowEvent e) {
	}

	public void windowDeactivated(final WindowEvent e) {
	}

	public void windowDeiconified(final WindowEvent e) {
	}

	public void windowIconified(final WindowEvent e) {
	}

	public void windowOpened(final WindowEvent e) {
	}

}

class FJStructure {

	void run(final ImagePlus imp, final boolean largest, final boolean middle,
			final boolean smallest, final String sscale, final String iscale) {

		try {
			double sscaleval, iscaleval;
			try {
				sscaleval = Double.parseDouble(sscale);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Invalid smoothing scale value");
			}
			try {
				iscaleval = Double.parseDouble(iscale);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Invalid integration scale value");
			}

			final Image img = Image.wrap(imp);
			final Aspects aspects = img.aspects();
			if (!FJ_Options.isotropic)
				img.aspects(new Aspects());
			final Structure structure = new Structure();
			structure.messenger.log(FJ_Options.log);
			structure.messenger.status(FJ_Options.pgs);
			structure.progressor.display(FJ_Options.pgs);

			final Vector<Image> eigenimages = structure.run(
					new FloatImage(img), sscaleval, iscaleval);

			final int nrimgs = eigenimages.size();
			for (int i = 0; i < nrimgs; ++i)
				eigenimages.get(i).aspects(aspects);
			if (nrimgs == 2) {
				if (largest)
					FJ.show(eigenimages.get(0), imp);
				if (smallest)
					FJ.show(eigenimages.get(1), imp);
			} else if (nrimgs == 3) {
				if (largest)
					FJ.show(eigenimages.get(0), imp);
				if (middle)
					FJ.show(eigenimages.get(1), imp);
				if (smallest)
					FJ.show(eigenimages.get(2), imp);
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
