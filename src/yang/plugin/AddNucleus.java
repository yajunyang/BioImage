package yang.plugin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.omg.CORBA.PUBLIC_MEMBER;

import ij.IJ;
import ij.plugin.PlugIn;

public class AddNucleus implements PlugIn{

	@Override
	public void run(String arg) {
		String note = "How to add the Nucleus on the linear feature images?\n"
				+ " " + "\n"
				+ "First: generate the Nucleus.\n"
				+ "Use the source gray image to sub the linear feature image.\n"
				+ "Use Image/Adjust/Brightness/Contrast 'contrast' extract Nucleus.\n"
				+ "Use 'Clear' to clear isolated points.\n"
				+ "Use NLM filter to eliminate noise.\n"
				+ "Add the Nucleus image to the linear feature image.\n"
				+ "\n"
				+ "We find it works not good, for there is no relationship between these two images.\n"
				+ "We can try to use the information of the Nucleus's edge information and the linear \n"
				+ "feature image's 'circle' information.\n"
				+ " " + "\n"
				+ "2015/5/26";
		IJ.showMessage(note);
	}
	
	public static void main(String[] args) {
		BufferedImage image = new BufferedImage(500,500, BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = image.getGraphics();
		g.setColor(new Color(150, 150, 150));
		g.drawLine(50, 50, 200, 200);
		g.drawLine(200, 400, 400, 200);
		
		
		class ATest{
			
			/**
			 * A Test of inner class 
			 */
			public void test(){
				
			}
		}
		
		ATest a = new ATest(){
			private int a = 10;
			
			@Override
			public void test(){
				System.out.println("AAAA");
				System.out.println(a);
			}
		};
		
		a.test();
		
		try {
			ImageIO.write(image, "jpg", new File("res/tt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
