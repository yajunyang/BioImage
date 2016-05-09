package yang.plugin.tracing;

import java.io.File;
import java.io.FilenameFilter;

final class ImageFilter implements FilenameFilter {
	
	@Override
	public boolean accept(File dir, String name) {
		
		final String ext = name.substring(name.lastIndexOf(".")+1);
		if (ext.equalsIgnoreCase("tif") ||
			ext.equalsIgnoreCase("tiff") ||
			ext.equalsIgnoreCase("gif") ||
			ext.equalsIgnoreCase("jpg")) return true;
		else return false;
	}
	
}