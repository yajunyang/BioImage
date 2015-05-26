
package ij3d;

import com.sun.j3d.utils.universe.SimpleUniverse;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.process.ByteProcessor;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.j3d.Background;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.J3DGraphics2D;
import javax.vecmath.Color3f;

public class ImageCanvas3D extends Canvas3D implements KeyListener {

	private final RoiImagePlus roiImagePlus;
	private final ImageCanvas roiImageCanvas;
	private Map<Integer, Long> pressed, released;
	private final Background background;
	private final UIAdapter ui;
	final private ExecutorService exec = Executors.newSingleThreadExecutor();

	protected void flush() {
		exec.shutdown();
	}

	private class RoiImagePlus extends ImagePlus {

		public RoiImagePlus(final String title, final ByteProcessor ip) {
			super();
			setProcessor(title, ip);
			pressed = new HashMap<Integer, Long>();
			released = new HashMap<Integer, Long>();
		}

		@Override
		public ImageCanvas getCanvas() {
			return roiImageCanvas;
		}
	}

	public ImageCanvas3D(final int width, final int height, final UIAdapter uia) {
		super(SimpleUniverse.getPreferredConfiguration());
		this.ui = uia;
		setPreferredSize(new Dimension(width, height));
		final ByteProcessor ip = new ByteProcessor(width, height);
		roiImagePlus = new RoiImagePlus("RoiImage", ip);
		roiImageCanvas = new ImageCanvas(roiImagePlus) {

			/* prevent ROI to enlarge/move on mouse click */
			@Override
			public void mousePressed(final MouseEvent e) {
				if (!ui.isMagnifierTool() && !ui.isPointTool()) super.mousePressed(e);
			}
		};
		roiImageCanvas.removeKeyListener(ij.IJ.getInstance());
		roiImageCanvas.removeMouseListener(roiImageCanvas);
		roiImageCanvas.removeMouseMotionListener(roiImageCanvas);
		roiImageCanvas.disablePopupMenu(true);

		background =
			new Background(new Color3f(UniverseSettings.defaultBackground));
		background.setCapability(Background.ALLOW_COLOR_WRITE);

		addListeners();
	}

	public Background getBG() { // can't use getBackground()
		return background;
	}

	public void killRoi() {
		roiImagePlus.killRoi();
		render();
	}

	void addListeners() {
		addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(final MouseEvent e) {
				if (ui.isRoiTool()) exec.submit(new Runnable() {

					@Override
					public void run() {
						postRender();
					}
				});
			}
		});
		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				if (ui.isRoiTool()) exec.submit(new Runnable() {

					@Override
					public void run() {
						render();
					}
				});
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				if (ui.isRoiTool()) exec.submit(new Runnable() {

					@Override
					public void run() {
						render();
					}
				});
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				if (!ui.isRoiTool()) roiImagePlus.killRoi();
			}
		});
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				exec.submit(new Runnable() {

					@Override
					public void run() {
						final ByteProcessor ip = new ByteProcessor(getWidth(), getHeight());
						roiImagePlus.setProcessor("RoiImagePlus", ip);
						render();
					}
				});
			}
		});
	}

	public ImageCanvas getRoiCanvas() {
		return roiImageCanvas;
	}

	public Roi getRoi() {
		return roiImagePlus.getRoi();
	}

	public void render() {
		stopRenderer();
		swap();
		startRenderer();
	}

	/*
	 * Needed for the isKeyDown() method. Problem:
	 * keyPressed() and keyReleased is fired periodically,
	 * dependent on the operating system preferences,
	 * even if the key is hold down.
	 */
	@Override
	public void keyTyped(final KeyEvent e) {}

	@Override
	public synchronized void keyPressed(final KeyEvent e) {
		final long when = e.getWhen();
		pressed.put(e.getKeyCode(), when);
	}

	@Override
	public synchronized void keyReleased(final KeyEvent e) {
		final long when = e.getWhen();
		released.put(e.getKeyCode(), when);
	}

	public synchronized void releaseKey(final int keycode) {
		pressed.remove(keycode);
		released.remove(keycode);
	}

	public synchronized boolean isKeyDown(final int keycode) {
		if (!pressed.containsKey(keycode)) return false;
		if (!released.containsKey(keycode)) return true;
		final long p = pressed.get(keycode);
		final long r = released.get(keycode);
		return p >= r || System.currentTimeMillis() - r < 100;
	}

	@Override
	public void postRender() {
		final J3DGraphics2D g3d = getGraphics2D();
		final Roi roi = roiImagePlus.getRoi();
		if (roi != null) {
			roi.draw(g3d);
		}
		g3d.flush(true);
	}
}
