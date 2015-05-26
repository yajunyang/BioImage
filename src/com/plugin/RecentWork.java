package com.plugin;

import ij.IJ;
import ij.plugin.PlugIn;

public class RecentWork implements PlugIn{

	@Override
	public void run(String arg) {
		String work = "所有的工作分为三个方向\n"
				+ "1, 神经图像纤维轴、树突的线形结构提取和增强。\n"
				+ "2, 基于全局的多分辨率高精度局部配准。\n"
				+ "3, 2D和3D神经线形纤维结构的示踪算法和代码研究.\n"
				+ "\n"
				+ "已经完成工作:\n"
				+ "配准的工作:见com.plugin.registration.ImageTool类中的说明。\n"
				+ "线形结构提取的工作:见com.plugin.segmentation.LinearFeature2D_1类中的说明。\n"
				+ "示踪的工作:主要基于两种代码：一种是包com.plugin.tracing.tracing下的代码，"
				+ "\n 	另一种是plugin中的NeuronJ_方法"
				+ "\n 	第二种方法的缺陷是不提供3D的示踪方法。\n"
				+ "论文:\n"
				+ "	基于Hessian矩阵的多尺度小鼠神经图像分割方法\n"
				+ "	MULTIRESOLUTION IMAGE REGISTRATION\n"
				+ "	Three-dimensional multi-scale line filter for segmentation and visualization of curvilinear structures in medical images\n"
				+ "	A perfect fit for signal and image processing\n"
				+ "	Multiscale vessel enhancement filtering\n"
				+ "	A non-local algorithm for image denoising\n"
				+ "	A new method for linear feature and junction enhancement in 2D images based on morphological operation, oriented anisotropic Gaussian function and Hessian information\n"
				+ " A Pyramid Approach to Subpixel Registration Based on Intensity\n"
				+ ""
				+ "下一步工作:\n"
				+ "1, 将神经中枢添加到分割后的线形结构中。\n"
				+ "2, 对得到的线形结构进行线形结构上的平滑和去噪。\n"
				+ "3，对配准算法的进一步测试。 \n"
				+ "4，示踪算法的进一步研究和改进。\n";
		IJ.showMessage(work);
	}

}
