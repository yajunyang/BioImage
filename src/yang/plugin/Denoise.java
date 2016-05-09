package yang.plugin;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.filter.Convolver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import imagescience.feature.Differentiator;
import imagescience.image.Aspects;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;

public class Denoise implements PlugIn{

	double sigma;
	double scale;
	double T;
	double spatialRadius;
	double rangeRadius;
	
	@Override
	public void run(String arg) {
		ImagePlus ips = WindowManager.getCurrentImage();
		if(null == ips || ips.getType() != ImagePlus.GRAY8) {
			return;
		}
		
		if(!showDialog()){
			return;
		}
		
		NLMeansDenoising nlMeansDenoising = new NLMeansDenoising();
		PDE_Filter pde_Filter = new PDE_Filter();
		Bilateral_Filter bilateral_Filter = new Bilateral_Filter();
		
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = wb.createSheet("Data");
		Row row0 = sheet.createRow(0);
		// create first row 
		for(int i=0; i<=3; i++) {
			Cell cell = row0.createCell(i);
			cell.setCellValue(3 + i*5);
		}
		
		for(int i=1; i<=9; i++) {
			 Row row = sheet.createRow(i);
			 for(int j=0; j<=4; j++) {
				 Cell cell = row.createCell(j);
				 cell.setCellType(Cell.CELL_TYPE_STRING);
			 }
		}
		
		for (double s=3; s<20; s+= 5) {
			int r = 1, c = (int)(s-3)/5 ;
			ImageProcessor ip = ips.getProcessor();
			ImageProcessor noiseIp = ip.duplicate();
			noiseIp.noise(s);
			
			ImageProcessor nlm = noiseIp.duplicate();
			nlMeansDenoising.applyNonLocalMeans(nlm, (int)sigma);
			ImageProcessor pde = pde_Filter.filter(new ImagePlus("PDE", noiseIp.duplicate()), scale, (int)T);
			ImageProcessor bil = bilateral_Filter.filter(new ImagePlus("Bil", noiseIp.duplicate()), spatialRadius, rangeRadius);
			
			String[] pdeS = getRemark(ip, pde);
			String[] nlmS = getRemark(ip, nlm);
			String[] bilS = getRemark(ip, bil);
			
			if(s == 3) {
				new ImagePlus("noise", noiseIp).show();
				new ImagePlus("nlm", nlm).show();
				new ImagePlus("pde", pde).show();
				new ImagePlus("bil", bil).show();
			}
			
			// save data	
			for(int i=0; i <3; r++, i++) {
				Cell cell = sheet.getRow(r).getCell(c);
				cell.setCellValue(pdeS[i]);
			}
			
			for(int i=0; i <3; r++, i++) {
				Cell cell = sheet.getRow(r).getCell(c);
				cell.setCellValue(nlmS[i]);
			}
			
			for(int i=0; i <3; r++, i++) {
				Cell cell = sheet.getRow(r).getCell(c);
				cell.setCellValue(bilS[i]);
			}
		}
		
		sheet.getRow(1).getCell(4).setCellValue("PDE_SNR");
		sheet.getRow(2).getCell(4).setCellValue("PDE_PSNR");
		sheet.getRow(3).getCell(4).setCellValue("PDE_RMSE");
		
		sheet.getRow(4).getCell(4).setCellValue("NLM_SNR");
		sheet.getRow(5).getCell(4).setCellValue("NLM_PSNR");
		sheet.getRow(6).getCell(4).setCellValue("NLM_RMSE");
		
		sheet.getRow(7).getCell(4).setCellValue("BIL_SNR");
		sheet.getRow(8).getCell(4).setCellValue("BIL_PSNR");
		sheet.getRow(9).getCell(4).setCellValue("BIL_RMSE");
		
		String file = ips.getTitle() + ".xls";
		
        if(wb instanceof XSSFWorkbook) 
        	file += "x";
        try {
            FileOutputStream out = new FileOutputStream(file);
            wb.write(out);
            out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        IJ.log("程序运行结束！");
	}
	
	
	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("NiBlack");
		gd.addNumericField("NL_Sigma", 10, 0);
		gd.addNumericField("PDE_Scale", 1.5, 1);
		gd.addNumericField("PDE_T", 40, 0);
		gd.addNumericField("Bil_spatialRadius", 3, 1);
		gd.addNumericField("Bil_rangeRadius", 50, 1);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		
		sigma = gd.getNextNumber();
		scale = gd.getNextNumber();
		T = gd.getNextNumber();
		spatialRadius = gd.getNextNumber();
		rangeRadius = gd.getNextNumber();
		return true;
	}
	
	/**
	 * @param src  source image 
	 * @param dst  noise image
	 * @return  [snr][psnr][rmse]
	 */
	String[] getRemark(ImageProcessor src, ImageProcessor dst) {
		int nx = src.getWidth();
		int ny = dst.getHeight();
		String row[] = new String[3];
		double maxSignal = -Double.MAX_VALUE;
		double s, t, mse = 0.0, es = 0.0, ms = 0.0; 
		int N = 0;
		for (int y=0; y<nx; y++)
			for (int x=0; x<ny; x++) {
				s =  src.getPixelValue(x, y);
				if (s > maxSignal)
					maxSignal = s;
				t = dst.getPixelValue(x, y);
				if (!Double.isNaN(t))
				if (!Double.isNaN(s)) {
					mse += (t-s)*(t-s);
					es += s*s;
					ms += s;
					N++;
				}
			}
			if (N > 0) {
				mse /= N;
				es /= N;
				ms /= N;
				if (Math.abs(mse) >= 0.00000001) {
					double snr = 10.0 * Math.log(es/mse) / Math.log(10.0);
					double psnr = 10.0 * Math.log(maxSignal*maxSignal/mse) / Math.log(10.0);
					double rmse = Math.sqrt(mse);
					DecimalFormat dfn = new DecimalFormat("##0.00");
					DecimalFormat dfs = new DecimalFormat("0.##E00");
					row[0] = (Math.abs(snr) < 0.001 ? dfs.format(snr) : dfn.format(snr));
					row[1] = (Math.abs(psnr) < 0.001 ? dfs.format(psnr) : dfn.format(psnr));
					row[2] = (Math.abs(rmse) < 0.001 ? dfs.format(rmse) : dfn.format(rmse));
				}
				else {
					row[0] = "0";
					row[1] = "0";
					row[2] = "0";
				}
			}
			else {
				row[0] = "Unvalid";
				row[1] = "Unvalid";
				row[2] = "Unvalid";
			}
			return row;
	}
	
}


/**
 * Antoni Buades, Bartomeu Coll, and Jean-Michel Morel, Non-Local Means Denoising, 
 * Image Processing On Line, vol. 2011.

including the changes proposed by

Darbon, J. et al., 2008.
 Fast nonlocal filtering applied to electron cryomicroscopy. 
 In 2008 5th IEEE International Symposium on Biomedical Imaging: From Nano to Macro, 
 Proceedings, ISBI. IEEE, pp. 1331–1334.
 *
 */
class NLMeansDenoising {

	private final int weightPrecision = 1000; // Precision of the used Weight.
												// Values > 1000 can cause
												// Overflow
	private ImagePlus imp; // ImagePlus representation of the Image
	private int width; // Width of the Image
	private int height; // Height of the Image

	private int[][] pixels; // Pixels of the Image. First Dimension is for
							// Colour-Channel, second for Pixels. Pixels are
							// arranged in width * height
	// private int[][] pixelsExpand; // Pixels of the expanded Version of the
	// Image. This Version is needed to prevent Walking out of Bounds.
	private int widthE; // Width of the expanded Image
	private int heightE; // Height of the expanded Image

