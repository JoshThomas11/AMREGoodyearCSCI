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
 * Date last modified: May 31, 2011
 * Version: 0.6.5
*/

/*
 * +-----------------+
 * | Package Imports |
 * +-----------------+
*/

// ImageJ Packages
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.measure.ResultsTable;
import ij.measure.Calibration;

// Java Packages
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

// Pt Plot Packages
import ptolemy.plot.Plot;


/*
 * +-------------------+
 * | Class Definitions |
 * +-------------------+
*/

/*
 * Versatile Wand
 * Written by: Michael Schmid
 * A wand selection tool extension, used in this plugin to auto-crop source images.
*/
/** An ImageJ magic wand with selectable tolerance, variable hue or
  * grayscale preference for RGB, gradient detection for grayscale,
  * 4-connected, 8-connected or disconnected operation and preview.
  *
  *
  * Parameters
  * ==========
  *
  * Value Tolerance:
  * The selection is expanded to all image points as long as the difference
  * between the pixel value of the point clicked and the image point is
  * less than the Value Tolerance. Ignored for thresholded images; there
  * the thresholded (red) area is selected instead. For 16-bit and float
  * images, note that rounding errors occur, thus the red area displayed
  * is only an approximation to the exact thresholded area. In contrast
  * to the ImageJ wand, you must click INTO the thresholded area; clicking
  * outside will create a beep.
  * For color images, see the color slider below.
  * The selection can be further restricted by the Gradient Tolerance.
  * For calibrated grayscale images, the Gray Value Tolerance is in
  * calibrated units (e.g., 'lux').
  *
  * Color:
  * For RGB images, this slider determines the relative weight of the
  * gray value and the color hue/saturation for the evaluation of the
  * tolerance. With a value of -100, only the gray value (brightness) is
  * taken into account (with weights of the color channels depending on
  * the Edit>Options>Conversions>Weighted RGB Conversions checkbox).
  * With a value of 0, the vector
  * distance between the (unweighted) RGB vectors is used, and with a
  * value of 100, only the difference of hue/saturation is taken into
  * account, independent of the gray value. Use a large value (e.g. 95)
  * if you want to select one color independent of brightness.
  *
  * Gradient Tolerance:
  * Irrespective of the Value Tolerance and color settings, the selection
  * is not expanded if the gray level gradient is larger than the Gradient
  * Tolerance.
  * Gradient tolerance is also active if a threshold is used instead of the
  * Value Tolerance; in this case visual inspection is easier with the
  * Threshold display in 'Over/Under' mode.
  * Gradient detection is less sensitive to noise than in 4-connected mode
  * than in 8-connected mode.
  * For calibrated images, the gradient tolerance is in calibrated units,
  * e.g., 'lux/mm'. For RGB images, gradient tolerance works on the gray
  * value only.
  *
  * Eyedropper Color:
  * Uses the ccurrent foreground color (color of the eyedropper tool) as
  * reference color (or pixel value), instead of the pixel color (value)
  * where the user has clicked. In 4-connected and 8-connected mode (see
  * 'connectedness', below) the starting pixel is selected irrespective
  * of its color (value). In non-contiguous mode, an empty selection may
  * result if no pixel matches the eydropper color within the tolerance.
  *
  * Connectedness:
  * Can be "Eight-Connected", "Four_Connected" or "Non-Contiguous":
  * In Eight-Connected mode, the selection proceeds to neighboring pixels
  * touching each other at a side or a corner. Four-Connected means that
  * the selection proceeds only to neighboring pixels sharing a side with
  * the previous pixel. Non-Contiguous mode selects all pixels of the image
  * within the tolerance irrespective of connectedness. In non-contiguous
  * mode, the 'Gradient Tolerance' and 'Include Holes' options are ignored.
  *
  * Include Holes:
  * When checking this option, the selection created by this wand operation
  * will not have interior holes. Nevertheless, holes may arise from
  * combining/subtracting selections with the SHIFT or ALT keys.
  *
  * Usage
  * =====
  * - When called from the plugins menu, the menu provides an option to
  *   install the tool in the ImageJ Toolbar. This replaces all other custom
  *   tools, e.g. those from the startup macros.
  * - Put the following into your ImageJ/macros/StartupMacros.txt file
  *   to have the Versatile Wand as a standard tool:

	macro 'Versatile Wand Tool-Cf00Lee55O2233' {
		getCursorLoc(x, y, z, flags);
		call('Versatile_Wand.mousePressed', x, y);
	}
	macro 'Versatile Wand Tool Options' {
		call('Versatile_Wand.setOptions');
	}

  * Left-click the tool icon for selecting the tool
  * Right-click or double-click the tool icon for the options menu
  * where the tolerance and other parameters can be selected.
  *
  * Compilation
  * ===========
  * Note that there is a line "gd.addHelp..." in showDialog that cannot be
  * compiled with ImageJ versions before 1.42p. Remove that line if you are
  * running an old version of ImageJ.
  *
  * Version 1.12, Michael Schmid, 2010-Sep-10: Can use eyedropper (foreground) color
  *											instead of the color (value) of the pixel
  * Version 1.11, Michael Schmid, 2009-Jul-10: Color, non-contiguous added.
  *											NullPointerException fixed.
  */
