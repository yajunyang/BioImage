package com.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * When the input images satisfy all the conditions, {@link #showDialog()} method
 * will be involved in method {@link #run(String)}. No, what we deal with is two objects
 * {@link #left}, {@link #right}.
 * @author yang
 */
public class Algorithm implements PlugIn{

	private String[] type = { "Add", "Sub", "Mul", "Divide", "And", "Or", "Not", "Min", "Max", "Average"};
	private int currentIndex = 0;
	private ImagePlus left;
	private ImagePlus right;
	private ImagePlus result;
	
	int width, height;
	
	private String[] titles;
	
	@Override
	public void run(String arg) {
		String[] titles = WindowManager.getImageTitles();
		this.titles = titles;
		if(titles.length != 2) {
			IJ.showMessage("You must input two images");
			return;
		}
		
		if(!showDialog())
			return;
		
		if(left.getType() != ImagePlus.GRAY8 || right.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("Only both gray 8bits images");
			return;
		}
		
		if(left.getWidth() != right.getWidth()
				|| left.getHeight() != right.getHeight()) {
			IJ.showMessage("The two images' dimension must be same");
			return;
		}
		
		width = left.getWidth();
		height = left.getHeight();
		
		ImageProcessor ip = new ByteProcessor(width, height);
		result = new ImagePlus(" ", ip);
		result.setCalibration(left.getCalibration());
	
		switch (currentIndex) {
		case 0: // Add
			add();
			break;
		case 1: // Subtraction
			subtraction();
			break;
		case 2: // Multiplication
			multiplication();
			break;
		case 3: // Divide
			divide();
			break;
		case 4: // And
			and();
			break;
		case 5: // Or
			or();
			break;
		case 6: // not
			not();
			break;
		case 7: // min
			min();
			break;
		case 8: // max
			max();
			break;
		case 9: // average
			average();
			break;
		}	
		if(!result.isVisible()) {
			result.show();
		}
	}	
	
	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Algorithm");
		gd.addChoice("Left:", titles, WindowManager.getCurrentImage().getTitle());
		gd.addChoice("Type:", type, type[0]);
		gd.showDialog();
		if(gd.wasCanceled()) return false;
		String currentLeftImage = gd.getNextChoice();
		currentIndex = gd.getNextChoiceIndex();
		
		left = WindowManager.getImage(currentLeftImage);
		if(currentLeftImage.equals(titles[0])){
			right = WindowManager.getImage(titles[1]);
		}
		else {
			right = WindowManager.getImage(titles[0]);
		}
		return true;
	}

	private void add() {
		result.setTitle(left.getTitle() + " add " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int addValue = left.getProcessor().get(x, y) + right.getProcessor().get(x, y);
				if(addValue > 255) addValue = 255;
				result.getProcessor().set(x, y, addValue);
			}
	}
	
	private void subtraction() { 
		result.setTitle(left.getTitle() + " subtraction " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int subValue = left.getProcessor().get(x, y) - right.getProcessor().get(x, y);
				if(subValue < 0) subValue = 0;
				result.getProcessor().set(x, y, subValue);
			}
	}
	
	private void multiplication() {
		result.setTitle(left.getTitle() + " multiplication " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int mulValue = left.getProcessor().get(x, y) * right.getProcessor().get(x, y);
				if(mulValue > 255) mulValue = 255;
				result.getProcessor().set(x, y, mulValue);
			}
	}
	
	private void divide() {
		result.setTitle(left.getTitle() + " divide " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				float divValue;
				int rightValue = right.getProcessor().get(x, y);
				if(rightValue == 0) divValue = 255;
				else {
					divValue = (float)left.getProcessor().get(x, y) / rightValue;
				}
				result.getProcessor().set(x, y, (int)divValue);
			}
	}
	
	private void and() {
		result.setTitle(left.getTitle() + " and " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int andValue = left.getProcessor().get(x, y) & right.getProcessor().get(x, y);
				result.getProcessor().set(x, y, andValue);
			}
	}
	
	private void or() {
		result.setTitle(left.getTitle() + " or " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int orValue = left.getProcessor().get(x, y) | right.getProcessor().get(x, y);
				result.getProcessor().set(x, y, orValue);
			}
	}
	
	private void not() {
		result.setTitle(left.getTitle() + " not ");
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int notValue = ~(left.getProcessor().get(x, y));
				result.getProcessor().set(x, y, notValue);
			}
	}
	
	private void max() {
		result.setTitle(left.getTitle() + " max " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int leftValue = left.getProcessor().get(x, y);
				int rightValue = left.getProcessor().get(x, y);
				int value = leftValue > rightValue ? leftValue : rightValue;
				result.getProcessor().set(x, y, value);;
			}
	}
	
	private void min() {
		result.setTitle(left.getTitle() + " min " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int leftValue = left.getProcessor().get(x, y);
				int rightValue = left.getProcessor().get(x, y);
				int value = leftValue < rightValue ? leftValue : rightValue;
				result.getProcessor().set(x, y, value);;
			}
	}
	
	private void average() {
		result.setTitle(left.getTitle() + " average " + right.getTitle());
		for(int y=0; y<height; y++)
			for(int x=0; x<width; x++) {
				int leftValue = left.getProcessor().get(x, y);
				int rightValue = left.getProcessor().get(x, y);
				int value = (leftValue + rightValue) / 2; 
				result.getProcessor().set(x, y, value);;
			}
	}

}
