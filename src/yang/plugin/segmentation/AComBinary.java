package yang.plugin.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

/**
 * This {@link threshold} method is a combination of NiBlack threshold method and Otsu threshold method with the relationship
 * Connected-Domain.  
 * @author yang
 * 2015/6/19 ~ 2015/6/23
 */
public class AComBinary implements PlugIn {

	ImageProcessor ip;
	static String dim = "5";
	static String k = "0.25";
	static String divation = "0.0";
	static String otsuError = "+20";
	
	static String maxConnectedDomain = "200";
	static String minConnectedDomain = "50";
	static String registerError = "50";
	static String a = "0.0";
	
	static boolean autoRegister = true;
	static boolean test = false;
	
	private NIBlack niBlack = new NIBlack();
	
	@Override
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return; 
		if (imp.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
			return;
		}
		if (imp.getNSlices() != 1)
			return;
		ip = imp.getProcessor().duplicate();
		
		if (!showDialog())
			return;
		
		//NOTE: The current image 'ip' changed!
		niBlackThreshold(Integer.parseInt(dim), Double.parseDouble(k));	
		
		ImagePlus nibleckIps = new ImagePlus("niblack" + dim + " " + k + " " + divation, ip.duplicate());
		nibleckIps.show();

		niBlackCorrection(imp.getProcessor(), ip);
		
		IJ.showProgress(1.0);
		ImagePlus ips = new ImagePlus("NiBlackCorrection ", ip);
		ips.show();
	}

	public boolean showDialog() {
		GenericDialog gd = new GenericDialog("NiBlack");
		gd.addStringField("kernel size: ", dim);
		gd.addStringField("k value: ", k);
		gd.addStringField("Deviation: ", divation);
		gd.addStringField("OtsuError: ", otsuError);
		gd.addStringField("MaxConnectedDomain: ", maxConnectedDomain);
		gd.addStringField("MinConnectedDomain: ", minConnectedDomain);
		gd.addStringField("Register Number: ", registerError);
		gd.addCheckbox("  Auto(Min = Reg = R)", autoRegister);
		gd.addStringField("a(R = m + a v)", a);
		gd.addCheckbox("  Test", test);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		dim = gd.getNextString();
		k = gd.getNextString();
		divation = gd.getNextString();
		otsuError = gd.getNextString();
		maxConnectedDomain = gd.getNextString();
		minConnectedDomain = gd.getNextString();
		registerError = gd.getNextString();
		autoRegister = gd.getNextBoolean();
		a = gd.getNextString();
		test = gd.getNextBoolean();
		return true;
	}

	/**
	 * <h1> NiBlack threshold. </h1>
	 * <br/>This method will process {@link ip} with the NiBlack threshold method.
	 * @param dims The local kernel dimension
	 * @param k The  coefficient of {@link s}
	 * <br/> T = m = k * s
	 */
	public void niBlackThreshold(int dims, double k) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		niBlack.setkValue(k);

		int[][] kernel = new int[dims][dims];
		ImageProcessor tmp = ip.duplicate();

		for (int y = 0; y < height; y++) {
			IJ.showProgress(1.0 / height * y);
			for (int x = 0; x < width; x++) {
				double threshold = getThreshNiblack(tmp, kernel, dims, x, y);
				threshold += Double.parseDouble(divation);
				if (tmp.getPixel(x, y) > threshold) {
					ip.set(x, y, 255);
				} else {
					ip.set(x, y, 0);
				}
			}
		}
	}
	
	public double getThreshNiblack(ImageProcessor ip, int[][] kernel, int dimen, int x, int y) {
		for (int j = 0; j < dimen; j++) {
			for (int i = 0; i < dimen; i++) {
				kernel[j][i] = ip.getPixel(x + i - dimen / 2, y + j - dimen / 2);
			}
		}
		niBlack.setPixelsArray(kernel, dimen);
		return niBlack.getThreshold();
	}

	public void niBlackCorrection(ImageProcessor cip, ImageProcessor nbip) {
		int error = 0;
		if(otsuError.startsWith("+")) {
			String errorString = otsuError.substring(1);
			error = Integer.parseInt(errorString);
		}
		else if(otsuError.startsWith("-")){
			String errorString = otsuError.substring(1);
			error = -Integer.parseInt(errorString);
		} else {
			error = Integer.parseInt(otsuError);
		}
		
		ConnectedDomain domain = new ConnectedDomain(cip, nbip);
		domain.init();
		domain.threshold(error);
		domain.connect();
	}
}


class NIBlack {
	private int[][] pixels;
	private int dimen;
	private double mean;
	private double var;
	private double k = 0.01; // the variable parameter
	private double threshold;
	private boolean state = true;
	
	NIBlack() {
		state = false;
	}