class Versatile_Wand
{
	private final static int EIGHT_CONNECTED=0, FOUR_CONNECTED=1, NON_CONTIGUOUS=2;
	private final static int UNKNOWN=0, OUTSIDE=1, INSIDE=-1;  //mask pixel values
	// dialog parameters
	static double valueTolerance = 0;
	static double gradientTolerance = 0;
	static double colorSensitivity = 0;
	static int connection = EIGHT_CONNECTED;
	static boolean includeHoles = false;
	static boolean useEyedropper = false;
	static int xStart, yStart;
	static boolean doWand;
	//
	ImagePlus imp;
	boolean colorMode;

	/** This method can be called from a tool macro to do the Wand operation */
	//public static void mousePressed(int xStart, int yStart) {
	public static void mousePressed(String xString, String yString) {
		xStart = Integer.parseInt(xString);
		yStart = Integer.parseInt(yString);
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) return;
		new Versatile_Wand().doWand(imp, xStart, yStart);
	}

	private void doWand(ImagePlus imp, int x0, int y0) {
		boolean shiftKeyDown = IJ.shiftKeyDown();
		boolean altKeyDown = IJ.altKeyDown();
		ImageProcessor ip = imp.getProcessor();
		int width = ip.getWidth();
		int height = ip.getHeight();

		// prepare mask
		ByteProcessor maskIp = new ByteProcessor(width, height);
		byte[] mPixels = (byte[])maskIp.getPixels();
		// offsets to neighboring pixels in x, y and pixel number
		int[] dirXoffset = new int[] {	0,	  1,	 1,	 1,		0,	 -1,	  -1,	-1	};
		int[] dirYoffset = new int[] {   -1,	 -1,	 0,	 1,		1,	  1,	   0,	-1,   };
		int[] dirOffset  = new int[] {-width, -width+1, +1, +width+1, +width, +width-1,   -1, -width-1 };
		// pixelPointers array for positions that we have to process; initial size is 4096 = 0x1000 points
		int pixelPointerMask = 0xf;   //binary AND with this mask gets array index
		int[] pixelPointers = new int[pixelPointerMask+1];

		// Pixel value range
		Calibration cal = imp.getCalibration();
		float lowLimit, highLimit;
		float grayRef;
		Color eyeDropperColor=null;
		if (useEyedropper) {
			eyeDropperColor = Toolbar.getForegroundColor(); // reference color is eyedropper color
			int index = ip.getBestIndex(eyeDropperColor);
			float[] calTable = ip.getCalibrationTable();
			grayRef = calTable==null ? index : calTable[index];
		} else
			grayRef = ip.getPixelValue(x0, y0);			 // reference color from this pixel
		if (ip.getMinThreshold()==ImageProcessor.NO_THRESHOLD) {
			lowLimit = grayRef - (float)valueTolerance;
			highLimit = grayRef + (float)valueTolerance;
		} else if (cal==null) {
			lowLimit = (float)ip.getMinThreshold();
			highLimit = (float)ip.getMaxThreshold();
		} else {
			lowLimit = (float)cal.getCValue((int)(ip.getMinThreshold()+0.5));
			highLimit = (float)cal.getCValue((int)(ip.getMaxThreshold()+0.5));
		}
		if (grayRef < lowLimit || grayRef > highLimit) {
			IJ.showStatus("Not in Thresholded Area");
			IJ.beep();
			return;
		}

		// Prepare color threshold
		colorMode = (ip instanceof ColorProcessor) && colorSensitivity > -1;
		int[] rgbPixels = null;
		int r0=0, g0=0, b0=0;					   //rgb of the starting/reference point
		double rWeight=0, gWeight=0, bWeight=0;	 //weights for getting 'parallel' or 'gray' comonent from rgb
		if (colorMode) {
			rgbPixels = (int[])ip.getPixels();
			if (useEyedropper) {
				float[] rgbalpha = eyeDropperColor.getRGBComponents(null);
				r0 = (int)(rgbalpha[0]*255+0.5);
				g0 = (int)(rgbalpha[1]*255+0.5);
				b0 = (int)(rgbalpha[2]*255+0.5);
			} else {
				int c0 = rgbPixels[x0 + y0*width];
				r0 = (c0>>16)&0xff;
				g0 = (c0>>8)&0xff;
				b0 = (c0)&0xff;
			}
			double[] refColor = ((r0==0&&g0==0&&b0==0) || colorSensitivity<=0) ?
				((ColorProcessor)ip).getWeightingFactors() : new double[] {r0, g0, b0};
			normalize(refColor);
			rWeight = refColor[0];
			gWeight = refColor[1];
			bWeight = refColor[2];
		}

		// Prepare gradient threshold
		boolean useGradient = gradientTolerance>0 && gradientTolerance<valueTolerance && connection!=NON_CONTIGUOUS;
		float aspectRatioSqr = 1;   // (pixel width/pixel height)^2
		float toleranceGrayGradTmp = (float)gradientTolerance;
		if (cal != null) {
			toleranceGrayGradTmp *= (float)cal.pixelWidth;
			aspectRatioSqr = (float)(cal.pixelWidth/cal.pixelHeight);
			aspectRatioSqr *= aspectRatioSqr;
		}
		float toleranceGrayGrad2 = toleranceGrayGradTmp*toleranceGrayGradTmp;

		int ymin = height;		  // the uppermost selected pixel, needed for reliable IJ.doWand
		if (connection == NON_CONTIGUOUS) {
			//create mask from full image
			if (colorMode) {
				for (int i=0; i<width*height; i++)
					if (checkColor(rgbPixels[i], r0, g0, b0, rWeight, gWeight, bWeight))
						mPixels[i] = INSIDE;
			} else {
				for (int y=0,offset=0; y<height; y++)
					for (int x=0; x<width; x++,offset++) {
						float v = ip.getPixelValue(x, y);
						if (v>=lowLimit && v<=highLimit) mPixels[offset] = INSIDE;
					}
			}
		} else {
			//queue-based flood fill algorithm
			int lastCoord = 0;
			int offset0 = x0 + y0*width;
			mPixels[offset0] = INSIDE;
			pixelPointers[0] = offset0;
			for (int iCoord = 0; iCoord<=lastCoord; iCoord++) {
				int offset = pixelPointers[iCoord & pixelPointerMask];
				int x = offset % width;
				int y = offset / width;
				boolean isInner = (x!=0 && y!=0 && x!=(width-1) && y!=(height-1));
				float v = ip.getPixelValue(x,y);
				boolean largeGradient = false;
				float xGradient=0, yGradient=0;
				if (useGradient) {
					if (isInner) {
						float vpp=ip.getPixelValue(x+1, y+1);
						float vpm=ip.getPixelValue(x+1, y-1);
						float vmp=ip.getPixelValue(x-1, y+1);
						float vmm=ip.getPixelValue(x-1, y-1);

						xGradient = 0.125f*(	//Sobel-filter like gradient
								2f*(ip.getPixelValue(x+1, y)-ip.getPixelValue(x-1, y))
								+ vpp-vmm +(vpm-vmp)); // v(x+1) - v(x-1)
						yGradient = 0.125f*(
								2f*(ip.getPixelValue(x, y+1)-ip.getPixelValue(x, y-1))
								+ vpp-vmm -(vpm-vmp)); // v(y+1) - v(y-1)
					} else {
						int xCount=0, yCount=0;
						for (int d=0; d<8; d++) if (isWithin(ip, x, y, d)) {
							int x2 = x+dirXoffset[d];
							int y2 = y+dirYoffset[d];
							float v2 = ip.getPixelValue(x2, y2);
							int weight = (2-(d&0x1));   //2 for straight, 1 for diag
							xGradient += dirXoffset[d] * (v2-v) * weight;
							xCount += weight * (dirXoffset[d]!=0 ? 1 : 0);
							yGradient += dirYoffset[d] * (v2-v) * weight;
							yCount += weight * (dirYoffset[d]!=0 ? 1 : 0);
						}
						xGradient /= xCount;
						yGradient /= yCount;
					}
					largeGradient = xGradient*xGradient + yGradient*yGradient*aspectRatioSqr > toleranceGrayGrad2;
				}
				for (int d=0; d<8; d+= (connection==FOUR_CONNECTED)?2:1) {  //analyze all neighbors (in 4 or 8 directions)
					int offset2 = offset+dirOffset[d];
					if ((isInner || isWithin(ip, x, y, d)) && mPixels[offset2] == UNKNOWN) {
						int x2 = x+dirXoffset[d];
						int y2 = y+dirYoffset[d];
						float v2 = 0;
						boolean valueOK;
						if (largeGradient || !colorMode)
							v2 = ip.getPixelValue(x2, y2);
						if (colorMode) {
							int c2 = rgbPixels[offset2];
							valueOK = checkColor(c2, r0, g0, b0, rWeight, gWeight, bWeight);
						} else	// if !colorMode
							valueOK = v2>=lowLimit && v2<=highLimit;
						if (!valueOK) {
							mPixels[offset2] = OUTSIDE;				 //don't analyze any more
						} else if (!largeGradient || (v2-v)*(xGradient*dirXoffset[d]+yGradient*dirYoffset[d])<=0) {
							mPixels[offset2] = INSIDE;				  //add new point
							if (ymin > y2) ymin = y2;
							if (lastCoord-iCoord > pixelPointerMask) {  //not enough space in array, expand it
								int newSize = 2*(pixelPointerMask+1);
								int newMask = newSize - 1;
								int[] newPixelPointers = new int[newSize];
								System.arraycopy(pixelPointers, 0, newPixelPointers, 0, pixelPointerMask+1);
								System.arraycopy(pixelPointers, 0, newPixelPointers, pixelPointerMask+1, pixelPointerMask+1);
								pixelPointers = newPixelPointers;
								pixelPointerMask = newMask;
							}
							lastCoord++;
							pixelPointers[lastCoord & pixelPointerMask] = offset2;

						}
					}
				} //for direction d
				if ((iCoord&0xfff)==1) {
					IJ.showProgress(iCoord/(double)(width*height));
					if (Thread.currentThread().isInterrupted()) return;
				}
			} //for iCoord
		} //else: if contiguous

		Roi previousRoi = imp.getRoi();
		//convert mask to roi
		Roi roi = null;
		if (includeHoles && connection!=NON_CONTIGUOUS) {
			Wand w = new Wand(maskIp);
			w.autoOutline(0, ymin, 255, 255);   //starting at x0, y0 might trace an inner hole
			if (w.npoints>0) {
				roi = new PolygonRoi(w.xpoints, w.ypoints, w.npoints, Roi.TRACED_ROI);
				if (Thread.currentThread().isInterrupted()) return;
				imp.killRoi();
				imp.setRoi(roi);
			}
		} else {
			maskIp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
			if (Thread.currentThread().isInterrupted()) return;
			imp.killRoi();
			ThresholdToSelection tts = new ThresholdToSelection();
			tts.setup("", imp);
			tts.run(maskIp);
			roi = imp.getRoi();
		}

		// add/subtract this ROI to the previous one if the shift/alt key is down
		if (Thread.currentThread().isInterrupted()) return;
		if (roi!=null && previousRoi!=null)
			roi.update(shiftKeyDown, altKeyDown);

		IJ.showProgress(1.0);
	}

	/** Returns whether pixel with rgb is inside color tolerance */
	boolean checkColor(int rgb, int r0, int g0, int b0, double rWeight, double gWeight, double bWeight) {
		int deltaR = ((rgb>>16)&0xff) - r0;
		int deltaG = ((rgb>>8)&0xff) - g0;
		int deltaB = ((rgb)&0xff) - b0;
		int deltaSqr = deltaR*deltaR + deltaG*deltaG + deltaB*deltaB;
		if (colorSensitivity==0)
			return deltaSqr <= valueTolerance*valueTolerance;
		else {
			double deltaPar = deltaR*rWeight + deltaG*gWeight + deltaB*bWeight;
			double deltaParSqr = deltaPar*deltaPar;
			// we want the following weighting of delta-squared parallel and perpendicular components:
			//   parallel + (1+colorSensitivity)*perpendicular if colorSensitivity <0,
			//   parallel*(1-colorSensitivity) + perpendicular if colorSensitivity >0
			// The perpendicular component is deltaSqr - deltaParSqr.
			if (colorSensitivity <0)
				return deltaSqr*(1.+colorSensitivity) - deltaParSqr*colorSensitivity <= valueTolerance*valueTolerance;
			else
				return deltaSqr - deltaParSqr*colorSensitivity <= valueTolerance*valueTolerance;
		}
	}

	/** Returns whether the neighbor in a given direction is within the image
	 * NOTE: it is assumed that the pixel x,y itself is within the image!
	 * Uses class variables width, height: dimensions of the image
	 * @param x		 x-coordinate of the pixel that has a neighbor in the given direction
	 * @param y		 y-coordinate of the pixel that has a neighbor in the given direction
	 * @param direction the direction from the pixel towards the neighbor (see makeDirectionOffsets)
	 * @return		  true if the neighbor is within the image (provided that x, y is within)
	 */
	boolean isWithin(ImageProcessor ip, int x, int y, int direction) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		int xmax = width - 1;
		int ymax = height -1;
		switch(direction) {
			case 0:
				return (y>0);
			case 1:
				return (x<xmax && y>0);
			case 2:
				return (x<xmax);
			case 3:
				return (x<xmax && y<ymax);
			case 4:
				return (y<ymax);
			case 5:
				return (x>0 && y<ymax);
			case 6:
				return (x>0);
			case 7:
				return (x>0 && y>0);
		}
		return false;   //should never occur, we use it only for directions 0-7
	} // isWithin

	/* normalize 3-element vector */
	void normalize (double[] v) {
		double norm = 1./Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
		for (int i=0; i<3; i++)
			v[i] *= norm;
			//IJ.log("weights:"+(float)v[0]+","+(float)v[1]+","+(float)v[2]);
			}
}

