import java.awt.*;
import javax.swing.*;

// Pt Plot Packages
import ptolemy.plot.Plot;


/*
 * PDF Window
 * A JFrame window used to display the PDF distributions in 2-D and 3-D.
*/
/**
 * A JFrame window that is launched during the execution of the main plugin.
 * The window provides plots of the particle distributions Na and Nv (2-D and 3-D),
 * with varying thicknesses for Nv. These plots are PDFs, not CDFs.
 * This class makes use of Ptolemy's Plot (Ptplot) functionality.
 *
 * For more information on Ptolemy and ptplot, see
 * http://ptolemy.berkeley.edu/java/ptplot/ .
 *
 * @author Benn Snyder
 * @author Josh Thomas
 * @version 0.1, 06/17/11
 */
class PDFWindow extends JPanel
{
	// Static global variables to conform to the initial constructor call and the second constructor call in setup
	private JFrame frame;

	// Plot object using ptplot
	private Plot p;

	// Counter variable to keep track of the datasets, based on which thicknesses are to be displayed
	private int idxCount = 1;

	/**
	 * Main constructor for the class. Uses a Plot object (see Ptplot) to create "bar graphs" / a histogram,
	 * which represent the PDFs for the 2-D distribution of particles as well as the selected 3-D distribution
	 * of particles.
	 *
	 * @param numParts A mulitdimensional array that holds the number of particles in each bin for each caluclated distribution.
	 * @param delta The bin width.
	 * @param nBins The number of bins, computed by Scott's Method for Optimal Binning.
	 * @param flags Specifies which thicknesses are to be displayed.
	*/
	public PDFWindow(double[][] numParts, double delta, int nBins, boolean[] flags, boolean opt, int parts)
	{
		// Gives the class's JPanel (since it extends that) a BorderLayout.
		super(new BorderLayout());

		// Creates the JFrame with the title of "Graph of 2-D CDF and 3-D CDF"
		frame = new JFrame("Graph of 2-D PDF and 3-D PDFs");

		// Creates the JPanel to be placed in the BorderLayout - this JPanel has its components centered
		JPanel graphPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		// Creates a new Plot object
		p = new Plot();
		p.setSize(750, 500); // Sets the size of the coordinate axes window that is generated
		p.setTitle("Plot of PDFs: 2-D Distribution, 3-D Distributions"); // Sets the "figure's" title
		//p.setGrid(false); // No grid behind the plotted functions/points
		if(!opt)
		{
			p.setYLabel("Number of particles"); // Sets the label for the y-axis
		}
		else
		{
			p.setYLabel("Percentage of particles");
			for(int i = 0; i < numParts.length; i++)
			{
				numParts[i][0] /= parts;
				numParts[i][1] /= parts;
				numParts[i][2] /= parts;
				numParts[i][3] /= parts;
			}
		}
		p.setXLabel("Diameter (px)"); // Sets the label for the x-axis
		p.setMarksStyle("none");
		p.setBars(0.3, 0.15); // Makes the plot a bar graph
		p.setImpulses(true);
		p.setButtons(true); // Sets the buttons to return to original zoom, etc. to be active

		/* Adds the points to the plot, with:
		 * Na = data set 0
		 * Nv0 = data set 1
		 * Nv50 = data set 2
		 * Nv100 = data set 3
		*/
		for (int i = 0; i <= nBins; i++)
		{
			p.addPoint(0, (i+0.5)*delta, numParts[i][0], false);
			if(flags[0])
			{
				p.addPoint(idxCount, (i+0.5)*delta, numParts[i][1], false);
				idxCount++;
			}
			if(flags[1])
			{
				p.addPoint(idxCount, (i+0.5)*delta, numParts[i][2], false);
				idxCount++;
			}
			if(flags[2])
			{
				p.addPoint(idxCount, (i+0.5)*delta, numParts[i][3], false);
			}
			idxCount = 1;
		}

		// Adds the legend for the data sets
		p.addLegend(0, "2-D Distribution");
		if(flags[0])
		{
			p.addLegend(idxCount, "H = 0 px");
			idxCount++;
		}
		if(flags[1])
		{
			p.addLegend(idxCount, "H = 50 px");
			idxCount++;
		}
		if(flags[2])
		{
			p.addLegend(idxCount, "H = 100 px");
		}

		// Scales plot to fit data
		p.fillPlot();

		// Adds the Plot object to the JPanel
		graphPanel.add(p);

		// Adds the JPanel to the BorderLayout
		add(graphPanel, BorderLayout.CENTER);

		// Makes the panel non-transparent
		setOpaque(true);
		// Sets the content pane of the frame to be this window
		frame.setContentPane(this);
	}

	/**
	 * Packs the frame, sets the location of the window on the screen,
	 * and sets the window to be visible.
	 */
	private void setup()
	{
		// Packs all of the components into the frame to prepare to make it visible
		frame.pack();
		// Sets the location of the frame in the center of the screen
		frame.setLocationRelativeTo(null);
		// Pops the frame up
		frame.setVisible(true);
	}

	/**
	 * Driver method for the class. Invokes the setup method to generate the window.
	 */
	public void start()
	{
		// Runs the setup method to generate the window.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			   setup();
			}
		});
	}
}