	NIBlack(int[][] pixels, int dimen) {
		setPixelsArray(pixels, dimen);
	}

	/**
	 * Once this method called, the value of the new local threshold will be updated.
	 * @param pixels
	 * @param dimen
	 */
	void setPixelsArray(int[][] pixels, int dimen) {
		state = true;
		this.pixels = pixels;
		this.dimen = dimen;
		init();
	}

	/**  
	 * This will calculate the {@link mean } and {@link var }(Standard Divation) 
	 * of the dimen x dimen local {@link pixels}.	
	 * <h1> T = mean + k * var </h1> 
	 * T is the local threshold value.
	 * */
	void init() {
		int addPixel = 0;
		for (int i = 0; i < dimen; i++) {
			for (int j = 0; j < dimen; j++) {
				addPixel += pixels[i][j];
			}
		}
		mean = (float) addPixel / (dimen * dimen); // initial the mean value

		double addErr2 = 0;
		for (int i = 0; i < dimen; i++) {
			for (int j = 0; j < dimen; j++) {
				addErr2 += (pixels[i][j] - mean) * (pixels[i][j] - mean);
			}
		}
		addErr2 /= (dimen * dimen);
		var = Math.sqrt(addErr2); // initial the standard deviation value

		threshold = mean + k * var;
	}

	void setkValue(double k) {
		this.k = k;
	}

	/**
	 * @return the local NiBLack threshold value. 
	 */
	double getThreshold() {
		if (!state)
			throw new IllegalAccessError("You must call the method "
					+ "setPixelsArray() before this method being called.");
		return threshold;
	}

}

class ConnectedDomain {
	
	private ImageProcessor nbip;	// The image need to be processed
	private ImageProcessor cdip;
	private int[][] marker;
	private final int[][] direction = {
			{1, 0}, {0, -1}, {0, 1}, {-1, 0}
	}; 
	
	private int threshold;
	private int num; 
	private int[] cDNum;
	private int loopControl; // Control the time of the recursion
	
	private int loopMax = 200;
	private int registerError = 50;	// The 'registration' number of Connected-Domain with the template image 'Otsu'
	private int minConnectedDomain = 50;; // The smallest allowed length of Connected-Domain
	
	private int width;
	private int height;

