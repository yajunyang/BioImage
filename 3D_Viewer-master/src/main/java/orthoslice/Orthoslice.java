
package orthoslice;

import ij.ImagePlus;

import java.util.BitSet;

import javax.media.j3d.Group;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture2D;
import javax.media.j3d.View;
import javax.vecmath.Color3f;

import voltex.VolumeRenderer;

/**
 * Orthoslice extends VolumeRenderer and modifies it in a way so that not the
 * whole volume is displayed, but only three orthogonal slices through the
 * volume.
 *
 * @author Benjamin Schmid
 */
public class Orthoslice extends VolumeRenderer {

	/** The indices of the currently displayed slices */
	private final int[] slices = new int[3];

	/** The dimensions in x-, y- and z- direction */
	private final int[] dimensions = new int[3];

	/** Flag indicating which planes are visible */
	private final boolean[] visible = new boolean[3];

	/** The visible children of the axis Switch in VolumeRenderer */
	private final BitSet whichChild = new BitSet(6);

	/**
	 * Initializes a new Orthoslice with the given image, color, transparency and
	 * channels. By default, the slice indices go through the center of the image
	 * stack.
	 *
	 * @param img The image stack
	 * @param color The color this Orthoslice should use
	 * @param tr The transparency of this Orthoslice
	 * @param channels A boolean[] array which indicates which color channels to
	 *          use (only affects RGB images). The length of the array must be 3.
	 */
	public Orthoslice(final ImagePlus img, final Color3f color, final float tr,
		final boolean[] channels)
	{
		super(img, color, tr, channels);
		getVolume().setAlphaLUTFullyOpaque();
		dimensions[0] = img.getWidth();
		dimensions[1] = img.getHeight();
		dimensions[2] = img.getStackSize();
		for (int i = 0; i < 3; i++) {
			slices[i] = dimensions[i] / 2;
			visible[i] = true;
			whichChild.set(i, true);
			whichChild.set(i + 3, true);
		}
	}

	/**
	 * Overwrites loadAxis() in VolumeRenderer to show only one plane in each
	 * direction.
	 * 
	 * @param axis Must be one of X_AXIS, Y_AXIS or Z_AXIS in VolumeRendConstants.
	 */
	@Override
	protected void loadAxis(final int axis) {

		final Group front = (Group) axisSwitch.getChild(axisIndex[axis][FRONT]);
		final Group back = (Group) axisSwitch.getChild(axisIndex[axis][BACK]);
		final int i = slices[axis];
		loadAxis(axis, i, front, back);
	}

	/**
	 * Override eyePtChanged() in VolumeRenderer to always show all slices.
	 * 
	 * @param view
	 */
	@Override
	public void eyePtChanged(final View view) {
		axisSwitch.setWhichChild(Switch.CHILD_MASK);
		axisSwitch.setChildMask(whichChild);
	}

	/**
	 * Returns the current index of the specified plane
	 * 
	 * @param axis
	 * @return
	 */
	public int getSlice(final int axis) {
		return slices[axis];
	}

	/**
	 * Returns whether the specified plane is visible at the moment
	 * 
	 * @param axis
	 * @return
	 */
	public boolean isVisible(final int axis) {
		return visible[axis];
	}

	/**
	 * Sets the specified plane visible.
	 * 
	 * @param axis
	 * @param b
	 */
	public void setVisible(final int axis, final boolean b) {
		if (visible[axis] != b) {
			visible[axis] = b;
			whichChild.set(axisIndex[axis][FRONT], b);
			whichChild.set(axisIndex[axis][BACK], b);
			axisSwitch.setChildMask(whichChild);
		}
	}

	/**
	 * Decreases the index of the specified plane by one.
	 * 
	 * @param axis
	 */
	public void decrease(final int axis) {
		setSlice(axis, slices[axis] - 1);
	}

	/**
	 * Increases the index of the specified plane by one.
	 * 
	 * @param axis
	 */
	public void increase(final int axis) {
		setSlice(axis, slices[axis] + 1);
	}

	/**
	 * Sets the slice index of the specified plane to the given value.
	 * 
	 * @param axis
	 * @param v
	 */
	public void setSlice(final int axis, final int v) {
		if (v >= dimensions[axis] || v < 0) return;
		slices[axis] = v;
		final Group g = (Group) axisSwitch.getChild(axisIndex[axis][FRONT]);
		final int num = g.numChildren();
		if (num > 1) System.out.println(num + " children, expected only 1");
		final Shape3D shape = (Shape3D) ((Group) g.getChild(num - 1)).getChild(0);

		final double[] quadCoords = geomCreator.getQuadCoords(axis, v);
		((QuadArray) shape.getGeometry()).setCoordinates(0, quadCoords);

		final Texture2D tex = appCreator.getTexture(axis, v);
		shape.getAppearance().setTexture(tex);
		final TexCoordGeneration tg = appCreator.getTg(axis);
		shape.getAppearance().setTexCoordGeneration(tg);
	}
}