	private int w; // Big Search-Window
	private int n; // Small Search-Window used for Patches

	private double sigma2; // Variance of the Image-Noise
	private double h2; // Smoothing-Parameter
	private int distConst; // Constant Value that the Distance gets Multiplied
							// with. Currently unused.
	private int dim; // Dimension of the Image. (1 = Grayscale, 3 = RGB)

	private int nextdx; // Variable used to store the next Value for dx.
	private int nextdy; // Variable used to store the next Value for dy.

	private long[][] uL; // Long Representation of the Denoising Image
	private long[] wMaxArrL; // Max-Weight that was used for each Pixel. (Long
								// Representation)
	private long[] wSumArrL;// Sum of all Weights per Pixel (Long
							// Representation)

	private boolean autoEstimate = false; // Use Auto-Estimation to determine
											// Image-Noise
	private int constantSigma = 15; // Standard-Value for Sigma
	private int smoothingFactor = 1;
	private int[] usedSigmas; // Saves the used sigmas when processing stacks..

	/**
	 * 
	 * @param ip
	 *            Image which should be denoised
	 * @param sigma
	 *            Estimated standard deviation of noise
	 * @param imageType
	 *            Type of image (e.g. ImagePlus.Gray8 etc.)
	 */
	public void applyNonLocalMeans(ImageProcessor ip, int sigma) {
		initSettings(sigma, ip);

		try {
			int width = 512;
			int height = 512;
			double[][] result = NLMeansDenoising(ip, width, height);
			createPicture(result, ip);
		} catch (InterruptedException e) {
			e.printStackTrace();
			// IJ.showMessage("Error while computing Denoised Image.");
		}
	}

