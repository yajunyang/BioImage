package yang.plugin.segmentation.anis;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.awt.Color;

/**
 * This plugin implements the global log() algorithm and provides the anistropic
 * log() algorithm for processing the linear feature structures.
 * <p>
 * You can view the linear feature detection result in 4 ways. <br/>
 * 1, points on linear feature <br/>
 * 2, + degree <br/>
 * 3, - degree
 * <p>
 * <h1>In fact, what I really want to do is using the linear feature detection
 * result(orientation, points) on the log() algorithm. log() method can
 * efficiently "fill" the broken linear feature. However, some
 * "not weak junction parts`" are enhanced too. This is not what I want.
 * <h1/>
 * 
 * @author yang
 *
 */
public class AnistropicLog implements PlugIn {
	private static double k = 0.1;
	private static double sigma = 3.0; 
	private static boolean isAnistropic = true;
	private static boolean testMode = false;
	private static String[] testModeStr = { "255", "degree", "+degree", "-degree" };
	private static int currentMode = 0;

	private ImageProcessor ip; // This is a deep copy of the current image
	private double[][] frangi;

	@Override
	public void run(String arg) {
		ImagePlus ips = WindowManager.getCurrentImage();
		if (null == ips) {
			IJ.noImage();
			return;
		}
		if (ips.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
			return;
		}
		if (!showDialog())
			return;
		ip = ips.getProcessor().duplicate();
		drawLog();
		anistropicImageLog();
		showImage();
	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("AnistropicLog");
		gd.addNumericField("k :", k, 2);
		gd.addNumericField("Sigma :", sigma, 1);
		gd.addCheckbox("Use Anistropic :", isAnistropic);
		gd.addCheckbox("Anistropic Test :", testMode);
		gd.addChoice("Test Mode :", testModeStr, testModeStr[0]);

		gd.showDialog();

		if (gd.wasCanceled())
			return false;

		k = gd.getNextNumber();
		sigma = gd.getNextNumber();
		isAnistropic = gd.getNextBoolean();
		testMode = gd.getNextBoolean();
		currentMode = gd.getNextChoiceIndex();
		return true; 
	}

	public void showFrangi() {
		ImagePlus frangi = new ImagePlus();
		int w = this.frangi[0].length;
		int h = this.frangi.length;
		FloatProcessor fp = new FloatProcessor(w, h);
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++) {
				fp.putPixelValue(x, y, this.frangi[y][x]);
			}
		frangi.setProcessor(fp);
		new ImageConverter(frangi).convertToGray8();
		frangi.setDisplayRange(0, 255);
		frangi.setTitle("Frangi");
		frangi.show();
	}

	public void showImage() {
		ImagePlus result = new ImagePlus(" ", ip);
		
		if (isAnistropic && testMode) {
			result.setTitle("Linear Feature Marked");
			showFrangi();
		}

		if (isAnistropic && !testMode) {
			result.setTitle("Anitropic Log" + " " + sigma);
		}

		if (!isAnistropic)
			result.setTitle("Non Anistropic Log");
		
		result.setCalibration(WindowManager.getCurrentImage().getCalibration());
		result.setDisplayRange(0, 255);
		result.show();
	}

	public void drawLog() {
		double max = ip.getMax();
		double[] xValues = new double[255];
		double[] yValues = new double[255]; 

		for (int i = 0; i < xValues.length; i++) {
			xValues[i] = i;
//			yValues[i] = log(xValues[i], max, k);
			
			double e = -(i-50)*(i-50);
			e /= 2 * 0.1 * 0.1;
			yValues[i] = 10 * (Math.exp(e) + 1);
			
		}

		PlotWindow.noGridLines = false; // draw grid lines
		Plot plot = new Plot("Log", "x", "y", xValues, yValues);
		plot.setLimits(0, 255, 0, 255);
		plot.setLineWidth(1);
		plot.addLabel(0, 0, "k=" + k);
		plot.drawLine(0, 0, 255, 255);
		plot.setColor(Color.black);
		plot.show();
	}
	
	public void drawCorrect() {
		double[] xValues = new double[100];
		double[] yValues = new double[100];
		
		for(int i=0; i<100; i++) {
			xValues[i] = i / 100.0;
			yValues[i] = correctK(xValues[i]);
		}
		
		PlotWindow.noGridLines = false; // draw grid lines
		Plot plot = new Plot("mapping frangi to  log k", "frangi", "k", xValues, yValues);
		plot.setLimits(0, 1, 0, k);
		plot.setLineWidth(1);
		plot.addLabel(0, 0, "sigma=" + sigma);
		plot.addLabel(0.2, 0, "Kb=" + k);
		plot.setColor(Color.black);
		plot.show();
	}
	
	public void anistropicImageLog() {
		if (isAnistropic) {
			drawCorrect();
			DirectionDetectHessian direct = new DirectionDetectHessian(ip);
			direct.run();
			int[][] degree = direct.getDegree();
			frangi = direct.getFrangi();
			int width = ip.getWidth();
			int height = ip.getHeight();
			
			if (testMode) { // Test mode, just show the "degree" information
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						if (degree[y][x] == -200) {
							ip.putPixel(x, y, 0);
							continue;
						}
						switch (currentMode) {
						case 0:
							ip.putPixel(x, y, 255);
							break;
						case 1:
							ip.putPixel(x, y, 3 * Math.abs(degree[y][x]));
							break;
						case 2:
							ip.putPixel(x, y, 3 * degree[y][x]);
							break;
						case 3:
							ip.putPixel(x, y, -3 * degree[y][x]);
							break;
						}
					}
				}
			} else { // Anistropic log processing
				anislog(frangi, ip);
			}

		} else { // Not use the information of linear feature orientation to
					// take log processing
			double max = ip.getMax();
			for (int y = 0; y < ip.getHeight(); y++) {
				for (int x = 0; x < ip.getWidth(); x++) {
					ip.putPixel(x, y, (int) log(ip.getPixel(x, y), max, k));
				}
			}
		}

	}

	public void anislog(double[][] frangi, ImageProcessor ip) {
		double max = ip.getMax();
		for (int y = 0; y < ip.getHeight(); y++) {
			for (int x = 0; x < ip.getWidth(); x++) {
				// Here, we shouldn't separate the condition of "frangi[y][x]"
				// For, in many tests, this will make the image not smooth.
				double correct = correctK(frangi[y][x]);
				ip.putPixel(x, y, (int) log(ip.getPixel(x, y), max, correct));
			}
		}
	}

	/**
	 * y = 255/ln(1+k*max) * ln(1+k*x)
	 */
	public double log(double x, double max, double k) {
		double c = 255.0 / Math.log(1 + k * max);
		return c * Math.log(1 + k * x);
	}

	// when x=0, correct should not be zero, but can be very small 
	public double correctK(double x) {
		double correct = k * (1 - sigma*(x - 0.5) * (x - 0.5));
		assert correct != 0;
		return correct;
	}
	
//	public static void main(String[] args) {
//		double a = 255.0 / Math.log(1);
//		double b = a * Math.log(1);
//		System.out.println(b);
//	}

}
