package yang;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class HugeImage {
	
	private BufferedImage subImage;
	private int x;
	private int y;
	private int w;
	private int h;
	
	public HugeImage(final Rectangle rect) {
		x = rect.x;
		y = rect.y;
		w = rect.width;
		h = rect.height;
	}
	
	public void execute(String file) {
		BufferedImage image = null;
		
		try {
			Rectangle subRegion = new Rectangle(x, y, w, h);
			File input = new File(file);
			ImageInputStream stream = ImageIO.createImageInputStream(input);
			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			
			if(readers.hasNext()) {
				ImageReader reader = readers.next();
				reader.setInput(stream);
				
				ImageReadParam param = reader.getDefaultReadParam();
				param.setSourceRegion(subRegion);
				
				image = reader.read(0, param);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		subImage = image;
	}
	
	public BufferedImage getSubImage() {
		return subImage;
	}
	
	public static void main(String[] args) {
		int x = 16000;
		int y = 15000;
		int width = 1000;
		int height = 1000;
		String sourcePath = "E:/Medical Images/Diadem/Hippocampal CA3 Interneuron Part_1/Neuron 1/Image Stacks/Section 1";
		String targetPath = "E:/Data/diadem/";
		int min = 2;
		int max = 10;
		
		HugeImage h = new HugeImage(new Rectangle(x, y, width, height));
		for(int i = min; i <= max; i++) {
			String path = sourcePath + i + ".tif";
			h.execute(path);
			BufferedImage subImage = h.getSubImage();
			try {
				ImageIO.write(subImage, "jpg", 
						new File(targetPath + i + ".jpg"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			Runtime.getRuntime().gc();
		}
	}
}
