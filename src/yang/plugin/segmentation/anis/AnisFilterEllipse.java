package yang.plugin.segmentation.anis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


class Point{
	int row;
	int col;
	public Point(int row, int col) {
		this.row = row;
		this.col = col;
	}
}

public class AnisFilterEllipse implements PlugIn {

	private double a = 2.0;
	private double b = 1.0;
	private double h = 5;
	private double sample = 1;
	private ImageProcessor ip;
	private ImageProcessor[] fs;
	private List<Point> ls = new ArrayList<Point>();
	private List<Integer> ps = new ArrayList<Integer>();
	private int[] nLps = new int[9];	// Non linear ps
	private static final double T = 0.1;
	
	int[][] degrees;
	
	@Override
	public void run(String arg0) {
		ImagePlus iPlus = WindowManager.getCurrentImage();
		if(null == iPlus){
			IJ.noImage();
			return;
		}
		if(iPlus.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
			return;
		}
		if(!showDialog())
			return;
		
		ip = iPlus.getProcessor();
		fs = new ImageProcessor[4];
		
		fs[0] = ip.duplicate();	// mean non direction 0
		fs[1] = ip.duplicate(); // med non direction 0
		fs[2] = ip.duplicate(); // mean non direction mean 
		fs[3] = ip.duplicate(); // med non direction mean
		
		DirectionDetectHessian direct = new DirectionDetectHessian(ip);
		direct.run();
		degrees = direct.getDegree();
		
		filter();
		
		new ImagePlus("Mean_0", fs[0]).show();
		new ImagePlus("Med_0", fs[1]).show();
		new ImagePlus("Mean_mean", fs[2]).show();
		new ImagePlus("Med_mean", fs[3]).show();
	}
	
	public boolean showDialog() {
		GenericDialog gd = new GenericDialog("AnisFilter");
		gd.addNumericField("a :", a, 1);
		gd.addNumericField("b :", b, 1);
		gd.addNumericField("h :", h, 0);
		gd.addNumericField("Sample :", sample, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
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

	public void filter() {
		int width = ip.getWidth();
		int height = ip.getHeight();
		int i, j, pixel;
		int mean, med;
		for(int y=0; y<height; y++) {
			for(int x=0; x<width; x++) {
				ps.clear();
				int degree = degrees[y][x];
				if(degree != -200) {
					double[][] gauss = getAnisdGaussKernel(a, b, degree, (int)h, sample);
					setKernelList(gauss);
					int s = ls.size();
					int c = 0;
					while(c < s) {
						Point p = ls.get(c);
						i = p.row; 
						j = p.col;
						pixel = ip.getPixel(x+j, y+i);
						ps.add(pixel);
						c++;
					}
					if(ps.isEmpty()) 
						continue;
				
					med = getMed(ps);
					mean = getMean(ps);
					
					fs[0].putPixel(x, y, mean);
					fs[1].putPixel(x, y, med);
					fs[2].putPixel(x, y, mean);
					fs[3].putPixel(x, y, med);
				} else { // solve 3x3 mean
					mean = getNonDirectMean(ip, x, y);
					fs[0].putPixel(x, y, 0);
					fs[1].putPixel(x, y, 0);
					fs[2].putPixel(x, y, mean);
					fs[3].putPixel(x, y, mean);
				}
			}
		}
	}
	
	public int getMed(List<Integer> ps) {
		int S = ps.size();
		int[] L = new int[S];
		int i = 0;
		for (int v : ps) {
			L[i] = v;
			i++;
		}
		Arrays.sort(L);
		return L[S/2];
	}
	
	public int getMean(List<Integer> ps) {
		int sum = 0;
		for (int i : ps) {
			sum += i;
		}
		return sum / ps.size();
	}
	
	public int getNonDirectMean(ImageProcessor ip, int x, int y) {
		float sum = 0;
		for(int i=0; i<3; i++) {
			for(int j=0; j<3; j++) {
				nLps[j + 3*i] = ip.getPixel(x + j - 1, y + i - 1);
				sum += nLps[j + 3*i];
			}
		}
		return Math.round(sum / nLps.length);
	}
	
	public void setKernelList(double[][] ds) {
		ls.clear();
		int S = ds.length;	// square kernel
		int N = S / 2;
		for(int i=-S/2; i<=S/2; i++) {
			for(int j=-S/2; j<=S/2; j++) {
				if(ds[i+N][j+N] >= T) {
					ls.add(new Point(i, j));
				}
			}
		}
	}
	
	public double[][] getAnisdGaussKernel(double a, double b, int degree, int h, double sample) {
		if(h < 3) h = 3;
		if(h % 2 == 0) h -= 1;
		if(a == 0 || b == 0) {
			IJ.error("Can't make the value a and b of the gaussian kernel zero");
		}
		if(sample <= 0.0) 
			sample = 1;
		
		double kernel[][] = new double[h][h];
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
			}
		return kernel;
	}
	
}