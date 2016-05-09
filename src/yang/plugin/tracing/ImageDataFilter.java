package yang.plugin.tracing;

import java.io.File;
import java.io.FilenameFilter;

final class ImageDataFilter implements FilenameFilter {
	
	@Override
	public boolean accept(File dir, String name) {
		
		final String ext = name.substring(name.lastIndexOf(".")+1);
		if (ext.equalsIgnoreCase("tif") ||
			ext.equalsIgnoreCase("tiff") ||
			ext.equalsIgnoreCase("gif") ||
			ext.equalsIgnoreCase("jpg") ||
			ext.equalsIgnoreCase("ndf") ||
			ext.equalsIgnoreCase("txt")) return true;
		else return false;
	}
	
}