/*
 * Option Window
 * A JFrame window used to toggle options during the plugin's execution.
*/
/**
 * A JFrame window that is launched at the start of the execution of the main plugin.
 * The window provides checkboxes that toggle options (Invert LUT, Select/Crop Image)
 * in the main plugin. These options are controlled by the boolean variables invertCheck
 * and cropCheck. The boolean variable finished determines when the window has been closed.
 *
 * @author Benn Snyder
 * @author Josh Thomas
 * @version 0.2, 06/02/11
 */
class Option_Window extends JPanel implements ActionListener, ItemListener
{
	// Boolean variables determining whether Invert LUT and Auto Crop Image are selected
	// Also, keeps track of if the window has been closed
	public static boolean invertCheck, cropCheck, includeNegatives, despeckleCheck, watershedCheck, finished;

	// Swing objects
	JCheckBox opt1; // Checkbox for Invert LUT
	JCheckBox opt2; // Checkbox for Auto Crop
	JCheckBox opt3; // Checkbox for Include Negatives
	JCheckBox opt4; // Checkbox for Despeckle
	JCheckBox opt5; // Checkbox for Watershed
	protected static JButton OKButton; // OK Button
	private static JFrame frame; // Main JFrame for the window

	/**
	 * Default constructor for the class.
	 * Defines the window components and adds them to the window.
	 */
	public Option_Window()
	{
		// Applies a 3 row by 1 column grid layout to the window
		super(new GridLayout(3,1));

		// Initializes the boolean variables to false, as the window has first been created
		invertCheck = false;
		cropCheck = false;
		includeNegatives = false;
		finished = false;

		// First panel of components in the window (with components centered)
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		// Label giving the user(s) instructions on how to proceed with selecting the relevant options
		JLabel instructs = new JLabel("<html>Select which options should be performed<br>on the input image.</html>");
		// Adds the label to the panel
		topPanel.add(instructs);
		// Adds the panel to the window (first row)
		add(topPanel);

		// Second panel of components in the window (left-aligned)
		JPanel checkBoxPanel = new JPanel(new FlowLayout());
		// Creates the checkbox for Invert LUT
		opt1 = new JCheckBox("Invert LUT");
		// Sets the Key Event for the checkbox
		opt1.setMnemonic(KeyEvent.VK_I);
		// Sets the initial state of the checkbox to be unchecked
		opt1.setSelected(false);
		// Creates the checkbox for Auto Crop
		opt2 = new JCheckBox("Auto Crop");
		// Sets the Key Event for the checkbox
		opt2.setMnemonic(KeyEvent.VK_C);
		// Sets the initial state of the checkbox to be unchecked
		opt2.setSelected(false);
		// Creates the checkbox for including negative values in the distribution
		opt3 = new JCheckBox("Include Negatives");
		// Sets the Key Event for the checkbox
		opt3.setMnemonic(KeyEvent.VK_C);
		// Sets the initial state of the checkbox to be unchecked
		opt3.setSelected(false);
		// Creates the checkbox for Despeckle
		opt4 = new JCheckBox("Despeckle");
		// Sets the Key Event for the checkbox
		opt4.setMnemonic(KeyEvent.VK_I);
		// Sets the initial state of the checkbox to be unchecked
		opt4.setSelected(false);
		// Creates the checkbox for Watershed
		opt5 = new JCheckBox("Watershed");
		// Sets the Key Event for the checkbox
		opt5.setMnemonic(KeyEvent.VK_I);
		// Sets the initial state of the checkbox to be unchecked
		opt5.setSelected(false);
		
		// Adds an item listener to all checkboxes
		opt1.addItemListener(this);
		opt2.addItemListener(this);
		opt3.addItemListener(this);
		opt4.addItemListener(this);
		opt5.addItemListener(this);

		// Adds all checkboxes to the panel
		checkBoxPanel.add(opt1);
		checkBoxPanel.add(opt2);
		checkBoxPanel.add(opt3);
		checkBoxPanel.add(opt4);
		checkBoxPanel.add(opt5);

		// Adds the panel to the window
		add(checkBoxPanel);

		// Third panel of components in the window (right-aligned)
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		// Creates the OK button to close the window
		OKButton = new JButton("OK");
		// Sets the Key Event for the button
		OKButton.setMnemonic(KeyEvent.VK_O);
		// Sets the string associated with the button for comparison in actionPerformed()
		OKButton.setActionCommand("OK");
		// Adds an action listener to the button
		OKButton.addActionListener(this);

		// Adds the button to the panel
		buttonPanel.add(OKButton);

		// Adds the panel to the window
		add(buttonPanel);
	}

