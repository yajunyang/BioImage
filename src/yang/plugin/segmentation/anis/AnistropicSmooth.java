package yang.plugin.segmentation.anis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * 2015/5/19
 * <p>This input image should be one filtered by LinearFeature2D method.
 * The input image whose pixel value being zero represents "this point is not on the linear feature structure".
 * We take the assumption that the max convolution result in one point represents the angle that fixed the linear feature orientation. 
 * <p>
 * =====================This is a wrong method=======================
 * <p>
 * @author yang
 */
@Deprecated
public class AnistropicSmooth implements PlugIn {
	private static String a = "2.0";
	private static String b = "1.0";
	private static String ratio = "1.0";
	private static String sizeStr = "3";
	public boolean testModel = false; // If this is a "main" app
	
	/** Current image to be smoothed */
	private ImagePlus ips; 
	
	/**
	 * When we set {@link #iteration} 9, the iteration value(degree) will be 20/
	 * When 4, it will be 45/
	 * When 5, it will be 36/
	 * When 18, it will be 10/  
	 */
	private static String iteration = "4";
	
	@Override
	public void run(String arg) {
		ips = WindowManager.getCurrentImage();
		if(null == ips) {
			IJ.noImage();
			return;
		}	
		if(!showDialog()) return;
		
		int size = ips.getNSlices();
		ImageStack stack = ips.getStack();
		ImageProcessor ip = null;
		
		for(int i=1; i<=size; i++) {
			ip = stack.getProcessor(i);
			anistropicGaussianSmooth(ip);
		}
	}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Anistropic Gauassian Smooth");
		gd.addStringField("sample ratio: ", ratio);
		gd.addStringField("kernel size: ", sizeStr);
		gd.addStringField("iteration:", iteration);
		gd.addStringField("a:", a);
		gd.addStringField("b:", b);
		gd.showDialog();
		
		if(gd.wasCanceled())
			return false;
		
		ratio = gd.getNextString();
		sizeStr = gd.getNextString();
		iteration = gd.getNextString();
		a = gd.getNextString();
		b = gd.getNextString();
		return true;
	}
	
	/**
	 * This method smoothes the imp image with an anistropic gaussian filter, 
	 * we don't calculate the orientation of the linear structure of the point,
	 * 	but take a loop of all directions and get the max convolution result, 
	 * 	if the kernel's direction along with  the current linear feature structure, 
	 * 	the convolution result should be max.
	 */
	public void anistropicGaussianSmooth(ImageProcessor imp) {
		ImageProcessor source = imp;
		ImageProcessor target = new ShortProcessor(imp.getWidth(), imp.getHeight());
		
		int width = imp.getWidth();
		int height = imp.getHeight();
		
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int value = source.getPixel(x, y);
				if(value == 0) {	// The point is not on the linear structure 
					continue;
				} else {
					setTargetMaxConvolve(source, target, x, y);
				}
			}
		
		if(null == ips) return;
		String title = ips.getTitle() + " a=" + a + " b=" + b + " ratio=" + ratio + " size=" + sizeStr + " iteration=" + iteration;
		ImagePlus result = new ImagePlus(title, target);
		result.show();
	}
	
	/**We convolve the input image {@link source} at the pixel position { {@link x}, {@link y} }.
	 * with an anistropic 2D gaussian kernel and get different convolution results with different
	 * orientation.
	 * We assume that the max convolution result representing the orientation of the linear feature.  
	 * @param source
	 * @param target
	 * @param x
	 * @param y
	 */
	public void setTargetMaxConvolve(ImageProcessor source, ImageProcessor target, int x, int y) {
		double aValue = Double.parseDouble(a);
		double bValue = Double.parseDouble(b);
		double ratioValue = Double.parseDouble(ratio);
		int size = Integer.parseInt(sizeStr);
		int number = Integer.parseInt(iteration);
		int delta = 180 / number;
		float value = 0;
		int keep = 0;		// keep the value of degree when "value" is max
		
		for(int degree = -90; degree < 90; degree += delta) {
			float pixelValue = 0;
			double[][] kernel = getOrientedGaussianKernel(aValue, bValue, degree, size, ratioValue);
			for(int j=0; j<size; j++) {	
				for(int i=0; i<size; i++) {
					pixelValue += kernel[i][j] * source.getPixel(x + i - size/2, y + j - size/2);
				}
			}
			if(pixelValue > value) {
				value = pixelValue;
				keep = degree;
			}
			if(testModel) 
				System.out.println(x + " " + y + " " + degree + " " + pixelValue + " " + keep + " " + value);
		}
		if(testModel)
			System.out.println();
		target.putPixel(x, y, Math.round(value));
	}
	
	/**
	 * paper "A Separable Filter for Directional Smoothing"
	 * The matrix we get is symmetric.
	 * @param a		a > b
	 * @param b
	 * @param sigma the orientation which represented by degree 0~90
	 * @param h the size of oriented gaussian kernel
	 * @param sample The sample rate, control the distance of sampling
	 * @return
	 */
	public  double[][] getOrientedGaussianKernel(double a, double b, int degree, int h, double sample) {
		if(h < 3) h = 3;
		if(h % 2 == 0) h -= 1;
		if(a == 0 || b == 0) {
			IJ.error("Can't make the value a and b of the gaussian kernel zero");
		}
		if(sample <= 0.0) sample = 1;
		double kernel[][] = new double[h][h];
		double sum = 0;
		double sigma = (double)degree / 180.0 * Math.PI;
		for(int y = -h/2; y <= h/2; y++)
			for(int x = -h/2; x <= h/2; x++) {
				double x2 = x * Math.cos(sigma) - y * Math.sin(sigma);
				x2 *= sample; x2 *= x2;
				double y2 = x * Math.sin(sigma) + y * Math.cos(sigma);
				y2 *= sample; y2 *= y2;
				x2 /= (a * a);
				y2 /= (b * b);
				kernel[x+h/2][y+h/2] = Math.exp(-(x2 + y2));
				sum += kernel[x+h/2][y+h/2];
			}
		for(int y=0; y<h; y++) {
			for(int x=0; x<h; x++) {
				kernel[x][y] /= sum;
				if(testModel) 
					System.out.print(kernel[x][y] + "    ");
			}
			if(testModel) 
				System.out.println();
		}
		return kernel;
	}
	
	public static void main(String[] args) {
		byte[] image = {
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ,0 ,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ,0 ,
			 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0 ,0 ,
			 0, 0, 0, 100, 40, 0, 0, 0, 0, 0, 0 ,0 ,
			 0, 0, 0, 10, 10, 0, 0, 0, 0, 0, 0 ,0 ,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 80, 0 ,0 ,
			 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0 ,0 ,
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ,0 
		};
		ImageProcessor ip = new ByteProcessor(12, 8, image);
		ImagePlus source = new ImagePlus("source", ip);
		source.show();
		
		AnistropicSmooth anistropicSmooth = new AnistropicSmooth();
		anistropicSmooth.testModel = true;
		anistropicSmooth.anistropicGaussianSmooth(ip);
	}
	
}

