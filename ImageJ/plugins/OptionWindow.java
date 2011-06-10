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
 * @version 0.2, 06/02/11
 */
class OptionWindow extends JPanel implements ActionListener, ItemListener, ChangeListener
{
	// Boolean variables determining whether each checkbox is checked
	public static boolean invertCheck = false, cropCheck = false, includeNegatives = false, despeckleCheck = false, watershedCheck = false, thresholdCheck = false, grayscaleCheck = false, removeScaleCheck = false;
	// Keeps track of if the window has been closed
	public static boolean finished = false;
	public static int percentParticles = 100;


	// Swing objects
	// Checkboxes for each property
	JCheckBox invertBox, cropBox, negativesBox, despeckleBox, watershedBox, thresholdBox, grayscaleBox, removeScaleBox;
	protected static JButton OKButton; // OK Button
	private static JFrame frame; // Main JFrame for the window

	/**
	 * Default constructor for the class.
	 * Defines the window components and adds them to the window.
	 */
	public OptionWindow()
	{
		// Applies a 4 row by 1 column grid layout to the window
		super(new BorderLayout());
		
		// fixes bug when opening new image
		finished = false;
		
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
		JPanel checkBoxPanel = new JPanel(new GridLayout(3, 3));
		
		// Creates the checkboxes for the options
		invertBox = new JCheckBox("Invert LUT");
		cropBox = new JCheckBox("Auto Crop");
		negativesBox = new JCheckBox("Include Negatives");
		despeckleBox = new JCheckBox("Despeckle");
		watershedBox = new JCheckBox("Watershed");
		thresholdBox = new JCheckBox("Binarize Image");
		grayscaleBox = new JCheckBox("Convert to Grayscale");
		removeScaleBox = new JCheckBox("Remove Scale");
		
		// Sets the Key Event for the checkboxes
		invertBox.setMnemonic(KeyEvent.VK_I);
		cropBox.setMnemonic(KeyEvent.VK_C);
		negativesBox.setMnemonic(KeyEvent.VK_N);
		despeckleBox.setMnemonic(KeyEvent.VK_D);
		watershedBox.setMnemonic(KeyEvent.VK_W);
		thresholdBox.setMnemonic(KeyEvent.VK_T);
		grayscaleBox.setMnemonic(KeyEvent.VK_G);
		removeScaleBox.setMnemonic(KeyEvent.VK_S);
		
		// Sets the initial state of the checkboxes to be unchecked
		invertBox.setSelected(false);
		cropBox.setSelected(false);
		negativesBox.setSelected(false);
		despeckleBox.setSelected(false);
		watershedBox.setSelected(false);
		thresholdBox.setSelected(false);
		grayscaleBox.setSelected(false);
		removeScaleBox.setSelected(false);

		// Adds an item listener to all checkboxes
		invertBox.addItemListener(this);
		cropBox.addItemListener(this);
		negativesBox.addItemListener(this);
		despeckleBox.addItemListener(this);
		watershedBox.addItemListener(this);
		thresholdBox.addItemListener(this);
		grayscaleBox.addItemListener(this);
		removeScaleBox.addItemListener(this);

		// Adds all checkboxes to the panel
		checkBoxPanel.add(grayscaleBox);
		checkBoxPanel.add(removeScaleBox);
		checkBoxPanel.add(thresholdBox);
		checkBoxPanel.add(invertBox);
		checkBoxPanel.add(cropBox);
		checkBoxPanel.add(despeckleBox);
		checkBoxPanel.add(watershedBox);
		checkBoxPanel.add(negativesBox);

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
		//add(slidePanel);
		
		//JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		//sliderPanel.add(slider);
		//add(sliderPanel);
		
		//add(slider);
		

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
		//buttonPanel.add(OKButton);
		
		buttonPanel.add(OKButton);
		slidePanel.add(buttonPanel, BorderLayout.SOUTH);
		add(slidePanel, BorderLayout.SOUTH);

		// Adds the panel to the window
		//add(buttonPanel);
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
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// Frame cannot be resized
		frame.setResizable(false);
		// Creates a JComponent from the default constructor
		JComponent contentPane = new OptionWindow();
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