	/**
	 * Creates the JFrame for the window and adds the content pane defined
	 * in the default constructor to the JFrame. Also sets the location of
	 * the window on the screen, sets the default button, and sets the
	 * window to be visible.
	 */
	private static void setup()
	{
		// Creates the JFrame with the title of "New_Plugin Options"
		frame = new JFrame("New_Plugin Options");
		// Prevents the user from clicking the X on the window to close it
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		// Creates a JComponent from the default constructor
		JComponent contentPane = new Option_Window();
		// Makes the content pane non-transparent
		contentPane.setOpaque(true);
		// Sets the content pane of the frame to be the content pane created by the constructor
		frame.setContentPane(contentPane);
		// Sets the default button for the frame to be the OK button
		frame.getRootPane().setDefaultButton(OKButton);
		// Packs all of the components into the frame to prepare to make it visible
		frame.pack();
		// Sets the location of the frame in the center of the screen
		frame.setLocationRelativeTo(null);
		// Pops the frame up
		frame.setVisible(true);
	}

	/**
	 * Handles item events generated by the checkbox objects.
	 * @param e The reference to the ItemEvent that the listener generates
	 */
	public void itemStateChanged(ItemEvent e)
	{
		// Grabs which object triggered the event
		Object box = e.getItemSelectable();
		// Sets the value of the checkbox that was checked to be true
		if (opt1 == box)
		{
			invertCheck = true;
		}
		else if (opt2 == box)
		{
			cropCheck = true;
		}
		else if (opt3 == box)
		{
			includeNegatives = true;
		}
		else if (opt4 == box)
		{
			despeckleCheck = true;
		}
		else if (opt5 == box)
		{
			watershedCheck = true;
		}

		// If, in fact, the checkbox was deselected, then set the value to false
		if(e.getStateChange() == ItemEvent.DESELECTED)
		{
			if (opt1 == box)
			{
				invertCheck = false;
			}
			else if (opt2 == box)
			{
				cropCheck = false;
			}
			else if (opt3 == box)
			{
				includeNegatives = false;
			}
			else if (opt4 == box)
			{
				despeckleCheck = false;
			}
			else if (opt5 == box)
			{
				watershedCheck = false;
			}
		}
	}

