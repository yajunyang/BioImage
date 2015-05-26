package com.plugin.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagescience.feature.Differentiator;
import imagescience.image.Aspects;
import imagescience.image.Coordinates;
import imagescience.image.FloatImage;
import imagescience.image.Image;

/**
 * In this class, we only deal with 8-bits image.
 * 
 * <p>2015/5/25
 * @author yang
 *
 */
public class AnistropicHessianSmooth implements PlugIn{

	private ImagePlus ips;
	private static double scale = 1.5;
	private static double a = 2.0;
	private static double b = 1.0;
	private static double h = 3;
	private static double sample = 1;
	private static double allowError = 0.02;
	private static boolean testMode = false;
	
	private Differentiator differentiator = new Differentiator();
	
	@Override
	public void run(String arg) {
		ips = WindowManager.getCurrentImage();
		if(null == ips) {
			IJ.noImage();
			return;
		}
		if(ips.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
			return;
		}
		if(!showDialog()) return;
		
		ImageProcessor result = ips.getProcessor().duplicate();
		anisSmooth(result);
		
		ImagePlus ipsResult = new ImagePlus(ips.getTitle()+" Anistropic Smmoth " + scale + " " + allowError, result);
		ipsResult.setCalibration(ips.getCalibration());
		ipsResult.show();
	}
	
	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("AnistropicHessianSmooth");
		gd.addNumericField("Gaussian 2th Scale: ", scale, 1);
		gd.addNumericField("a: ", a, 1);
		gd.addNumericField("b:", b, 1);
		gd.addNumericField("h: ", h, 0);
		gd.addNumericField("Sample: ", sample, 0);
		gd.addNumericField("allow error: ", allowError, 2);
		gd.addCheckbox("Test Mode: ", testMode);
		gd.showDialog();
	
		if(gd.wasCanceled()) return false;
		scale = gd.getNextNumber();
		a = gd.getNextNumber();
		b = gd.getNextNumber();
		h = gd.getNextNumber();
		sample = gd.getNextNumber();
		allowError = gd.getNextNumber();
		testMode = gd.getNextBoolean();
		
