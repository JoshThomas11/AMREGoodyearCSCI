/*
 * +-----------------+
 * | New_Plugin.java |
 * +-----------------+
 * A plugin written for Goodyear Tire and Rubber Company to be used in the open-source
 * program ImageJ. This plugin implements the Schwartz-Saltikov algorithm (see
 * "Analysis of polymer blend morphologies from transmission election micrographs" by
 * L. Corté and L. Leibler) to produce a 3-D particle distribution from a 2-D particle
 * distribution that was obtained from a cross-section scan of rubber by an electron
 * microscope.
 *
 * Written by: Benn Snyder - benn.snyder@gmail.com
 *			   Ruth Steinhour - rasteinhour@gmail.com
 *			   Joshua Thomas - jet4416@gmail.com
 *
 * Date last modified: June 8, 2011
 * Version: 0.7
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
import java.util.*;


/*
 * +--------------------+
 * | Associated Classes |
 * +--------------------+
 *
 * Alg.java
 * GraphWindow.java
 * LaunchWindow.java
 * OptionWindow.java
 * ParticleBox.java
 * VersatileWand.java
*/


/*
 * New_Plugin
 * The main class of the file, which represents the entire plugin.
*/
/**
 * The main class. This class represents the plugin developed for Goodyear
 * Tire and Rubber Company. This plugin automates a previously manual process of analyzing
 * a source image (2-D cross section obtained via electron microscope), implementing the
 * Schwartz-Saltikov algorithm for converting from a 2-D particle distribution to a 3-D
 * particle distribution, and producing a plot of the resulting diameters in the distributions.
 *
 * @author		Benn Snyder
 * @author		Josh Thomas
 * @version		0.6.1, 05/20/11
 */
public class New_Plugin implements PlugInFilter
{
	// Global ImageJ variables
	ImagePlus imp;
	ImageProcessor ip;
	ImagePlus impCopy;
	ImageProcessor ipCopy;
	//Roi roiCopy;

	public int setup(String arg, ImagePlus inIMP)
	{
		imp = inIMP;
		impCopy = imp.duplicate();
		return DOES_ALL;
	}

	/**
	 * Pauses execution for a specific amount of time.
	 *
	 * @param	milliseconds Time to pause in milliseconds
	 */
	void pause(long milliseconds)
	{
		try
		{
			// Suspends the thread for the specified amount of time
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e) {}
	}

	/**
	 * Gathers a variety of statistics about the passed in data and puts those in pars[].
	 * Taken from ImageJ's Distribution class.
	 */
	void stats(int nc, float [] data, float [] pars)
	{
		float s = 0, min = Float.MAX_VALUE, max = -Float.MAX_VALUE, totl=0, ave=0, adev=0, sdev=0, var=0, skew=0, kurt=0, p;

		for (int i = 0; i < nc; i++)
		{
			totl += data[i];
			if (data[i] < min)
			{
				min = data[i];
			}
			if (data[i] > max)
			{
				max = data[i];
			}
		}

		ave = totl/nc;

		for (int i = 0; i < nc; i++)
		{
			s = data[i] - ave;
			adev += Math.abs(s);
			p = s * s;
			var += p;
			p *= s;
			skew += p;
			p *= s;
			kurt += p;
		}

		adev /= nc;
		var /= nc-1;
		sdev = (float)Math.sqrt(var);

		if (var > 0)
		{
			skew /= (nc * (float) Math.pow(sdev, 3));
			kurt = kurt / (nc * (float) Math.pow(var, 2)) - 3;
		}

		pars[1] = nc;
		pars[2] = totl;
		pars[3] = min;
		pars[4] = max;
		pars[5] = ave;
		pars[6] = adev;
		pars[7] = sdev;
		pars[8] = var;
		pars[9] = skew;
		pars[10] = kurt;
	}

	/**
	 * This function has neither Easter nor eggs.
	 */
	private void dysfunction()
	{
		if (System.getProperty("os.name").equals("Linux"))
		{
			System.out.println("I'm climbin' in yo' kernel, snatchin' yo' penguins up.");
		}
	}