	/**
	 * Handles the action events generated by the button.
	 * @param e The reference to the ActionEvent that the listener generates
	 */
	public void actionPerformed(ActionEvent e)
	{
		// If the user clicked the OK button, user is done and destroy window to free memory
		if ("OK".equals(e.getActionCommand()))
		{
			finished = true;
			frame.dispose();
		}
	}

	/**
	 * Driver method for the class. Invokes the setup method to generate the window.
	 */
	public static void start()
	{
		// Runs the setup method to generate the window.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setup();
			}
		});
	}
}

/*
 * Alg
 * A class used to compute the Schwartz-Saltikov algorithm.
*/
/**
 * This class implements the Schwartz-Salkitov algorithm, as defined in Corté and Leibler (2005).
 *
 * @author Benn Snyder
 * @author Ruth Steinhour
 * @author Josh Thomas
 * @version 0.2, 05/25/11
 */
class Alg
{
	// Global variable definitions
	static Vector< Double > Na;
	static double delta = -1;

	/**
	 * The main function of the class. Computes the Schwartz-Saltikov algorithm from
	 * the given input parameters.
	 *
	 * @param data Input data from ImageJ, consisting of the diameters of each particle in pixels
	 * @param nBins The number of bins computed by ImageJ that the particles are placed into
	 * @param thick The thickness of the slice/cross-section in pixels
	 * @param area The area of the image, computed by multiplying the cropped image's width and height
	 *
	 * @return Nv The 3-D particle distribution, stored as an array of doubles
	*/
	public static double[] SSAlg(Vector< Double > data, int nBins, int thick, double area)
	{
		// Finds the minimum and maximum diameters from the data
		double max = Collections.max(data);
		double min = Collections.min(data);

		// Computes delta, the bin width
		if (delta == -1)
		{
			delta = (max-min)/nBins;
		}

		// Constructs Na, the 2-D particle distribution
		Na = new Vector(nBins+1);

		// "counter" variable used to determine the values in Na
		double sum = 0;

		// Nested looping structure that determines sum for each entry in Na and assigns (sum/area) to each entry in Na
		for(int i = 0; i <= nBins; i++)
		{
			for(int j = 0; j < data.size(); j++)
			{
				// Checks to see if the diameter in data falls within the range [i*delta, (i+1)*delta]
				if((data.elementAt(j) >= delta*i) && (data.elementAt(j) <= delta*(i+1)))
				{
					sum++;
				}
			}
			// Stores the value in Na
			Na.add(i, (sum/area));
			sum = 0;
		}

		// Constructs the transition matrix, A (note that A is an upper-triangular matrix)
		double[][] A = new double[nBins+1][nBins+1];

		// "Piecewise" definition of A (see Corté's paper, p. 6363)
		for(int i = 1; i <= nBins+1; i++)
		{
			for(int j = 1; j <= nBins+1; j++)
			{
				if (i == j)
				{
					A[i-1][j-1] = thick+delta*Math.sqrt(Math.pow((j+1),2)-Math.pow(i,2));
				}
				else if (i < j)
				{
					A[i-1][j-1] = delta*(Math.sqrt(Math.pow((j+1),2)-Math.pow(i,2))-Math.sqrt(Math.pow(j,2)-Math.pow(i,2)));
				}
				else
				{
					A[i-1][j-1] = 0;
				}
			}
		}

		// Constructs Nv, the 3-D distribution of the particles
		double[] Nv = new double[nBins+1];

		// Back-substitution implementation
		Nv[nBins] = Na.lastElement() / A[nBins][nBins]; // Assigns the last element of Nv to be Na's last element divided by A[last][last]

		// Back-populates Nv - k = rows, q = columns
		for(int k = nBins-1; k >= 0; k--)
		{
			sum = 0;
			for(int q = 0; q < nBins+1; q++)
			{
				// Only care when we are at or above the main diagonal, as A is upper-triangular
				if(q >= k)
				{
					sum += A[k][q] * Nv[q];
				}
			}
			Nv[k] = (Na.elementAt(k) - sum) / A[k][k];
		}
		// Returns the 3-D distribution
		return Nv;
	}

