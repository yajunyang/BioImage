package yang.plugin.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.Thresholder;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import imagescience.mesh.Cone;

public class CCC implements PlugIn{
	
	@Override
	public void run(String arg0) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return;
		if (imp.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
			return;
		}
		if (imp.getNSlices() != 1)
			return;
		ImagePlus niblack = duplicateImage(imp.getProcessor());
		ImagePlus otsu = duplicateImage(imp.getProcessor());
				
		Otsu(otsu, 15, 0.0, 0.0, true);
		Niblack(niblack, 15, 0, 0, true);

		niblack.setTitle("Niblack");
		niblack.show();
		otsu.setTitle("Otsu");
		otsu.show("Otsu");
		
		
		ImageProcessor res = niblack.getProcessor().duplicate();
		Connect c = new Connect(res, otsu.getProcessor());
		c.ratioAlgorithm(0.5);
		
		new ImagePlus("NiblackCorrection", res).show();
	}
	
	void Otsu(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite) {
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini. Same algorithm as in Auto_Threshold, this time on local circular regions
		int[] data;
		int w=imp.getWidth();
		int h=imp.getHeight();
		int position;
		int radiusx2=radius * 2;
		ImageProcessor ip=imp.getProcessor();
		byte[] pixels = (byte []) ip.getPixels();
		byte[] pixelsOut = new byte[pixels.length]; // need this to avoid changing the image data (and further histograms)
		byte object;
		byte backg;

		if (doIwhite){
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		int k,kStar;  // k = the current threshold; kStar = optimal threshold
		int N1, N;    // N1 = # points with intensity <=k; N = total number of points
		double BCV, BCVmax; // The current Between Class Variance and maximum BCV
		double num, denom;  // temporary bookeeping
		int Sk;  // The total intensity for all histogram points <=k
		int S, L=256; // The total intensity of the image. Need to hange here if modifying for >8 bits images
		int roiy;

		Roi roi = new OvalRoi(0, 0, radiusx2, radiusx2);
		//ip.setRoi(roi);
		for (int y =0; y<h; y++){
			IJ.showProgress((double)(y)/(h-1)); // this method is slow, so let's show the progress bar
			roiy = y-radius;
			for (int x = 0; x<w; x++){
				roi.setLocation(x-radius,roiy);
				ip.setRoi(roi);
				//ip.setRoi(new OvalRoi(x-radius, roiy, radiusx2, radiusx2));
				position=x+y*w;
				data = ip.getHistogram();

				// Initialize values:
				S = N = 0;
				for (k=0; k<L; k++){
					S += k * data[k];	// Total histogram intensity
					N += data[k];		// Total number of data points
				}

				Sk = 0;
				N1 = data[0]; // The entry for zero intensity
				BCV = 0;
				BCVmax=0;
				kStar = 0;

				// Look at each possible threshold value,
				// calculate the between-class variance, and decide if it's a max
				for (k=1; k<L-1; k++) { // No need to check endpoints k = 0 or k = L-1
					Sk += k * data[k];
					N1 += data[k];

					// The float casting here is to avoid compiler warning about loss of precision and
					// will prevent overflow in the case of large saturated images
					denom = (double)( N1) * (N - N1); // Maximum value of denom is (N^2)/4 =  approx. 3E10

					if (denom != 0 ){
						// Float here is to avoid loss of precision when dividing
						num = ( (double)N1 / N ) * S - Sk; 	// Maximum value of num =  255*N = approx 8E7
						BCV = (num * num) / denom;
					}
					else
						BCV = 0;

					if (BCV >= BCVmax){ // Assign the best threshold found so far
						BCVmax = BCV;
						kStar = k;
					}
				}
				// kStar += 1;	// Use QTI convention that intensity -> 1 if intensity >= k
				// (the algorithm was developed for I-> 1 if I <= k.)
				//return kStar;
				pixelsOut[position] = ((int) (pixels[position]&0xff)>kStar) ? object : backg;
			}
		}
		for (position=0; position<w*h; position++) pixels[position]=pixelsOut[position]; //update with thresholded pixels
	}

	void Niblack(ImagePlus imp, int radius,  double par1, double par2, boolean doIwhite  ) {
		// Niblack recommends K_VALUE = -0.2 for images with black foreground 
		// objects, and K_VALUE = +0.2 for images with white foreground objects.
		//  Niblack W. (1986) "An introduction to Digital Image Processing" Prentice-Hall.
		// Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
		// This version uses a circular local window, instead of a rectagular one

		ImagePlus Meanimp, Varimp;
		ImageProcessor ip=imp.getProcessor(), ipMean, ipVar;
		double k_value;
		int c_value=0;

		byte object;
		byte backg ;

		if (doIwhite){
			k_value=0.2;
			object =  (byte) 0xff;
			backg =   (byte) 0;
		}
		else {
			k_value= -0.2;
			object =  (byte) 0;
			backg =  (byte) 0xff;
		}

		if (par1!=0) {
			IJ.log("Niblack: changed k_value from :"+ k_value + "  to:" + par1);
			k_value= par1;
		}

		if (par2!=0) {
			IJ.log("Niblack: changed c_value from :"+ c_value + "  to:" + par2);// requested feature, not in original
			c_value=(int)par2;
		}

		Meanimp=duplicateImage(ip);
		ImageConverter ic = new ImageConverter(Meanimp);
		ic.convertToGray32();

		ipMean=Meanimp.getProcessor();
		RankFilters rf=new RankFilters();
		rf.rank(ipMean, radius, rf.MEAN);// Mean
		//Meanimp.show();
		Varimp=duplicateImage(ip);
		ic = new ImageConverter(Varimp);
		ic.convertToGray32();
		ipVar=Varimp.getProcessor();
		rf.rank(ipVar, radius, rf.VARIANCE); //Variance
		//Varimp.show();
		byte[] pixels = (byte []) ip.getPixels();
		float[] mean = (float []) ipMean.getPixels();
		float[] var = (float []) ipVar.getPixels();

		for (int i=0; i<pixels.length; i++) 
			pixels[i] = ( (int)(pixels[i] &0xff) > (int)( mean[i] + k_value * Math.sqrt ( var[i] ) - c_value)) ? object : backg;
		//imp.updateAndDraw();
		return;
	}
	
	private ImagePlus duplicateImage(ImageProcessor iProcessor){
		int w=iProcessor.getWidth();
		int h=iProcessor.getHeight();
		ImagePlus iPlus=NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
		ImageProcessor imageProcessor=iPlus.getProcessor();
		imageProcessor.copyBits(iProcessor, 0,0, Blitter.COPY);
		return iPlus;
	} 
}

