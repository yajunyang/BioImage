/**
 * 对LinearFeature2D方法的计划性改进：
 * 	1， 不对 DifferDifferentiator进行更改
 * 	2， 在多尺度线性融合时，不针对每一个尺度增强结果分配存储空间。
 * 
 * @author yang
 * 2015/5/17
 */

package com.plugin.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import imagescience.feature.Differentiator;
import imagescience.image.Aspects;
import imagescience.image.Axes;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;

public class ALinearFeature3D implements PlugIn {

	private String scaleMinStr = "1.0";
	private String scaleStepStr = "0.2";
	private String scaleMaxStr = "3.0";

	@Override
	public void run(String arg) {
		Runtime.getRuntime().gc();
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return;
		if (imp.getNSlices() == 1) { // only deal with 3D condition
			IJ.showMessage("You need inpout a stack\n");
			return;
		}

		// The two objects share the actual image data.
		Image img = Image.wrap(imp);

		if (!showDialog())
			return;

		double scaleMin = Double.parseDouble(scaleMinStr);
		double scaleStep = Double.parseDouble(scaleStepStr);
		double scaleMax = Double.parseDouble(scaleMaxStr);

		MyHessian3D myHessian3D = new MyHessian3D();
		Image eigenImage = myHessian3D.run(img, scaleMin); // object share as
															// the optimal
															// result
		ImagePlus imps = eigenImage.imageplus();
		new ImageConverter(imps).convertToGray8();

		for (double scale = scaleMin + scaleStep; scale <= scaleMax; scale += scaleStep) {
			
			Image eigenImage1 = myHessian3D.run(img, scale);
			ImagePlus imps1 = eigenImage1.imageplus();
			new ImageConverter(imps1).convertToGray8();

			int w = imp.getWidth();
			int h = imp.getHeight();
			int d = imp.getStackSize();

			for (int z = 1; z <= d; z++)
				for (int y = 0; y < h; y++)
					for (int x = 0; x < w; x++) {
						int value = imps.getStack().getProcessor(z).get(x, y);
						int value1 = imps1.getStack().getProcessor(z).get(x, y);
						value = value > value1 ? value : value1; // 最大值融合方式
						imps.getStack().getProcessor(z).set(x, y, value1);
					}
		}

		imps.show();

	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("LinearFeature3D");
		gd.addStringField("The Min scale: ", scaleMinStr);
		gd.addStringField("The step scale:", scaleStepStr);
		gd.addStringField("The Max scale:", scaleMaxStr);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		scaleMinStr = gd.getNextString();
		scaleStepStr = gd.getNextString();
		scaleMaxStr = gd.getNextString();
		return true;
	}

}

class MyHessian3D {

	public final Differentiator differentiator = new Differentiator();

	public MyHessian3D() {}

