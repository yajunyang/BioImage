package yang.plugin.segmentation.anis;

import java.text.DecimalFormat;


public class AnitropicGaussian {
	
	/**
	z = exp(-1/2{[xcos(s)+ysin(s)]^2/p^2 + [xsin(s)+ycos(s)]^2/(k*p)^2}) <br>
     ^y <br>
     |<br>
	 |<br>
	 |<br>
	 |<br>
	 |<br>
	 ------------------------->  x <br>
	 */
	public static double getGaussianValue(int x, int y, double s, double p, double k) {
		// y inverse
		if(isDoubleZero(p) || isDoubleZero(k)) {
			throw new IllegalArgumentException("p or k can't be zero!");
		}
		double L = x * Math.cos(s) + y * Math.sin(s);
		L *= L;
		L /= (p*p);
		double R = x * Math.sin(s) + y * Math.cos(s);
		R *= R;
		R /= ((k*p) * (k*p));
		return Math.exp(-0.5 * (L + R));
	}
	
	static boolean isDoubleZero(double d) {
		return Math.abs(d-0.0) < 0.000001; 
	}
	
	public static void main(String[] args) {
		DecimalFormat df = new DecimalFormat("#######0.0000");
		for(int y=-5; y<=5; y++) {
			System.out.println();
			System.out.println();
			for(int x=-5; x<=5; x++) {
				double s = Math.PI/4;// Math.PI/4;
				double v = getGaussianValue(x, y, s, 1, 2);
				System.out.print("   " + df.format(v));
			}
		}
	}
 	
}
