package yang.plugin;

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

/*
 * Java 2 Standard Edition shorted as J2SE is a widely used platform for development and deployment of portable applications for desktop and server environments.
 * Java SE uses the object-oriented Java programming language. 
 * It is part of the Java software platform family. 
 * Java SE defines a wide range of general purpose APIs �C such as Java APIs for the Java Class Library �C 
 * and also includes the Java Language Specification and the Java Virtual Machine Specification.
 * One of the most well-known implementations of Java SE is Oracle Corporation's Java Development Kit (JDK)
 * 
 * 1 General purpose packages
 	java.lang
 	java.io
 	java.nio
 	java.math
 	java.net
 	java.text
 	java.util
 * 2 Special purpose packages
 	java.applet
 	java.beans
	java.awt
	java.rmi
	java.security
	java.sql
	javax.rmi
	javax.swing
	javax.swing.text.html.parser
 	javax.xml.bind.annotation
 * 3 OMG packages
	org.omg.CORBA
	org.omg.PortableInterceptor
 * 
 * 
 * Java Platform, Enterprise Edition or Java EE is Oracle's enterprise Java computing platform. 
 * The platform provides an API and runtime environment for developing and running enterprise software, including network and web services, 
 * and other large-scale, multi-tiered, scalable, reliable, and secure network applications. 
 * Java EE extends the Java Platform, Standard Edition (Java SE),
 * providing an API for object-relational mapping, distributed and multi-tier architectures, and web services. 
 * The platform incorporates a design based largely on modular components running on an application server. 
 * Software for Java EE is primarily developed in the Java programming language. 
 * The platform emphasizes convention over configuration and annotations for configuration. 
 * Optionally XML can be used to override annotations or to deviate from the platform defaults.
 * 
	javax.servlet.*
	javax.websocket.*
	javax.faces.*
	javax.faces.component.*
	javax.el.*
	javax.enterprise.inject.*
	javax.enterprise.context.*
	javax.ejb.*
	javax.validation.*
 	javax.persistence.*
 	javax.transaction.*
 	javax.security.auth.message.*
 	javax.enterprise.concurrent.*
 	javax.jms.*
 	javax.batch.api.*
 	javax.resource.*
 */
