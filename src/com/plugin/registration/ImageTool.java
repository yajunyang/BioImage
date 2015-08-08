/*
 * 2015/5/9
 * Author: yajun yang
 * 
 * ***********************************************************************************
 * If you want to use this class
 * Pleas notice:
 * 
 * Set the parameter resolution/deltaX/deltaY 	
 * 
 * Put the "high-resolution" images under the file ""res/registration/data/".
 * 		  You must put the images in order and named by 2 3 4 5 6....n
 * 
 * **************************************************************************************
 *
 * Notice:
 * 	For we need to multiply all the matrix if we want to register with the first picture,
 *  This is  a coordinate transform,
 *  There is no problem in math M1 M2 M3 x = M1 M2 x1
 *  But there is problem in program!!! 
 *  This is the problem of precision 
 *  For the value cos sin in matrix is similar and small such as 0.9999999999.....
 *  But the coordinate value x y z is much big 
 *  So we cam't multiply all the matrix and then to multiply the coordinate x
 * 
 *   You can see this change in the method "transform()"
 */

/*
 * 2015/4/15
 * 
 * 本类是5月10号前完成的 基本工作，当时试图设计一种由低精度图像配准高精度图像的方法。
 * 		在StackReg_或新给的MultiStackReg_程序中，脱离程序的复杂理解之外，可以对图像进行多种方法
 * 		的配准。我们通用的方式是刚体变换方法，至于用的相似性配准法则则是：
 * 		A Pyramid Approach to Subpixel Registration Based on Intensity这篇论文中提到的。
 * 		从这个MultiStackReg_方法中，除了得到已经配准的图像外，还可以得到相应的六个点
 * 		targetPoints 和 sourcePoints
 * 	        这六个点展示了怎么样由一幅图像变换到另一幅图像，如果单从刚体变换的角度分析这六个点，
 * 		实际上是两条距离相等的直线。
 * 		Mx = x0 如果 M只是旋转、平移变换，实际上分别有两个点就可以求取。
 * 
 * 		那么？对于非刚体变换，是不是所有的点的变换都是采用 M 时不变矩阵？
 * 		假设 M 都是 3x3矩阵，对于不变的M, 任意的三个点构成的非奇异矩阵X X0都可以解出 M
 * 
 * 		在10号以前的工作中，用了两个假设？假设对于同一幅图（相同原点，不同分辨率），假设得到的 低分辨率图像 的“ 配准点 ”坐标为 x ,那么对应高分辨率图像的 “ 配准点 ” 的坐标可以为
 * 		r 乘以 x， 其中 r 为高分辨率对低分辨率的倍数。
 * 		在得到的高分辨率坐标中的 “ 配准点  ”后，将坐标系进行平移，新的坐标系原点对应我们从高分辨率图像上 “ 平行截取  ”的感兴趣区域子图像的原点。
 * 		相应的新的子图像（也就是我们最终要配准的图像） 的 “配准点” 可以由 点 rx 进行平移得到。
 * 		最后根据 “ 配准点 ”，求取变换矩阵，在这里可以参考 AlignImage中的变换方法或 TJ 中的方法，本人写的这个变换结果有时候有规则的噪声点。
 * 		
 * 		缺点：
 * 			因为得到的  “ 配准点  ” 是基于原始的相邻两幅图像的，理论上，对于堆栈图像，要想和第一幅图像配准，矩阵 M 需要进行阶乘，
 * 			这在transform()方法中已经做出了说明，并给出了改进，但是对于坐标很大，且感兴趣子图像很小的高精度神经图像来说，这种方法对在计算中精度误差
 * 			要求横额，所以虽然Lena测试时，可以较理想实现，但是对于很大的神经图像，效果很差。
 * 
 * 
 * 在10 ~ 15号的方法中，对低分辨率的全局图像进行配准，截取配准好了的低分辨率图的感兴趣子图像（当然，也是配准的），对该子图像进行插值，有线性插值和
 * 		cubic插值两种方法，原始子图像与插值放大后的图像进行配准，因为两幅图像几乎一样，除了位置和角度的某些变换，我们将两幅图像取阈值进而找到两对完全匹配的点，
 * 		通过AlignImage方法进行变换。结果如 E:\bioimage\实验\5\最终结果 所示。
 * 		这项工作与本类的纯粹坐标变换的配准方法没有关系。
 * 
 */

