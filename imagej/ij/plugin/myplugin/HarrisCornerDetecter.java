package ij.plugin.myplugin;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.ImageProperties;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class HarrisCornerDetecter {
	public static final float DEFAULT_ALPHA = 0.050f;
	public static final int DEFAULT_THRESHOLD = 2000;
	float alpha = DEFAULT_ALPHA;
	int threshold = DEFAULT_THRESHOLD;
	double dmin = 10;
	final int border = 20;
	
	// 滤波核（可分离2D滤波器的1D部分）
	final float[] pfilt = {0.223755f, 0.552490f, 0.223755f};
		// = [2, 5, 2] / 9
	final float[] dfilt = {0.453014f, 0.0f, -0.453014f};
	final float[] bfilt = {0.01563f, 0.09375f, 0.234375f, 0.3125f, 0.234375f, 0.09375f, 0.01563f};
		// = [1, 6, 15, 20, 15, 6, 1] / 64
	ImageProcessor ipOrig;	 	// 	原始图像
	FloatProcessor A;
	FloatProcessor B;
	FloatProcessor C;
	FloatProcessor Q;
	
	/**					A(u,v)   C(u,v)
	 * 局部结构矩阵: 	B(u,v)	 C(u,v)		  A(u,v)=Ix2(u,v)  B(u,v)=Iy2(u,v)  C(u,v)=Ix(u,v)·Iy(u,v) 2指的是平方
	 */
	
	List<Corner_My> corners;
	
	HarrisCornerDetecter(ImageProcessor ip) {
		this.ipOrig = ip;
	}
	
	public HarrisCornerDetecter(ImageProcessor ip, 
			float alpha, int threshold) {
		this.ipOrig = ip;
		this.alpha = alpha;
		this.threshold = threshold;
	}
	
	public void findCorners() {
		 makeDerivatives();
		 makeCrf();	// 角点响应函数
		 corners = collectCorners(border);
		 corners = cleanupCorners(corners);
	}
	
	void makeDerivatives() {
		FloatProcessor Ix = 
				(FloatProcessor) ipOrig.convertToFloat();
		FloatProcessor Iy =
				(FloatProcessor) ipOrig.convertToFloat();
		Ix = convolve1h(convolve1h(Ix, pfilt), dfilt);	// 先进行滤波平滑处理，再进行便微分找边界处理
		Iy = convolve1v(convolve1v(Iy, pfilt), dfilt);
		
		A = sqr((FloatProcessor)Ix.duplicate());
		A = convolve2(A, bfilt);	// 平方以后的图像进行 模糊处理
		
		B = sqr((FloatProcessor)Iy.duplicate());
		B = convolve2(B, bfilt);
		
		C = mult((FloatProcessor)Ix.duplicate(), Iy);
		C = convolve2(C, bfilt);
	}
	
	void makeCrf() {	// 		角点响应函数 Q = (A·B-C^2) - a·(A+B)^2
		int w = ipOrig.getWidth();
		int h = ipOrig.getHeight();
		Q = new FloatProcessor(w, h);
		float[] Apix = (float[]) A.getPixels();
		float[] Bpix = (float[]) B.getPixels();
		float[] CPix = (float[]) C.getPixels();
		float[] QPix = (float[]) Q.getPixels();
		
		for(int v=0; v<h; v++)
			for(int u=0; u<w; u++) {
				int i=v*w+u;
				float a = Apix[i], b = Bpix[i], c = CPix[i];
				float det = a*b-c*c;
				float trace = a+b;
				QPix[i] = det - alpha * (trace * trace);
			}
	}
	
	@SuppressWarnings("unchecked")
	List<Corner_My> collectCorners(int border) {
		List<Corner_My> cornerlist = new Vector<Corner_My>(1000);
		int w = Q.getWidth();
		int h = Q.getHeight();
		float[] Qpix = (float[]) Q.getPixels();
		for(int v=border; v<h-border; v++) {
			for(int u=border; u<w-border; u++) {
				float q = Qpix[v*w + u];
				if(q>threshold && isLocalMax(Q, u, v)) {
					Corner_My c = new Corner_My(u, v, q);  // 保存像素坐标与值
					cornerlist.add(c);
				}
			}
		}
		Collections.sort(cornerlist);
		return cornerlist;
	}
	
	List<Corner_My> cleanupCorners(List<Corner_My> corners) {
		double dmin2 = dmin*dmin;
		Corner_My[] cornerArray = new Corner_My[corners.size()];
		cornerArray = corners.toArray(cornerArray);
		List<Corner_My> goodCorners = 
				new Vector<Corner_My>(corners.size());
	    for(int i=0; i<cornerArray.length; i++) {
	    	if(cornerArray[i] != null) {
	    		Corner_My c1 = cornerArray[i];
	    		goodCorners.add(c1);
	    		// 删除所有剩下与c相邻的角点
	    		for(int j=i+1; j<cornerArray.length; j++) {
	    			if(cornerArray[j] != null) {
	    				Corner_My c2 = cornerArray[j];
	    				if(c1.dist2(c2) < dmin2) 
	    					cornerArray[j] = null;	// 删除角点
	    			}
	    		}
	    	}
	    }
	    return goodCorners;
	}
	
	@SuppressWarnings("deprecation")
	void printCornerPoints(List<Corner_My> crf) {
		int i = 0;
		for(Corner_My ipt : crf) {
			IJ.write((i++) + ": " + (int)ipt.q + " " + ipt.u + " " + ipt.v);
		}
	}
	
	public ImageProcessor showCornerPoints(ImageProcessor ip) {
		ByteProcessor ipResult= (ByteProcessor) ip.duplicate();
		// 改变图像的对比度和亮度
		int[] lookuptable = new int[256];
		for(int i=0; i<256; i++) {
			lookuptable[i] = 128+(i/2);  
		}
		ipResult.applyTable(lookuptable);
		// 画出角点
		for(Corner_My c : corners) {
			c.draw(ipResult);
		}
		return ipResult;
	}
	
	void showProcessor(ImageProcessor ip, String title) {
		ImagePlus win = new ImagePlus(title, ip);
		win.show();
	}
	
	//  浮点处理器的工具方法
	static FloatProcessor convolve1h(FloatProcessor p, float[] h) {
		Convolver conv = new Convolver();
		conv.setNormalize(false);
		conv.convolveFloat(p, h, 1, h.length);
		return p;
	}
	
	static FloatProcessor convolve1v(FloatProcessor p, float[] h) {
		Convolver conv = new Convolver();
		conv.setNormalize(false);
		conv.convolve(p, h, h.length, 1);
		return p;
	}
	
	static FloatProcessor convolve2(FloatProcessor p, float[] h) {
		convolve1h(p,  h);
		convolve1v(p, h);
		return p;
	}
	
	static FloatProcessor sqr(FloatProcessor fpi) {
		fpi.sqr();
		return fpi;
	}
	
	static FloatProcessor mult(FloatProcessor fp1, FloatProcessor fp2) {
		int mode = Blitter.MULTIPLY;
		fp1.copyBits(fp2, 0, 0, mode);
		return fp1;
	}
	
	static boolean isLocalMax(FloatProcessor fp, int u, int v) {
		int w = fp.getWidth();
		int h = fp.getHeight();
		if(u<=0||u>=w-1||v>=h-1)
			return false;
		else {
			float[] pix = (float[]) fp.getPixels();
			int i0 = (v-1) * w + u, i1 = v * w + u, i2 = (v + 1) * w + u;
			float cp = pix[i1];
			return
					cp>pix[i0-1] && cp>pix[i0] && cp>pix[i0+1] &&
					cp>pix[i1-1] && cp>pix[i1+1] &&
					cp>pix[i2-1] && cp>pix[i2] && cp>pix[i2+1];
		}
	}
}
