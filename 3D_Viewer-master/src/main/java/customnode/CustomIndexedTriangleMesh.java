
package customnode;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.process.StackConverter;

import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import vib.InterpolatedImage;

public class CustomIndexedTriangleMesh extends CustomMesh {

	protected Point3f[] vertices;
	protected Color3f[] colors;
	protected int[] faces;
	protected int nFaces;
	protected int nVertices;

	public CustomIndexedTriangleMesh(final List<Point3f> mesh) {
		// TODO
	}

	public CustomIndexedTriangleMesh(final Point3f[] vertices, final int[] faces)
	{
		this(vertices, faces, DEFAULT_COLOR, 0);
	}

	public CustomIndexedTriangleMesh(final Point3f[] vertices, final int[] faces,
		final Color3f color, final float transp)
	{
		this.nVertices = vertices.length;
		this.nFaces = faces.length;
		this.vertices = vertices;
		this.faces = faces;
		if (color != null) setColor(color);
		this.transparency = transp;
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		update();
	}

	@Override
	public String getFile() {
		return loadedFromFile;
	}

	@Override
	public String getName() {
		return loadedFromName;
	}

	@Override
	public boolean hasChanged() {
		return changed;
	}

	@Override
	public void update() {
		this.setGeometry(createGeometry());
		this.setAppearance(createAppearance());
		changed = true;
	}

	@Override
	public List getMesh() {
		return mesh;
	}

	@Override
	public Color3f getColor() {
		return color;
	}

	@Override
	public float getTransparency() {
		return transparency;
	}

	@Override
	public boolean isShaded() {
		return shaded;
	}

	@Override
	public void setShaded(final boolean b) {
		this.shaded = b;
		final PolygonAttributes pa = getAppearance().getPolygonAttributes();
		if (b) pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}

	@Override
	public void calculateMinMaxCenterPoint(final Point3f min, final Point3f max,
		final Point3f center)
	{

		if (vertices == null || nVertices == 0) {
			min.set(0, 0, 0);
			max.set(0, 0, 0);
			center.set(0, 0, 0);
			return;
		}

		min.x = min.y = min.z = Float.MAX_VALUE;
		max.x = max.y = max.z = Float.MIN_VALUE;
		for (int i = 0; i < nVertices; i++) {
			final Point3f p = vertices[i];
			if (p.x < min.x) min.x = p.x;
			if (p.y < min.y) min.y = p.y;
			if (p.z < min.z) min.z = p.z;
			if (p.x > max.x) max.x = p.x;
			if (p.y > max.y) max.y = p.y;
			if (p.z > max.z) max.z = p.z;
		}
		center.x = (max.x + min.x) / 2;
		center.y = (max.y + min.y) / 2;
		center.z = (max.z + min.z) / 2;
	}

	@Override
	public float getVolume() {
		throw new IllegalArgumentException("Not supported yet");
	}