	/**
	 * Runs the Schwartz-Saltikov algorithm, but takes an additional parameter - binWidth.
	 *
	 * @param data Input data from ImageJ, consisting of the diameters of each particle in pixels
	 * @param nBins The number of bins computed by ImageJ that the particles are placed into
	 * @param binWidth The width of each bin
	 * @param thick The thickness of the slice/cross-section in pixels
	 * @param area The area of the image, computed by multiplying the cropped image's width and height
	 *
	 * @return Nv The 3-D particle distribution, stored as an array of doubles
	 */
	public static double[] SSAlg(Vector<Double> data, int nBins, float binWidth, int thick, double area)
	{
		delta = binWidth;
		return SSAlg(data, nBins, thick, area);
	}
}

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
 * @version 0.2, 05/31/11
 */
class Graph_Window extends JPanel
{
	// Static global variables to conform to the initial constructor call and the second constructor call in setup
	static JFrame frame;
	static Vector<Double> Na;
	static double[] Nv0;
	static double[] Nv50;
	static double[] Nv100;
	static double max;
	static int nBins;

	// Plot object using ptplot
	static Plot p;

	/**
	 * First constructor for the class, which is called during the execution of run() in the New_Plugin class.
	 * This constructor takes in the pertinent data that has been grabbed and generated in the main class and stores
	 * the data in global variables to be used when constructing the plot in the frame.
	 *
	 * @param Na The 2-D particle distribution obtained in the New_Plugin class.
	 * @param Nv0 The 3-D particle distribution obtained in the New_Plugin class, with a thickness of 0 pixels.
	 * @param Nv50 The 3-D particle distribution obtained in the New_Plugin class, with a thickness of 50 pixels.
	 * @param Nv100 The 3-D particle distribution obtained in the New_Plugin class, with a thickness of 100 pixels.
	*/
	public Graph_Window(Vector<Double> Na, double[] Nv0, double[] Nv50, double[] Nv100, double max, int nBins)
	{
		// Stores the input arguments into the class-local variables
		this.Na = Na;
		this.Nv0 = Nv0;
		this.Nv50 = Nv50;
		this.Nv100 = Nv100;
		this.max = max;
		this.nBins = nBins;
	}

