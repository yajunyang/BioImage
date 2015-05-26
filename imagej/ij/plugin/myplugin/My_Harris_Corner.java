package ij.plugin.myplugin;

import java.awt.AlphaComposite;
import java.awt.Menu;
import java.awt.Panel;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Menus;
import ij.gui.GenericDialog;
import ij.gui.HTMLDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class My_Harris_Corner implements PlugInFilter{

	ImagePlus im;
	static float alpha = HarrisCornerDetecter.DEFAULT_ALPHA;
	static int threshold = HarrisCornerDetecter.DEFAULT_THRESHOLD;
	static int nmax = 0;	// 显示的点
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		this.im = imp;
		if(arg.equals("about")) {
			showAbout();
			return DONE;
		}
		return DOES_8G + NO_CHANGES;
	}

	@Override
	public void run(ImageProcessor ip) {
		if(!showDialog()) return;
		HarrisCornerDetecter hcd = 
				new HarrisCornerDetecter(ip, alpha, threshold);
		hcd.findCorners();
		ImageProcessor result = hcd.showCornerPoints(ip);
		ImagePlus win = 
				new ImagePlus("Corners from" + im.getTitle(), result);
		win.show();
	}
	
	void showAbout() { 
		String cn = getClass().getName();
		IJ.showMessage("About " + cn + "...", "Harris Corner Detecter");
	}
	
	private boolean showDialog() {
		// 显示对话框，若取消或出错则返回false
		GenericDialog dlg = new GenericDialog("Harris Corner Detecter", IJ.getInstance());
		float def_alpha = HarrisCornerDetecter.DEFAULT_ALPHA;
		dlg.addNumericField("Threshold (default: "+def_alpha+")", threshold, 3);
		int def_threshold = HarrisCornerDetecter.DEFAULT_THRESHOLD;
		dlg.addNumericField("Threshold (default: "+def_threshold+")", threshold, 0);
		dlg.addNumericField("Max.points(0=show all)", nmax, 0);
		dlg.showDialog();
		if(dlg.wasCanceled()) {
			return false;
		}
		if(dlg.invalidNumber()) {
			IJ.showMessage("Error", "Invalid input number");
			return false;
		}
		
		alpha = (float) dlg.getNextNumber();
		threshold = (int) dlg.getNextNumber();
		nmax = (int) dlg.getNextNumber();
		return true;
	}
}








