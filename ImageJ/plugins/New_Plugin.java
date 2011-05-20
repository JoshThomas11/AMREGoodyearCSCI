import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

import ij.measure.ResultsTable;


// @author?
/**
 * This Plugin does a thing.  C'est vrai.
 *
 * @version		1.6
 * @since		2011.0520
 */
public class New_Plugin implements PlugInFilter
{
	// variables
	ImagePlus imp;
	ImageProcessor ip;

	public int setup(String arg, ImagePlus imp)
	{
		this.imp = imp;
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
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e) {}
	}


	// necessary?  will images be pre-cropped?
	/**
	 * Automatically crops the image
	 */
	private void autoCrop()
	{
		// Find a white point in the image.
		ColorProcessor cproc = new ColorProcessor(ip.createImage());

		// Start in center of image and spiral out.
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
		// Use the wand tool to select an area.
		Wand magic = new Wand(ip);
		magic.autoOutline(curX, curY);
		// Find the coords of the rectangle that the wand selected.
		int minX = ip.getWidth();
		int minY = ip.getHeight();
		int maxX = 0;
		int maxY = 0;
		for (int i = 0; i < magic.npoints; i++)
		{
			if (magic.xpoints[i] > maxX)
			{
				maxX = magic.xpoints[i];
			}
			else if (magic.xpoints[i] < minX)
			{
				minX = magic.xpoints[i];
			}
			if (magic.ypoints[i] > maxY)
			{
				maxY = magic.ypoints[i];
			}
			else if (magic.ypoints[i] < minY)
			{
				minY = magic.ypoints[i];
			}
		}
		// Select and crop the rectangle.
		IJ.makeRectangle(minX, minY, maxX-minX, maxY-minY);
		IJ.run("Crop");
	}

	/**
	 * Despeckle the image
	 */
	private void despeckle()
	{
		RankFilters filter = new RankFilters();
		filter.rank(imp.getProcessor(), 1, RankFilters.MEDIAN);
	}

	/**
	 * Run the Threshold dialog to get threshold data
	 */
	private void threshold()
	{
		// Automatic Thresholding doesn't work (yet), so we get the user to do it.
		// Show dialog with instructions.
		WaitForUserDialog waiter = new WaitForUserDialog("User input required", "Please click 'Apply' in the Threshold window and close the Threshold window.");
		waiter.show();
		IJ.run("Threshold...");
		// Wait to make sure the Threshold dialog is ready.
		pause(500);

		while ((WindowManager.getFrontWindow() != null) && (WindowManager.getFrontWindow().getTitle().equals("Threshold")))
		{
			pause(500);
		}
	}

	public void run(ImageProcessor ip)
	{
		// PlugInFilter automatically locks images, so we need to unlock them.
		imp.unlock();

		this.ip = ip;

		// Invert image
		ip.invert();
		// Crop
		autoCrop();
		// Despeckle
		despeckle();
		// Threshold
		threshold();
		// Analyze particles
		IJ.run("Analyze Particles...");


		imp.updateAndDraw();


		// Get results
		ResultsTable rt = ResultsTable.getResultsTable();
		int numResults = rt.getCounter();

		for (int i = 0; i < numResults; i++)
		{
			System.out.println(i + " | Area = " + rt.getValue("Area", i));
		}


		// relock image?
		imp.lock();
	}
}
