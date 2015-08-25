package yang.plugin;

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

/*
 * Java 2 Standard Edition shorted as J2SE is a widely used platform for development and deployment of portable applications for desktop and server environments.
 * Java SE uses the object-oriented Java programming language. 
 * It is part of the Java software platform family. 
 * Java SE defines a wide range of general purpose APIs – such as Java APIs for the Java Class Library – 
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