	public Image run(final Image image, final double scale) {
		System.out.println(scale);
		if (scale <= 0)
			throw new IllegalArgumentException(
					"Smoothing scale less than or equal to 0");
		final Dimensions dims = image.dimensions();
		final Aspects asps = image.aspects();

		if (asps.x <= 0)
			throw new IllegalStateException(
					"Aspect-ratio value in x-dimension less than or equal to 0");
		if (asps.y <= 0)
			throw new IllegalStateException(
					"Aspect-ratio value in y-dimension less than or equal to 0");
		if (asps.z <= 0)
			throw new IllegalStateException(
					"Aspect-ratio value in z-dimension less than or equal to 0");
		/*
		 * It's very important to annotate whether it is a reference soft copy
		 * or a deep copy. The code shows if image is the type of FloatImage,
		 * just use the "object" if not, create a new FloatImage object which
		 * don't share the pixels data.
		 */
		final Image smoothImage = (image instanceof FloatImage) ? image
				: new FloatImage(image); // It's relationship of Deep Copy not just Reference
		
		final Image Hxx = differentiator.run(smoothImage.duplicate(), scale, 2, 0, 0);
		final Image Hxy = differentiator.run(smoothImage.duplicate(), scale, 1, 1, 0);

		final Image Hxz = differentiator.run(smoothImage.duplicate(), scale, 1, 0, 1);
		final Image Hyy = differentiator.run(smoothImage.duplicate(), scale, 0, 2, 0);
		final Image Hyz = differentiator.run(smoothImage.duplicate(), scale, 0, 1, 1);
		final Image Hzz = differentiator.run(smoothImage, scale, 0, 0, 2);

		Hxx.axes(Axes.X);
		Hxy.axes(Axes.X);
		Hxz.axes(Axes.X);
		Hyy.axes(Axes.X);
		Hyz.axes(Axes.X);
		Hzz.axes(Axes.X);
		final double[] ahxx = new double[dims.x];
		final double[] ahxy = new double[dims.x];
		final double[] ahxz = new double[dims.x];
		final double[] ahyy = new double[dims.x];
		final double[] ahyz = new double[dims.x];
		final double[] ahzz = new double[dims.x];
		final Coordinates coords = new Coordinates();

		for (coords.c = 0; coords.c < dims.c; ++coords.c)
			for (coords.t = 0; coords.t < dims.t; ++coords.t)
				for (coords.z = 0; coords.z < dims.z; ++coords.z)
					for (coords.y = 0; coords.y < dims.y; ++coords.y) {
						Hxx.get(coords, ahxx);
						Hxy.get(coords, ahxy);
						Hxz.get(coords, ahxz);
						Hyy.get(coords, ahyy);
						Hyz.get(coords, ahyz);
						Hzz.get(coords, ahzz);
						for (int x = 0; x < dims.x; ++x) {
							final double fhxx = ahxx[x];
							final double fhxy = ahxy[x];
							final double fhxz = ahxz[x];
							final double fhyy = ahyy[x];
							final double fhyz = ahyz[x];
							final double fhzz = ahzz[x];
							final double a = -(fhxx + fhyy + fhzz);
							final double b = fhxx * fhyy + fhxx * fhzz + fhyy
									* fhzz - fhxy * fhxy - fhxz * fhxz - fhyz
									* fhyz;
							final double c = fhxx * (fhyz * fhyz - fhyy * fhzz)
									+ fhyy * fhxz * fhxz + fhzz * fhxy * fhxy
									- 2 * fhxy * fhxz * fhyz;
							final double q = (a * a - 3 * b) / 9;
							final double r = (a * a * a - 4.5 * a * b + 13.5 * c) / 27;
							final double sqrtq = (q > 0) ? Math.sqrt(q) : 0;
							final double sqrtq3 = sqrtq * sqrtq * sqrtq;

							double absh1, absh2, absh3;
							double value1, value2, value3;
							if (sqrtq3 == 0) {
								absh1 = absh2 = absh3 = 0;
								value1 = value2 = value3 = 0;
							} else {
								final double rsqq3 = r / sqrtq3;
								final double angle = (rsqq3 * rsqq3 <= 1) ? Math.acos(rsqq3) : Math.acos(rsqq3 < 0 ? -1: 1);
								
								value1 = -2 * sqrtq * Math.cos(angle / 3) - a / 3;
								value2 = -2 * sqrtq * Math.cos((angle + TWOPI) / 3) - a / 3;
								value3 = -2 * sqrtq * Math.cos((angle - TWOPI) / 3) - a / 3;
								
								absh1 = Math.abs(value1);
								absh2 = Math.abs(value2);
								absh3 = Math.abs(value3);
							} // get the characteristic value's absolute value be ordered by |a1| >= |a2| >= |a3|

							if (absh2 < absh3) {
								final double tmp = value2;
								value2 = value3;
								value3 = tmp;
							}
							if (absh1 < absh2) {
								final double tmp1 = value1;
								value1 = value2;
								value2 = tmp1;
								if (absh2 < absh3) {
									final double tmp2 = value2;
									value2 = value3;
									value3 = tmp2;
								}
							}
							
							ahxx[x] = frangi(value1, value2, value3);
						}
						Hxx.set(coords, ahxx);
					}
		return Hxx;
	}

	private double frangi(double value1, double value2, double value3) {
		double value = 0;
		// Here |value1| >= |value2| >= |value3|
		if(value1 >= 0 || value2 >= 0)
			return 0;
		double Rb = Math.abs(value3) / (Math.sqrt(value1 * value2));
		double Ra = Math.abs(value2 / value1);
		double S = Math.sqrt(value1 * value1 + value2 * value2 + value3 * value3);
		double alpha = 2 * 0.5 * 0.5;
		double beta = 0.5 * 0.5;
		value = (1 - Math.exp(-Ra * Ra / alpha)) * (Math.exp(-Rb * Rb / beta)) * (1 - Math.exp(-S * S) / 2);
		return value;
	}

	private double frangi(double value1, double value2) {
		if(value1 >= 0 || value2 >= 0)
			return 0;
		return Math.sqrt(value1 * value2);
	}

	private static final double TWOPI = 2 * Math.PI;

}