	/**
	 * Second constructor for the class, called during the setup of the JFrame. This constructor
	 * creates the Swing components and the Plot components. It places them in the JFrame.
	*/
	public Graph_Window()
	{
		// Gives the class's JPanel (since it extends that) a BorderLayout.
		super(new BorderLayout());

		// Creates the JPanel to be placed in the BorderLayout - this JPanel has its components centered
		JPanel graphPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		// Creates a new Plot object
		p = new Plot();
		p.setSize(750, 500); // Sets the size of the coordinate axes window that is generated
		p.setTitle("Plot of CDFs, 2-D Distribution, 3-D Distribution (H = 0, H = 50, H = 100)"); // Sets the "figure's" title
		p.setGrid(false); // No grid behind the plotted functions/points
		p.setYRange(0, 100); // Sets the range of the y-axis
		p.setXRange(max/(nBins+1), (max*(nBins+2))/(nBins+1)); // Sets the domain of the x-axis
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
		for (int i = 0; i < Na.size(); i++)
		{
			tsumA += Na.elementAt(i);
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
			csumA += Na.elementAt(i-1);
			csumV0 += Nv0[i-1];
			csumV50 += Nv50[i-1];
			csumV100 += Nv100[i-1];

			// Adds the points to the Plot object for each data set
			// x-axis is particle diameter
			// y-axis is cumulative percent of number of particles whose diameter is less than or equal to the given diameter
			p.addPoint(0, i*(max/(nBins+1)), (csumA/tsumA)*100, connected);
			p.addPoint(1, i*(max/(nBins+1)), (csumV0/tsumV0)*100, connected);
			p.addPoint(2, i*(max/(nBins+1)), (csumV50/tsumV50)*100, connected);
			p.addPoint(3, i*(max/(nBins+1)), (csumV100/tsumV100)*100, connected);

			// Connects points after the first iteration of the loop
			connected = true;
		}

		// Scales plot to fit data
		p.fillPlot();

		// Adds the legend for the data sets
		p.addLegend(0, "2-D Distribution");
		p.addLegend(1, "H = 0 px");
		p.addLegend(2, "H = 50 px");
		p.addLegend(3, "H = 100 px");

		// Adds the Plot object to the JPanel
		graphPanel.add(p);

		// Adds the JPanel to the BorderLayout
		add(graphPanel, BorderLayout.CENTER);
	}

