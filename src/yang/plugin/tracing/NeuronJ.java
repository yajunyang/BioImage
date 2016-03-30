package yang.plugin.tracing;

import ij.IJ;
import ij.plugin.PlugIn;
import imagescience.ImageScience;

// Launches NeuronJ and together with its auxiliary classes takes care of handling all interactions.
public final class NeuronJ implements PlugIn {

	// Minimum version numbers:
	private final static String MINIJVERSION = "1.50a";
	private final static String MINISVERSION = "3.0.0";
	private final static String MINJREVERSION = "1.6.0";

	// Performs checks and launches the application:
	@Override
	public void run(final String arg) {

		// Check version numbers:
		if (System.getProperty("java.version").compareTo(MINJREVERSION) < 0) {
			NJ.error("This plugin requires Java version " + MINJREVERSION
					+ " or higher");
			return;
		}

		if (IJ.getVersion().compareTo(MINIJVERSION) < 0) {
			NJ.error("This plugin requires ImageJ version " + MINIJVERSION
					+ " or higher");
			return;
		}

		try { // This also works to check if ImageScience is installed
			if (ImageScience.version().compareTo(MINISVERSION) < 0)
				throw new IllegalStateException();
		} catch (Throwable e) {
			NJ.error("This plugin requires ImageScience version " + MINISVERSION
					+ " or higher");
			return;
		}

		// NeuronJ does not work in batch mode:
		if (IJ.getInstance() == null) {
			NJ.error("This plugin does not work in batch mode");
			return;
		}

		// Currently it is not possible to have multiple instances of NeuronJ
		// working in parallel:
		if (NJ.ntb != null) {
			NJ.notify(NJ.NAME + " is already running");
			return;
		}

		// Initialize program:
		NJ.init();
	}
}
