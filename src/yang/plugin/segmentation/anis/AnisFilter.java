package yang.plugin.segmentation.anis;

import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class AnisFilter implements PlugIn{
	
	@Override
	public void run(String arg0) {
		ImagePlus ips = WindowManager.getCurrentImage();
		if(null == ips) {
			IJ.noImage();
			return;
		}
		if(ips.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only 8-bits gray image");
			return;
		}
		
		DirectionDetectHessian direct = new DirectionDetectHessian(ips.getProcessor());
		direct.run();
		int[][] degree = direct.getDegree();
		
		ImageProcessor[] filters = filter(ips.getProcessor(), degree, true);
		new ImagePlus("AnsMean", filters[0]).show();
		new ImagePlus("AnsMed", filters[1]).show();
		new ImagePlus("Mean", filters[2]).show();
		new ImagePlus("Med", filters[3]).show();
//		print2DArray(degree);
		
	}
	
	public ImageProcessor[] filter(ImageProcessor ip, int[][] degrees, boolean test) {
		ImageProcessor[] ips = new ImageProcessor[4];
		int p;
		ips[0] = ip.duplicate();
		ips[1] = ip.duplicate();
		ips[2] = ip.duplicate();
		ips[3] = ip.duplicate();		
		for(int y=0, h=ip.getHeight(); y<h; y++){
			for(int x=0, w=ip.getWidth(); x<w; x++) {
				// AnisKernel
				int degree = degrees[y][x];	// 二维 正 坐标系的关系
				int d = getKernelDegree(degree);
				int[] ks = get1DKernelPixels(ip, x, y, d);
				
				// AnisMean
				int sum = 0;
				if(ks != null) {
					for(int i=0, l = ks.length; i<l; i++) {
						sum += ks[i];
					}
					p = (int)((double)sum / ks.length);
					ips[0].putPixel(x, y, p);
				} else {
					if(test) {
//						ips[0].putPixel(x, y, 0);
						continue;
					} else {					
						int[] kT = get1DKernelPixels(ip, x, y, -2);	// 3x3 kernel
						sum = 0;
						for(int i=0, l = kT.length; i<l; i++) {
							sum += kT[i];
						}
						p = (int)((double)sum / kT.length);
						ips[0].putPixel(x, y, p);
					}
				}
				
				// AnisMed
				if(ks != null) {
					Arrays.sort(ks);
					p = ks[ks.length/2];
					ips[1].putPixel(x, y, p);
				} else {
					if(test) {
//						ips[1].putPixel(x, y, 0);
						continue;
					} else {
						int[] kT = get1DKernelPixels(ip, x, y, -2);	// 3x3 kernel
						Arrays.sort(kT);
						p = kT[kT.length/2];
						ips[1].putPixel(x, y, p);
					}
				}
				
				// Kernel
				ks = get1DKernelPixels(ip, x, y, -2);	// 3x3 kernel
				
				// Mean
				sum = 0;
				for(int i=0, l = ks.length; i<l; i++) {
					sum += ks[i];
				}
				p = (int)((double)sum / ks.length);
				ips[2].putPixel(x, y, p);
				
				// Med
				Arrays.sort(ks);
				p = ks[ks.length/2];
				ips[3].putPixel(x, y, p);
			}
		}
		return ips;
	}
	
	
	int[] get1DKernelPixels(ImageProcessor ip, int x, int y, int d) {		
		int[] ks = null;
		
		if(d == -1) {
			return null;
		}
		
		// 3x3
		if(d == -2) {
			ks = new int[9];
			for(int j=0; j<3; j++) {
				for(int i=0; i<3; i++) {
					int v = ip.getPixel(x+i-1, y+j-1);
					ks[i + j * 3] = v;
				}
			}
		}
		
		// -------------------------------> x
		// |
		// |
		// |         
		// |        =======       
		// |        ===X===		0 度
		// |        =======
		// |
		// |
		// v y
		if(d == 0) {
			ks = new int[21];
			for(int j=0; j<3; j++) {
				for(int i=0; i<7; i++) {
					int v = ip.getPixel(x+i-3, y+j-1);
					ks[i + j * 7] = v;
				}
			}
		}
		
		// -------------------------------> x
		// |              ==
		// |            ===
		// |           ===  
		// |          =X=             45 度 
		// |        ===
		// |       ===
		// |      == 
		// |
		// v y
		
		if(d == 45) {
			ks = new int[19];
			for(int i=0; i<6; i++) {
				int v = ip.getPixel(x-3+i, y+2-i);
				ks[i] = v;
			}
			for(int i=0; i<7; i++) {
				int v = ip.getPixel(x-3+i, y+3-i);
				ks[6+i] = v;
			}
			for(int i=0; i<6; i++) {
				int v = ip.getPixel(x-3+i, y+4-i);
				ks[13+i] = v;
			}
			return ks;
		}
		
		
		// ------------------------------->  x
		// |             
		// |       ==    
		// |        ===   
		// |         ===             -45 度
		// |          =X=
		// |           ===
		// |            ===
		// |             ==
		// v y 
		if(d == -45) {
			ks = new int[19];
			for(int i=0; i<6; i++) {
				int v = ip.getPixel(x-3+i, y-2+i);
				ks[i] = v;
			}
			for(int i=0; i<7; i++) {
				int v = ip.getPixel(x-3+i, y-3+i);
				ks[6+i] = v;
			}
			for(int i=0; i<6; i++) {
				int v = ip.getPixel(x-3+i, y-4+i);
				ks[13+i] = v;
			}
		}
		
		// ------------------------------->
		// |           
		// |          ===
		// |          ===
		// |          ===
		// |          =X=          90 度
		// |          ===
		// |          ===
		// |          ===
		// |
		// v
		if(d == 90) {
			ks = new int[21];
			for(int j=0; j<7; j++) {
				for(int i=0; i<3; i++) {
					int v = ip.getPixel(x+i-1, y+j-3);
					ks[i + j * 3] = v;
				}
			}
		}
		return ks;
	}
	
	// 正坐标系  y朝上，x朝右，根据像素方向的范围选取对应滤波核的方向 
	int getKernelDegree(int degree) {
		if(degree == -200) return -1;
		if(Math.abs(degree) <= 22.5) return 0;
		if(degree > 22.5 && degree <= 67.5) return 45;
		if(degree >= -67.5 && degree < -22.5) return -45;
		else return 90;
	}

	public void print2DArray(int[][] array) {
		int w = array[0].length;
		int h = array.length;
		
		for(int i=0; i<h; i++) {
			for(int j=0; j<w; j++) {
				if(array[i][j] == -200) 
					System.out.println("*" + "         ");
				else 
					System.out.print(array[i][j] + "		");
			}
			System.out.println();
		}
	}
	
}