	/**
	 * Automatically crops the image.
	 * This can be toggled on and off in run() via a boolean variable in OptionWindow.
	 * 
	 * @return A copy of the region of interest that was cropped.
	 */
	private Roi autoCrop()
	{
		// Find a white point in the image.
		ColorProcessor cproc = new ColorProcessor(ip.createImage());

		// Start in center of image and spiral out until we find a white point.
		int curX = ip.getWidth() / 2;
		int curY = ip.getHeight() / 2;
		int iter = 1;
		byte flip = 1;
		for (Color color = cproc.getColor(curX, curY); color.getRed() != 255; color = cproc.getColor(curX, curY))
		{
			if (iter % 2 == 1)
			{
				curX += iter * flip;
				flip *= -1;
			}
			else
			{
				curY += iter * flip;
			}
			iter++;
		}

		System.out.println(iter);
		System.out.println("wandX = " + curX);
		System.out.println("wandY = " + curY);

		// Use the VersatileWand in the white point and then crop the image.
		new VersatileWand().mousePressed(Integer.toString(curX), Integer.toString(curY));
		
		Roi tmpROI = (Roi)imp.getRoi().clone();
				
		IJ.run("Crop");
		
		return tmpROI;
	}
	
	public static ImagePlus copyCrop(ImageProcessor inIP, Roi inROI)
	{
		ImageProcessor tempIP = inIP;
		tempIP.setRoi(inROI);
		tempIP = tempIP.crop();
		return new ImagePlus("Cropped copy", tempIP);
	}

	/**
	 * Despeckle the image.
	 */
	private void despeckle()
	{
		RankFilters filter = new RankFilters();
		filter.rank(imp.getProcessor(), 1, RankFilters.MEDIAN);
	}

	/**
	 * Run the Threshold dialog to get threshold data.
	 */
	private void threshold()
	{
		// Automatic Thresholding doesn't work (yet), so we get the user to do it.
		IJ.run("Threshold...");
		// Wait to make sure the Threshold dialog is ready.
		pause(500);

		Frame frame = WindowManager.getFrame("Threshold");

		try
		{
			// Wait for the user to close the Threshold window
			while (frame.isShowing())
			{
				pause(500);
			}
		}
		catch (NullPointerException e)
		{
			System.err.println("Threshold frame is null");
		}
	}

	/**
	 * Computes delta, the width of the bins, using Scott's method for optimal binning.
	 * See (1979 Biometrika, 66:605-610) for more details on this method.
	 *
	 * @param data An array containing the diameters of all of the particles.
	 * @param rt A ResultsTable object, used to compute data for the array, pars.
	 *
	 * @return Returns the optimal number of bins for the image.
	*/
	private float sizeBins(float[] data, ResultsTable rt)
	{
		float [] pars = new float [11];
		// Calls the stats function to compute pars
		// standard deviation has index 7
		// minimum area has index 3
		// maximum area has index 4
		stats(rt.getCounter(), data, pars);

		// use Scott's method (1979 Biometrika, 66:605-610) for optimal binning: 3.49*sd*N^-1/3
		return (float)(3.49 * pars[7]*(float)Math.pow(rt.getCounter(), -1.0/3.0));
	}

	/**
	 * Computes the number of bins, based on the bin width as computed in the sizeBins
	 * function.
	 *
	 * @param data An array containing the diameters of all of the particles.
	 * @param rt A ResultsTable object, used to compute data for the array, pars.
	 *
	 * @return Returns the number of bins.
	*/
	private int numBins(float[] data, ResultsTable rt)
	{
		float [] pars = new float [11];
		// Calls the stats function to compute pars
		// standard deviation has index 7
		// minimum area has index 3
		// maximum area has index 4
		stats(rt.getCounter(), data, pars);
		// Computes the number of bins
		int nBins = (int)Math.floor(((pars[4]-pars[3])/sizeBins(data, rt))+.5);
		// Forces the number of bins to be at least 2
		if (nBins < 2)
		{
			nBins = 2;
		}
		return nBins;
	}

