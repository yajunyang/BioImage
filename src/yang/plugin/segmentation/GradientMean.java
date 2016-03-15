package yang.plugin.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class GradientMean implements PlugIn{

	private int size = 15;
	private int R = 40;
	private double K = -1.5;
	
	/**
	 // gray image 'short' type <br>
	 1   2   3  ---> i <br>
	
	 1   *   3   <br>
	 
	 1   2   3  <br>
	 
	 | <br>
	 V <br>
	 j <br>
	 
	 
	 T(x,y) = Mean + k * G  if C > R <br>
	 		= 0.5Mean       else     <br>
	 G sqrt|Vx2 + Vy2|/size  gradient mean of neighbor <br>		
	 C: max - min of 2D array   R=40 k=-1.5 <br>
	 
	 */
	public short getLocalThreshold(short[][] ps, int R, double k) {
		int w = ps[0].length; 
		int h = ps.length;
		double g = 0;	// gradient mean in  iXj neighbor 
		int r,c;  // r: x orientation gradient, c: y orientation gradient
		short max = ps[0][0], min = max;
		double mean = 0.0;
		for(int j=0, H=h-1; j<H; j++) {
			for(int i=0, W=w-1; i<W; i++) {
				c = ps[j+1][i]-ps[j][i];
				r = ps[j][i+1]-ps[j][i];
				g += Math.sqrt(r*r + c*c);
				short v = ps[j][i];
				if(v > max) 
					max = v;
				if(v < min)
					min = v;
				mean += v;
			}
		}
		int size = (w-1) * (h-1);
		g /= size;
		mean /= size;
		if((max - min) > R) {
			double T = mean + k * g;
			return (short) T;
		} else {
			return (short)(0.5*mean);
		}
	}
	
	/**
	// -------------------------> j x
	// |(x+j-S/2,y+i-S/2)=(X,Y)
	// |		Center(x,y)
	// |
	// i y
	*/
	public short[][] get2DNeighborArray(ImageProcessor ip, int x, int y, int S) {
		if((S & 1) == 0) // can't be even 
			return null;
		short[][] ps = new short[S+1][S+1];
		for(int i=0; i<S; i++){
			for(int j=0; j<S; j++) {
				int X = x + j - S/2;
				int Y = y + i - S/2;
				ps[j][i] = (short)ip.getPixel(X, Y);
			}
		}
		for(int i=0; i<=S; i++) {
			int X = x + S/2 + 1;
			for(int Y=y-S/2; Y<y+S/2+1; Y++) {
				ps[i][S] = (short)ip.getPixel(X, Y);
			}
		}
		for(int j=0; j<=S; j++) {
			int Y = y + S/2 + 1;
			for(int X=x-S/2; X<x+S/2+1; X++) {
				ps[S][j] = (short)ip.getPixel(X, Y);
			}
		}
		return ps;
	}
	
	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Mean-Gradient");
		gd.addStringField("Size", "3");
		gd.addStringField("R", "40");
		gd.addStringField("K", "-1.5");
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		size = Integer.parseInt(gd.getNextString());
		R = Integer.parseInt(gd.getNextString());
		K = Double.parseDouble(gd.getNextString());
		return true;
	}

	@Override
	public void run(String arg) {
		ImagePlus ips = WindowManager.getCurrentImage();
		if(null ==ips || ips.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
			return;
		}
		
		if(!showDialog()){
			return;
		}
		
		ImageProcessor ip = ips.getProcessor();
		ImagePlus ips1 = ips.duplicate();
		ImageProcessor ip1 = ips1.getProcessor();
		
		int h = ip.getHeight();
		int w = ip.getWidth();
		for(int x=0; x<w; x++) {
			for(int y=0; y<h; y++) {
				short[][] ps = get2DNeighborArray(ip, x, y, size);
				short T = getLocalThreshold(ps, R, K);
				int v = ip.getPixel(x, y);
				if(T > v)
					ip1.set(x, y, 0);
				else 
					ip1.set(x, y, 255);
			}
		}
		ips1.setTitle("Mean Gradient");
		ips1.show();
	}
}

