	private double[][] NLMeansDenoising(ImageProcessor ip, int windowWidth,
			int windowHeight) throws InterruptedException {

		double[][] result = new double[dim][ip.getWidth() * ip.getHeight()];

		for (int ys = 0; ys < ip.getHeight(); ys += windowHeight) {
			for (int xs = 0; xs < ip.getWidth(); xs += windowWidth) {
				int imagePartWidth = (windowWidth + xs > ip.getWidth())
						? windowWidth - ((windowWidth + xs) - ip.getWidth())
						: windowWidth;
				int imagePartHeight = (windowHeight + ys > ip.getHeight())
						? windowHeight - ((windowHeight + ys) - ip.getHeight())
						: windowHeight;
				int[][] imagePartE = expandImage(pixels, xs, ys, imagePartWidth,
						imagePartHeight, ip.getWidth(), ip.getHeight(), false);

				// double[][] partResult =
				// NLMeansMultithreadInstance(imagePartE,
				// Runtime.getRuntime().availableProcessors(), imagePartWidth,
				// imagePartHeight);
				double[][] partResult = NLMeansMultithreadInstance(imagePartE,
						Runtime.getRuntime().availableProcessors(),
						imagePartWidth, imagePartHeight);

				// save Partial Result in Image
				nextdx = -w;
				nextdy = -w;
				int ystart = ys;
				int xstart = xs;
				for (int y = ystart; y < ystart + imagePartHeight; y++) {
					// if (y >= ip.getHeight()) continue;
					for (int x = xstart; x < xstart + imagePartWidth; x++) {
						// if (x >= ip.getWidth()) continue;
						for (int d = 0; d < dim; d++) {
							result[d][y * ip.getWidth()
									+ x] = partResult[d][(y - ystart)
											* imagePartWidth + x - xstart];
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Multi Threaded Implementation of the Non-local Means Algorithm.
	 * "Fast nonlocal filtering applied to electron cryomicroscopy." Biomedical
	 * Imaging: From Nano to Macro, 2008. ISBI 2008. 5th IEEE International
	 * Symposium on. IEEE, 2008.
	 * 
	 * @param image
	 *            The image as Integer Array. Colors are stored within first
	 *            dimension of Array. Gets computed via convertImage()
	 * @param threadcount
	 *            Number of Threads used for Denoising
	 * @param ip
	 *            ImageProcessor for the original Image
	 * @throws InterruptedException
	 */
	private double[][] NLMeansMultithreadInstance(int[][] image,
			int threadcount, int width, int height)
			throws InterruptedException {
		int widthE = width + 2 * w + 2 * n;
		int heightE = height + 2 * w + 2 * n;
		long[][] u = new long[dim][widthE * heightE];
		long[] wMaxArr = new long[widthE * heightE];
		long[] wSumArr = new long[widthE * heightE];

		List<Worker> workerList = new ArrayList<Worker>(threadcount);
		for (int i = 0; i < threadcount; i++) {
			Worker worker = new Worker(width, height, image, u, wMaxArr,
					wSumArr);
			worker.start();
			workerList.add(worker);
		}
		for (Worker worker : workerList) {
			worker.join();
		}

		return finishPicture(u, image, wMaxArr, wSumArr, width, height);
	}

	private synchronized void deliverImagePart(long[][] imagePart, long[][] u,
			int widthE, int heightE) {
		for (int y = 0; y < heightE; y++) {
			int offset = y * widthE;
			for (int x = 0; x < widthE; x++) {
				for (int d = 0; d < dim; d++) {
					u[d][offset + x] += imagePart[d][offset + x];
				}
			}
		}
	}

	/**
	 * This Method is used to deliver a partial result of the Weight Sum Array.
	 * The Weight Sum Array stores the sum of all Weights that are used for each
	 * pixel. It is used within finishPicture(...) to properly Scale each Pixel.
	 * 
	 * @param arr
	 *            Weight Sum Array
	 */
	private synchronized void deliverWSumArr(long[] arr, long[] wSumArr,
			int widthE, int heightE) {
		for (int y = 0; y < heightE; y++) {
			int offset = y * widthE;
			for (int x = 0; x < widthE; x++) {
				wSumArr[offset + x] += arr[offset + x];
			}
		}
	}

	/**
	 * This Method is used to deliver a partial result of the Weight Max Array.
	 * The Weight Max Array stores the maximum Weight that is used per Pixel.
	 * This Weight is used as Weight between the Pixel and itself.
	 * 
	 * @param arr
	 *            Maximum Weight Array
	 */
	private synchronized void deliverWMaxArr(long[] arr, long[] wMaxArr,
			int widthE, int heightE) {
		for (int y = 0; y < heightE; y++) {
			int offset = y * widthE;
			for (int x = 0; x < widthE; x++) {
				if (wMaxArr[offset + x] < arr[offset + x]) {
					wMaxArr[offset + x] = arr[offset + x];
				}
			}
		}
	}

	/**
	 * Finishes the Picture by dividing every Pixel with the Sum of all Weights
	 * for the respective Pixel, and by performing the last denoising step. As
	 * last Step, the Pixels get weighted with the maximum Weight for each
	 * Pixel.
	 * 
	 * @param picture
	 *            The Denoised Picture
	 * @param wMaxArr
	 *            Array with highest used Weight for each Pixel
	 * @param wSumArr
	 *            Array with Sum of Weights for each Pixel
	 * @return
	 */
	private double[][] finishPicture(long[][] picture, int[][] pixelsExpand,
			long[] wMaxArr, long[] wSumArr, int width, int height) {
		double[][] result = new double[dim][width * height];
		int wn = w + n;
		int widthE = width + 2 * wn;

		// x and y coordinates are based off the original Image (NOT the
		// expanded Image)
		for (int y = 0; y < height; y++) {
			int offset = y * width; // y offset for original Image coordinates
			int offset2 = (y + wn) * widthE; // y offset for expanded Image
												// coordinates
			for (int x = 0; x < width; x++) {
				int k = offset + x; // array Position for Pixel x, y
				int kwn = offset2 + x + wn; // same as k, but in expanded Image
											// coordinates
				for (int d = 0; d < result.length; d++) {
					result[d][k] = picture[d][kwn];

					if (wMaxArr[kwn] == 0) {
						// If Sum of all Weights is 0, just copy the original
						// Pixel
						result[d][k] += pixelsExpand[d][kwn];
					} else {
						// Weight the original Pixel with the maximum Weight
						result[d][k] += pixelsExpand[d][kwn] * wMaxArr[kwn];
						wSumArr[kwn] += wMaxArr[kwn];

						// Divide Pixel by sum of all Weights
						result[d][k] /= wSumArr[kwn];
					}
				}
			}
		}

		return result;
	}

	private void denoise(long[][] targetArr, int[][] pixelsExpand, long[][] S,
			long[] wMaxArr, long[] wSumArr, int widthE, int heightE, int dx,
			int dy) {
		int wn = w + n;
		for (int y = wn; y < heightE - wn; y++) {
			int offset = y * widthE;
			int offsetn = (y + dy) * widthE;
			for (int x = wn; x < widthE - wn; x++) {
				int k = offset + x;
				int kn = offsetn + x + dx;
				int weight = computeWeight(S, widthE, x, y, weightPrecision);
				wMaxArr[k] = Math.max(weight, wMaxArr[k]);
				wSumArr[k] += weight;
				wMaxArr[kn] = Math.max(weight, wMaxArr[kn]);
				wSumArr[kn] += weight;

				for (int d = 0; d < dim; d++) {
					int wk = weight * pixelsExpand[d][k];
					int wkn = weight * pixelsExpand[d][kn];

					targetArr[d][k] += wkn;
					targetArr[d][kn] += wk;
				}
			}
		}
	}

	/**
	 * Computes the Weight between the Pixel x,y and the Pixel that lies at x +
	 * dx, y + dy. dx and dy are implicitly given because the Difference Image
	 * is based on them.
	 * 
	 * @param S
	 *            Difference Image for a dx / dy pair
	 * @param x
	 *            x-Coordinate of the current Pixel
	 * @param y
	 *            y-Coordinate of the current Pixel
	 * @param precision
	 *            Precision of the Weight. Should be multiple of 10
	 * @return
	 */
	private int computeWeight(long[][] S, int widthE, int x, int y,
			int precision) {
		double distance = computeDistance(S, widthE, x, y);

		double exp = Math.max(distance - sigma2, 0.0);

		// exp /= h2;
		// double weight = Math.exp(-exp);
		double weight = h2 / (h2 + exp);

		// int iWeight = FastMathStuff.fastRound(weight * precision) + 1;
		// if (iWeight == 0) iWeight = 1;

		return FastMathStuff.fastRound(weight * precision);
	}

	/**
	 * Computes the Difference between the Surroundings of the Pixel x,y and the
	 * Pixel that lies at x + dx, y + dy. dx and dy are implicitly given because
	 * the Difference Image is based on them. Is used to compute the Weights.
	 * 
	 * @param S
	 *            Difference Image for a dx / dy pair
	 * @param x
	 *            x-Coordinate of the current Pixel
	 * @param y
	 *            y-Coordinate of the current Pixel
	 * @return
	 */
	private double computeDistance(long[][] S, int widthE, int x, int y) {
		double distance = 0;
		for (int d = 0; d < dim; d++) {
			distance += S[d][(y + n) * widthE + (x + n)]
					+ S[d][(y - n) * widthE + (x - n)]
					- S[d][(y - n) * widthE + (x + n)]
					- S[d][(y + n) * widthE + (x - n)];
		}

		return distance;
	}

	/**
	 * Computes the Difference Image for a given dx / dy Pair. As dx and dy can
	 * be negative, image needs to be expanded to prevent out of bounds errors.
	 * 
	 * @param image
	 *            Expanded Version of Original Image
	 * @param targetArr
	 *            Target Array in which the Difference Image gets stored into
	 * @param dx
	 * @param dy
	 */
	private void computeDifferenceImage(int[][] image, long[][] targetArr,
			int dx, int dy, int widthE, int heightE) {
		int wn = w + n;
		long temp;

		// Compute very first Pixel of Image (x = 0; y = 0)
		for (int d = 0; d < dim; d++) {
			temp = image[d][wn * widthE + wn]
					- image[d][(wn + dy) * widthE + dx + wn];
			targetArr[d][wn * widthE + wn] = temp * temp;
		}

		// Compute first Row of Image (y = 0)
		int offset = wn * widthE;
		int offsetdy = (wn + dy) * widthE;
		for (int x = wn + 1; x < widthE; x++) {
			for (int d = 0; d < dim; d++) {
				temp = image[d][offset + x] - image[d][offsetdy + x + dx];
				targetArr[d][offset + x] = targetArr[d][offset + x - 1]
						+ temp * temp;
			}
		}

		// Compute first Column of Image (x = 0)
		for (int y = wn + 1; y < heightE; y++) {
			int offsety = y * widthE;
			offsetdy = (y + dy) * widthE;
			for (int d = 0; d < dim; d++) {
				temp = image[d][offsety + wn] - image[d][offsetdy + wn + dx];
				targetArr[d][offsety + wn] = targetArr[d][offsety - widthE + wn]
						+ temp * temp;
			}
		}

		// Compute rest of the Image
		for (int y = wn + 1; y < heightE; y++) {
			offset = y * widthE;
			int offset2 = (y + dy) * widthE;
			for (int x = wn + 1; x < widthE; x++) {
				for (int d = 0; d < dim; d++) {
					targetArr[d][offset + x] = targetArr[d][offset + x - 1];
					targetArr[d][offset + x] += targetArr[d][offset + x
							- widthE];
					targetArr[d][offset + x] -= targetArr[d][offset + x - 1
							- widthE];

					temp = image[d][offset + x] - image[d][offset2 + x + dx];
					double temp2 = temp * temp;
					targetArr[d][offset + x] += temp2;
				}
			}
		}
	}

	/**
	 * Expands the boundaries of an image in all four directions. The new
	 * content of the Image gets filled with the adjacent parts of the Image. To
	 * view a Preview of this Image, use display = true
	 * 
	 * @param image
	 *            Original Image
	 * @param display
	 *            Display Preview of generated Image
	 * @return
	 */
	private int[][] expandImage(int[][] image, int xstart, int ystart,
			int width, int height, int orgWidth, int orgHeight,
			boolean display) {
		int heightE = height + 2 * w + 2 * n;
		int widthE = width + 2 * w + 2 * n;
		int[][] result = new int[dim][widthE * heightE];

		for (int y = 0; y < heightE; y++) {
			int yr = y - w - n + ystart;

			// if (yr >= orgHeight) yr = (ystart - w - n) + yr - orgHeight;
			if (yr >= orgHeight)
				yr = yr - orgHeight;
			if (yr < 0)
				yr = height + yr;

			int offset = y * widthE;
			int offsetr = yr * orgWidth;
			for (int x = 0; x < widthE; x++) {
				int xr = x + (xstart - w - n);
				// if (xr >= orgWidth) xr = xstart + xr - orgWidth;
				if (xr >= orgWidth)
					xr = xr - orgWidth;
				if (xr < 0)
					xr = width + xr;
				for (int d = 0; d < dim; d++) {
					result[d][offset + x] = image[d][offsetr + xr];
				}
			}
		}

		if (display) {
			int[] pixelsPicture = new int[result[0].length];

			for (int y = 0; y < heightE; y++) {
				int offset = y * widthE;
				for (int x = 0; x < widthE; x++) {
					int p = offset + x;
					int red = (int) result[0][p];
					int green = (int) result[1][p];
					int blue = (int) result[2][p];
					int pixel = ((red & 0xff) << 16) + ((green & 0xff) << 8)
							+ (blue & 0xff);
					pixelsPicture[p] = pixel;
				}
			}

			BufferedImage bimg = convertToImage(widthE, heightE, pixelsPicture);
			ImagePlus imp2 = new ImagePlus("Expanded Image", bimg);
			imp2.show();
		}

		return result;
	}

	/**
	 * Implements the gaussian noise level estimation algorithm of Immerkaer,
	 * J., 1996. Fast noise variance estimation. Computer Vision and Image
	 * Understanding, 64(2), pp.300–302.
	 * 
	 * @param imp
	 * @return noise level
	 */
	public static double getGlobalNoiseLevel(ImageProcessor ip) {
		FloatProcessor fp = null;

		switch (ip.getBitDepth()) {

		case 8:
			ByteProcessor bp = (ByteProcessor) ip;
			fp = bp.duplicate().convertToFloatProcessor();
			break;
		case 24:
			ColorProcessor cp = (ColorProcessor) ip;
			fp = cp.duplicate().convertToFloatProcessor();
			break;
		case 16:
			ShortProcessor sp = (ShortProcessor) ip;
			fp = sp.duplicate().convertToFloatProcessor();
			break;
		case 32:
			fp = (FloatProcessor) ip.duplicate();
			break;
		default:
			break;
		}

		Convolver convolver = new Convolver();
		int k = 6;
		float[] kernel = generateKernel(k);// {1,-2,1,-2,4,-2,1,-2,1};
		convolver.convolve(fp, kernel, 2 * k + 1, 2 * k + 1);

		int w = fp.getWidth();
		int h = fp.getHeight();
		double sum = 0;
		double sub = 2 * k;
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				sum += Math.abs(fp.getPixelValue(x, y));
			}
		}
		double sigma = Math.sqrt(Math.PI / 2) * 1.0
				/ (6.0 * (w - sub) * (h - sub)) * sum;

		return sigma;
	}

	public static float[] generateKernel(int k) {
		int n = 2 * k + 1;
		float[] kernel = new float[n * n];

		// Set all zero
		for (int i = 0; i < n * n; i++) {
			kernel[i] = 0;
		}
		kernel[0] = 1;
		kernel[k] = -2;
		kernel[2 * k] = 1;
		kernel[k * n] = -2;
		kernel[k * n + k] = 4;
		kernel[k * n + 2 * k] = -2;
		kernel[2 * k * n] = 1;
		kernel[2 * k * n + k] = -2;
		kernel[2 * k * n + 2 * k] = 1;

		return kernel;

	}

	/**
	 * Initialize needed Settings
	 * 
	 * @param sigma
	 *            An estimate of the standard deviation of the Noise-Level
	 *            within the Image
	 * @param ip
	 *            The Image-Processor of the original Image
	 */
	private void initSettings(int sigma, ImageProcessor ip) {
		int type = new ImagePlus(null, ip).getType();

		// Init recommended Algorithm Settings
		double hfactor;
		if (type == ImagePlus.COLOR_256 || type == ImagePlus.COLOR_RGB) {

			// Color Image

			if (sigma > 0 && sigma <= 25) {
				n = 1;
				w = 10;
				// n = 3;
				// w = 17;
				hfactor = 0.55;
			} else if (sigma > 25 && sigma <= 55) {
				n = 2;
				w = 17;
				hfactor = 0.4;
			} else {
				n = 3;
				w = 17;
				hfactor = 0.35;
			}
		} else {

			// Gray Image

			if (sigma > 0 && sigma <= 15) {
				n = 1;
				w = 10;
				hfactor = 0.4;
			} else if (sigma > 15 && sigma <= 30) {
				n = 2;
				w = 10;
				hfactor = 0.4;
			} else if (sigma > 30 && sigma <= 45) {
				n = 3;
				w = 17;
				hfactor = 0.35;
			} else if (sigma > 45 && sigma <= 75) {
				n = 4;
				w = 17;
				hfactor = 0.35;
			} else {
				n = 5;
				w = 17;
				hfactor = 0.3;
			}
		}

		width = ip.getWidth();
		height = ip.getHeight();
		widthE = width + 2 * w + 2 * n;
		heightE = height + 2 * w + 2 * n;

		// Read Pixels from ImageProcessor and store them in pixels Array
		convertPixels(ip, type);

		double h = hfactor * sigma;
		sigma2 = sigma * sigma * 2 * (dim * (2 * n + 1) * (2 * n + 1));
		// sigma2 = 2 * sigma * sigma;
		distConst = (dim * (2 * n + 1) * (2 * n + 1));
		// h2 = (h * h) / (dim * (2 * n + 1) * (2 * n + 1));
		h2 = (h * h);

		// Multithreadding related Initializations
		nextdx = -w;
		nextdy = -w;
	}

	/**
	 * Returns next dx / dy Pair dx and dy are needed to compute a specific
	 * iteration of the Algorithm. This method provides the next unused dx / dy
	 * Pair to be used in a denoising Thread.
	 * 
	 * @return dx and dy as int array, in this respective order
	 */
	private synchronized int[] getNextDV() {
		if (nextdy > 0)
			return null;

		int[] result = new int[] { nextdx, nextdy };

		if (nextdx == w) {
			nextdy++;
			nextdx = -w;
		} else {
			nextdx++;
		}

		return result;
	}

	/**
	 * Converts the Image into its proper form sothat it can be used by the
	 * Algorithm
	 * 
	 * @param ip
	 * @param type
	 *            Type of the Image based on ImageJ ImageTypes
	 */
	private void convertPixels(ImageProcessor ip, int type) {
		switch (type) {

		case ImagePlus.COLOR_256:
			convertColor256(ip);
			break;
		case ImagePlus.COLOR_RGB:
			convertRGB(ip);
			break;
		case ImagePlus.GRAY16:
			convertGray16(ip);
			break;
		case ImagePlus.GRAY32:
			convertGray32(ip);
			break;
		case ImagePlus.GRAY8:
			convertGray8(ip);
			break;
		default:
			break;
		}
	}

	private void convertColor256(ImageProcessor ip) {
		dim = 1;

		byte[] pixelArray = (byte[]) ip.getPixels();
		pixels = new int[dim][width * height];

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pos = offset + x;
				pixels[0][pos] = pixelArray[pos] & (0xff);
			}
		}
	}

	private void convertRGB(ImageProcessor ip) {
		dim = 3;

		int[] pixelArray = (int[]) ip.getPixels();
		pixels = new int[dim][width * height];

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int qtemp = pixelArray[offset + x];
				pixels[0][offset + x] = ((qtemp & 0xff0000) >> 16);
				pixels[1][offset + x] = ((qtemp & 0x00ff00) >> 8);
				pixels[2][offset + x] = ((qtemp & 0x0000ff));
			}
		}
	}

