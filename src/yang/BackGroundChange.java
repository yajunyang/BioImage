package yang;

import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class BackGroundChange implements PlugIn{

	private ImagePlus ips;
	
	@Override
	public void run(String arg) {
		ips = WindowManager.getCurrentImage();
		if(null == ips) 
			return;
		
		
		
	}

}
