package yang.plugin.tracing;

import ij.IJ;
import imagescience.ImageScience;

final class Catcher implements Thread.UncaughtExceptionHandler {
	
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		
		IJ.log("Unexpected exception in "+NJ.NAME+" "+NJ.VERSION);
		IJ.log("Please send a copy of this message");
		IJ.log("and a description of how to reproduce it");
		IJ.log("to Erik Meijering: meijering@imagescience.org");
		IJ.log("OS version: "+System.getProperty("os.name")+" "+System.getProperty("os.version"));
		IJ.log("Java version: "+System.getProperty("java.version"));
		IJ.log("ImageJ version: "+IJ.getVersion());
		IJ.log("ImageScience version: "+ImageScience.version());
		IJ.log(t.toString());
		final java.io.CharArrayWriter cw = new java.io.CharArrayWriter();
		final java.io.PrintWriter pw = new java.io.PrintWriter(cw);
		e.printStackTrace(pw);
		IJ.log(cw.toString());
	}
	
}