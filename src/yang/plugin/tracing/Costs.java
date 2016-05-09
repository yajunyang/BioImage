package yang.plugin.tracing;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import imagescience.feature.Differentiator;
import imagescience.image.Dimensions;
import imagescience.image.Image;
import imagescience.utility.Progressor;

final class Costs {
	
	// Returns a cost image and vector field computed from the
	// eigenvalues and eigenvectors of the Hessian of the input
	// image. The second and third index correspond, respectively, to
	// the y- and x-coordinate. The first index selects the image:
	// element 0 contains the cost image, element 1 the x-component of
	// the vector field, and element 2 the y-component of the vector
	// field. The gray-value at any point in the cost image is computed
	// from the eigenvalues of the Hessian matrix at that
	// point. Specifically, the method computes both (adjusted)
	// eigenvalues and selects the one with the largest magnitude. Since
	// in the NeuronJ application we are interested in bright structures
	// on a dark background, the method stores this absolute eigenvalue
	// only if the actual eigenvalue is negative. Otherwise it stores a
	// zero. The eventual largest-eigenvalue image is inverted and
	// scaled to gray-value range [0,255]. The vector at any point in
	// the vector field is simply the eigenvector corresponding to the
	// largest absolute eigenvalue at that point.
	public float[][][] run(final ByteProcessor image, final boolean bright, final float scale) {
		
		NJ.log("Cost image and vector field from Hessian at scale "+scale+" ...");
		final Progressor pgs = new Progressor();
		pgs.display(true); pgs.enforce(true);
		
		// Compute Hessian components:
		pgs.status("Computing derivatives...");
		final Image inImage = Image.wrap(new ImagePlus("",image));
		final Differentiator differ = new Differentiator();
		differ.progressor.parent(pgs);
		pgs.range(0.0,0.3); final Image Hxx = differ.run(inImage,scale,2,0,0);
		pgs.range(0.3,0.6); final Image Hxy = differ.run(inImage,scale,1,1,0);
		pgs.range(0.6,0.9); final Image Hyy = differ.run(inImage,scale,0,2,0);
		
		// Compute and select adjusted eigenvalues and eigenvectors:
		pgs.status("Computing eigenimages...");
		final Dimensions dims = inImage.dimensions();
		pgs.steps(dims.y); pgs.range(0.9,1.0); pgs.start();
		final float[] ahxx = (float[])Hxx.imageplus().getStack().getPixels(1);
		final float[] ahxy = (float[])Hxy.imageplus().getStack().getPixels(1);
		final float[] ahyy = (float[])Hyy.imageplus().getStack().getPixels(1);
		final float[][][] evv = new float[3][dims.y][dims.x];
		final float[][] value = evv[0];
		final float[][] vectx = evv[1];
		final float[][] vecty = evv[2];
		final float inv = bright ? 1 : -1;
		for (int y=0, i=0; y<dims.y; ++y) {
			for (int x=0; x<dims.x; ++x, ++i) {
				final float b1 = inv*(ahxx[i] + ahyy[i]);
				final float b2 = inv*(ahxx[i] - ahyy[i]);
				final float d = (float)Math.sqrt(4*ahxy[i]*ahxy[i] + b2*b2);
				final float L1 = (b1 + 2*d)/3.0f;
				final float L2 = (b1 - 2*d)/3.0f;
				final float absL1 = Math.abs(L1);
				final float absL2 = Math.abs(L2);
				if (absL1 > absL2) {
					if (L1 > 0) value[y][x] = 0;
					else value[y][x] = absL1;
					vectx[y][x] = b2 - d;
				} else {
					if (L2 > 0) value[y][x] = 0;
					else value[y][x] = absL2;
					vectx[y][x] = b2 + d;
				}
				vecty[y][x] = 2*inv*ahxy[i];
			}
			pgs.step();
		}
		pgs.stop();
		
		// Convert eigenvalues to costs and normalize eigenvectors:
		float minval = value[0][0];
		float maxval = minval;
		for (int y=0; y<dims.y; ++y)
			for (int x=0; x<dims.x; ++x)
				if (value[y][x] > maxval) maxval = value[y][x];
				else if (value[y][x] < minval) minval = value[y][x];
		final float roof = 255;
		final float offset = 0;
		final float factor = (roof - offset)/(maxval - minval);
		final byte[] pixels = (byte[])image.getPixels();
		for (int y=0, i=0; y<dims.y; ++y)
			for (int x=0; x<dims.x; ++x, ++i) {
				value[y][x] = roof - (value[y][x] - minval)*factor;
				final float vectlen = (float)Math.sqrt(vectx[y][x]*vectx[y][x] + vecty[y][x]*vecty[y][x]);
				if (vectlen > 0) { vectx[y][x] /= vectlen; vecty[y][x] /= vectlen; }
			}
		
		return evv;
	}
	
}
