import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

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
 * @version 0.2.5, 06/17/11
 */
class OptionWindow extends JPanel implements ActionListener, ItemListener, ChangeListener
{
	// Boolean variables for each checkbox
	public boolean invertCheck, cropCheck, includeNegatives, despeckleCheck, watershedCheck, thresholdCheck, grayscaleCheck, removeScaleCheck, PDFCheck, PDFCheck2, CDFCheck;
	public boolean[] thicknessCheck;
	// Keeps track of if the window has been closed
	public boolean finished;
	public int percentParticles = 100;

	// Swing objects
	// Checkboxes for each property
	JCheckBox invertBox, cropBox, negativesBox, despeckleBox, watershedBox, thresholdBox, grayscaleBox, removeScaleBox, PDFBox, PDFBox2, CDFBox, thick0Box, thick50Box, thick100Box;
	protected JButton OKButton; // OK Button
	private JFrame frame; // Main JFrame for the window

	/**
	 * Default constructor for the class.
	 * Defines the window components and adds them to the window.
	 */
	public OptionWindow()
	{
		// Applies a 4 row by 1 column grid layout to the window
		super(new BorderLayout());
		
		thicknessCheck = new boolean[3];

		// Creates the JFrame with the title of "New_Plugin Options"
		frame = new JFrame("New_Plugin Options");
		// Prevents the user from clicking the X on the window to close it
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// Frame cannot be resized
		frame.setResizable(false);

		// First panel of components in the window (with components centered)
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		// Label giving the user(s) instructions on how to proceed with selecting the relevant options
		JLabel instructs = new JLabel("Select which options should be performed on the input image.");
		// Adds the label to the panel
		topPanel.add(instructs);
		// Adds the panel to the window (first row)
		add(topPanel, BorderLayout.NORTH);

		// Second panel of components in the window (left-aligned)
		//JPanel checkBoxPanel = new JPanel(new FlowLayout());
		JPanel checkBoxPanel = new JPanel(new GridLayout(5, 3));

		// Creates the checkboxes for the options
		invertBox = new JCheckBox("Invert LUT");
		cropBox = new JCheckBox("Auto Crop");
		negativesBox = new JCheckBox("Include Negatives");
		despeckleBox = new JCheckBox("Despeckle");
		watershedBox = new JCheckBox("Watershed");
		thresholdBox = new JCheckBox("Binarize Image");
		grayscaleBox = new JCheckBox("Convert to Grayscale");
		removeScaleBox = new JCheckBox("Remove Scale");
		PDFBox = new JCheckBox("Display PDFs (Total)");
		PDFBox2 = new JCheckBox("Display PDFs (Fractional)");
		CDFBox = new JCheckBox("Display CDFs");
		thick0Box = new JCheckBox("Display H=0");
		thick50Box = new JCheckBox("Display H=50");
		thick100Box = new JCheckBox("Display H=100");

		// Sets the Key Event for the checkboxes
		invertBox.setMnemonic(KeyEvent.VK_I);
		cropBox.setMnemonic(KeyEvent.VK_C);
		negativesBox.setMnemonic(KeyEvent.VK_N);
		despeckleBox.setMnemonic(KeyEvent.VK_D);
		watershedBox.setMnemonic(KeyEvent.VK_W);
		thresholdBox.setMnemonic(KeyEvent.VK_T);
		grayscaleBox.setMnemonic(KeyEvent.VK_G);
		removeScaleBox.setMnemonic(KeyEvent.VK_S);
		PDFBox.setMnemonic(KeyEvent.VK_P);
		PDFBox2.setMnemonic(KeyEvent.VK_F);
		CDFBox.setMnemonic(KeyEvent.VK_U);
		thick0Box.setMnemonic(KeyEvent.VK_H);
		thick50Box.setMnemonic(KeyEvent.VK_J);
		thick100Box.setMnemonic(KeyEvent.VK_K);

		// Sets the initial state of the checkboxes to be unchecked
		invertBox.setSelected(false);
		cropBox.setSelected(false);
		negativesBox.setSelected(false);
		despeckleBox.setSelected(false);
		watershedBox.setSelected(false);
		thresholdBox.setSelected(false);
		grayscaleBox.setSelected(false);
		removeScaleBox.setSelected(false);
		PDFBox.setSelected(false);
		PDFBox2.setSelected(false);
		CDFBox.setSelected(false);
		thick0Box.setSelected(false);
		thick50Box.setSelected(false);
		thick100Box.setSelected(false);
		
		// Renders the thickness checkboxes to be disabled until the user has selected
		// that they want to display either the PDFs, the CDFs, or both
		thick0Box.setEnabled(false);
		thick50Box.setEnabled(false);
		thick100Box.setEnabled(false);

		// Adds an item listener to all checkboxes
		invertBox.addItemListener(this);
		cropBox.addItemListener(this);
		negativesBox.addItemListener(this);
		despeckleBox.addItemListener(this);
		watershedBox.addItemListener(this);
		thresholdBox.addItemListener(this);
		grayscaleBox.addItemListener(this);
		removeScaleBox.addItemListener(this);
		PDFBox.addItemListener(this);
		PDFBox2.addItemListener(this);
		CDFBox.addItemListener(this);
		thick0Box.addItemListener(this);
		thick50Box.addItemListener(this);
		thick100Box.addItemListener(this);

		// Adds all checkboxes to the panel
		checkBoxPanel.add(grayscaleBox);
		checkBoxPanel.add(removeScaleBox);
		checkBoxPanel.add(thresholdBox);
		checkBoxPanel.add(invertBox);
		checkBoxPanel.add(cropBox);
		checkBoxPanel.add(despeckleBox);
		checkBoxPanel.add(watershedBox);
		checkBoxPanel.add(negativesBox);
		checkBoxPanel.add(PDFBox);
		checkBoxPanel.add(PDFBox2);
		checkBoxPanel.add(CDFBox);
		checkBoxPanel.add(thick0Box);
		checkBoxPanel.add(thick50Box);
		checkBoxPanel.add(thick100Box);

		// Adds the panel to the window
		add(checkBoxPanel, BorderLayout.CENTER);

		JSlider slider = new JSlider(0, 100, percentParticles);
		slider.addChangeListener(this);
		slider.setMajorTickSpacing(10);
		slider.setMinorTickSpacing(5);
		slider.setPaintTicks(true);
		slider.setSnapToTicks(true);
		slider.setPaintLabels(true);

		JPanel slidePanel = new JPanel(new BorderLayout());
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JLabel partLabel = new JLabel("Percent of particles to display in the 3-D Visualization:");
		labelPanel.add(partLabel);
		slidePanel.add(labelPanel, BorderLayout.NORTH);
		slidePanel.add(slider, BorderLayout.CENTER);

		// Third panel of components in the window (right-aligned)
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		// Creates the OK button to close the window
		OKButton = new JButton("OK");
		// Sets the Key Event for the button
		OKButton.setMnemonic(KeyEvent.VK_O);
		// Sets the string associated with the button for comparison in actionPerformed()
		OKButton.setActionCommand("OK");
		// Adds an action listener to the button
		OKButton.addActionListener(this);
		OKButton.setSize(100,75);

		// Adds the button to the panel
		buttonPanel.add(OKButton);
		slidePanel.add(buttonPanel, BorderLayout.SOUTH);
		add(slidePanel, BorderLayout.SOUTH);

		// Makes the panel non-transparent
		setOpaque(true);
		// Sets the content pane of the frame to be this window
		frame.setContentPane(this);
		// Sets the default button for the frame to be the OK button
		frame.getRootPane().setDefaultButton(OKButton);
	}

