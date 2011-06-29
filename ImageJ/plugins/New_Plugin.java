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
 * Written by:
 * Benn Snyder - benn.snyder@gmail.com
 * Ruth Steinhour - rasteinhour@gmail.com
 * Joshua Thomas - jet4416@gmail.com
 *
 * Date last modified: June 28, 2011
 * Version: 0.7.5
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
 * OptionWindow.java
 * ParticleBox.java
 * PDFWindow.java
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
 * @version		0.7.5, 06/28/11
 */
public class New_Plugin implements PlugInFilter
{
	// Global ImageJ variables
	ImagePlus imp;
	ImageProcessor ip;
	ImagePlus impCopy;
	ImageProcessor ipCopy;

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
	 * Taken and modified from ImageJ's Distribution class.
	 */
	void stats(int nc, double[] data, double[] pars)
	{
		double s = 0, min = Double.MAX_VALUE, max = Double.MIN_VALUE, totl=0, ave=0, adev=0, sdev=0, var=0, skew=0, kurt=0, p;

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
		sdev = Math.sqrt(var);

		if (var > 0)
		{
			skew /= nc * Math.pow(sdev, 3);
			kurt = kurt / (nc * Math.pow(var, 2)) - 3;
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

		// Use the VersatileWand in the white point and then crop the image.
		VersatileWand.mousePressed(Integer.toString(curX), Integer.toString(curY));

		// Make a copy of the region of interest to return
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
	 * @return Returns the optimal bin width.
	*/
	double sizeBins(double[] data, ResultsTable rt)
	{
		double [] pars = new double [11];
		// Calls the stats function to compute pars
		// standard deviation has index 7
		// minimum area has index 3
		// maximum area has index 4
		stats(rt.getCounter(), data, pars);

		// use Scott's method (1979 Biometrika, 66:605-610) for optimal binning: 3.49*sd*N^-1/3
		return (3.49 * pars[7]*Math.pow(rt.getCounter(), -1.0/3.0));
	}

	/**
	 * Computes the number of bins, based on the bin width as computed in the sizeBins
	 * function.
	 *
	 * @param data An array containing the diameters of all of the particles.
	 * @param rt A ResultsTable object, used to compute data for the array, pars.
	 *
	 * @return Returns the optimal number of bins for the image.
	*/
	int numBins(double[] data, ResultsTable rt)
	{
		double [] pars = new double [11];
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
	 * Computes the distance between two centers of mass.
	 *
	 * @param c11 The first coordinate of the first particle.
	 * @param c12 The second coordinate of the first particle.
	 * @param c21 The first coordinate of the second particle.
	 * @param c22 The second coordinate of the second particle.
	 *
	 * @return The Euclidean distance between the two particles.
	*/
	double computeDistance(double c11, double c12, double c21, double c22)
	{
		return Math.sqrt(Math.pow(c11-c21,2)+Math.pow(c12-c22,2));
	}

	/**
	 * Finds the avergae RDF.
	 *
	 * @param count The number of particles.
	 * @param locs The (x,y,z) locations of the particles.
	 * @param idx1 Specifies what the first coordinate is for each particle (x, y, or z).
	 * @param idx2 Specifies what the second coordinate is for each particle (x, y, or z).
	 * @param w The image's width.
	 * @param h The image's height.
	 *
	 * @return The average number of particles in each bin.
	*/
	double[] getBinAvg(int count, double[][] locs, int idx1, int idx2, int w, int h)
	{
		// Multi-dimensional array to store the distances between particles
		double[][] partDists = new double[count][count];

		// Computes the distances between the particles
		for (int i = 0; i < count; i++)
		{
			for (int j = 0; j < count; j++)
			{
				// Particle's distance to itself is 0
				if(j == i)
				{
					partDists[i][j] = 0;
				}
				// Otherwise, compute distance
				else
				{
					partDists[i][j] = computeDistance(locs[i][idx1], locs[i][idx2], locs[j][idx1], locs[j][idx2]);
				}
			}
		}

		// Computes length of diagonal of image to be used as maximum search radius
		int maxRad = (int)(Math.sqrt(Math.pow(w,2)+Math.pow(h,2)));
		// Number of radial bins
		int numPartBin = 30; // Placeholder value - eventually will be a factor of maxRad
		// Change in radius
		int deltaRad = maxRad/numPartBin;
		// Counter variable representing number of bins
		int partCount = 0;

		// Stores number of particles in each radial bin
		int[][] binCount = new int[count][numPartBin];

		// Computes number of particles in each radial bin
		for (int i = 0; i < count; i++)
		{
			// Resets the counter
			partCount = 0;
			// Starts at change in radius, increases by change in radius at each pass
			for(int radius = deltaRad; radius <= maxRad; radius+=deltaRad)
			{
				// Summing variable
				int loopSum = 0;
				for(int j = 0; j < count; j++)
				{
					// If the distance between particles i and j falls in the range (previous radius, current radius],
					// then the number of particles increases by 1
					if((partDists[i][j] > (radius - deltaRad)) && (partDists[i][j] <= radius))
					{
						loopSum++;
					}
				}
				// Assigns number of particles to binCount
				binCount[i][partCount] = loopSum;
				// Increment counter
				partCount++;
			}
		}

		// Stores average number of particles per radial bin
		double[] binAvg = new double[numPartBin];

		// Computes average number of particles per radial bin
		for (int i = 0; i < numPartBin; i++)
		{
			for (int j = 0; j < count; j++)
			{
				binAvg[i] = binCount[j][i];
			}
			//binAvg[i] /= count;
			binAvg[i] /= (count-1); // Not sure whether this is right or not?
		}

		return binAvg;
	}

	/**
	 * Main driving method for the New_Plugin class. Executes the main functionality for the class.
	 *
	 * @param inIP An ImageProcessor object that is used to work with the image.
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
		if (!ow.thresholdCheck)
		{
			threshold();
		}
		// Checks if the user specified that the image needs to watersheded
		if (ow.watershedCheck)
		{
			IJ.run(imp, "Watershed", "");
		}

		// Analyze particles
		IJ.run("Set Measurements...", "area center shape redirect=None decimal=6");
		IJ.run("Analyze Particles...");

		// Get results
		ResultsTable rt = ResultsTable.getResultsTable();

		// Creates an array to store the diameters that ImageJ computes for each particle
		double[] data = new double[rt.getCounter()];
		for (int i = 0; i < rt.getCounter(); i++)
		{
			// Stores the diameters of each recognized particle into the array
			data[i] = (2*Math.sqrt(rt.getValue("Area", i) / Math.PI));
		}

		// Grabs the maximum diameter from the data set of diameters
		double max = Alg.arrayMax(data);

		double binWidth = sizeBins(data, rt);
		int nBins = numBins(data, rt);
		binWidth = (max/(nBins+1));

		double[] Na = Alg.getNa(data, nBins, binWidth, imp.getWidth() * imp.getHeight());

		// Computes Nv for thickness = 0 pixels
		double[] results = Alg.SSAlg(data, nBins, binWidth, 0, imp.getWidth() * imp.getHeight());
		// Computes Nv for thickness = 50 pixels
		double[] results2 = Alg.SSAlg(data, nBins, binWidth, 50, imp.getWidth() * imp.getHeight());
		// Computes Nv for thickness = 100 pixels
		double[] results3 = Alg.SSAlg(data, nBins, binWidth, 100, imp.getWidth() * imp.getHeight());

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

		// Growth rate computations (values for alpha)
		double[][] combinedNaNv = new double[nBins+1][4];
		double area = imp.getWidth() * imp.getHeight();
		double sumNa = 0, sumNv0 = 0, sumNv50 = 0, sumNv100 = 0;
		// Computes sums of particle size distributions
		for(int i = 0; i < Na.length; i++)
		{
			sumNa += Na[i];
			sumNv0 += results[i];
			sumNv50 += results2[i];
			sumNv100 += results3[i];
		}
		// Finds number of particles in bins
		for(int i = 0; i <= nBins; i++)
		{
			combinedNaNv[i][0] = Na[i]*area;
			combinedNaNv[i][1] = results[i]*area*(sumNa/sumNv0);
			combinedNaNv[i][2] = results2[i]*area*(sumNa/sumNv50);
			combinedNaNv[i][3] = results3[i]*area*(sumNa/sumNv100);
		}

		// Computes alpha values for 2-D -> 3-D (H=0), 2-D -> 3-D (H=50), and 2-D -> 3-D (H=100)
		double alpha0 = Alg.getAlpha(combinedNaNv, binWidth, rt.getCounter(), 1);
		double alpha1 = Alg.getAlpha(combinedNaNv, binWidth, rt.getCounter(), 2);
		double alpha2 = Alg.getAlpha(combinedNaNv, binWidth, rt.getCounter(), 3);

		// Creates and launchs the PDF Window, which produces the PDFs for Na and the selected Nv sets
		if(ow.PDFCheck)
		{
			PDFWindow pw = new PDFWindow(combinedNaNv, binWidth, nBins, ow.thicknessCheck, false, rt.getCounter());
			pw.start();
		}
		if(ow.PDFCheck2)
		{
			PDFWindow pw2 = new PDFWindow(combinedNaNv, binWidth, nBins, ow.thicknessCheck, true, rt.getCounter());
			pw2.start();
		}
		// Creates and launches the Graph Window, which plots Na and the selected computed Nv sets
		if(ow.CDFCheck)
		{
			GraphWindow gw = new GraphWindow(Na, results, results2, results3, max, nBins, ow.thicknessCheck);
			gw.start();
		}

		// New random number generator
		Random RNG = new Random();

		// Holds the (x,y,z) locations of each particle's center of mass
		double[][] centroids = new double[rt.getCounter()][3];

		// Grabs and stores the centroid information
		for (int i = 0; i < centroids.length; i++)
		{
			// Center of mass in x
			centroids[i][0] = rt.getValue("XM", i);
			// Center of mass in y
			centroids[i][1] = rt.getValue("YM", i);
			// "Center of mass" in z - defined by a normal distribution
			centroids[i][2] = RNG.nextGaussian();
		}

		// Computes the bin counts in each 2-D plane: XY, XZ, and YZ
		// Used for average RDF
		double[] binCountXY = getBinAvg(rt.getCounter(), centroids, 0, 1, imp.getWidth(), imp.getHeight());
		double[] binCountXZ = getBinAvg(rt.getCounter(), centroids, 0, 2, imp.getWidth(), imp.getHeight());
		double[] binCountYZ = getBinAvg(rt.getCounter(), centroids, 1, 2, imp.getWidth(), imp.getHeight());

		if (ow.percentParticles > 0)
		{
			// Creates the 3-D view for the particles
			// Passes in the image width, image height, maximum diameter, and number of particles
			if (ow.thicknessCheck[0])
			{
				ParticleBox pb0 = new ParticleBox(imp.getWidth(), imp.getHeight(), data, rt.getCounter(), centroids, (int)(sumNa/sumNv0), alpha0, ow.percentParticles);
				pb0.run();
			}
			if (ow.thicknessCheck[1])
			{
				ParticleBox pb1 = new ParticleBox(imp.getWidth(), imp.getHeight(), data, rt.getCounter(), centroids, (int)(sumNa/sumNv50), alpha1, ow.percentParticles);
				pb1.run();
			}
			if (ow.thicknessCheck[2])
			{
				ParticleBox pb2 = new ParticleBox(imp.getWidth(), imp.getHeight(), data, rt.getCounter(), centroids, (int)(sumNa/sumNv100), alpha2, ow.percentParticles);
				pb2.run();
			}
		}

		// Updates the image on screen
		imp.updateAndDraw();

		// relock image?
		imp.lock();
	}
}