	private void convertGray32(ImageProcessor ip) {
		dim = 1;

		float[] pixelArray = (float[]) ip.getPixels();
		pixels = new int[dim][width * height];

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pos = offset + x;
				pixels[0][pos] = (int) pixelArray[pos];
			}
		}
	}

	private void convertGray16(ImageProcessor ip) {
		dim = 1;

		short[] pixelArray = (short[]) ip.getPixels();
		pixels = new int[dim][width * height];

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pos = offset + x;
				pixels[0][pos] = (int) (pixelArray[pos] & (0xffff));
			}
		}
	}

	private void convertGray8(ImageProcessor ip) {
		dim = 1;

		byte[] pixelArray = (byte[]) ip.getPixels();
		pixels = new int[dim][width * height];

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pos = offset + x;
				pixels[0][pos] = (int) (pixelArray[pos] & (0xff));
			}
		}
	}

	/**
	 * Converts a denoised Picture back to its original Format and saves it in
	 * the ImageProcessor
	 * 
	 * @param image
	 * @param ip
	 */
	private void createPicture(double[][] image, ImageProcessor ip) {

		switch (ip.getBitDepth()) {

		case 8:
			createPicture8Bit(image, ip);
			break;
		case 24:
			createPicture24Bit(image, ip);
			break;
		case 16:
			createPicture16Bit(image, ip);
			break;
		case 32:
			createPicture32Bit(image, ip);
			break;
		default:
			break;
		}

		// imp.repaintWindow();

		// impNew.setTitle(newImageTitle);
		// impNew.show();

	}

	private void createPicture24Bit(double[][] image, ImageProcessor ip) {
		// impNew = imp.createImagePlus();
		// impNew.setProcessor(imp.getProcessor().duplicate());
		int[] pixelsPicture = (int[]) ip.getPixels();

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int p = offset + x;
				int red = (int) image[0][p];
				int green = (int) image[1][p];
				int blue = (int) image[2][p];
				int pixel = ((red & 0xff) << 16) + ((green & 0xff) << 8)
						+ (blue & 0xff);
				pixelsPicture[p] = pixel;
			}
		}
		ip.setPixels(pixelsPicture);
	}

	private void createPicture32Bit(double[][] image, ImageProcessor ip) {
		// impNew = imp.createImagePlus();
		// impNew.setProcessor(imp.getProcessor().duplicate());
		float[] pixelsPicture = (float[]) ip.getPixels();

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pos = offset + x;
				float pixel = (float) (image[0][pos]);
				pixelsPicture[pos] = pixel;
			}
		}

		ip.setPixels(pixelsPicture);
	}

	private void createPicture16Bit(double[][] image, ImageProcessor ip) {
		// impNew = imp.createImagePlus();
		// impNew.setProcessor(imp.getProcessor().duplicate());
		short[] pixelsPicture = (short[]) ip.getPixels();

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pos = offset + x;
				short pixel = (short) (image[0][pos]);
				pixelsPicture[pos] = pixel;
			}
		}

		ip.setPixels(pixelsPicture);
	}

	private void createPicture8Bit(double[][] image, ImageProcessor ip) {
		// ImagePlus impNew = imp.createImagePlus();
		// impNew.setProcessor(imp.getProcessor().duplicate());
		byte[] pixelsPicture = (byte[]) ip.getPixels();
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pos = offset + x;
				byte pixel = (byte) (image[0][pos]);
				pixelsPicture[pos] = pixel;
			}
		}

		ip.setPixels(pixelsPicture);
	}

	public static BufferedImage convertToImage(int width, int height,
			int[] pixels) {
		int wh = width * height;
		int[] newPixels = new int[wh * 3];
		for (int i = 0; i < wh; i++) {
			int rgb = pixels[i];
			int red = (rgb >> 16) & 0xFF;
			int green = (rgb >> 8) & 0xFF;
			int blue = rgb & 0xFF;
			newPixels[i * 3] = red;
			newPixels[i * 3 + 1] = green;
			newPixels[i * 3 + 2] = blue;
		}

		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = (WritableRaster) image.getData();
		raster.setPixels(0, 0, width, height, newPixels);
		image.setData(raster);

		return image;
	}

	class Worker extends Thread {
		private int[][] image;

		private long[][] u;
		private long[] wMaxArr;
		private long[] wSumArr;

		int width;
		int height;

		public Worker() {
		}

		public Worker(int width, int height, int[][] image, long[][] u,
				long[] wMaxArr, long[] wSumArr) {
			this.width = width;
			this.height = height;
			this.image = image;
			this.u = u;
			this.wMaxArr = wMaxArr;
			this.wSumArr = wSumArr;
		}

		@Override
		public void run() {
			int[] vec;
			int dx, dy;
			int heightE = height + 2 * w + 2 * n;
			int widthE = width + 2 * w + 2 * n;
			long[] TwMaxArr = new long[widthE * heightE];
			long[] TwSumArr = new long[widthE * heightE];
			long[][] TimagePart = new long[dim][widthE * heightE];
			long[][] TS = new long[dim][widthE * heightE];

			vec = getNextDV();
			while (vec != null) {
				dx = vec[0];
				dy = vec[1];
				if ((2 * w + 1) * dy + dx >= 0) {
					vec = getNextDV();
					continue;
				}

				// compute Sdx
				computeDifferenceImage(image, TS, dx, dy, widthE, heightE);

				// denoise with Sdx
				denoise(TimagePart, image, TS, TwMaxArr, TwSumArr, widthE,
						heightE, dx, dy);

				// get next Vector
				vec = getNextDV();
			}

			// save to global variables
			deliverImagePart(TimagePart, u, widthE, heightE);
			deliverWMaxArr(TwMaxArr, wMaxArr, widthE, heightE);
			deliverWSumArr(TwSumArr, wSumArr, widthE, heightE);
		}
	}
}