		if((int)h % 2 == 0) h -= 1;
		if(h < 3) h = 3;
		return true;
	}
	
	public void anisSmooth(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		ImagePlus ips = new ImagePlus("ip", ip);
		Image image = Image.wrap(ips);
		checkAspects(image);
		
		final Image smoothImage = (image instanceof FloatImage) ? image : new FloatImage(image); 
		final Image Hxx = differentiator.run(smoothImage.duplicate(), scale, 2, 0, 0);
		final Image Hxy = differentiator.run(smoothImage.duplicate(), scale, 1, 1, 0);
		final Image Hyy = differentiator.run(smoothImage.duplicate(),  scale, 0, 2, 0);
	
		Coordinates coords = new Coordinates();
		for(int y=0; y<height; y++) {
			for(int x=0; x<width; x++) {	
				coords.x = x;
				coords.y = y;
				double hxx = Hxx.get(coords);
				double hyy = Hyy.get(coords);
				double hxy = Hxy.get(coords);
				int degree = getAbsMaxEigenVectorAngle(hxx, hxy, hyy);
				if(degree == -200 || degree == -201) continue;	// If not on the linear feature
//				if(degree == -201) { 	// on the linear feature but orientation not clear			
//					double[][] kernel = getOrientedGaussianKernel(b, b, 0, (int)h, sample);
//					convolve(ip, x, y, kernel);
//				} 
				else { 
					double[][] kernel = getOrientedGaussianKernel(a, b, degree, (int)h, sample);
					kernel = reverseKernel(kernel, 0);
					convolve(ip, x, y, kernel);
				}
			}
		}
		
	}
	
	/**The original point is in the center of the matrix which is odd
	 * If axis is 0, inverse by y axis
	 * If axis is 1, inverse by x axis 
	 * @param kernel
	 * @param axis
	 * @return
	 */
	public double[][] reverseKernel(double[][] kernel, int axis) {
		int col = kernel[0].length;
		int row = kernel.length;
		if(col != row || row %2 ==0) throw new IllegalArgumentException();
		double[][] k = new double[row][col];
		for(int i=0; i<row; i++) {
			for(int j=0; j<col; j++) {
				switch(axis) { 
					case 0:
						k[i][j] = kernel[row-1-i][j];
						break;
					case 1:
						k[i][j] = kernel[i][col-1-j];	
				}
			}
		}
		return k;
	}
	
	public void checkAspects(Image image) {
		final Aspects asps = image.aspects();
		if (asps.x <= 0) throw new IllegalStateException( "Aspect-ratio value in x-dimension less than or equal to 0");
		if (asps.y <= 0) throw new IllegalStateException( "Aspect-ratio value in y-dimension less than or equal to 0");
		if (asps.z <= 0) throw new IllegalStateException( "Aspect-ratio value in z-dimension less than or equal to 0");
	}
	
	public void convolve(ImageProcessor ip, int x, int y, double[][] kernel) {
		double pixelValue = 0;
		for(int i=0; i<(int)h; i++) {	
			for(int j=0; j<(int)h; j++) { // scan from row/x
				pixelValue += kernel[i][j] * ip.getPixel(x + j - (int)h/2, y + i - (int)h/2);
			}
		}
		// This is for seeing which point is smoothed.
		if(testMode) pixelValue = 255; 
		ip.putPixel(x, y, (int)pixelValue);
	}
	
	/**
	 * The input value {@link hxx}, {@link hyy}, {@link hxy} will generate a symmetry 2x2 matrix.
	 * We call it hessian matrix. It must have 2 eigenvalues. Choose the {@link eigen} value which is bigger
	 * in the form of {@link Math.abs()}.
	 *  <p> Then calculate the {@link eigen vector(x,y)} of {@link eigen}. 
	 *  
	 * @param hxx
	 * @param hxy
	 * @param hyy
	 * @return
	 */
	public int getAbsMaxEigenVectorAngle(double hxx, double hxy, double hyy) {
		final double b = -(hxx + hyy);
		final double c = hxx * hyy - hxy * hxy;
		final double q = -0.5 * (b + (b < 0 ? -1 : 1) * Math.sqrt(b * b - 4 * c));	// sure having solve
		final double lam1 = q;
		final double lam2 = c / q;
		
		double eigen;		
		if(Math.abs(lam1) > Math.abs(lam2)) 
			eigen = lam1;
		else {
			eigen = lam2;
		}
		if(eigen > 0) return -200;
		
		// a b       (a-r)x + by = 0     | a-r  b ||x| 
		// b c       bx + (c-r)y = 0     | b   c-r||y| = 0 
		double x=0, y=0;
		// If having solution, the determinant will be zero
		// We don't consider the situation that element be zero
		// If any element be zero, let vector be (0, 0)
		if (Math.abs(hxx-eigen)>allowError &&    // If element not near zero
				Math.abs(hxy)>allowError &&
				Math.abs(hyy-eigen)>allowError) {
			x = -hxy;
			y = hxx-eigen;
		}
		if(x == 0)
			return -201; 
		
		return (int)( Math.atan(y / x) * 180.0 / Math.PI );
	}
	
	/**
	 * This will produce a two dimensional anistropic gaussian kernel.
	 * Here, the coordinate system is different from the image coordinate system.
	 * The "original point" is located at the ellipse center and x axis pointing right, y axis pointing top.
	 * <p>f(x, y) = exp(-(xcosr-ysinr)^2/a^2 - (xsinr+ycosr)^2/b^2)<p>
	 * @param a	 a > b	
	 * @param b
	 * @param degree is the variable 'r' in the above equation. 
	 * @param h	 the kernel size, usually be 3, 5, 7...
	 * @param sample  the sampling rate, when calculating f(x,y), {@link sample} multiply x, y.
	 * @return a 2D gaussian kernel
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
			}
		}
		return kernel;
	}

	
	public static void main(String[] args) {
//		byte[] image = {
//				 0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
//				 0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
//				 0,   0,  100,  0,   0,   0,   0,   0,   0,   0,   0,   0 ,
//				 0,   0,   0,  100,  40,  0,   0,   0,   0,   0,   0,   0 ,
//				 0,   0,   0,   10,  10,  0,   0,   0,   0,   0,   0,   0 ,
//				 0,   0,   0,   0,   0,   0,   0,   0,   0,   80,  0,   0 ,
//				 0,   0,   0,   0,   0,   0,   0,   0,  100,  0,   0,   0 ,
//				 0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0 
//			};
//			ImageProcessor ip = new ByteProcessor(12, 8, image);
//			ImagePlus source = new ImagePlus("source", ip);
//			source.show();
	}
}
