package yang.plugin.segmentation.anis;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * <p>
 *===========What annotates has moved to class {@link DirectionDetectHessian.java}
 * <p>
 * In this class, we only deal with 8-bits images.
 * <p>Using gaussian function to convolve with the original image
 * to get the hessian matrix which relates to {@link scale}, and
 * we get the eigen values of the hessian matrix. <p>Get the eigen vector
 * of the eigen value which is bigger in absolute form. By the vector,
 * we can get the angle, this is considered as the orientation of the linear feature.
 * <p> We provide two modes to see the process. The {@link testMode} checkbox shows
 * which point is smoothed.<p> In fact, there are two procedures to smooth the linear feature of the 
 * dentrite and axion. First, judge which point need smoothed and find the orientation 
 * of this point.Second, generating the anistropic gaussian smooth kernel to smooth the
 * "point" along the direction.
 * <p>2015/5/25
 * @author yajun yang
 */
public class AnistropicHessianSmooth implements PlugIn{

	private ImagePlus ips;

	private static double a = 2.0;
	private static double b = 1.0;
	private static double h = 3;
	private static double sample = 1;
	
	/** If {@link testMode}is true, it will show the "linear feature marked" image */
	private static boolean testMode = false;
	/** Only when {@link testMode} is true, if {@link isMainMode} is true, it will put out the marked points' orientation degree and the smooth matrix */
	private static boolean isMainMode = false; 
	
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
		if(!showDialog(true)) return;
		
		ImageProcessor result = ips.getProcessor().duplicate();
		anisSmooth(result);
		
		ImagePlus ipsResult = new ImagePlus(ips.getTitle()+" Anistropic Smmoth " + DirectionDetectHessian.scale, result);
		ipsResult.setCalibration(ips.getCalibration());
		ipsResult.setDisplayRange(0, 255);
		ipsResult.show();
	}
	
	public boolean showDialog(boolean isPlugin) {
		GenericDialog gd = new GenericDialog("AnistropicHessianSmooth");
		
		gd.addCheckbox("Whether Test Mode ", testMode);
		gd.addCheckbox("Whether Test  Out ", isMainMode);
		
		gd.addNumericField("a :", a, 1);
		gd.addNumericField("b :", b, 1);
		gd.addNumericField("h :", h, 0);
		gd.addNumericField("Sample :", sample, 0);
		gd.showDialog();

		if (gd.wasCanceled())
			return false;
		
		testMode = gd.getNextBoolean();
		isMainMode = gd.getNextBoolean();
		
		a = gd.getNextNumber();
		b = gd.getNextNumber();
		h = gd.getNextNumber();
		sample = gd.getNextNumber();
		if ((int) h % 2 == 0)
			h -= 1;
		if (h < 3)
			h = 3;
		return true;
	}
	
	public void anisSmooth(ImageProcessor ip) {
		DirectionDetectHessian direct = new DirectionDetectHessian(ip);
		direct.run();
		
		int[][] degree = direct.getDegree();
		if (degree == null) {
			IJ.showMessage("wrong degree output!");
			return;
		}
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				if(degree[y][x] != -200) {
						System.out.println("x= " + x + " y= " + y + " " + degree[y][x]);
					double[][] kernel = getOrientedGaussianKernel(a, b, degree[y][x], (int)h, sample);
					convolve(ip, x, y, kernel);
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
						break;
				}
			}
		}
		return k;
	}
	
	
	public void convolve(ImageProcessor ip, int x, int y, double[][] kernel) {	
		double pixelValue = 0;
		
		if(testMode) { // mark the smoothed point 	
			pixelValue = 255; 
			ip.putPixel(x, y, (int)pixelValue);
			if(isMainMode) {
				for(int i=0; i<(int)h; i++) {	
					for(int j=0; j<(int)h; j++) { 
						System.out.print(kernel[i][j] + " ");
					}
					System.out.println();
				}
			}
			return;
		} 
		
		for(int i=0; i<(int)h; i++) {	
			for(int j=0; j<(int)h; j++) { 
				pixelValue += kernel[i][j] * ip.getPixel(x + j - (int)h/2, y + i - (int)h/2);
			}
		}
		ip.putPixel(x, y, Math.round((float)pixelValue));
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
	public double[][] getOrientedGaussianKernel(double a, double b, int degree, int h, double sample) {
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

	public AnistropicHessianSmooth() {}
	
	public void main(String[] args) {	
		isMainMode = true;
		byte[] image = {
				 0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
				 0,   80,  0,   0,   0,   0,   0,   0,   0,   0,   0,   0,
				 0,   0,  100,  0,   0,   0,   0,   0,   0,   0,   0,   0 ,
				 0,   0,   0,  100,  0,   0,   0,   0,   0,   0,   0,   0 ,
				 0,   0,   0,   0,  100,  0,   0,   0,   0,   0,  100,  0 ,
				 0,   0,   0,   0,   0,   0,   0,   0,   0,   80,  0,   0 ,
				 0,   0,   0,   0,   0,   0,   0,   0,  100,  0,   0,   0 ,
				 0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0 
			};
			ImageProcessor ip = new ByteProcessor(12, 8, image);
			
			for(int y=0; y<ip.getHeight(); y++) {
				for(int x=0; x<ip.getWidth(); x++) {
					System.out.print(ip.getPixel(x, y) + "    ");
				}
				System.out.println();
			}
			
			AnistropicHessianSmooth ani = new AnistropicHessianSmooth();
			ani.anisSmooth(ip);
			
			for(int y=0; y<ip.getHeight(); y++) {
				for(int x=0; x<ip.getWidth(); x++) {
					System.out.print(ip.getPixel(x, y) + "    ");
				}
				System.out.println();
			}
	}
}