class FastMathStuff {
	private static final int BIG_ENOUGH_INT = 16 * 1024;
	private static final double BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5;

	public static double max(final double a, final double b) {
		if (a > b) {
			return a;
		}
		if (a < b) {
			return b;
		}
		/* if either arg is NaN, return NaN */
		if (a != b) {
			return Double.NaN;
		}
		/* min(+0.0,-0.0) == -0.0 */
		/* 0x8000000000000000L == Double.doubleToRawLongBits(-0.0d) */
		long bits = Double.doubleToRawLongBits(a);
		if (bits == 0x8000000000000000L) {
			return b;
		}
		return a;
	}

	// http://www.java-gaming.org/index.php?topic=24194.0
	public static int fastRound(double x) {
		return (int) (x + BIG_ENOUGH_ROUND) - BIG_ENOUGH_INT;
	}

}

/**
 * Ref :Yu-Li You, M. Kaveh, "Fourth Order Partial Differential Equations for
 * Noise Removal, IEEE Trans. Image Processing, vol. 9, no. 10, pp 1723-1730,
 * October 2000] Method : du/dt = - del^2[c(del^2(u))del^u] where u is the noisy
 * input image.
 */
class PDE_Filter {

	Differentiator d = new Differentiator();
	double dt = 0.9;
	int T;
	double scale;

