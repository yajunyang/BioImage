/**
 * This filter can be seen in the book "Digital Image Processing, Third Edition"
 * Rafael C.Gonzalez
 */

package com.plugin.segmentation;

import java.awt.Color;

import javax.swing.JOptionPane;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class HomomorphicFilter implements PlugInFilter {

	private int M, N, size, w, h;
	private ImagePlus imp;
	private FHT fht;
	private ImageProcessor mask, ipFilter;

	private int D0 = 80;
	private double gammaL = 0.25, gammaH = 2, c = 1;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	@Override
	public void run(ImageProcessor ip) {
		ip = imp.getProcessor();
		if(showDialog(ip)) {
			IJ.showProgress(0.0);
			filtering(ip, imp);
		}
		IJ.showProgress(1.0);
	}

	private boolean showDialog(ImageProcessor ip) {
		int dim = 0;
		M = ip.getWidth();
		N = ip.getHeight();
		if (M != N)
			dim = (int) (Math.min(M, N) / 2);
		else
			dim = M / 2;

		GenericDialog gd1 = new GenericDialog("Filter Parameters");
		gd1.addNumericField("High frequency parameter:", gammaH, 0);
		gd1.addNumericField("Low frequency parameter:", gammaL, 2);
		gd1.addNumericField("Coefficient c:", c, 0);
		gd1.addNumericField("D0", D0, 0);
		gd1.showDialog();

		if (gd1.wasCanceled())
			return false;
		if (gd1.invalidNumber()) {
			IJ.error("Error", "Invalid input number");
			return false;
		}

		gammaH = (double) gd1.getNextNumber();
		gammaL = (double) gd1.getNextNumber();
		c = (double) gd1.getNextNumber();
		D0 = (int) gd1.getNextNumber();

		if (D0 >= 0 && D0 <= dim)
			return true;
		else {
			GenericDialog gd2;
			boolean flag = true;
			while (flag) {
				D0 = 80;
				JOptionPane.showMessageDialog(null, "D0 must belong to [" + 0 + "," + dim + "]");
				
				gd2 = new GenericDialog("D0");
				gd2.addNumericField("D0", D0, 0);
				gd2.showDialog();
				if (gd2.wasCanceled() || gd2.invalidNumber())
					return false;
				else {
					D0 = (int) gd2.getNextNumber();
					if (D0 >= 0 && D0 <= dim)
						flag = false;
				}
			}
		}
		
		if (gammaL < 1 && gammaH > 1)
			return true;
		else {
			GenericDialog gd3;
			boolean flag = true;
			while (flag) {
				gammaH = 2;
				gammaL = 0.25;
				JOptionPane.showMessageDialog(
						null,
						"error, High freq. gamma must be less than 1 and low freq. gamma must be less than 1.");
				gd3 = new GenericDialog("Parameter correction");
				gd3.addNumericField("high frequency parameter:", gammaH, 0);
				gd3.addNumericField("Low frequency parameter:", gammaL, 0);
				gd3.showDialog();
				if (gd3.wasCanceled() || gd3.invalidNumber())
					return false;
				else {
					gammaH = (double) gd3.getNextNumber();
					gammaL = (double) gd3.getNextNumber();

					if (gammaL < 1 && gammaH > 1)
						flag = false;
				}
			}
		}
		return true;
	}

	// shows the power spectrum and filters the image
	public void filtering(ImageProcessor ip, ImagePlus imp) {
		int maxN = Math.max(M, N);
		size = 2;
		while (size < maxN) {
			size *= 2;
		}
		IJ.runPlugIn("ij.plugin.FFT", "forward");
		h = Math.round((size - N) / 2);
		w = Math.round((size - M) / 2);
		ImageProcessor ip2 = ip.createProcessor(size, size);
		ip2.fill();
		ip2.insert(ip, w, h);	// Put the image ip in the dark image's center part
		
		ImagePlus imagePlus = new ImagePlus("The image will be FFT transformed", ip2);
		imagePlus.show();
		
		if (ip instanceof ColorProcessor) {
			ImageProcessor bright = ((ColorProcessor) ip2).getBrightness();
			fht = new FHT(bright);
			fht.rgb = (ColorProcessor) ip.duplicate();
		} else {
			fht = new FHT(ip2);
		}
		
		IJ.showProgress(0.5);
		
		fht.originalColorModel = ip.getColorModel();
		fht.originalBitDepth = imp.getBitDepth();
		fht.transform(); // calculates the Fourier transformation

		ipFilter = Homomorphic();
		fht.swapQuadrants(ipFilter);
		byte[] pixels_id = (byte[]) ipFilter.getPixels();
		float[] pixels_fht = (float[]) fht.getPixels();

		for (int i = 0; i < size * size; i++) {
			pixels_fht[i] = (float) (pixels_fht[i] * (pixels_id[i] & 255) / 255.0);
		}

		mask = fht.getPowerSpectrum();
		ImagePlus imp2 = new ImagePlus("inverse FFT of " + imp.getTitle(), mask);
		imp2.setProperty("FHT", fht);
		imp2.setCalibration(imp.getCalibration());

		// Filter spectrum(swapped)
		FHT fht2 = new FHT();
		fht2.swapQuadrants(ipFilter);
		ImagePlus imp3 = new ImagePlus("Homomorphic filter", ipFilter);
		imp3.show();

		doInverseTransform(fht);
	}

	public ByteProcessor Homomorphic() {
		ByteProcessor ip = new ByteProcessor(M, N);
		double value = 0;
		double distance = 0;
		int xcenter = (M / 2) + 1;
		int ycenter = (N / 2) + 1;

		double xValues[] = new double[M];
		double yValues[] = new double[M];
		int k = 0;
		double max = 0;

		for (int y = 0; y < N; y++) {
			for (int x = 0; x < M; x++) {
				distance = Math.abs(x - xcenter) * Math.abs(x - xcenter)
						+ Math.abs(y - ycenter) * Math.abs(y - ycenter);
				distance = Math.sqrt(distance);
				value = (gammaH - gammaL)*(1 - Math.exp(-1*c*((distance*distance)/(D0*D0)))) + gammaL;
				value *= 255;
				ip.putPixelValue(x, y, value);

				if (y == ycenter) {
					xValues[k] = distance;
					if (max < distance)
						max = distance;

					// Adjust the scale of values
					yValues[k] = value / 255;
					k = k + 1;
				}
			}
		}
		ByteProcessor ip2 = new ByteProcessor(size,size);
        byte[] p = (byte[]) ip2.getPixels();
        for (int i=0; i<size*size; i++) p[i] = (byte)255;
        ip2.insert(ip, w, h);
	
        PlotWindow.noGridLines = false; // draw grid lines
        Plot plot = new Plot("1D filter profile", "D(u,v)","H(u,v)", xValues, yValues);
        plot.setLimits(0, max, 0, gammaH+1);
	
        plot.setLineWidth(1);
	
        float []x = {0, 0};
        float []y = {(float)gammaH, (float)gammaL};
        plot.setColor(Color.blue);
        plot.addPoints(x, y, PlotWindow.X);
       
        plot.setColor(Color.black);

        plot.show();
	
        return ip2;
	}
	
	void doInverseTransform(FHT fht) {
        fht = fht.getCopy();
        fht.inverseTransform();
        fht.resetMinAndMax();
        ImageProcessor ip2 = fht;
        fht.setRoi(w, h, M, N);
        ip2 = fht.crop();
        
        int bitDepth = fht.originalBitDepth>0?fht.originalBitDepth:imp.getBitDepth();
		switch (bitDepth) {
		case 8:
			ip2 = ip2.convertToByte(true);
			break;
		case 16:
			ip2 = ip2.convertToShort(true);
			break;
		case 24:
			if (fht.rgb == null || ip2 == null) {
				IJ.error("FFT", "Unable to set brightness");
				return;
			}
			ColorProcessor rgb = (ColorProcessor) fht.rgb.duplicate();
			rgb.setBrightness((FloatProcessor) ip2);
			ip2 = rgb;
			fht.rgb = null;
			break;
		case 32:
			break;
		}
		if (bitDepth != 24 && fht.originalColorModel != null)
			ip2.setColorModel(fht.originalColorModel);
		String title = imp.getTitle();
		if (title.startsWith("FFT of "))
			title = title.substring(7, title.length());
		ImagePlus imp2 = new ImagePlus("Inverse FFT of " + title, ip2);
		if (imp2.getWidth() == imp.getWidth())
			imp2.setCalibration(imp.getCalibration());
		imp2.show();
    }

}
