package com.plugin.segmentation;

/**
 * Y. Sato, S. Nakajima, N. Shiraga, H. Atsumi, S. Yoshida, T. Koller, G. Gerig, and R. Kikinis, 
 * ¡°Three-dimensional multi-scale line filter for segmentation and visualization of curvilinear structures in medical images,¡±
 *  Med Image Anal., vol. 2, no. 2, pp. 143-168, June 1998.
 */

import features.TubenessProcessor;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

public class ATubeness_ implements PlugIn{
	
	static final String PLUGIN_VERSION = "1.2";
	
	@Override
	public void run(String arg) {
		ImagePlus original = WindowManager.getCurrentImage();
		if(null == original) {
			IJ.error("No current image to calculate tubeness of.");
			return;
		}
		
		Calibration calibration = original.getCalibration();
		
		double minimumSeparation = 1;
		if(null != calibration) {
			minimumSeparation = Math.min(calibration.pixelWidth, 
											Math.min(calibration.pixelHeight, calibration.pixelDepth));
		}
		
		GenericDialog gd = new GenericDialog("\"Tubeness\" Filter (version " + PLUGIN_VERSION + ")");
		gd.addNumericField("Sigma: ", (calibration==null) ? 1f : minimumSeparation, 4);
		gd.addMessage("(The default value for sigma is the minimum voxel separation.)");
		gd.addCheckbox("Use calibration information", calibration!=null);
		
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		
		double sigma = gd.getNextNumber();
		if(sigma <= 0) {
			IJ.error("The value of sigma must be positive");
			return;
		}
		
		boolean useCalibration = gd.getNextBoolean();
		
		TubenessProcessor tp = new TubenessProcessor(sigma, useCalibration);
		
		ImagePlus result = tp.generateImage(original);
		result.setTitle("tubenss of " + original.getTitle());
		
		result.show();
	}
}