	/**
	 * Creates the JFrame for the window and adds the content pane defined
	 * in the default constructor to the JFrame. Also sets the location of
	 * the window on the screen, sets the default button, and sets the
	 * window to be visible.
	 */
	private static void setup()
	{
		// Creates the JFrame with the title of "Graph of 2-D CDF and 3-D CDF"
		frame = new JFrame("Graph of 2-D CDF and 3-D CDF");

		// Creates a JComponent from the secondary constructor
		JComponent contentPane = new Graph_Window();
		// Makes the content pane non-transparent
		contentPane.setOpaque(true);
		// Sets the content pane of the frame to be the content pane created by the constructor
		frame.setContentPane(contentPane);
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
	public static void start()
	{
		// Runs the setup method to generate the window.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
											   public void run() {
											   setup();
											   }
											   });
	}
}

/*
 * New_Plugin
 * The main class of the file, which represents the entire plugin.
*/
/**
 * The main class of the code. This class represents the plugin developed for Goodyear
 * Tire and Rubber Company. This plugin automates a previously manual process of analyzing
 * a source image (2-D cross section obtained via electron microscope), implements the
 * Schwartz-Saltikov algorithm for converting from a 2-D particle distribution to a 3-D
 * particle distribution, and produces a plot of the resulting diameters in the distributions.
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
	 * Automatically crops the image.
	 * This can be toggled on and off in run() via a boolean variable in Option_Window.
	 */
	private void autoCrop()
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

		// Use the Versatile_Wand in the white point and then crop the image.
		new Versatile_Wand().mousePressed(Integer.toString(curX), Integer.toString(curY));
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
		IJ.run("Threshold...");
		// Wait to make sure the Threshold dialog is ready.
		pause(500);

		// Wait for the user to close the Threshold window
		while ((WindowManager.getFrontWindow() != null) && (WindowManager.getFrontWindow().getTitle().equals("Threshold")))
		{
			pause(500);
		}
	}
	
	private float sizeBins(float[] data, ResultsTable rt)
	{
		float [] pars = new float [11];
		stats(rt.getCounter(), data, pars);
		// sd = 7, min = 3, max = 4
		// use Scott's method (1979 Biometrika, 66:605-610) for optimal binning: 3.49*sd*N^-1/3
		return (float)(3.49 * pars[7]*(float)Math.pow(rt.getCounter(), -1.0/3.0));		
	}
	
	private int numBins(float[] data, ResultsTable rt)
	{
		float [] pars = new float [11];
		stats(rt.getCounter(), data, pars);
		int nBins = (int)Math.floor(((pars[4]-pars[3])/sizeBins(data, rt))+.5);
		if (nBins < 2)
		{
			nBins = 2;
		}
		return nBins;
	}

	/**
	 * Main driving method for the New_Plugin class. Executes the main functionality for the class.
	*/
	public void run(ImageProcessor ip)
	{
		// PlugInFilter automatically locks images, so we need to unlock them.
		imp.unlock();

		this.ip = ip;

		// Generates the Option Window to determine what the user needs to do for the given image
		Option_Window ow = new Option_Window();
		ow.start();
		// Waits until the Option Window is closed before proceeding
		while (!ow.finished)
		{
			pause(500);
		}
		// Checks if the user specified that the image needs its LUT inverted
		if (ow.invertCheck)
		{
			ip.invert();
		}
		// Checks if the user specified that the image needs to be cropped
		if (ow.cropCheck)
		{
			autoCrop();
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
		IJ.run("Set Measurements...", "area shape redirect=None decimal=3");
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

		float binWidth = sizeBins(data, rt);
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

		// Grabs the maximum diameter from the data set of diameters
		double max = Collections.max(imageData);
		// Creates and launches the Graph Window, which plots Na and each of the computed Nv sets
		Graph_Window gw = new Graph_Window(Alg.Na, results, results2, results3, max, nBins);
		gw.start();		

		// Updates the image on screen
		imp.updateAndDraw();

		// relock image?
		imp.lock();
	}
}