	/**
	 * Main driving method for the New_Plugin class. Executes the main functionality for the class.
	 *
	 * @param ip An ImageProcessor object that is used to work with the image.
	*/
	public void run(ImageProcessor inIP)
	{
		// PlugInFilter automatically locks images, so we need to unlock them.
		imp.unlock();

		ip = inIP;
		ipCopy = ip.duplicate();

		// Generates the Option Window to determine what the user needs to do for the given image
		OptionWindow ow = new OptionWindow();
		ow.start();
		// Waits until the Option Window is closed before proceeding
		while (!ow.finished)
		{
			pause(1500);
		}
		// Checks if the user specified that the image needs to be converted to 8-bit grayscale
		if (ow.grayscaleCheck)
		{
			ImageConverter converter = new ImageConverter(imp);
			converter.convertToGray8();
			ip = imp.getProcessor();
		}
		// Checks if the user specified that the image needs to have its scale removed
		if (ow.removeScaleCheck)
		{
			IJ.run(imp, "Set Scale...", "distance=0 known=0 pixel=1 unit=pixel");
		}
		// Checks if the user specified that the image needs its LUT inverted
		if (ow.invertCheck)
		{
			ip.invert();
			//ip.invertLut();
		}
		// Checks if the user specified that the image needs to be binarized
		if (ow.thresholdCheck)
		{
			threshold();
		}
		// Checks if the user specified that the image needs to be cropped
		if (ow.cropCheck)
		{
			impCopy = copyCrop(ipCopy, autoCrop());	
		}
		// Checks if the user specified that the image needs to be despeckled
		if (ow.despeckleCheck)
		{
			despeckle();
		}
		// Threshold
		threshold();
		// Checks if the user specified that the image needs to watersheded
		if (ow.watershedCheck)
		{
			IJ.run(imp, "Watershed", "");
		}

		// Analyze particles
		IJ.run("Set Measurements...", "area center shape redirect=None decimal=3");
		IJ.run("Analyze Particles...");
		//IJ.run("Analyze Particles...", "show=Outlines display exclude clear record");
		//IJ.run("Analyze Particles...", "show=Outlines display clear record");
		//IJ.run("Analyze Particles...", "display exclude clear record");

		// Get results
		ResultsTable rt = ResultsTable.getResultsTable();

		// Creates a vector to store the diameters that ImageJ computes for each particle
		Vector<Double> imageData = new Vector();
		// Creates an array of the same diameters for bin computations
		float[] data = new float[rt.getCounter()];
		for (int i = 0; i < rt.getCounter(); i++)
		{
			// Stores the diameters of each recognized particle into the Vector
			imageData.add(2*Math.sqrt(rt.getValue("Area", i) / Math.PI));
			data[i] = (float)(2*Math.sqrt(rt.getValue("Area", i) / Math.PI));
		}
		
		System.out.println("num particles = " + imageData.size());

		float binWidth = sizeBins(data, rt);
		System.out.println("binWidth = " + binWidth);
		int nBins = numBins(data, rt);

		//nBins = 5;
		//System.out.println("nBins = " + nBins);

		// Computes Nv for thickness = 0 pixels
		//double[] results = Alg.SSAlg(imageData, nBins, 0, imp.getWidth() * imp.getHeight());
		double[] results = Alg.SSAlg(imageData, nBins, binWidth, 0, imp.getWidth() * imp.getHeight());
		// Computes Nv for thickness = 50 pixels
		//double[] results2 = Alg.SSAlg(imageData, nBins, 50, imp.getWidth() * imp.getHeight());
		double[] results2 = Alg.SSAlg(imageData, nBins, binWidth, 50, imp.getWidth() * imp.getHeight());
		// Computes Nv for thickness = 100 pixels
		//double[] results3 = Alg.SSAlg(imageData, nBins, 100, imp.getWidth() * imp.getHeight());
		double[] results3 = Alg.SSAlg(imageData, nBins, binWidth, 100, imp.getWidth() * imp.getHeight());

		// If the user did NOT select to include negatives, then replace these negative values with 0
		if (!ow.includeNegatives)
		{
			for (int i = 0; i < results.length; i++)
			{
				if (results[i] < 0)
				{
					results[i] = 0;
				}
				if (results2[i] < 0)
				{
					results2[i] = 0;
				}
				if (results3[i] < 0)
				{
					results3[i] = 0;
				}
			}
		}

		//new HistogramWindow("Distribution", imp, stats);

		// Grabs the maximum diameter from the data set of diameters
		double max = Collections.max(imageData);
		// Creates and launches the Graph Window, which plots Na and each of the computed Nv sets
		GraphWindow gw = new GraphWindow(Alg.Na, results, results2, results3, max, nBins);
		gw.start();


		float[] volData = new float[data.length];
		for (int i = 0; i < data.length; i++)
		{
			volData[i] = (float)((4.0/3.0)*Math.PI*Math.pow(data[i] / 2, 3));
		}
		float volBinWidth = sizeBins(volData, rt);

		// original histogram
		/*
		{
			JFrame window = new JFrame();
			JPanel panel = new JPanel(new FlowLayout());
			Histogram hist = new Histogram();

			hist.setSize(750, 500); // Sets the size of the coordinate axes window that is generated
			hist.setTitle("Histogram, 3-D Distributions"); // Sets the histogram's title
			//hist.setBinWidth(binWidth);
			hist.setBinWidth(volBinWidth);
			//hist.setBinWidth(50);
			System.out.println("volBinWidth = " + volBinWidth);
			//hist.setGrid(false); // No grid behind the plotted functions/points
			//hist.setYRange(0, 100); // Sets the range of the y-axis
			//hist.setXRange(max/(nBins+1), (max*(nBins+2))/(nBins+1)); // Sets the domain of the x-axis
			//hist.setYLabel("Cumulative number, percent under size"); // Sets the label for the y-axis
			//hist.setXLabel("Diameter (px)"); // Sets the label for the x-axis
			hist.setButtons(true); // Sets the buttons to return to original zoom, etc. to be active
			for (int i = 0; i < data.length; i++)
			{
				//hist.addPoint(0, (4/3)*Math.PI*Math.pow(data[i]/2, 3));
				hist.addPoint(0, volData[i]);

				//hist.addPoint(1, results2[i]);
				//hist.addPoint(2, results3[i]);
			}
			// Scales plot to fit data
			hist.fillPlot();

			// Adds the legend for the data sets
			hist.addLegend(0, "H = 0 px");
			//hist.addLegend(1, "H = 50 px");
			//hist.addLegend(2, "H = 100 px");
			//hist.setBars(.25, 1);
			panel.add(hist);
			window.setContentPane(panel);
			window.setSize(750,500);
			// Packs all of the components into the frame to prepare to make it visible
			window.pack();
			// Sets the location of the frame in the center of the screen
			window.setLocationRelativeTo(null);
			window.setVisible(true);
		}
		*/

		/*
		for (int i = 0; i < results.length; i++)
		{
			System.out.println(results[i]);
		}
		*/

		// New Histogram
		/*
		{
			JFrame window = new JFrame();
			JPanel panel = new JPanel(new FlowLayout());

			Plot resultsPlot = new Plot();
			resultsPlot.setBars(true);

			resultsPlot.setSize(750, 500); // Sets the size of the coordinate axes window that is generated
			resultsPlot.setTitle("Histogram of 3-D Particle Distribution"); // Sets the plot's title
			//resultsPlot.setXLabel("Cumulative number, percent under size"); // Sets the label for the x-axis
			resultsPlot.setYLabel("Number of particles per unit volume"); // Sets the label for the y-axis
			resultsPlot.setMarksStyle("none");
			resultsPlot.setImpulses(true);
			resultsPlot.setButtons(true); // Sets the buttons to return to original zoom, etc. to be active

			for (int i = 0; i < results.length; i++)
			{
				resultsPlot.addPoint(0, i, results[i], false);
				resultsPlot.addPoint(1, i, results2[i], false);
				resultsPlot.addPoint(2, i, results3[i], false);
			}

			resultsPlot.addLegend(0, "H = 0 px");
			resultsPlot.addLegend(1, "H = 50 px");
			resultsPlot.addLegend(2, "H = 100 px");

			panel.add(resultsPlot);
			window.setContentPane(panel);
			window.setSize(750,500);
			window.setTitle("Histogram of 3-D Particle Distribution");
			// Packs all of the components into the frame to prepare to make it visible
			window.pack();
			// Sets the location of the frame in the center of the screen
			window.setLocationRelativeTo(null);
			window.setVisible(true);
		}
		*/

		double[][] centroids = new double[rt.getCounter()][2];

		for (int i = 0; i < centroids.length; i++)
		{
			centroids[i][0] = rt.getValue("XM", i);
			centroids[i][1] = rt.getValue("YM", i);
		}
		
		WindowManager.setCurrentWindow(new ImageWindow(impCopy));
		
		for (int i = 0; i < centroids.length; i++)
		{
			/*
			System.out.print("(" + (int)centroids[i][0] + ", " + (int)centroids[i][1] + "): " + impCopy.getPixel((int)centroids[i][0], (int)centroids[i][1])[0] + " "); // R
			System.out.print(impCopy.getPixel((int)centroids[i][0], (int)centroids[i][1])[1] + " "); // G
			System.out.print(impCopy.getPixel((int)centroids[i][0], (int)centroids[i][1])[2] + " "); // B
			System.out.print(impCopy.getPixel((int)centroids[i][0], (int)centroids[i][1])[3]); // a
			System.out.println();
			*/
		}

		// Creates the 3-D view for the particles
		// Passes in the image width, image height, maximum diameter, and number of particles
		if (ow.percentParticles > 0)
		{
			ParticleBox pb = new ParticleBox(imp.getWidth(), imp.getHeight(), max, rt.getCounter(), centroids, ow.percentParticles);
			pb.run();

			//LaunchWindow lw = new LaunchWindow(gw, pb);
			//lw.start();
		}

		// Updates the image on screen
		imp.updateAndDraw();

		// relock image?
		imp.lock();
	}
}
