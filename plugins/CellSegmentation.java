package yang.plugin.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import imagescience.feature.Differentiator;
import imagescience.image.Aspects;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;

/**
 * 
 * @author yang 3/4/2015
 */
public class CellSegmentation implements PlugIn {

	private static String scaleMinStr = "1.0";
	private static String scaleStepStr = "0.2";
	private static String scaleMaxStr = "3.0";

	public static String beta = "0.5";
	public static String c = "15";

	@Override
	public void run(String arg) {
		Runtime.getRuntime().gc();
		final ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null)
			return;
		if (imp.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
		}
		if (imp.getNSlices() != 1)
			return;
		Image img = Image.wrap(imp);

		if (!showDialog())
			return;

		double scaleMin = Double.parseDouble(scaleMinStr);
		double scaleStep = Double.parseDouble(scaleStepStr);
		double scaleMax = Double.parseDouble(scaleMaxStr);

		MyHessian2D myHessian2D = new MyHessian2D();
		Image eigenImage = myHessian2D.run(img, scaleMin);

		ImagePlus imps = eigenImage.imageplus(); // share
		new ImageConverter(imps).convertToGray8();

		// Multi-scale method
		for (double scale = scaleMin + scaleStep; scale <= scaleMax; scale += scaleStep) {
			IJ.showProgress(scale / scaleMax);
			Image eigenImage1 = myHessian2D.run(img, scale);
			ImagePlus imps1 = eigenImage1.imageplus(); // share
			new ImageConverter(imps1).convertToGray8();

			int w = imp.getWidth();
			int h = imp.getHeight();
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++) {
					int value = imps.getProcessor().get(x, y);
					int value1 = imps1.getProcessor().get(x, y);
					value = value > value1 ? value : value1; // 最大值融合方式
					imps.getProcessor().set(x, y, value);
				}
		}
		IJ.showProgress(1.0);

		imps.setTitle("result");
		imps.show();
	}
	
	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("LinearFeature2D_1");
		gd.addStringField("scaleMin: ", scaleMinStr);
		gd.addStringField("scaleStep: ", scaleStepStr);
		gd.addStringField("scaleMax: ", scaleMaxStr);
		gd.addStringField("beta: ", beta);
		gd.addStringField("c: ", c);

		gd.showDialog();

		if (gd.wasCanceled())
			return false;

		scaleMinStr = gd.getNextString();
		scaleStepStr = gd.getNextString();
		scaleMaxStr = gd.getNextString();
		beta = gd.getNextString();
		c = gd.getNextString();

		return true;
	}
}

class MyHessianRev {
	public final Differentiator differentiator = new Differentiator();

	public Image run(final Image image, final double scale) {
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

		final Image smoothImage = (image instanceof FloatImage) ? image
				: new FloatImage(image);
		final Image Hxx = differentiator.run(smoothImage.duplicate(), scale, 2,
				0, 0);
		final Image Hxy = differentiator.run(smoothImage.duplicate(), scale, 1,
				1, 0);
		final Image Hyy = differentiator.run(smoothImage.duplicate(), scale, 0,
				2, 0);

		double ahxx, ahyy, ahxy;
		final Coordinates coords = new Coordinates();
		for (coords.c = 0; coords.c < dims.c; ++coords.c)
			for (coords.t = 0; coords.t < dims.t; ++coords.t)
				for (coords.y = 0; coords.y < dims.y; ++coords.y)
					for (coords.x = 0; coords.x < dims.x; ++coords.x) {
						ahxx = Hxx.get(coords);
						ahxy = Hxy.get(coords);
						ahyy = Hyy.get(coords);

						final double b = -(ahxx + ahyy);
						final double c = ahxx * ahyy - ahxy * ahxy;
						final double q = -0.5
								* (b + (b < 0 ? -1 : 1)
										* Math.sqrt(b * b - 4 * c));
						double lamubda1 = q;
						double lamubda2 = c / q;

						double abs1 = Math.abs(lamubda1);
						double abs2 = Math.abs(lamubda2);
						if (abs1 > abs2) {
							double temp = lamubda1;
							lamubda1 = lamubda2;
							lamubda2 = temp;
						} // Be sure |lamubda1| <= |lamubda2|
						double beta = Double.parseDouble(ALinearFeature2D.beta);
						double cValue = Double.parseDouble(ALinearFeature2D.c);

						ahxx = getFrangi(lamubda1, lamubda2, beta, cValue);
						Hxx.set(coords, ahxx);
					}
		Hxx.name(image.name() + " " + scale);
		return Hxx;
	}

	private double getFrangi(double lam1, double lam2, double beta, double c) {
		// |lam1| <= |lam2|
		if (lam2 > 0)
			return 0;

		double rb2 = (lam1 / lam2) * (lam1 / lam2);
		double b2 = 2 * beta * beta;
		double s2 = lam1 * lam1 + lam2 * lam2;
		double r = -s2 / (2 * c * c);

		double left = Math.exp(-rb2 / b2);
		double right = 1 - Math.exp(r);
		return left * right;
	}
}