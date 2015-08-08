package com.plugin.segmentation.anis;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import imagescience.feature.Differentiator;
import imagescience.image.Aspects;
import imagescience.image.Coordinates;
import imagescience.image.FloatImage;
import imagescience.image.Image;

/**
 * This class is for detecting the linear feature with default corresponding {@link scale} whose value is 1.5.
 * <p>Every point which is on the linear feature structure has a main orientation (-90'~90').
 * <br/>We get the matrix of "degree" at the corresponding position while if the point is not on
 * the linear feature structure. We set the corresponding position value -200.
 * <p> <h1>NOTICE!!!<h1/>
 * We can add some other innovate methods in {@link isLinearFeature()} method, such as morphology or neighbored filed based orientation... 
 * <p>
 * 
 * 	<br/>	//================================================================================//
	<br/>	// WE NEED ADD THE CONDITION THAT THE POINT IS ON THE LINEAR FEATURE STRUCTURE    
	<br/>	// Frangi 
	<br/>	// small   big  absolute
	<br/>	// L 		H-  	tubular structure (bright)  $ $
	<br/>	// L 		H+		tubular structure (dark)
	<br/>	// H-		H-      blob (bright)				$ $
	<br/>	// H+ 		H+		blob (dark)
	<br/>	// Just as the picture showing in res/frangi.bmp
	<br/>	// It shows the approaching degree between eigen values and linear feature
	<nr/>	// We can see if |H-| much bigger and L much smaller(no matter + or -), the value z will
	<br/>	// be much bigger. The value z can be regarded as the pixel point generating the linear 
	<br/>	// feature image.
	<br/>	//================================================================================//
		 * 
 * @author yang
 * 2015/5/27
 */
public class DirectionDetectHessian {
	private ImageProcessor ip;
	private int[][] degree; 
	private double[][] frangi;
	public static double scale = 1.5;
	private static double alpha = 0.5;
	private static double beta = 10;
    private static double allowCoeError = 0.01;
	private static double allowDetError = 1e-10;
	private double currentLocFrangi;
	
	/**
	 * Reference relationship
	 * @param ip
	 */
	public DirectionDetectHessian(ImageProcessor ip) {
		this.ip = ip;
		if(null != ip) {
			int width = ip.getWidth();
			int height = ip.getHeight();
			degree = new int[height][width];
			frangi = new double[height][width];
		}
		showDialog();
	}
	
	private void showDialog() {
		GenericDialog gd = new GenericDialog("Set Direction Direct Params");
		gd.addNumericField("Scale :", scale, 1);
		gd.addNumericField("Frangi alopha :", alpha, 1);
		gd.addNumericField("Frangi beta :", beta, 1);
		gd.addNumericField("CoeError :", allowCoeError, 2);
		gd.addNumericField("DetError :", allowDetError, 10);
		
		gd.showDialog();
		if(gd.wasCanceled()) return;
		
		scale = gd.getNextNumber();
		alpha = gd.getNextNumber();
		beta = gd.getNextNumber();
		allowCoeError = gd.getNextNumber();
		allowDetError = gd.getNextNumber();	
	}
	
	public void run() {
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		ImagePlus ips = new ImagePlus("ip", ip);
		Image image = Image.wrap(ips);
		checkAspects(image);
		
		Differentiator differentiator = new Differentiator();
		
		final Image smoothImage = (image instanceof FloatImage) ? image : new FloatImage(image); 
		final Image Hxx = differentiator.run(smoothImage.duplicate(), scale, 2, 0, 0);
		final Image Hxy = differentiator.run(smoothImage.duplicate(), scale, 1, 1, 0);
		final Image Hyy = differentiator.run(smoothImage,  scale, 0, 2, 0);
		
		Coordinates coords = new Coordinates();
		for(int y=0; y<height; y++) {
			for(int x=0; x<width; x++) {	
				coords.x = x;
				coords.y = y;
				double hxx = Hxx.get(coords);
				double hyy = Hyy.get(coords);
				double hxy = Hxy.get(coords);
				int degree = getAbsMaxEigenVectorAngle(hxx, hxy, hyy);
				this.degree[y][x] = degree;
				frangi[y][x] = currentLocFrangi;
			}
		}
//		wipeIsolatePoints(degree, true);
	}
	