package com.plugin.registration;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;

public class ImageTool {
 	
	/**
	 * This is used for saving the transformation matrix.
	 * This will be cleaned when the "MultiStackReg_" plugin is over.
	 */
	public static Vector<Array2D> transArray = new Vector<>();	
	
	public static final int resolution = 4;
	public static final int deltaX = 40;
	public static final int deltaY = 40;
	
	private ImageProcessor ip = null;
	
	public static void main(String[] args) {
		String[] file1 = {"res/22.jpg"};
		String[] file2 = {"res/222.jpg"};
		cutROIs(file1, file2, 200, 100, 40, 40);
	}
	
	
	/*=============================Transform Method 1==========================*/
	
	/**
	 * Get the current rigid body transformation 3x3 matrix.
	 * @param source The points on the registered image or the image will be transformed
	 * @param target The points on the reference image or the image that the source image transforming to
	 * @param resolution Zoom in or Zoom out the source points and the target points.   
	 * @return
	 */
	public static double[][]  getTransArray(double[][] source, double[][] target,  float resolution, int deltaX, int deltaY) {
		
		double[][] trans = new double[3][3];
		
		/**
		 *Here is a very important transform and assumption.
		 *Assumption: assume the low-resolution images's registration points(sources and targets, targets are fixed, which corresponding to the reference image).
		 *Corresponding to the high-resolution images, assume the point(sources * resolution, targets * resolution) is the ones which are registered.
		 *Further, take a "transformation' transform, the new high-resolution coordinate system "on the big big big one...".The corresponding points will be
		 *(sources - deviation ,targets - deviation) and deviation is the distance of the new "small coordinate" with the original "big big coordinate".
		 */
		double x0 = resolution * source[0][0] - deltaX,   y0 = resolution * source[0][1] - deltaY,  x1 = resolution * source[1][0] - deltaX,  y1 = resolution *  source[1][1] - deltaY;
		double x0n  = resolution * target[0][0] - deltaX, y0n = resolution * target[0][1] - deltaY,  x1n = resolution * target[1][0] - deltaX, y1n = resolution * target[1][1] - deltaY;
		
		final double dx1 = x1 - x0;
		final double dy1 = y1 - y0;
		final double dx2 = x1n - x0n;
		final double dy2 = y1n - y0n;
		
		final double aTan = Math.atan2(dy1, dx1) - Math.atan2(dy2, dx2);
		final double a00 = (float) Math.cos(aTan);
		final double a10 = (float) Math.sin(aTan);
		final double a01 = (float) -Math.sin(aTan);
		final double a11 = (float) Math.cos(aTan);
		
		final double sourceX = x0 + dx1 / 2.0;
		final double sourceY = y0 + dy1 / 2.0;
		final double targetX = x0n + dx2 / 2.0;
		final double targetY = y0n + dy2 / 2.0;
		
		final double a02 = sourceX - a00 * targetX - a01 * targetY;
		final double a12 = sourceY - a10 * targetX - a11 * targetY;
			
		trans[0][0] = a00; trans[0][1] = a01; trans[0][2] = a02;
		trans[1][0] = a10; trans[1][1] = a11; trans[1][2] = a12;
		trans[2][0] = 0;   trans[2][1] = 0;	  trans[2][2] = 0;
		
		return trans;
	}
	
	
	/**
	 * Transform the image from "file" width matrix "M"
	 * @param file
	 * @param M
	 * @param deltaX  The distance deviation to x of the original coordinate where the transform matrix happens
	 * @param deltaY 
	 */
	public static void transform(String file, String resultFile, double[][] M) {
		
		try {
			BufferedImage image = ImageIO.read(new File(file));
			int width = image.getWidth();
			int height = image.getHeight();
			
			BufferedImage  targetImage = new BufferedImage(width, height, image.getType());
			
			Graphics g = targetImage.getGraphics();
			g.fillRect(0, 0, targetImage.getWidth(), targetImage.getHeight());
			
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++) {
					int xN = Math.round((float)(M[0][0] * x +  M[0][1] * y + M[0][2]));
					int yN = Math.round((float)(M[1][0] * y + M[1][1] * y + M[1][2]));
					
					if( xN < width && yN < height && xN >= 0  && yN >= 0) {
						targetImage.setRGB(xN, yN, image.getRGB(x, y));
					}
				}
			ImageIO.write(targetImage, "jpg", new File(resultFile));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	
	/**
	 * Assume the transArray is a vector whose length is "n = files.lenght - 1". it have n transformation matrix
	 * which every one is just representing a kind of relationship among two images(source images)neighbored.
	 * @param files
	 * @param saveFiles
	 * @param transArray
	 */
	public static void transform() {
		
		File file = new File("res/registration/data");
		String resultPath = "res/registration/result/";
		String dataPath = "res/registration/data/";
		
		File[] allFiles = file.listFiles();
		
		if(allFiles.length != transArray.size()) {
			System.err.println("You must put the image data under the file:" + file.getName());
			return;
		}
		String[] files = new String[allFiles.length];
		String[] saveFiles = new String[allFiles.length];
		
		for(int i=0; i < allFiles.length; i++) {
			files[i] = dataPath + (i+2) + ".jpg";
			saveFiles[i] = resultPath + (i+2);
		}
		if(files.length != saveFiles.length) throw new IllegalArgumentException();
		
		/**
  		factorialArray(transArray);   // It's not a good idea to multiply all the matrix!
		
		 THERE IS A BIG ERROR HERE!!!!!!!!!
		 For the value of cos sin may be very small
		 If we multiply all the matrix, the precision will be slow!!!!!!!!
		 It's better to multiply each matrix one by one, 
		
		       NOTICE: x, y value is big, however, the value of sin cos is much small
		*/
		
		for(int i=0; i<files.length; i++) {
			transform(files[i], saveFiles[i], transArray.get(i).getArray());
			
			/**
			 * This loop is a big change different with the use of the method 
			 * 	factorialArray(transArray)
			 */
			for(int k = i-1; k>=0; k--) {
				transform(saveFiles[i], saveFiles[i], transArray.get(k).getArray());
			}
//			transArray.get(i).displayArray();
		}
		transArray.clear();
		
	}
	
	
	@SuppressWarnings({ "unchecked", "unused" })
	@Deprecated
	private static void factorialArray(Object objectArrays) {
		
		Vector<Array2D > arrays = null;
		if(objectArrays instanceof Vector<?>) {
			 arrays = (Vector<Array2D>)objectArrays;
		} else
			throw new IllegalArgumentException();
			
		int length = arrays.size();
		for(int i=length-1; i > 0; i--) {
			Array2D array = arrays.get(i);
			for(int k = i-1; k >= 0; k--) {
				array.multiArray2D(arrays.get(k));
			}
		}
	}
	
	/*==========================================================================*/
	
	
	
	
	/*=============================Transform Method 2===========================*/
	
	public void transform2() {
		File file = new File("res/registration/data");
		String resultPath = "res/registration/result/";
		String dataPath = "res/registration/data/";
		
		File[] allFiles = file.listFiles();
		
		if(allFiles.length != transArray.size()) {
			System.err.println("You must put the image data under the file:" + file.getName());
			return;
		}
		String[] files = new String[allFiles.length];
		String[] saveFiles = new String[allFiles.length];
		
		for(int i=0; i < allFiles.length; i++) {
			files[i] = dataPath + (i+2) + ".jpg";
			saveFiles[i] = resultPath + (i+2) + ".jpg";
		}
		if(files.length != saveFiles.length) throw new IllegalArgumentException();

		// Notice here!!!		
		for(int i=0; i<files.length; i++) {
			transAndSave(files[i], saveFiles[i], transArray.get(i).getArray());
			for(int k = i-1; k>=0; k--) {
				transAndSave(saveFiles[i], saveFiles[i], transArray.get(k).getArray());
			}
			transArray.get(i).displayArray();
		}
		transArray.clear();
	}
	
	public void transAndSave(String sourceFile, String resultFile, double[][] M) {
		try {
			BufferedImage source = ImageIO.read(new File(sourceFile));
			if(source.getType() == BufferedImage.TYPE_BYTE_GRAY) 
				ip = new ByteProcessor(source);
			if(source.getType() == BufferedImage.TYPE_3BYTE_BGR) 
				ip = new ColorProcessor(source);
			
			int w = source.getWidth();
			int h = source.getHeight();
			
			final ImageProcessor result = new FloatProcessor(w, h);
			final float[] pixels = (float[]) result.getPixels();
			
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					final float x = (float) (i * M[0][0] + j * M[0][1] + M[0][2]);
					final float y = (float)(i * M[1][0] + j * M[1][1] + M[1][2]);
					pixels[i + j * w] = get(x, y);
				}
			}
			result.setMinAndMax(ip.getMin(), ip.getMax());
			ImagePlus imp = new ImagePlus(null, result);
			IJ.save(imp, resultFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public float get(float x, float y) {
		final int i = (int) x;
		final int j = (int) y;
		final float fx = x - i;
		final float fy = y - j;
		final float v00 = ip.getPixelValue(i, j);
		final float v01 = ip.getPixelValue(i + 1, j);
		final float v10 = ip.getPixelValue(i, j + 1);
		final float v11 = ip.getPixelValue(i + 1, j + 1);
		return (1 - fx) * (1 - fy) * v00 + fx * (1 - fy) * v01 + (1 - fx)
				* fy * v10 + fx * fy * v11;
	}
	
	/*==========================================================================*/
	
	
	
	
	/*============================Some tools====================================*/
	
	/**
	 * Adjust the images with the same width and height
	 * @param fileNames
	 */
	public static void adjustImagesToSameMaxBig(String[] fileNames, String[] resultPath) {
		
		if(fileNames.length != resultPath.length) return;
		
		BufferedImage[] images = new BufferedImage[fileNames.length];
		int maxWidth = 0, maxHeight = 0;
	
		for(int i=0; i<fileNames.length; i++) {
			try {
				images[i] = ImageIO.read(new File(fileNames[i]));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		maxWidth = images[0].getWidth();
		maxHeight = images[0].getHeight();
		
		/**
		 * Get the max width and height of the images
		 */
		for(int i=1; i<fileNames.length; i++) {
			if(images[i].getWidth() > maxWidth) maxWidth = images[i].getWidth();
			if(images[i].getHeight() > maxHeight) maxHeight = images[i].getHeight();
		}
		
		for(int i=0; i < fileNames.length; i++) {
			BufferedImage image = new BufferedImage(maxWidth, maxHeight, images[0].getType());
			Graphics g = image.getGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, maxWidth, maxHeight);
			
			image.setData(images[i].getRaster());
			
			try {
				ImageIO.write(image, "jpg",	new File(resultPath[i]));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * Use the high-resolution image to get the low-resolution image.  n>=1
	 * @param imageFile
	 * @param newImageFile
	 * @param n  
	 */
	public static void makeMultiResolutionImage(String imageFile, String newImageFile, int n) {
		
		try {
			BufferedImage image = ImageIO.read(new File(imageFile));
			int width = image.getWidth();
			int height = image.getHeight();
			
			width /= n;
			height /= n;
			
			BufferedImage imageNew = new BufferedImage(width, height, image.getType());
			Graphics g = imageNew.getGraphics();
			g.fillRect(0, 0, width, height);
			
			for(int y=0; y<height; y++)
				for(int x=0; x<width; x++) {
					imageNew.setRGB(x, y, image.getRGB(n * x, n * y));
				}
			
			ImageIO.write(imageNew, "jpg", new File(newImageFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	*  Create a new image and draw the given image from the path "fileName" on it.
	 * @param fileName	The given image file path and will be "drawn" 
	 * @param newImageFile The new image file path
	 * @param width The new image's width
	 * @param height The new image's height
	 * @param xPosition The x position where the given image starts drawn on the new image
	 * @param yPosition The y position where the given image starts drawn on the new image
	 */
	public static void generateAndDrawImage(String fileName, String newImageFile,int width, int height, int xPosition, int yPosition) {
		
		try {
			BufferedImage lena = ImageIO.read(new File(fileName)); 
			
			assert width >= lena.getWidth();
			assert height >= lena.getHeight();
			
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics g = image.getGraphics();
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.drawImage(lena, xPosition, yPosition, lena.getWidth(), lena.getHeight(), null);
			ImageIO.write(image, "jpg", new File(newImageFile));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Cut the stack images, start at the point (xStart, yStart) ,
	 * this method will cut all the images of the stack with same width and height.
	 * @param fileNames
	 * @param xStart 
	 * @param yStart
	 * @param width   The new image's width
	 * @param height  The new image's height
	 */
	public static void cutROIs(String[] fileNames, String[] files,int xStart, int yStart, int width, int height) {
		
		for(int i=0; i<fileNames.length; i++) {
			BufferedImage src;
			try {
				src = ImageIO.read(new File(fileNames[i]));
				assert xStart + width <= src.getWidth();
				assert yStart + height <= src.getHeight();
				
				BufferedImage image = new BufferedImage(width, height, src.getType());
				for(int y=0; y<height; y++) 
					for(int x=0; x<width; x++) {
						image.setRGB(x, y, src.getRGB(x+xStart, y+yStart));
					}
				ImageIO.write(image, "jpg", new File(files[i]));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * Cut the image from the point(xStart, yStart)
	 * @param fileName
	 * @param xStart
	 * @param yStart
	 */
	public static void cutROI(String fileName, int xStart, int yStart) {
			
			BufferedImage src;
			try {
				src = ImageIO.read(new File(fileName));
				int width = src.getWidth() - xStart;
				int height = src.getHeight() - yStart;
				
				assert xStart >= 0 ;
				assert yStart >= 0; 
				
				BufferedImage image = new BufferedImage(width, height, src.getType());
				for(int y=0; y<height; y++) 
					for(int x=0; x<width; x++) {
						image.setRGB(x, y, src.getRGB(x + xStart, y + yStart));
					}
				ImageIO.write(image, "jpg", new File(fileName+"M"));
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	//===========================================================================*/
	
	
	
	/*==========================================================================*/
	
	public static class Array2D {
		
		private double[][] array;
		
		/**
		 * Initial the Data Member reference. 
		 * @param array
		 */
		public Array2D(double[][] array) {
			this.array = array;
		}
		
		/**
		 * Pass a reference to the current array 
		 * @param array
		 */
		public void setArray(double[][] array) {
			this.array = array;
		}
		
		/**
		 * @return a reference
		 */
		public double[][] getArray() {
			return array;
		}
		
		/**
		 * If you want to use this method, they must be square array
		 * @param array2d
		 */
		public void multiArray2D(Array2D array2d) {
			
			if (array2d.getArray().length != array.length) throw new IllegalArgumentException();
			if (array2d.getArray().length != array2d.getArray()[0].length) throw new IllegalArgumentException();
			if (array.length != array[0].length) throw new IllegalArgumentException() ;
			
			double[][] b = array2d.getArray();
			double[][] a = this.clone().getArray();
		
			setZeros();
	
			for(int i=0; i<array.length; i++)
				for(int j=0; j<array[i].length; j++) 
					for(int k=0; k<array.length; k++) {
						array[i][j] += b[i][k] *  a[k][j];
					}
		}
		
		public Array2D clone() {
			
			double[][] array = new double[this.array.length][this.array[0].length];
			for(int i=0; i<this.array.length; i++)
				for(int j=0; j<this.array[i].length; j++) {
					array[i][j] = this.array[i][j];
				}
			Array2D array2d = new Array2D(array);
			return array2d;
		}
		
		public void displayArray() {
			
			System.out.println();
			for(int i=0; i<array.length; i++) {
				for(int j=0; j<array[i].length; j++) {
					System.out.print(array[i][j] + "          ");
				}
				System.out.println();
			}
		}
		
		public void setZeros() {
			
			for(int i=0; i<array.length; i++)
				for(int j=0; j<array[i].length; j++) 
					array[i][j] = 0;
		}
		
	}
			
}
