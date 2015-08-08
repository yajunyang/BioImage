package com.plugin.segmentation;

import imagescience.feature.Differentiator;
import imagescience.image.Aspects;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;

public class AMyHessian2D {
	public final Differentiator differentiator = new Differentiator();

	public AMyHessian2D() {
	}

	/**
	 * This will not change the object {@link image}
	 * 
	 * @param image
	 * @param scale
	 * @return a new Image with nothing with the input object {@link image}
	 */
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

		double rb2 = (lam1 / lam2) * (lam1 / lam2 );
		double b2 = 2 * beta * beta;
		double s2 = lam1 * lam1 + lam2 * lam2;
		double r = -s2 / (2 * c * c);

		double left = Math.exp(-rb2 / b2);
		double right = 1 - Math.exp(r);
		return left * right;
	}

}
