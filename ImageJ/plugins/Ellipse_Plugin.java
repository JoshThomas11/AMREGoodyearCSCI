/*
 * +---------------------+
 * | Ellipse_Plugin.java |
 * +---------------------+
 * A plugin written for Goodyear Tire and Rubber Company to be used in the open-source
 * program ImageJ. This plugin creates cylinders extending through the ellipses in the
 * source image using OpenGL.
 *
 * Written by:
 * Benn Snyder - benn.snyder@gmail.com
 * Ruth Steinhour - rasteinhour@gmail.com
 * Joshua Thomas - jet4416@gmail.com
 *
 * Special thanks to:
 * Dr. John Ramsay, Professor of Mathematics and Computer Science at The College of Wooster
 *
 * Date last modified: June 28, 2011
 * Version: 0.5
*/

/*
 * +-----------------+
 * | Package Imports |
 * +-----------------+
*/

// ImageJ Packages
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.measure.ResultsTable;

// Java Packages
import java.awt.*;
import javax.swing.*;
import java.util.*;

/*
 * +--------------------+
 * | Associated Classes |
 * +--------------------+
 *
 * CylinderWindow.java
 *
*/


/*
 * Ellipse_Plugin
 * The main class of the file, which represents the entire plugin.
*/
/**
 * The main class. This class represents the secondary plugin developed for Goodyear
 * Tire and Rubber Company. This plugin creates cylinders using OpenGL from a 2-D image featuring
 * ellipses. If the 2-D image is thought of as a plane intersecting the cylinders, the ellipses are
 * the projections of the cylinders on the plane.
 *
 * @author		Benn Snyder
 * @author		Josh Thomas
 * @version		0.5, 06/28/11
 */
public class Ellipse_Plugin implements PlugInFilter
{
	// Global ImageJ variables
	ImagePlus imp;
	ImageProcessor ip;

	/**
	 * Setup function required by PlugInFilter.
	 *
	 * @param arg Argument passed into the plugin.
	 * @param inIMP The ImagePlus object currently active in ImageJ.
	 *
	 * @return Flag that lists the capabilities of the plugin.
	*/
	public int setup(String arg, ImagePlus inIMP)
	{
		imp = inIMP;
		return DOES_ALL;
	}

	/**
	 * Main driving method for the Ellipse_Plugin class. Executes the main functionality for the class.
	 *
	 * @param inIP An ImageProcessor object that is used to work with the image.
	*/
	public void run(ImageProcessor inIP)
	{
		// Unlocks the image
		imp.unlock();

		// Sets the plugin's image processor to the current one
		ip = inIP;

		// Analyze particles
		IJ.run("Set Measurements...", "area center fit shape redirect=None decimal=6");
		IJ.run("Analyze Particles...");

		// Gets the Results Table
		ResultsTable rt = ResultsTable.getResultsTable();

		// Grabs data from the Results Table and calls Cylinder Window to construct the OpenGL components
		// Variables for the data
		double[] xLoc = new double[rt.getCounter()];
		double[] yLoc = new double[rt.getCounter()];
		double[] minA = new double[rt.getCounter()];
		double[] majA = new double[rt.getCounter()];
		double[] XYAng = new double[rt.getCounter()];
		double[] cylAng = new double[rt.getCounter()];
		
		// Loop grabs data for each "particle"
		for(int i = 0; i < rt.getCounter(); i++)
		{
			// (x,y) center of mass
			xLoc[i] = rt.getValue("XM", i);
			yLoc[i] = rt.getValue("YM", i);
			// Minor axis (radius)
			minA[i] = rt.getValue("Minor", i) / 2;
			// Major axis (radius)
			majA[i] = rt.getValue("Major", i) / 2;
			// Angle of rotation in xy-plane
			XYAng[i] = rt.getValue("Angle", i);
			cylAng[i] = (180/Math.PI)*Math.acos(minA[i]/majA[i]);
		}

		// Calls Cylinder Window class to do OpenGL stuff
		CylinderWindow cw = new CylinderWindow(xLoc, yLoc, minA, XYAng, cylAng, imp.getWidth(), imp.getHeight());
		cw.run();
		
		// Re-locks the image
		imp.lock();
	}
}
