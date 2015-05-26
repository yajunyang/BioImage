package ij.plugin.myplugin;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class My_Display_Demo implements PlugInFilter{

	ImagePlus im = null;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		if(im == null) {
			IJ.noImage();
			return DONE;
		}
		this.im = imp;
		return DOES_ALL;
	}

	@Override
	public void run(ImageProcessor ip) {
		if(im == IJ.getImage()) {
			for(int i=0; i<10; i++) {
				ip.smooth();
				ip.rotate(30);
				im.updateAndDraw();
				IJ.wait(100);
			}
		}
	}
}