	/**
	 * We use the image {@link cip} to make an Otsu threshold template. the
	 * {@link nbip} image will compare with the Otsu threshold image by the
	 * connected-domain method.
	 * <p>
	 * <br> ConnectedCompare c = new ConnectedCompare(source, niBlack); 
	 * <br>	c.init();
	 * <br> c.threshold(); 
	 * <br> c.connect();<p>
	 * <h1> Finally, the reference parameter {@link nbip } will be changed and will be the final result.</h1><p>
	 * @param cip The source image
	 * @param nbip The image processed with NiBlack method
	 */
	ConnectedDomain(ImageProcessor cip, ImageProcessor nbip) {
		this.nbip = nbip;
		cdip = cip.duplicate();
		width = nbip.getWidth(); 
		height = nbip.getHeight();

		marker = new int[height][width];
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				if(nbip.getPixel(col, row) == 255) {
					marker[row][col] = 1;
				} else
					marker[row][col] = 0;
			}
		}
	}

	void init() {
		loopMax = Integer.parseInt(AComBinary.maxConnectedDomain);
		minConnectedDomain = Integer.parseInt(AComBinary.minConnectedDomain);
		registerError = Integer.parseInt(AComBinary.registerError);
	}
	
	/**
	 * You must invoke threshold() before this method called.
	 */
	void connect() {	
		marker();	
		
		if(AComBinary.autoRegister) 
			autoRegister(Double.parseDouble(AComBinary.a));

		int[] mark = new int[cDNum.length];
		for(int i = 0; i < mark.length; i++) {
			mark[i] = 0;
		}
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// NOTE: We assume that the number of the marked points with the same number
				// represents the length of the correspondent Connected-Domain.
				// Only if the minimum length of the Connected-Domain bigger than 
				// an allowed length and the correspondent position's value in the "Otsu template image"'s 4 or 8 neighbored 
				// is not all 0, then "Connected-Domain will be stayed".
				if(nbip.getPixel(x, y) == 0) continue;
				
				if(cDNum[marker[y][x]] < minConnectedDomain) { // Clear the Connected-Domain that is too short
					nbip.set(x, y, 0);
				}

				if (!AComBinary.test) {
					// NOTE: We set a 'mark' array which has the same length with the number of 'label+1' and 
					// 'label' [2 ~ label] represents the Connected-Domain marker value.
					// If the Otsu template image pixel value is 255, the correspondence Connected-Domain
					// will be stayed, we 'mark' the mark array 1 at the correspondence position.
					if (cdip.get(x, y) == 255) {
						mark[marker[y][x]]++;	// statistics of the 'registration' number
					}
				}
			}
		}
		if (!AComBinary.test) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if(nbip.get(x, y) == 0) continue;
					if (mark[marker[y][x]] < registerError && marker[y][x] != 0 && marker[y][x] != 1) {
						nbip.set(x, y, 0);
					}
				}
			}
		}
	}
	
	
	private void marker() {
		int label = 2;
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {					
				loopControl = 0;
				if(DFS(x, y, label)) {  
					label++;  
	            }  
			}
		}
		
		// NOTE:  0 1 Start->2 3 4 5 6 7 8 9<-End 
		// Assuming the max value of label is 9, and 0 1 useless 
		// We want the subscript of the array 'cdNum' equal to the value of label
		// SO the length of the array 'cdNum' should be label+1 == 10
		num = label + 1;
		
		cDNum = new int[num];
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				++cDNum[marker[y][x]];	// statistics of the number of correspondent marker 
										// and the subscript representing the labeling value 
			}
		}
	}

	/** Depth-First Search*/
	private boolean DFS(int x, int y, int label) {
		if(++loopControl > loopMax) {
			return false;
		}
		if(1 != marker[y][x]) {
			return false;
		} else {
			marker[y][x] = label;
			for(int i = 0; i < 4; i++) {
				if(check(x + direction[i][0], y + direction[i][1])) {
					DFS(x + direction[i][0], y + direction[i][1], label);
				}
			}
		}
		return true;
	}
	
	private void autoRegister(double a) {
		double mean = 0;
		double var = 0;
		for(int i=0; i<cDNum.length; i++) {
			mean += cDNum[i];
		}
		mean /= cDNum.length;
		
		for(int i=0; i<cDNum.length; i++) {
			var += (cDNum[i]-mean) * (cDNum[i]-mean);
		}
		var /= cDNum.length;
		var = Math.sqrt(var);
		
		registerError = minConnectedDomain = (int)(mean + var * a);		
//		System.out.println(minConnectedDomain + " " + registerError);
	}
	
	private boolean check(int x, int y) {
		if(x >= 0 
				&& x < width
				&& y >= 0
				&& y < height
				&& marker[y][x] == 1) {
			return true;
		} else {
			return  false;
		}
	}
	
	
	/**
	 * This method will make the {@link cdip } (current duplicate imageprocessor) 
	 * becoming Otsu image template.
	 */
	void threshold(int error) {
		this.threshold = getOtsuThreshold(cdip, error);
	}

	/** Return the Otsu threshold value*/
	int getThresholdValue() {
		return threshold;
	}

	int getOtsuThreshold(ImageProcessor ip, int error) {
		byte[] pixels = (byte[]) ip.getPixels();

		int bestThreshold = 0;
		double bestValue = 0.0;
		double currentValue = 0.0;

		int[] histogram = computeHist(pixels);

		for (int i = 0; i < 255; i++) {
			currentValue = otsu(histogram, i);
			if (currentValue > bestValue) {
				bestValue = currentValue;
				bestThreshold = i;
			}
		}

		int width = ip.getWidth();
		int height = ip.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (ip.get(x, y) < bestThreshold + error) {
					ip.set(x, y, 0);
				} else {
					ip.set(x, y, 255);
				}
			}
		}

		ImagePlus ips = new ImagePlus("Otsu " + bestThreshold + error, ip);
		ips.show();

		return bestThreshold;
	}

	private int[] computeHist(byte[] pixels) {
		int numPixels = pixels.length;
		int[] histogram = new int[256];
		int i, j;
		for (i = 0; i <= 255; i++) {
			histogram[i] = 0;
		}
		for (i = 0; i <= 255; i++) {
			for (j = 0; j < numPixels; j++) {
				if (pixels[j] == i) {
					histogram[i] += 1;
				}
			}
		}
		return histogram;
	}

	private double otsu(int[] histogram, int t) {
		double meanBg = 0, meanFg = 0;
		long numBg = 0, numFg = 0;
		double probBg, probFg, variance;
		int i;

		for (i = 0; i < t; i++) {
			numBg += histogram[i];
			meanBg += i * histogram[i];
		}

		if (numBg > 0) {
			meanBg = meanBg / numBg;
		}

		for (i = t; i < 256; i++) {
			numFg += histogram[i];
			meanFg += i * histogram[i];
		}
		if (numFg > 0) {
			meanFg = meanFg / numFg;
		}

		probBg = (double) numBg / (numBg + numFg);
		probFg = 1.0 - probBg;
		variance = probBg * probFg * Math.pow((meanBg - meanFg), 2);

		return variance;
	}
	
}