class Connect {
	private ImageProcessor niblack; 
	private ImageProcessor otsu;
	private int loopMax = 200;
	
	private int[][] marker;
	private final int[][] direction = { { 1, 0 }, { 0, -1 }, { 0, 1 }, { -1, 0 } };
	private int[] Ai; 
	private int[] Bi; 
	private int width;
	private int height;
	private int loopControl; // Control the time of the recursion
	
	public Connect(ImageProcessor niblack, ImageProcessor otsu) {
		this.niblack = niblack;
		this.otsu = otsu;
		width = niblack.getWidth();
		height = niblack.getHeight();
		marker = new int[height][width];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (niblack.getPixel(col, row) == 255) {	// convert Niblack to (0, 1) format
					marker[row][col] = 1;
				} else
					marker[row][col] = 0;
			}
		}
	}

	void ratioAlgorithm(double ratio) {
		marker();
		Bi = new int[Ai.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (otsu.get(x, y) == 255) {
					Bi[marker[y][x]]++;
				}
			}
		}
		int cdValue = 0;
		int markValue = 0; 
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				cdValue = Ai[marker[y][x]];
				markValue = Bi[marker[y][x]];
				if (niblack.get(x, y) == 0)
					continue;
				if (cdValue == 0 || markValue == 0
						|| (double) markValue / cdValue < ratio) {
					niblack.set(x, y, 0);
				}
			}
		}
	}
	
	private void marker() {
		int label = 2;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				loopControl = 0;
				if (DFS(x, y, label)) {
					label++;
				}
			}
		}
		Ai = new int[label+1]; 
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				++Ai[marker[y][x]]; 
			}
		}
	}

	private boolean DFS(int x, int y, int label) {
		if (++loopControl > loopMax) {
			return false;
		}
		if (1 != marker[y][x]) {
			return false; 
		} else {
			marker[y][x] = label;
			for (int i = 0; i < 4; i++) {
				int xi = x + direction[i][0],  yi = y + direction[i][1];
				if (check(xi, yi)) {
					DFS(xi, yi, label);
				}
			}
		}
		return true;
	}
	
	private boolean check(int x, int y) {
		if (x >= 0 && x < width && y >= 0 && y < height && marker[y][x] == 1) {
			return true;
		} else {
			return false;
		}
	}
}