	// private int[] valid = new int[1];
	@Override
	protected void addVerticesToGeometryStripArray(final Point3f[] v) {
		throw new RuntimeException(
			"CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	protected void addVerticesToGeometryArray(final Point3f[] v) {
		throw new RuntimeException(
			"CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	public int[] vertexIndicesOfPoint(final Point3f p) {
		final int i = vertexIndexOfPoint(p);
		if (i == -1) return new int[] {};
		return new int[] { i };
	}

	public int vertexIndexOfPoint(final Point3f p) {
		for (int i = 0; i < nVertices; i++) {
			final Point3f v = vertices[i];
			if (p.equals(v)) return i;
		}
		return -1;
	}

	@Override
	public void setCoordinate(final int i, final Point3f p) {
		changed = true;
		vertices[i].set(p);
		((GeometryArray) getGeometry()).setCoordinate(i, p);
	}

	@Override
	public void setCoordinates(final int[] indices, final Point3f p) {
		changed = true;
		final GeometryArray ga = (GeometryArray) getGeometry();
		for (int i = 0; i < indices.length; i++) {
			ga.setCoordinate(indices[i], p);
			vertices[indices[i]].set(p);
		}
	}

	@Override
	public void recalculateNormals(final GeometryArray ga) {}

	@Override
	protected void addVertices(final Point3f[] v) {
		throw new RuntimeException(
			"CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	protected void removeVertices(final int[] indices) {
		throw new RuntimeException(
			"CustomIndexedTriangleMesh does not support adding vertices");
	}

	@Override
	public void setColor(final Color3f color) {
		if (this.colors == null || this.colors.length != this.vertices.length) this.colors =
			new Color3f[this.vertices.length];
		this.color = color != null ? color : DEFAULT_COLOR;
		for (int i = 0; i < nVertices; i++)
			colors[i] = this.color;

		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;

		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setColor(final List<Color3f> color) {
		if (color.size() != colors.length) throw new IllegalArgumentException(
			"Number of colors must equal number of vertices");

		this.color = null;
		color.toArray(this.colors);

		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setColor(final int vtxIndex, final Color3f color) {
		this.color = null;
		this.colors[vtxIndex] = color;
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		ga.setColor(vtxIndex, color);
		changed = true;
	}

	@Override
	public void loadSurfaceColorsFromImage(ImagePlus imp) {
		if (imp.getType() != ImagePlus.COLOR_RGB) {
			imp = new Duplicator().run(imp);
			new StackConverter(imp).convertToRGB();
		}
		final InterpolatedImage ii = new InterpolatedImage(imp);

		final Calibration cal = imp.getCalibration();
		final double pw = cal.pixelWidth;
		final double ph = cal.pixelHeight;
		final double pd = cal.pixelDepth;
		for (int i = 0; i < nVertices; i++) {
			final Point3f coord = vertices[i];
			final int v =
				(int) Math.round(ii.interpol.get(coord.x / pw, coord.y / ph, coord.z /
					pd));
			colors[i] =
				new Color3f(((v & 0xff0000) >> 16) / 255f, ((v & 0xff00) >> 8) / 255f,
					(v & 0xff) / 255f);
		}
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setTransparency(final float transparency) {
		final TransparencyAttributes ta =
			getAppearance().getTransparencyAttributes();
		if (transparency <= .01f) {
			this.transparency = 0.0f;
			ta.setTransparencyMode(TransparencyAttributes.NONE);
		}
		else {
			this.transparency = transparency;
			ta.setTransparencyMode(TransparencyAttributes.FASTEST);
		}
		ta.setTransparency(this.transparency);
	}

	@Override
	protected Appearance createAppearance() {
		final Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		final PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		if (this.shaded) polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		appearance.setPolygonAttributes(polyAttrib);

		final ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		if (null != color) // is null when colors are vertex-wise
		colorAttrib.setColor(color);
		appearance.setColoringAttributes(colorAttrib);

		final TransparencyAttributes tr = new TransparencyAttributes();
		final int mode =
			transparency == 0f ? TransparencyAttributes.NONE
				: TransparencyAttributes.FASTEST;
		tr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		tr.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
		appearance.setTransparencyAttributes(tr);

		final Material material = new Material();
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.1f, 0.1f, 0.1f);
		material.setDiffuseColor(0.1f, 0.1f, 0.1f);
		appearance.setMaterial(material);
		return appearance;
	}

	@Override
	public void restoreDisplayedData(final String path, final String name) {
		throw new RuntimeException(
			"CustomIndexedTriangleMesh does not support swapping data.");
	}

	@Override
	public void swapDisplayedData(final String path, final String name) {
		throw new RuntimeException(
			"CustomIndexedTriangleMesh does not support swapping data.");
	}

	@Override
	public void clearDisplayedData() {
		throw new RuntimeException(
			"CustomIndexedTriangleMesh does not support swapping data.");
	}

	@Override
	protected GeometryArray createGeometry() {
		if (nVertices == 0) return null;
		final IndexedTriangleArray ta =
			new IndexedTriangleArray(vertices.length, GeometryArray.COORDINATES |
				GeometryArray.COLOR_3 | GeometryArray.NORMALS, faces.length);

		ta.setValidIndexCount(nFaces);

		ta.setCoordinates(0, vertices);
		ta.setColors(0, colors);

		ta.setCoordinateIndices(0, faces);
		ta.setColorIndices(0, faces);

		ta.setNormals(0, getNormals());
		ta.setNormalIndices(0, faces);

		ta.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		ta.setCapability(Geometry.ALLOW_INTERSECT);

		return ta;
	}

	public Vector3f[] getNormals() {
		final Vector3f[] normals = new Vector3f[nVertices];
		for (int i = 0; i < nVertices; i++)
			normals[i] = new Vector3f();

		final Vector3f v1 = new Vector3f(), v2 = new Vector3f();
		for (int i = 0; i < nFaces; i += 3) {
			final int f1 = faces[i];
			final int f2 = faces[i + 1];
			final int f3 = faces[i + 2];

			v1.sub(vertices[f2], vertices[f1]);
			v2.sub(vertices[f3], vertices[f1]);
			v1.cross(v1, v2);

			normals[f1].add(v1);
			normals[f2].add(v1);
			normals[f3].add(v1);
		}
		for (int i = 0; i < nVertices; i++)
			normals[i].normalize();

		return normals;
	}

}
