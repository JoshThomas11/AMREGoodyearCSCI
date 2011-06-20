import java.awt.*;
import javax.swing.*;

// Pt Plot Packages
import ptolemy.plot.Plot;


/*
 * Graph Window
 * A JFrame window used to display the plots of the 2-D particle distribution and
 * the 3-D particle distributions (of varying thicknesses).
*/
/**
 * A JFrame window that is launched at the end of the execution of the main plugin.
 * The window provides plots of the particle distributions Na and Nv (2-D and 3-D),
 * with varying thicknesses for Nv. This class makes use of Ptolemy's Plot (Ptplot)
 * functionality.
 *
 * For more information on Ptolemy and ptplot, see
 * http://ptolemy.berkeley.edu/java/ptplot/ .
 *
 * @author Benn Snyder
 * @author Josh Thomas
 * @version 0.2.1, 06/17/11
 */
class GraphWindow extends JPanel
{
	// Static global variables to conform to the initial constructor call and the second constructor call in setup
	private JFrame frame;

	// Plot object using ptplot
	private Plot p;

	// Counter variable to keep track of the datasets, based on which thicknesses are to be displayed
	private int idxCount = 1;

	/**
	 * Constructor for the class.
	 * This constructor takes in the pertinent data that has been grabbed and generated in the main class and stores
	 * the data in global variables to be used when constructing the plot in the frame. It then constructs the panel
	 * and plots the data.
	 *
	 * @param Na The 2-D particle distribution obtained in the New_Plugin class.
	 * @param Nv0 The 3-D particle distribution obtained in the New_Plugin class, with a thickness of 0 pixels.
	 * @param Nv50 The 3-D particle distribution obtained in the New_Plugin class, with a thickness of 50 pixels.
	 * @param Nv100 The 3-D particle distribution obtained in the New_Plugin class, with a thickness of 100 pixels.
	 * @param flags Boolean array that specifies which thicknesses in 3-D to plot.
	*/
	public GraphWindow(double[] Na, double[] Nv0, double[] Nv50, double[] Nv100, double max, int nBins, boolean[] flags)
	{
		// Gives the class's JPanel (since it extends that) a BorderLayout.
		super(new BorderLayout());

		// Creates the JFrame with the title of "Graph of 2-D CDF and 3-D CDF"
		frame = new JFrame("Graph of 2-D CDF and 3-D CDF");

		// Creates the JPanel to be placed in the BorderLayout - this JPanel has its components centered
		JPanel graphPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		// Creates a new Plot object
		p = new Plot();
		p.setSize(750, 500); // Sets the size of the coordinate axes window that is generated
		p.setTitle("Plot of CDFs, 2-D Distribution, 3-D Distribution (H = 0, H = 50, H = 100)"); // Sets the "figure's" title
		p.setGrid(false); // No grid behind the plotted functions/points
		//p.setYRange(0, 100); // Sets the range of the y-axis
		//p.setXRange(max/(nBins+1), (max*(nBins+2))/(nBins+1)); // Sets the domain of the x-axis
		p.setYLabel("Cumulative number, percent under size"); // Sets the label for the y-axis
		p.setXLabel("Diameter (px)"); // Sets the label for the x-axis
		p.setMarksStyle("none");
		p.setImpulses(true);
		p.setButtons(true); // Sets the buttons to return to original zoom, etc. to be active

		// Total sum variables (sums of all entries in Na and each Nv)
		double tsumA = 0, tsumV0 = 0, tsumV50 = 0, tsumV100 = 0;
		// Cumulative sum variables (progressive sums of the entries in Na or the Nvs)
		double csumA = 0, csumV0 = 0, csumV50 = 0, csumV100 = 0;

		// Precomputes the total sums of Na, Nv0, Nv50, and Nv100
		for (int i = 0; i < Na.length; i++)
		{
			tsumA += Na[i];
			tsumV0 += Nv0[i];
			tsumV50 += Nv50[i];
			tsumV100 += Nv100[i];
		}

		// First points plotted for each data set are not connected
		boolean connected = false;

		/* Adds the points to the plot, with:
		 * Na = data set 0
		 * Nv0 = data set 1
		 * Nv50 = data set 2
		 * Nv100 = data set 3
		*/
		for (int i = 1; i <= nBins+1; i++)
		{
			// Computes cumulative sums
			csumA += Na[i-1];
			csumV0 += Nv0[i-1];
			csumV50 += Nv50[i-1];
			csumV100 += Nv100[i-1];

			// Adds the points to the Plot object for each data set
			// x-axis is particle diameter
			// y-axis is cumulative percent of number of particles whose diameter is less than or equal to the given diameter
			p.addPoint(0, i*(max/(nBins+1)), (csumA/tsumA)*100, connected);
			if(flags[0])
			{
				p.addPoint(idxCount, i*(max/(nBins+1)), (csumV0/tsumV0)*100, connected);
				idxCount++;
			}
			if(flags[1])
			{
				p.addPoint(idxCount, i*(max/(nBins+1)), (csumV50/tsumV50)*100, connected);
				idxCount++;
			}
			if(flags[2])
			{
				p.addPoint(idxCount, i*(max/(nBins+1)), (csumV100/tsumV100)*100, connected);
			}
			idxCount = 1;

			// Connects points after the first iteration of the loop
			connected = true;
		}

		// Scales plot to fit data
		//p.fillPlot();

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