	/**
	 * The input value {@link hxx}, {@link hyy}, {@link hxy} will generate a symmetry 2x2 matrix.
	 * We call it hessian matrix. It is sure having 2 eigenvalues. Choose the {@link eigen} value which is bigger
	 * in the form of {@link Math.abs()}.
	 * <p> Then calculate the {@link eigen vector(x,y)} of {@link eigen}. 
	 * <p> We assume only if the coefficient's absolution is bigger than the allowed error,then we
	 * judge whether its determinant among the allowed error, if ok, it can be regarded that having 
	 * infinite solutions.
	 * <p> If not satisfy, the eigen vector will be {@link {0, 0}}.
	 * @param hxx
	 * @param hxy
	 * @param hyy
	 * @return If the point (x, y) is not on the linear feature or the determinant is not zero. return -200 meaning
	 * this point is not what we want.
	 */
	public int getAbsMaxEigenVectorAngle(double hxx, double hxy, double hyy) {
		final double b = -(hxx + hyy);
		final double c = hxx * hyy - hxy * hxy;
		final double q = -0.5 * (b + (b < 0 ? -1 : 1) * Math.sqrt(b * b - 4 * c));	// sure having solution
		final double lam1 = q;
		final double lam2 = c / q;
		
		double eigen, eigenL;		
		if(Math.abs(lam1) > Math.abs(lam2)) {
			eigen = lam1;
			eigenL = lam2;
		}
		else {
			eigen = lam2;
			eigenL = lam1;
		}

		if(eigen > 0) {
			currentLocFrangi = 0;
			return -200;
		}
		
		double x=0, y=0;
		if (Math.abs(hxx - eigen) > allowCoeError
				&& // If element not near zero
				Math.abs(hxy) > allowCoeError
				&& Math.abs(hyy - eigen) > allowCoeError) {
			double det = (hxx - eigen) * (hyy - eigen) - hxy * hxy; // calculate determinant
			if (Math.abs(det) < allowDetError) {
				x = -hxy;
				y = hxx-eigen;
			} else {
				x = y = 0;
			}
		}
		if(x == 0) {
			currentLocFrangi = 0;
			return -200; 
		}		
		currentLocFrangi = frangi(eigen, eigenL, alpha, beta);
		return (int)( Math.atan(y / x) * 180.0 / Math.PI );
	}
	
	public double frangi(double eigen, double eigenL, double alpha, double beta) {
		double x = eigenL, y = eigen;
		double left = -(x / y) * (x / y) / (2 * alpha * alpha);
		double right = -(x * x + y * y) / (2 * beta * beta);
		return Math.exp(left) * (1 - Math.exp(right));
	}
	
	public void checkAspects(Image image) {
		final Aspects asps = image.aspects();
		if (asps.x <= 0) throw new IllegalStateException( "Aspect-ratio value in x-dimension less than or equal to 0");
		if (asps.y <= 0) throw new IllegalStateException( "Aspect-ratio value in y-dimension less than or equal to 0");
		if (asps.z <= 0) throw new IllegalStateException( "Aspect-ratio value in z-dimension less than or equal to 0");
	}
	
	/** @return a reference */
	public int[][] getDegree() {
		return degree;
	}
	
	/** @return a reference  */
	public double[][] getFrangi() {
		return frangi;
	}
	
	public void showDegreeMatrix() {
		for(int i=0; i<degree.length; i++) {
			for(int j=0; j<degree[i].length; j++) {
				System.out.print(degree[i][j] + "   ");
			}
			System.out.println();
		}
	}
	
	//======================= Some Tools ==============================//
	
	public static void wipeIsolatePoints(int[][] kernel, boolean is4Neighbor) {
		int row = kernel.length;
		int col = kernel[0].length;
		
		int[][] temp = new int[row][col];
		for(int i=0; i<row; i++) 
			for(int j=0; j<col; j++) {
				temp[i][j] = kernel[i][j];
			}
		
		if(is4Neighbor) {
			for(int i=1; i<row-1; i++) 
				for(int j=1; j<col-1; j++) {
					if(temp[i][j] != -200) {
						if(temp[i-1][j] == -200 
								&& temp[i+1][j] == -200 
								&& temp[i][j-1] == -200
								&& temp[i][j+1] == -200) {// 4 neighbored isolated points
							kernel[i][j] = -200;
						}
					}
				}
		} else {
			for(int i=1; i<row-1; i++) 
				for(int j=1; j<col-1; j++) {
					if(temp[i][j] != -200) {
						if(temp[i-1][j] == -200 
								&& temp[i+1][j] == -200 
								&& temp[i][j-1] == -200
								&& temp[i][j+1] == -200
								&& temp[i-1][j-1] == -200
								&& temp[i-1][j+1] == -200
								&& temp[i+1][j-1] == -200 
								&& temp[i+1][j+1] == -200 ) {// 8 neighbored isolated points
							kernel[i][j] = -200;
							i += 2;
						}
					}
				}
			}
		}
	
	//=================================================================//

	
	//===================== Additional Multi Part =====================//
	
	
	
}
