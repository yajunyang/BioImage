package yang.plugin.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * This {@link threshold} method is a combination of NiBlack threshold method
 * and Otsu threshold method with the relationship Connected-Domain.
 * 
 * @author yang 2015/6/19 ~ 2015/6/23
 */
public class AComBinary implements PlugIn {

	ImageProcessor ip;
	static String dim = "5";
	static String k = "0.25";
	static String divation = "0.0";
	static String otsuError = "30";

	static String maxConnectedDomain = "200"; // 防止递归栈溢出设置的最大连通域点数
	static String minConnectedDomain = "50"; // 允许的最小连通域点数

	static String registerError = "50"; // otsu 图像和 niblack 建立“匹配规则”用的点数
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

		// NOTE: The current image 'ip' changed!
		niBlackThreshold(Integer.parseInt(dim), Double.parseDouble(k));

		ImagePlus nibleckIps = new ImagePlus("niblack" + dim + " " + k + " "
				+ divation, ip.duplicate());
		nibleckIps.show();

		niBlackCorrection(imp.getProcessor(), ip);

		IJ.showProgress(1.0);
		ImagePlus ips = new ImagePlus("NiBlackCorrection ", ip);
		ips.show();
	}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("NiBlack");
		gd.addStringField("kernel size: ", dim);
		gd.addStringField("k value: ", k);
		gd.addStringField("Deviation: ", divation);
		gd.addStringField("OtsuError: ", otsuError);
		gd.addStringField("MaxConnectedDomain: ", maxConnectedDomain);
		gd.addStringField("MinConnectedDomain: ", minConnectedDomain);
		gd.addStringField("Register Number: ", registerError);
		gd.addCheckbox("  Auto(2*Min = Reg = R)", autoRegister);
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
	 * <h1>NiBlack threshold.</h1> <br/>
	 * This method will process {@link ip} with the NiBlack threshold method.
	 * 
	 * @param dims
	 *            The local kernel dimension
	 * @param k
	 *            The coefficient of {@link s} <br/>
	 *            T = m = k * s
	 */
	private void niBlackThreshold(int dims, double k) {
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

	private double getThreshNiblack(ImageProcessor ip, int[][] kernel,
			int dimen, int x, int y) {
		for (int j = 0; j < dimen; j++) {
			for (int i = 0; i < dimen; i++) {
				kernel[j][i] = ip
						.getPixel(x + i - dimen / 2, y + j - dimen / 2);
			}
		}
		niBlack.setPixelsArray(kernel, dimen);
		return niBlack.getThreshold();
	}

	private void niBlackCorrection(ImageProcessor cip, ImageProcessor nbip) {
		int error = Integer.parseInt(otsuError);
		ConnectedDomain domain = new ConnectedDomain(cip, nbip);
		domain.init();
		domain.threshold(error);
//		domain.connect();
//		domain.connectVariableRatio(0.6); // 可变阈值比值法
		domain.histogramBasedMethod();  // 基于直方图的全局阈值方法
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
	 * Once this method called, the value of the new local threshold will be
	 * updated.
	 * 
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
	 * of the dimen x dimen local {@link pixels}. <h1>T = mean + k * var</h1> T
	 * is the local threshold value.
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

	private ImageProcessor nbip; // The image need to be processed
	private ImageProcessor cdip;
	private int[][] marker;
	private final int[][] direction = { { 1, 0 }, { 0, -1 }, { 0, 1 },
			{ -1, 0 } };

	private int threshold;
	private int num;
	private int[] cDNum; // NiBlack标号连通域的长度
	private int[] mark; // NiBlack连通域与Otsu对应位置匹配后的匹配点数

	private int loopControl; // Control the time of the recursion

	private int loopMax = 200;
	private int registerError = 50; // The 'registration' number of
									// Connected-Domain with the template image
									// 'Otsu'
	private int minConnectedDomain = 50;; // The smallest allowed length of
											// Connected-Domain

	private int width;
	private int height;

	private enum THRESHOLD {
		OTSU, EXPECT, ITERATIVE,YEN,SHANBHAG, ISO_DATA, KITTLER_MIN_ERROR, MAX_ENTROPY
	};

	/**
	 * We use the image {@link cip} to make an Otsu threshold temp late. the
	 * {@link nbip} image will compare with the Otsu threshold image by the
	 * connected-domain method.
	 * <p>
	 * <br>
	 * ConnectedCompare c = new ConnectedCompare(source, niBlack); <br>
	 * c.init(); <br>
	 * c.threshold(); <br>
	 * c.connect();
	 * <p>
	 * <h1>Finally, the reference parameter {@link nbip } will be changed and
	 * will be the final result.</h1>
	 * <p>
	 * 
	 * @param cip
	 *            The source image
	 * @param nbip
	 *            The image processed with NiBlack method
	 */
	ConnectedDomain(ImageProcessor cip, ImageProcessor nbip) {
		this.nbip = nbip;
		cdip = cip.duplicate();
		width = nbip.getWidth();
		height = nbip.getHeight();

		marker = new int[height][width];
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (nbip.getPixel(col, row) == 255) {
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
	 * @deprecated You must invoke threshold() before this method called.
	 */
	void connect() {
		marker();
		mark = new int[cDNum.length];

		// 在设置“Otsu落在连通域上点数”值时，建立了一种方法。
		// "自动点数生成方法"
		// 对数组cdNum求均值和方差
		// 那么“点数” = mean + a * var
		if (AComBinary.autoRegister)
			autoRegister(Double.parseDouble(AComBinary.a));

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				// NOTE: We assume that the number of the marked points with the
				// same number represents the length of the correspondent
				// Connected-Domain.
				// Only if the minimum length of the Connected-Domain bigger
				// than
				// an allowed length and the correspondent position's value in
				// the "Otsu template image"'s 4 or 8 neighbored
				// is not all 0, then "Connected-Domain will be stayed".
				if (nbip.getPixel(x, y) == 0) // INGORE　THE CONDITION THAT PIXEL
												// VALUE ZERO
					continue; // 不考虑像素为0的情况

				if (cDNum[marker[y][x]] < minConnectedDomain) { // Clear the
																// Connected-Domain
																// that is too
																// short
					nbip.set(x, y, 0); // 如果该点所落在的连通域长度小于允许的最小连通域长度，舍弃该连通域。
				}

				if (!AComBinary.test) {
					// NOTE: We set a 'mark' array which has the same length
					// with the number of 'label+1' and
					// 'label' [2 ~ label] represents the Connected-Domain
					// marker value.
					// If the Otsu template image pixel value is 255, the
					// correspondence Connected-Domain
					// will be stayed, we mark the 'mark' array 1 at the
					// correspondence position.
					if (cdip.get(x, y) == 255) {
						mark[marker[y][x]]++; // 统计NiBlack相应连通域位置对应Otsu图像上位置匹配点数（Otsu上对应位置值为255）
					}
				}
			}
		}

		if (!AComBinary.test) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (nbip.get(x, y) == 0)
						continue;
					if (mark[marker[y][x]] < registerError && marker[y][x] != 0
							&& marker[y][x] != 1) {
						nbip.set(x, y, 0);
					}
				}
			}
		}
	}

	/**
	 * 比值法
	 * 
	 * @param ratio
	 *            比率
	 */
	void connectVariableRatio(double ratio) {
		marker();
		mark = new int[cDNum.length];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (cdip.get(x, y) == 255) {
					mark[marker[y][x]]++;
				}
			}
		}

		int cdValue = 0;
		int markValue = 0; // 当前像素位置所在连通域对应Otsu“裁判”的长度

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				cdValue = cDNum[marker[y][x]];
				markValue = mark[marker[y][x]];
				if (nbip.get(x, y) == 0)
					continue;
				if (cdValue == 0 || markValue == 0
						|| (double) markValue / cdValue < ratio) {
					nbip.set(x, y, 0);
				}
			}
		}
	}

	/**
	 * 基于直方图的连通域阈值分割方法
	 */
	void histogramBasedMethod() {
		marker();
		mark = new int[cDNum.length];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (cdip.get(x, y) == 255) {
					mark[marker[y][x]]++;
				}
			}
		}

		int T = getConnectThreshold(THRESHOLD.ITERATIVE);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// marker[y][x] 对应标记结果
				// mark[marker[y][x]]Otsu落在相应连通域上的亮点数
				int markValue = mark[marker[y][x]]; 
				if (nbip.get(x, y) == 0)
					continue;
				if (markValue <= T) 
					nbip.set(x, y, 0);
			}
		}
	}

	/**
	 * <b>Get the connected-domain threshold.</b>
	 * <p>
	 * Here we regard the marker value(>=2) as <b>the pixel position</b> in an
	 * image and the value of the connected-domain length(0~loopNax) as <b>the
	 * pixel value</b> in a gray image(0~255). <br/>
	 * We want to get the best value of the Connected-Domain threshold just like
	 * <b>the threshold value of a gray image.</b> <br/>
	 * So. Here is a problem of <b>threshold of histogram</b>
	 * 
	 * @return
	 */
	int getConnectThreshold(THRESHOLD c) {
		final int[] histogram = new int[loopMax + 1]; // 下标为连通域长度，值为对应连通域的个数
		Arrays.fill(histogram, 0);
		for (int i = 2; i < mark.length; i++) {
			histogram[mark[i]]++; 
			/**			// NOTE
			 * 这里mark[i] == 0 的太多了
			 * 因为Otsu对应NiBlack连通域的统计点数为0的太多了 
			 */
		}
		saveArray("Histogram "+c, histogram);
		int threshold = 0;
		switch (c) {
		case EXPECT:
			threshold = getExpectThreshold(histogram);
			break;
		case OTSU:
			threshold = getOSTUThreshold(histogram);
			break;
		case ITERATIVE:
			threshold = getIterativeBestThreshold(histogram);
			break;
		case YEN:
			threshold = getYenThreshold(histogram);
			break;
		case ISO_DATA:
			threshold = getIsoDataThreshold(histogram);
			break;
		case SHANBHAG:
			threshold = getShanbhagThreshold(histogram);
			break;
		case KITTLER_MIN_ERROR:
			threshold = getKittlerMinError(histogram);
			break;
		case MAX_ENTROPY:
			threshold = get1DMaxEntropyThreshold(histogram);
			break;
		}
		return threshold;
	}

	/**
	 * @see http://blog.csdn.net/pi9nc/article/details/11537417
	 * @param histogram
	 * @return
	 */
	@Deprecated
	private int getExpectThreshold(int[] histogram) {
		double sum = 0;
		int length = histogram.length;
		for (int i = 0; i < length; i++) {
			sum += histogram[i];
		}

		final double[] pdf = new double[loopMax + 1];
		Arrays.fill(pdf, 0);

		double expection = 0;
		for (int i = 2; i < length; i++) {
			pdf[i] = (double) histogram[i] / sum;
			expection += pdf[i] * histogram[i];
		}

		saveArray("histogram", pdf);
		return (int) (expection + 0.5);
	}

	private int getIterativeBestThreshold(int[] HistGram) {
		int X, Iter = 0;
		int MeanValueOne, MeanValueTwo, SumOne, SumTwo, SumIntegralOne, SumIntegralTwo;
		int MinValue, MaxValue;
		int Threshold, NewThreshold;

		for (MinValue = 0; MinValue <= loopMax && HistGram[MinValue] == 0; MinValue++);
		for (MaxValue = loopMax; MaxValue > MinValue && HistGram[MinValue] == 0; MaxValue--);

		if (MaxValue == MinValue)
			return MaxValue;
		if (MinValue + 1 == MaxValue)
			return MinValue;

		Threshold = MinValue;
		NewThreshold = (MaxValue + MinValue) >> 1;
		while (Threshold != NewThreshold) {
			SumOne = 0;
			SumIntegralOne = 0;
			SumTwo = 0;
			SumIntegralTwo = 0;
			Threshold = NewThreshold;
			for (X = MinValue; X <= Threshold; X++) {
				SumIntegralOne += HistGram[X] * X;
				SumOne += HistGram[X];
			}
			MeanValueOne = SumIntegralOne / SumOne;
			for (X = Threshold + 1; X <= MaxValue; X++) {
				SumIntegralTwo += HistGram[X] * X;
				SumTwo += HistGram[X];
			}
			MeanValueTwo = SumIntegralTwo / SumTwo;
			NewThreshold = (MeanValueOne + MeanValueTwo) >> 1; 
			Iter++;
			if (Iter >= 1000)
				return -1;
		}
		return Threshold;
	}

	private int getOSTUThreshold(int[] HistGram) {
		int Y, Amount = 0;
		int PixelBack = 0, PixelFore = 0, PixelIntegralBack = 0, PixelIntegralFore = 0, PixelIntegral = 0;
		double OmegaBack, OmegaFore, MicroBack, MicroFore, SigmaB, Sigma;
		int MinValue, MaxValue;
		int Threshold = 0;

		for (MinValue = 0; MinValue <= loopMax && HistGram[MinValue] == 0; MinValue++);
		for (MaxValue = loopMax; MaxValue > MinValue && HistGram[MinValue] == 0; MaxValue--);
		if (MaxValue == MinValue)
			return MaxValue;
		if (MinValue + 1 == MaxValue)
			return MinValue;

		for (Y = MinValue; Y <= MaxValue; Y++)
			Amount += HistGram[Y];

		PixelIntegral = 0;
		for (Y = MinValue; Y <= MaxValue; Y++)
			PixelIntegral += HistGram[Y] * Y;
		SigmaB = -1;
		for (Y = MinValue; Y < MaxValue; Y++) {
			PixelBack = PixelBack + HistGram[Y];
			PixelFore = Amount - PixelBack;
			OmegaBack = (double) PixelBack / Amount;
			OmegaFore = (double) PixelFore / Amount;
			PixelIntegralBack += HistGram[Y] * Y;
			PixelIntegralFore = PixelIntegral - PixelIntegralBack;
			MicroBack = (double) PixelIntegralBack / PixelBack;
			MicroFore = (double) PixelIntegralFore / PixelFore;
			Sigma = OmegaBack * OmegaFore * (MicroBack - MicroFore) * (MicroBack - MicroFore);
			if (Sigma > SigmaB) {
				SigmaB = Sigma;
				Threshold = Y;
			}
		}
		return Threshold;
	}

	private int getYenThreshold(int[] HistGram) {
		int threshold;
		int ih, it;
		double crit;
		double max_crit;
		double[] norm_histo = new double[HistGram.length]; /*
															 * normalized
															 * histogram
															 */
		double[] P1 = new double[HistGram.length]; /*
													 * cumulative normalized
													 * histogram
													 */
		double[] P1_sq = new double[HistGram.length];
		double[] P2_sq = new double[HistGram.length];

		int total = 0;
		for (ih = 0; ih < HistGram.length; ih++)
			total += HistGram[ih];

		for (ih = 0; ih < HistGram.length; ih++)
			norm_histo[ih] = (double) HistGram[ih] / total;

		P1[0] = norm_histo[0];
		for (ih = 1; ih < HistGram.length; ih++)
			P1[ih] = P1[ih - 1] + norm_histo[ih];

		P1_sq[0] = norm_histo[0] * norm_histo[0];
		for (ih = 1; ih < HistGram.length; ih++)
			P1_sq[ih] = P1_sq[ih - 1] + norm_histo[ih] * norm_histo[ih];

		P2_sq[HistGram.length - 1] = 0.0;
		for (ih = HistGram.length - 2; ih >= 0; ih--)
			P2_sq[ih] = P2_sq[ih + 1] + norm_histo[ih + 1] * norm_histo[ih + 1];

		/* Find the threshold that maximizes the criterion */
		threshold = -1;
		max_crit = Double.MIN_VALUE;
		for (it = 0; it < HistGram.length; it++) {
			crit = -1.0 * ((P1_sq[it] * P2_sq[it]) > 0.0 ? Math.log(P1_sq[it] * P2_sq[it]) : 0.0) + 2
					* ((P1[it] * (1.0 - P1[it])) > 0.0 ? Math.log(P1[it]
							* (1.0 - P1[it])) : 0.0);
			if (crit > max_crit) {
				max_crit = crit;
				threshold = it;
			}
		}
		return threshold;
	}
	
	@Deprecated
	private int getShanbhagThreshold(int[] HistGram) {
		int threshold;
		int ih, it;
		int first_bin;
		int last_bin;
		double term;
		double tot_ent; /* total entropy */
		double min_ent; /* max entropy */
		double ent_back; /* entropy of the background pixels at a given threshold */
		double ent_obj; /* entropy of the object pixels at a given threshold */
		double[] norm_histo = new double[HistGram.length]; /*
															 * normalized
															 * histogram
															 */
		double[] P1 = new double[HistGram.length]; /*
													 * cumulative normalized
													 * histogram
													 */
		double[] P2 = new double[HistGram.length];

		int total = 0;
		for (ih = 0; ih < HistGram.length; ih++)
			total += HistGram[ih];

		for (ih = 0; ih < HistGram.length; ih++)
			norm_histo[ih] = (double) HistGram[ih] / total;

		P1[0] = norm_histo[0];
		P2[0] = 1.0 - P1[0];
		for (ih = 1; ih < HistGram.length; ih++) {
			P1[ih] = P1[ih - 1] + norm_histo[ih];
			P2[ih] = 1.0 - P1[ih];
		}

		/* Determine the first non-zero bin */
		first_bin = 0;
		for (ih = 0; ih < HistGram.length; ih++) {
			if (!(Math.abs(P1[ih]) < 2.220446049250313E-16)) {
				first_bin = ih;
				break;
			}
		}

		/* Determine the last non-zero bin */
		last_bin = HistGram.length - 1;
		for (ih = HistGram.length - 1; ih >= first_bin; ih--) {
			if (!(Math.abs(P2[ih]) < 2.220446049250313E-16)) {
				last_bin = ih;
				break;
			}
		}

		// Calculate the total entropy each gray-level
		// and find the threshold that maximizes it
		threshold = -1;
		min_ent = Double.MAX_VALUE;

		for (it = first_bin; it <= last_bin; it++) {
			/* Entropy of the background pixels */
			ent_back = 0.0;
			term = 0.5 / P1[it];
			for (ih = 1; ih <= it; ih++) { // 0+1?
				ent_back -= norm_histo[ih] * Math.log(1.0 - term * P1[ih - 1]);
			}
			ent_back *= term;

			/* Entropy of the object pixels */
			ent_obj = 0.0;
			term = 0.5 / P2[it];
			for (ih = it + 1; ih < HistGram.length; ih++) {
				ent_obj -= norm_histo[ih] * Math.log(1.0 - term * P2[ih]);
			}
			ent_obj *= term;

			/* Total entropy */
			tot_ent = Math.abs(ent_back - ent_obj);

			if (tot_ent < min_ent) {
				min_ent = tot_ent;
				threshold = it;
			}
		}
		return threshold;
	}

	private int getIsoDataThreshold(int[] HistGram) {
		int i, l, toth, totl, h, g = 0;
		for (i = 1; i < HistGram.length; i++) {
			if (HistGram[i] > 0) {
				g = i + 1;
				break;
			}
		}
		while (true) {
			l = 0;
			totl = 0;
			for (i = 0; i < g; i++) {
				totl = totl + HistGram[i];
				l = l + (HistGram[i] * i);
			}
			h = 0;
			toth = 0;
			for (i = g + 1; i < HistGram.length; i++) {
				toth += HistGram[i];
				h += (HistGram[i] * i);
			}
			if (totl > 0 && toth > 0) {
				l /= totl;
				h /= toth;
				if (g == (int) Math.round((l + h) / 2.0))
					break;
			}
			g++;
			if (g > HistGram.length - 2) {
				return 0;
			}
		}
		return g;
	}
	
	private int getKittlerMinError(int[] HistGram) {
		int X, Y;
		int MinValue, MaxValue;
		int Threshold;
		int PixelBack, PixelFore;
		double OmegaBack, OmegaFore, MinSigma, Sigma, SigmaBack, SigmaFore;
		for (MinValue = 0; MinValue <= loopMax && HistGram[MinValue] == 0; MinValue++);
		for (MaxValue = loopMax; MaxValue > MinValue && HistGram[MinValue] == 0; MaxValue--);
		if (MaxValue == MinValue)
			return MaxValue; // 图像中只有一个颜色
		if (MinValue + 1 == MaxValue)
			return MinValue; // 图像中只有二个颜色
		Threshold = -1;
		MinSigma = 1E+20;
		for (Y = MinValue; Y < MaxValue; Y++) {
			PixelBack = 0;
			PixelFore = 0;
			OmegaBack = 0;
			OmegaFore = 0;
			for (X = MinValue; X <= Y; X++) {
				PixelBack += HistGram[X];
				OmegaBack = OmegaBack + X * HistGram[X];
			}
			for (X = Y + 1; X <= MaxValue; X++) {
				PixelFore += HistGram[X];
				OmegaFore = OmegaFore + X * HistGram[X];
			}
			OmegaBack = OmegaBack / PixelBack;
			OmegaFore = OmegaFore / PixelFore;
			SigmaBack = 0;
			SigmaFore = 0;
			for (X = MinValue; X <= Y; X++)
				SigmaBack = SigmaBack + (X - OmegaBack) * (X - OmegaBack)
						* HistGram[X];
			for (X = Y + 1; X <= MaxValue; X++)
				SigmaFore = SigmaFore + (X - OmegaFore) * (X - OmegaFore)
						* HistGram[X];
			if (SigmaBack == 0 || SigmaFore == 0) {
				if (Threshold == -1)
					Threshold = Y;
			} else {
				SigmaBack = Math.sqrt(SigmaBack / PixelBack);
				SigmaFore = Math.sqrt(SigmaFore / PixelFore);
				Sigma = 1 + 2 * (PixelBack * Math.log(SigmaBack / PixelBack) + PixelFore
						* Math.log(SigmaFore / PixelFore));
				if (Sigma < MinSigma) {
					MinSigma = Sigma;
					Threshold = Y;
				}
			}
		}
		return Threshold;
	}

	private int get1DMaxEntropyThreshold(int[] HistGram) {
		int X, Y, Amount = 0;
		double[] HistGramD = new double[loopMax+1];
		double SumIntegral, EntropyBack, EntropyFore, MaxEntropy;
		int MinValue = loopMax+1, MaxValue = 0;
		int Threshold = 0;

		for (MinValue = 0; MinValue <= loopMax && HistGram[MinValue] == 0; MinValue++);
		for (MaxValue = loopMax; MaxValue > MinValue && HistGram[MinValue] == 0; MaxValue--);
		if (MaxValue == MinValue)
			return MaxValue; // 图像中只有一个颜色
		if (MinValue + 1 == MaxValue)
			return MinValue; // 图像中只有二个颜色

		for (Y = MinValue; Y <= MaxValue; Y++)
			Amount += HistGram[Y]; // 像素总数

		for (Y = MinValue; Y <= MaxValue; Y++)
			HistGramD[Y] = (double) HistGram[Y] / Amount + 1e-17;

		MaxEntropy = Double.MIN_VALUE;
		;
		for (Y = MinValue + 1; Y < MaxValue; Y++) {
			SumIntegral = 0;
			for (X = MinValue; X <= Y; X++)
				SumIntegral += HistGramD[X];
			EntropyBack = 0;
			for (X = MinValue; X <= Y; X++)
				EntropyBack += (-HistGramD[X] / SumIntegral * Math.log(HistGramD[X] / SumIntegral));
			EntropyFore = 0;
			for (X = Y + 1; X <= MaxValue; X++)
				EntropyFore += (-HistGramD[X] / (1 - SumIntegral) * Math.log(HistGramD[X] / (1 - SumIntegral)));
			if (MaxEntropy < EntropyBack + EntropyFore) {
				Threshold = Y;
				MaxEntropy = EntropyBack + EntropyFore;
			}
		}
		return Threshold;
	}

	
	
	
	public void saveArray(String title, Object data) {
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		path = path.substring(0, path.length() - 1) + "res/" + title + ".txt";

		if(!data.getClass().isArray()) 
			return;
		
		File file = new File(path);
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		FileWriter fw;
		try {
			fw = new FileWriter(file);
			fw.write("# This is the list of histogram data!\r\n");
			fw.write("# Number\r\n");
			if(data.getClass() == int[].class){
				int[] intData = (int[])data; 
				for (int i = 0; i < intData.length; i++) {
					fw.write(i + "\t" + intData[i] + "\r\n");
				}
				fw.close();
			}else if(data.getClass() == double[].class){
				double[] doubleData = (double[])data; 
				for (int i = 0; i < doubleData.length; i++) {
					fw.write(i + "\t" + doubleData[i] + "\r\n");
				}
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Deprecated
	public void saveHistogramDataToTxt() {
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		path = path.substring(0, path.length() - 1) + "res/data.txt";

		File file = new File(path);
		if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		FileWriter fw;
		try {
			fw = new FileWriter(file);
			fw.write("# This is the list of histogram data!\r\n");
			fw.write("# Number\tcdNum\tmark\r\n");
			for (int i = 2; i < cDNum.length; i++) {
				fw.write(i + "\t" + cDNum[i] + "\t" + mark[i] + "\r\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
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

		// NOTE: 0 1 Start->2 3 4 5 6 7 8 9<-End
		// Assuming the max value of label is 9, and 0 1 useless
		// We want the subscript of the array 'cdNum' equal to the value of
		// label
		// SO the length of the array 'cdNum' should be label+1 == 10
		num = label + 1;

		// statistics the length of connected-domain
		cDNum = new int[num]; // all be zero in default condition
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				++cDNum[marker[y][x]]; // statistics of the number of
										// correspondent marker
										// and the subscript representing the
										// labeling value
			}
		}
	}

	/** Depth-First Search */
	private boolean DFS(int x, int y, int label) {
		if (++loopControl > loopMax) {
			return false;
		}
		if (1 != marker[y][x]) {
			return false; // / only the number ONE to be marked
		} else {
			marker[y][x] = label;
			for (int i = 0; i < 4; i++) {
				if (check(x + direction[i][0], y + direction[i][1])) {
					DFS(x + direction[i][0], y + direction[i][1], label);
				}
			}
		}
		return true;
	}

	@Deprecated
	private void autoRegister(double a) {
		double mean = 0;
		double var = 0;
		for (int i = 0; i < cDNum.length; i++) {
			mean += cDNum[i];
		}
		mean /= cDNum.length; // 均值

		for (int i = 0; i < cDNum.length; i++) {
			var += (cDNum[i] - mean) * (cDNum[i] - mean);
		}
		var /= cDNum.length;
		var = Math.sqrt(var); // 方差
		registerError = minConnectedDomain = (int) (mean + var * a);
	}

	private boolean check(int x, int y) {
		if (x >= 0 && x < width && y >= 0 && y < height && marker[y][x] == 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method will make the {@link cdip } (current duplicate imageprocessor)
	 * becoming Otsu image template.
	 */
	void threshold(int error) {
		this.threshold = getOtsuThreshold(cdip, error);
	}

	/** Return the Otsu threshold value */
	int getThresholdValue() {
		return threshold;
	}

	private int getOtsuThreshold(ImageProcessor ip, int error) {
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
