package com.plugin;

import ij.IJ;
import ij.plugin.PlugIn;

public class RecentWork implements PlugIn{

	@Override
	public void run(String arg) {
		String work = "���еĹ�����Ϊ��������\n"
				+ "1, ��ͼ����ά�ᡢ��ͻ�����νṹ��ȡ����ǿ��\n"
				+ "2, ����ȫ�ֵĶ�ֱ��ʸ߾��Ⱦֲ���׼��\n"
				+ "3, 2D��3D��������ά�ṹ��ʾ���㷨�ʹ����о�.\n"
				+ "\n"
				+ "�Ѿ���ɹ���:\n"
				+ "��׼�Ĺ���:��com.plugin.registration.ImageTool���е�˵����\n"
				+ "���νṹ��ȡ�Ĺ���:��com.plugin.segmentation.LinearFeature2D_1���е�˵����\n"
				+ "ʾ�ٵĹ���:��Ҫ�������ִ��룺һ���ǰ�com.plugin.tracing.tracing�µĴ��룬"
				+ "\n 	��һ����plugin�е�NeuronJ_����"
				+ "\n 	�ڶ��ַ�����ȱ���ǲ��ṩ3D��ʾ�ٷ�����\n"
				+ "����:\n"
				+ "	����Hessian����Ķ�߶�С����ͼ��ָ��\n"
				+ "	MULTIRESOLUTION IMAGE REGISTRATION\n"
				+ "	Three-dimensional multi-scale line filter for segmentation and visualization of curvilinear structures in medical images\n"
				+ "	A perfect fit for signal and image processing\n"
				+ "	Multiscale vessel enhancement filtering\n"
				+ "	A non-local algorithm for image denoising\n"
				+ "	A new method for linear feature and junction enhancement in 2D images based on morphological operation, oriented anisotropic Gaussian function and Hessian information\n"
				+ " A Pyramid Approach to Subpixel Registration Based on Intensity\n"
				+ ""
				+ "��һ������:\n"
				+ "1, ����������ӵ��ָ������νṹ�С�\n"
				+ "2, �Եõ������νṹ�������νṹ�ϵ�ƽ����ȥ�롣\n"
				+ "3������׼�㷨�Ľ�һ�����ԡ� \n"
				+ "4��ʾ���㷨�Ľ�һ���о��͸Ľ���\n";
		IJ.showMessage(work);
	}

}