	/**
	 * Handles item events generated by the checkbox objects.
	 *
	 * @param e The reference to the ItemEvent that the listener generates
	 */
	public void itemStateChanged(ItemEvent e)
	{
		// Grabs which object triggered the event
		Object box = e.getItemSelectable();
		// Sets the value of the checkbox that was checked to be true
		if (invertBox == box)
		{
			invertCheck = true;
		}
		else if (cropBox == box)
		{
			cropCheck = true;
		}
		else if (negativesBox == box)
		{
			includeNegatives = true;
		}
		else if (despeckleBox == box)
		{
			despeckleCheck = true;
		}
		else if (watershedBox == box)
		{
			watershedCheck = true;
		}
		else if (thresholdBox == box)
		{
			thresholdCheck = true;
		}
		else if (grayscaleBox == box)
		{
			grayscaleCheck = true;
		}
		else if (removeScaleBox == box)
		{
			removeScaleCheck = true;
		}
		else if (PDFBox == box)
		{
			PDFCheck = true;
			thick0Box.setEnabled(true);
			thick50Box.setEnabled(true);
			thick100Box.setEnabled(true);
		}
		else if (PDFBox2 == box)
		{
			PDFCheck2 = true;
			thick0Box.setEnabled(true);
			thick50Box.setEnabled(true);
			thick100Box.setEnabled(true);
		}
		else if (CDFBox == box)
		{
			CDFCheck = true;
			thick0Box.setEnabled(true);
			thick50Box.setEnabled(true);
			thick100Box.setEnabled(true);
		}
		else if (thick0Box == box)
		{
			thicknessCheck[0] = true;
		}
		else if (thick50Box == box)
		{
			thicknessCheck[1] = true;
		}
		else if (thick100Box == box)
		{
			thicknessCheck[2] = true;
		}

		// If, in fact, the checkbox was deselected, then set the value to false
		if(e.getStateChange() == ItemEvent.DESELECTED)
		{
			if (invertBox == box)
			{
				invertCheck = false;
			}
			else if (cropBox == box)
			{
				cropCheck = false;
			}
			else if (negativesBox == box)
			{
				includeNegatives = false;
			}
			else if (despeckleBox == box)
			{
				despeckleCheck = false;
			}
			else if (watershedBox == box)
			{
				watershedCheck = false;
			}
			else if (thresholdBox == box)
			{
				thresholdCheck = false;
			}
			else if (grayscaleBox == box)
			{
				grayscaleCheck = false;
			}
			else if (removeScaleBox == box)
			{
				removeScaleCheck = false;
			}
			else if (PDFBox == box)
			{
				PDFCheck = false;
				if (!CDFCheck && !PDFCheck2)
				{
					thick0Box.setSelected(false);
					thick0Box.setEnabled(false);
					thicknessCheck[0] = false;
					thick50Box.setSelected(false);
					thick50Box.setEnabled(false);
					thicknessCheck[1] = false;
					thick100Box.setSelected(false);
					thick100Box.setEnabled(false);
					thicknessCheck[2] = false;
				}
			}
			else if (PDFBox2 == box)
			{
				PDFCheck2 = false;
				if (!CDFCheck && !PDFCheck)
				{
					thick0Box.setSelected(false);
					thick0Box.setEnabled(false);
					thicknessCheck[0] = false;
					thick50Box.setSelected(false);
					thick50Box.setEnabled(false);
					thicknessCheck[1] = false;
					thick100Box.setSelected(false);
					thick100Box.setEnabled(false);
					thicknessCheck[2] = false;
				}
			}
			else if (CDFBox == box)
			{
				CDFCheck = false;
				if (!PDFCheck && !PDFCheck2)
				{
					thick0Box.setSelected(false);
					thick0Box.setEnabled(false);
					thicknessCheck[0] = false;
					thick50Box.setSelected(false);
					thick50Box.setEnabled(false);
					thicknessCheck[1] = false;
					thick100Box.setSelected(false);
					thick100Box.setEnabled(false);
					thicknessCheck[2] = false;
				}
			}
			else if (thick0Box == box)
			{
				thicknessCheck[0] = false;
			}
			else if (thick50Box == box)
			{
				thicknessCheck[1] = false;
			}
			else if (thick100Box == box)
			{
				thicknessCheck[2] = false;
			}
		}
	}

	public void stateChanged(ChangeEvent e)
	{
		JSlider source = (JSlider)e.getSource();
		if (!source.getValueIsAdjusting())
		{
			percentParticles = source.getValue();
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