	ImageProcessor filter(ImagePlus imp, double scale, int T) {
		this.scale = scale;
		this.T = T;
		imp = imp.duplicate();
		new ImageConverter(imp).convertToGray32();
		
		Image img = Image.wrap(imp);
		final Aspects asps = img.aspects();
		if (asps.x <= 0)
			throw new IllegalStateException(
					"Aspect-ratio value in x-dimension less than or equal to 0");
		if (asps.y <= 0)
			throw new IllegalStateException(
					"Aspect-ratio value in y-dimension less than or equal to 0");
		if (asps.z <= 0)
			throw new IllegalStateException(
					"Aspect-ratio value in z-dimension less than or equal to 0");
		Image I1 = (img instanceof FloatImage) ? 
				img : new FloatImage(img);
		for (int t = 0; t < T; t++) {
			Image Ixx = d.run(I1.duplicate(), scale, 2, 0, 0);
			Image Iyy = d.run(I1.duplicate(), scale, 0, 2, 0);
			Image C = getC(Ixx, Iyy);
			
			Image CMIxx = C.duplicate();
			CMIxx.multiply(Ixx);
			Image CMIyy = C.duplicate();
			CMIyy.multiply(Iyy);
			
			Image Div = d.run(CMIxx.duplicate(), scale, 2, 0, 0);
			Div.add(d.run(CMIyy.duplicate(), scale, 0, 2, 0));
			
			Div.multiply(dt);
			I1.subtract(Div);
		}
		ImagePlus res = I1.imageplus();
		new ImageConverter(res).convertToGray8();
		return res.getProcessor();
		
	}

	Image getI2(Image I1, Image Div) {
		final Coordinates coords = new Coordinates();
		final Dimensions dims = I1.dimensions();
		Image I2 = I1.duplicate();
		double i1, div, i2;
		for (coords.c = 0; coords.c < dims.c; ++coords.c)
			for (coords.t = 0; coords.t < dims.t; ++coords.t)
				for (coords.y = 0; coords.y < dims.y; ++coords.y)
					for (coords.x = 0; coords.x < dims.x; ++coords.x) {
						i1 = I1.get(coords);
						div = Div.get(coords);
						i2 = i1 - (dt * div);
						Div.set(coords, i2);
					}
		return I2;
	}

	Image getDiv(Image Div11, Image Div22) {
		final Coordinates coords = new Coordinates();
		final Dimensions dims = Div11.dimensions();
		Image Div = Div11.duplicate();
		double div1, div2, div;
		for (coords.c = 0; coords.c < dims.c; ++coords.c)
			for (coords.t = 0; coords.t < dims.t; ++coords.t)
				for (coords.y = 0; coords.y < dims.y; ++coords.y)
					for (coords.x = 0; coords.x < dims.x; ++coords.x) {
						div1 = Div11.get(coords);
						div2 = Div22.get(coords);
						div = div1 + div2;
						Div.set(coords, div);
					}
		return Div;
	}

	Image getDiv1(Image C, Image Ixx) {
		final Coordinates coords = new Coordinates();
		final Dimensions dims = Ixx.dimensions();
		Image Div1 = Ixx.duplicate();
		double ixx, c, div1;
		for (coords.c = 0; coords.c < dims.c; ++coords.c)
			for (coords.t = 0; coords.t < dims.t; ++coords.t)
				for (coords.y = 0; coords.y < dims.y; ++coords.y)
					for (coords.x = 0; coords.x < dims.x; ++coords.x) {
						ixx = Ixx.get(coords);
						c = C.get(coords);
						div1 = ixx * c;
						Div1.set(coords, div1);
					}
		Div1 = d.run(Div1, scale, 1, 0, 0);
		return Div1;
	}

	Image getDiv2(Image C, Image Iyy) {
		final Coordinates coords = new Coordinates();
		final Dimensions dims = Iyy.dimensions();
		Image Div2 = Iyy.duplicate();
		double iyy, c, div2;
		for (coords.c = 0; coords.c < dims.c; ++coords.c)
			for (coords.t = 0; coords.t < dims.t; ++coords.t)
				for (coords.y = 0; coords.y < dims.y; ++coords.y)
					for (coords.x = 0; coords.x < dims.x; ++coords.x) {
						iyy = Iyy.get(coords);
						c = C.get(coords);
						div2 = iyy * c;
						Div2.set(coords, div2);
					}
		Div2 = d.run(Div2, scale, 0, 1, 0);
		return Div2;
	}

	Image getC(Image Ixx, Image Iyy) {
		final Coordinates coords = new Coordinates();
		final Dimensions dims = Ixx.dimensions();
		Image C = Ixx.duplicate();
		double ixx, iyy, ic;
		for (coords.c = 0; coords.c < dims.c; ++coords.c)
			for (coords.t = 0; coords.t < dims.t; ++coords.t)
				for (coords.y = 0; coords.y < dims.y; ++coords.y)
					for (coords.x = 0; coords.x < dims.x; ++coords.x) {
						ixx = Ixx.get(coords);
						iyy = Iyy.get(coords);
						ic = 1 / (1 + Math.sqrt(ixx * ixx + iyy * iyy) + 0.0000001);
						C.set(coords, ic);
					}
		return C;
	}
}

/**
 * This plugin implements the Bilateral Filter, described in
 * 
 * C. Tomasi and R. Manduchi, "Bilateral Filtering for Gray and Color Images",
 * Proceedings of the 1998 IEEE International Conference on Computer Vision,
 * Bombay, India.
 * 
 * Basically, it does a Gaussian blur taking into account the intensity domain
 * in addition to the spatial domain (i.e. pixels are smoothed when they are
 * close together _both_ spatially and by intensity. // spatialRadius 3 //
 * rangeRadius 50
 */
class Bilateral_Filter {
	ImageProcessor filter(ImagePlus image, double spatialRadius,
			double rangeRadius) {
		double chrono = System.currentTimeMillis();
		final InterpolatedImage orig = new InterpolatedImage(image);
		InterpolatedImage res = orig.cloneDimensionsOnly();
		final float[] spatial = makeKernel(spatialRadius);
		final float[] range = makeKernel(rangeRadius);
		res.image.setTitle("Fiji " + orig.image.getTitle() + "-" + spatialRadius
				+ "-" + rangeRadius);

		InterpolatedImage.Iterator iter = res.iterator(true);
		InterpolatedImage o = orig;
		float[] s = spatial;
		int sc = spatial.length / 2;
		float[] r = range;
		int rc = range.length / 2;
		while (iter.next() != null) {
			int v0 = o.getNoInterpol(iter.i, iter.j, iter.k);
			float v = 0, total = 0;
			for (int n = 0; n < s.length; n++)
				for (int m = 0; m < s.length; m++) {
					int v1 = o.getNoInterpol(iter.i + m - sc, iter.j + n - sc,
							iter.k);
					if (Math.abs(v1 - v0) > rc)
						continue;
					float w = s[m] * s[n] * r[v1 - v0 + rc];
					v += v1 * w;
					total += w;
				}
			res.set(iter.i, iter.j, iter.k, (int) (v / total));
		}
//		IJ.log("Bilateral Filter Fiji || size: " + "-" + (sc * 2) + "-"
//				+ (rc * 2) + " Time:" + (System.currentTimeMillis() - chrono));
		return res.image.getProcessor();
	}

	public static float[] makeKernel(double radius) {
		radius += 1;
		int size = (int) radius * 2 - 1;
		float[] kernel = new float[size];
		float total = 0;
		for (int i = 0; i < size; i++) {
			double x = (i + 1 - radius) / (radius * 2) / 0.2;
			float v = (float) Math.exp(-0.5 * x * x);
			kernel[i] = v;
			total += v;
		}
		if (total <= 0.0)
			for (int i = 0; i < size; i++)
				kernel[i] = 1.0f / size;
		else if (total != 1.0)
			for (int i = 0; i < size; i++)
				kernel[i] /= total;
		return kernel;
	}
}

