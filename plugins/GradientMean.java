package yang.plugin.segmentation;

import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class GradientMean implements PlugIn{

	
	
	/**
	 // gray image 'short' type
	 1   2   3  $ ---> i
	 
	 1   *   3  $
	 
	 1   2   3  $
	 
	 $	 $	 $  $
	 
	 |
	 V
	 j
	 
	 
	 //////
	 T(x,y) = Mean + k * G  if C > R
	 		= 0.5Mean       else
	 G sqrt|Vx2 + Vy2|/size  gradient mean of neighbor		
	 C: man - min of 2D array   R=40 k=-1.5
	 
	 */
	public short getLocalThreshold(short[][] ps, int R, double k) {
		int w = ps[0].length; 
		int h = ps.length;
		double g = 0;	// gradient mean in  iXj neighbor 
		int r,c;  // r: x orientation gradient, c: y orientation gradient
		short max = ps[0][0], min = max;
		double mean = 0.0;
		for(int j=0; j<h-1; j++) {
			for(int i=0; i<w-1; i++) {
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
		short[][] ps = new short[S][S];
		for(int i=0, s = S+1; i<s; i++){
			for(int j=0; j<s; j++) {
				int X = x + j - S/2;
				int Y = y + i - S/2;
				ps[j][i] = (short)ip.getPixel(X, Y);
			}
		}
		return ps;
	}

	@Override
	public void run(String arg) {
		ImagePlus ips = WindowManager.getCurrentImage();
		ImageProcessor ip = ips.getProcessor();
		ImagePlus ips1 = ips.duplicate();
		ImageProcessor ip1 = ips1.getProcessor();
		int h = ip.getHeight();
		int w = ip.getWidth();
		for(int x=0; x<w; x++) {
			for(int y=0; y<h; y++) {
				short[][] ps = get2DNeighborArray(ip, x, y, 3);
				short T = getLocalThreshold(ps, 40, -1.5);
				ip1.set(x, y, (int)T);
			}
		}
		ips1.show("Mean-Gradient");
	}
}