class InterpolatedImage {
	public ImagePlus image;
	public int w, h, d;
	private byte[][] pixels;
	private float[][] pixelsFloat;
	private short[][] pixelsShort;
	private int[][] pixelsInt;
	public Interpolate interpol;
	int type;

	public InterpolatedImage(ImagePlus image) {
		this.image = image;
		ImageStack stack = image.getStack();
		d = stack.getSize();
		h = stack.getHeight();
		w = stack.getWidth();
		type = image.getType();

		if (type == ImagePlus.GRAY8 || type == ImagePlus.COLOR_256) {
			pixels = new byte[d][];
			for (int i = 0; i < d; i++)
				pixels[i] = (byte[]) stack.getPixels(i + 1);

			if (type == ImagePlus.GRAY8 && !image.getProcessor().isColorLut())
				interpol = new AverageByte();
			else
				interpol = new NearestNeighbourByte();
		} else if (type == ImagePlus.GRAY32) {
			pixelsFloat = new float[d][];
			for (int i = 0; i < d; i++)
				pixelsFloat[i] = (float[]) stack.getPixels(i + 1);

			interpol = new AverageFloat();
		} else if (type == ImagePlus.GRAY16) {
			pixelsShort = new short[d][];
			for (int i = 0; i < d; i++)
				pixelsShort[i] = (short[]) stack.getPixels(i + 1);

			interpol = new AverageShort();
		} else if (type == ImagePlus.COLOR_RGB) {
			pixelsInt = new int[d][];
			for (int i = 0; i < d; i++)
				pixelsInt[i] = (int[]) stack.getPixels(i + 1);

			interpol = new AverageInt();
		} else {
			throw new RuntimeException("Image type not supported");
		}
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	public int getDepth() {
		return d;
	}

	protected InterpolatedImage() {
	}

	public ImagePlus getImage() {
		return image;
	}

	public interface Interpolate {
		double get(double x, double y, double z);
	}

	class AverageByte implements Interpolate {
		final public double get(double x, double y, double z) {
			int x1 = (int) Math.floor(x);
			int y1 = (int) Math.floor(y);
			int z1 = (int) Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpol(x1, y1, z1),
					v001 = getNoInterpol(x1, y1, z1 + 1),
					v010 = getNoInterpol(x1, y1 + 1, z1),
					v011 = getNoInterpol(x1, y1 + 1, z1 + 1),
					v100 = getNoInterpol(x1 + 1, y1, z1),
					v101 = getNoInterpol(x1 + 1, y1, z1 + 1),
					v110 = getNoInterpol(x1 + 1, y1 + 1, z1),
					v111 = getNoInterpol(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR
					* (yR * (zR * v000 + (1 - zR) * v001)
							+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
					+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
							+ (1 - yR) * (zR * v110 + (1 - zR) * v111));

			return ret;
		}
	}

	/*
	 * This weights the values of the 8 ligands by the inverted distance and
	 * picks the one with the maximum
	 */
	class MaxLikelihoodByte implements Interpolate {
		int[] value = new int[8];
		double[] histo = new double[256];
		double xF, yF, zF;

		public MaxLikelihoodByte(double pixelWidth, double pixelHeight,
				double pixelDepth) {
			xF = pixelWidth;
			yF = pixelHeight;
			zF = pixelDepth;
		}

		double xR, yR, zR;
		final double eps = 1e-10;

		final double factor(int dx, int dy, int dz) {
			double x = (dx == 0 ? xR : 1 - xR);
			double y = (dy == 0 ? yR : 1 - yR);
			double z = (dz == 0 ? zR : 1 - zR);
			return 1.0 / (eps + x * x + y * y + z * z);
		}

		final public double get(double x, double y, double z) {
			int x1 = (int) Math.floor(x);
			int y1 = (int) Math.floor(y);
			int z1 = (int) Math.floor(z);
			xR = x1 + 1 - x;
			yR = y1 + 1 - y;
			zR = z1 + 1 - z;

			for (int i = 0; i < 2; i++)
				for (int j = 0; j < 2; j++)
					for (int k = 0; k < 2; k++) {
						int l = i + 2 * (j + 2 * k);
						value[l] = getNoInterpol(x1 + i, y1 + j, z1 + k);
						histo[value[l]]++;
						/*
						 * histo[value[l]] += factor(i, j, k);
						 */
					}

			int winner = value[0];

			for (int i = 1; i < 8; i++)
				// if (histo[value[i]] >= histo[winner])
				if (value[i] >= winner)
					winner = value[i];

			for (int i = 0; i < 8; i++)
				histo[value[i]] = 0;

			return winner;
		}
	}

	public class NearestNeighbourByte implements Interpolate {
		final public double get(double x, double y, double z) {
			return getInt(x, y, z);
		}

		final public int getInt(double x, double y, double z) {
			double x1 = Math.round(x);
			double y1 = Math.round(y);
			double z1 = Math.round(z);
			return getNoInterpol((int) x1, (int) y1, (int) z1);
		}
	}

	final public int getNoCheck(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixels[z][x + w * y] & 0xff;
	}

	final public int getNoInterpol(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheck(x, y, z);
	}

	final public byte getNearestByte(double x, double y, double z) {
		int i = (int) Math.round(x);
		if (i < 0 || i >= w)
			return 0;
		int j = (int) Math.round(y);
		if (j < 0 || j >= h)
			return 0;
		int k = (int) Math.round(z);
		if (k < 0 || k >= d)
			return 0;
		return pixels[k][i + w * j];
	}

	public void set(int x, int y, int z, int value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixels[z][x + w * y] = (byte) value;
	}

	public class Iterator implements java.util.Iterator {
		// these are the coordinates
		public int i, j, k;

		boolean showProgress = false;

		int x0, x1, y0, y1, z0, z1, xd, zd;

		public Iterator(boolean showProgress, int x0, int y0, int z0, int x1,
				int y1, int z1) {
			this.showProgress = showProgress;
			this.x0 = x0;
			this.y0 = y0;
			this.z0 = z0;
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			xd = x1 - x0;
			zd = z1 - z0;
			i = x0 - 1;
			j = y0;
			k = z0;
		}

		public boolean hasNext() {
			return i + 1 < x1 || j + 1 < y1 || k + 1 < z1;
		}

		public Object next() {
			if (++i >= x1) {
				i = x0;
				if (++j >= y1) {
					j = y0;
					if (++k >= z1)
						return null;
					if (showProgress)
						IJ.showProgress(k - z0 + 1, zd);
				}
			}
			return this;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Iterator iterator() {
		return iterator(false);
	}

	public Iterator iterator(boolean showProgress) {
		return iterator(showProgress, 0, 0, 0, w, h, d);
	}

	public Iterator iterator(boolean showProgress, int x0, int y0, int z0,
			int x1, int y1, int z1) {
		return new Iterator(showProgress, x0, y0, z0, x1, y1, z1);
	}

	// this is quick'n dirty
	public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
			int value) {
		int c1 = Math.abs(x1 - x2);
		int c2 = Math.abs(y1 - y2);
		int c3 = Math.abs(z1 - z2);
		if (c2 > c1)
			c1 = c2;
		if (c3 > c1)
			c1 = c3;
		if (c1 == 0) {
			set(x1, y1, z1, value);
			return;
		}
		for (int i = 0; i <= c1; i++)
			set(x1 + i * (x2 - x1) / c1, y1 + i * (y2 - y1) / c1,
					z1 + i * (z2 - z1) / c1, value);
	}

	/* float */
	class AverageFloat implements Interpolate {
		public double get(double x, double y, double z) {
			int x1 = (int) Math.floor(x);
			int y1 = (int) Math.floor(y);
			int z1 = (int) Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpolFloat(x1, y1, z1),
					v001 = getNoInterpolFloat(x1, y1, z1 + 1),
					v010 = getNoInterpolFloat(x1, y1 + 1, z1),
					v011 = getNoInterpolFloat(x1, y1 + 1, z1 + 1),
					v100 = getNoInterpolFloat(x1 + 1, y1, z1),
					v101 = getNoInterpolFloat(x1 + 1, y1, z1 + 1),
					v110 = getNoInterpolFloat(x1 + 1, y1 + 1, z1),
					v111 = getNoInterpolFloat(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR
					* (yR * (zR * v000 + (1 - zR) * v001)
							+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
					+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
							+ (1 - yR) * (zR * v110 + (1 - zR) * v111));

			return ret;
		}
	}

	public float getNoCheckFloat(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixelsFloat[z][x + w * y];
	}

	public float getNoInterpolFloat(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheckFloat(x, y, z);
	}

	public void setFloat(int x, int y, int z, float value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixelsFloat[z][x + w * y] = value;
	}

	/* int */
	class AverageInt implements Interpolate {
		public double get(double x, double y, double z) {
			int x1 = (int) Math.floor(x);
			int y1 = (int) Math.floor(y);
			int z1 = (int) Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			int v000 = getNoInterpolInt(x1, y1, z1),
					v001 = getNoInterpolInt(x1, y1, z1 + 1),
					v010 = getNoInterpolInt(x1, y1 + 1, z1),
					v011 = getNoInterpolInt(x1, y1 + 1, z1 + 1),
					v100 = getNoInterpolInt(x1 + 1, y1, z1),
					v101 = getNoInterpolInt(x1 + 1, y1, z1 + 1),
					v110 = getNoInterpolInt(x1 + 1, y1 + 1, z1),
					v111 = getNoInterpolInt(x1 + 1, y1 + 1, z1 + 1);

			int red = (int) Math.round(xR
					* (yR * (zR * r(v000) + (1 - zR) * r(v001))
							+ (1 - yR) * (zR * r(v010) + (1 - zR) * r(v011)))
					+ (1 - xR) * (yR * (zR * r(v100) + (1 - zR) * r(v101))
							+ (1 - yR) * (zR * r(v110) + (1 - zR) * r(v111))));

			int green = (int) Math.round(xR
					* (yR * (zR * g(v000) + (1 - zR) * g(v001))
							+ (1 - yR) * (zR * g(v010) + (1 - zR) * g(v011)))
					+ (1 - xR) * (yR * (zR * g(v100) + (1 - zR) * g(v101))
							+ (1 - yR) * (zR * g(v110) + (1 - zR) * g(v111))));

			int blue = (int) Math.round(xR
					* (yR * (zR * b(v000) + (1 - zR) * b(v001))
							+ (1 - yR) * (zR * b(v010) + (1 - zR) * b(v011)))
					+ (1 - xR) * (yR * (zR * b(v100) + (1 - zR) * b(v101))
							+ (1 - yR) * (zR * b(v110) + (1 - zR) * b(v111))));

			return (red << 16) + (green << 8) + blue;
		}

		private double r(int v) {
			return (double) ((v & 0xff0000) >> 16);
		}

		private double g(int v) {
			return (double) ((v & 0xff00) >> 8);
		}

		private double b(int v) {
			return (double) (v & 0xff);
		}
	}

	public int getNoCheckInt(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixelsInt[z][x + w * y];
	}

	public int getNoInterpolInt(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheckInt(x, y, z);
	}

	public void setInt(int x, int y, int z, int value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixelsInt[z][x + w * y] = value;
	}

	/* short */
	class AverageShort implements Interpolate {
		public double get(double x, double y, double z) {
			int x1 = (int) Math.floor(x);
			int y1 = (int) Math.floor(y);
			int z1 = (int) Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpolShort(x1, y1, z1),
					v001 = getNoInterpolShort(x1, y1, z1 + 1),
					v010 = getNoInterpolShort(x1, y1 + 1, z1),
					v011 = getNoInterpolShort(x1, y1 + 1, z1 + 1),
					v100 = getNoInterpolShort(x1 + 1, y1, z1),
					v101 = getNoInterpolShort(x1 + 1, y1, z1 + 1),
					v110 = getNoInterpolShort(x1 + 1, y1 + 1, z1),
					v111 = getNoInterpolShort(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR
					* (yR * (zR * v000 + (1 - zR) * v001)
							+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
					+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
							+ (1 - yR) * (zR * v110 + (1 - zR) * v111));

			return ret;
		}
	}

	public short getNoCheckShort(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixelsShort[z][x + w * y];
	}

	public short getNoInterpolShort(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheckShort(x, y, z);
	}

	public void setShort(int x, int y, int z, short value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixelsShort[z][x + w * y] = value;
	}

	public InterpolatedImage cloneDimensionsOnly() {
		return cloneDimensionsOnly(image, type);
	}

	public static InterpolatedImage cloneDimensionsOnly(ImagePlus ip,
			int type) {
		InterpolatedImage result = new InterpolatedImage();
		result.w = ip.getWidth();
		result.h = ip.getHeight();
		result.d = ip.getStack().getSize();

		switch (type) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
			result.pixels = new byte[result.d][];
			break;
		case ImagePlus.GRAY32:
			result.pixelsFloat = new float[result.d][];
			break;
		case ImagePlus.GRAY16:
			result.pixelsShort = new short[result.d][];
			break;
		case ImagePlus.COLOR_RGB:
			result.pixelsInt = new int[result.d][];
			break;
		default:
			throw new RuntimeException("Image type not supported");
		}

		ImageStack stack = new ImageStack(result.w, result.h, null);
		for (int i = 0; i < result.d; i++)
			switch (type) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				result.pixels[i] = new byte[result.w * result.h];
				stack.addSlice("", result.pixels[i]);
				break;
			case ImagePlus.GRAY32:
				result.pixelsFloat[i] = new float[result.w * result.h];
				stack.addSlice("", result.pixelsFloat[i]);
				break;
			case ImagePlus.GRAY16:
				result.pixelsShort[i] = new short[result.w * result.h];
				stack.addSlice("", result.pixelsShort[i]);
				break;
			case ImagePlus.COLOR_RGB:
				result.pixelsInt[i] = new int[result.w * result.h];
				stack.addSlice("", result.pixelsInt[i]);
				break;
			}

		result.image = new ImagePlus("", stack);
		result.image.setCalibration(ip.getCalibration());
		return result;
	}

	public InterpolatedImage cloneImage() {
		InterpolatedImage res = cloneDimensionsOnly();
		for (int k = 0; k < d; k++)
			switch (type) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				System.arraycopy(pixels[k], 0, res.pixels[k], 0, w * h);
				break;
			case ImagePlus.GRAY32:
				System.arraycopy(pixelsFloat[k], 0, res.pixelsFloat[k], 0,
						w * h);
				break;
			case ImagePlus.GRAY16:
				System.arraycopy(pixelsShort[k], 0, res.pixelsShort[k], 0,
						w * h);
				break;
			case ImagePlus.COLOR_RGB:
				System.arraycopy(pixelsInt[k], 0, res.pixelsInt[k], 0, w * h);
				break;
			}
		return res;
	}
}
