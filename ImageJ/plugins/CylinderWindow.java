// Package Imports

// Java Packages
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

// OpenGL Packages
import javax.media.opengl.glu.*;
import javax.media.opengl.*;

// GLUT Package
import com.sun.opengl.util.GLUT;


/*
 * Cylinder Window
 * A JFrame window that uses OpenGL (JOGL) to produce a 3-D visualization of cylinders
 * extending outward from ellipses in a 2-D image.
*/
/**
 * A JFrame window that is launched at the end of the execution of the main plugin.
 * The window provides a 3-D visualization of cylinders which come from their imprint (ellipses)
 * on a 2-D plane.
 *
 * @author Benn Snyder
 * @author Josh Thomas
 * @version 0.5, 06/28/11
 */
class CylinderWindow extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener
{
	// Global Variables

	// OpenGL variables
	private GLU glu;
	private GLUT glut;
	private GLCapabilities caps;
	private GLCanvas canvas;

	// Mouse variables
	private Point pickPoint = new Point();
	int startx, starty;

	// Trackball Movement variables
	float angle = 20;
	float angle2 = 15;

	// Quadric object for particles (spheres)
	GLUquadric quad;

	// Variables to be passed in to the constructor
	int imageW = 0, imageH = 0;
	double[] xLocs, yLocs, minorA, rotateAng, cylAng;
	boolean lightOn = false;
	int percents = 100;
		
	// Array of which cylinders are randomly chosen if percents != 100
	int[] chosenParts;
	
	// Eh? What's this do...?
	boolean teapots = false;

	// Length/height of the cylinders
	final float height = 400;

	// Lighting Variables

	// Colors for objects
	float mat_emission2[] = {0.5f, 0.5f, 0.5f, 1.0f}; // Used for ambient, diffuse, and specular lighting - spheres
	float clear_mat[] = {0.0f, 0.0f, 0.0f, 1.0f}; // Used for emission lighting

	// Light position
	float lightPosition[] = {-20.0f, 20.0f, 150.0f, 1.0f};

	// Ambient
	float light_ambient[] = {1.0f, 1.0f, 1.0f, 1.0f}; // ambient light intensity

	// Diffuse
	float light_diffuse[] = {1.0f, 1.0f, 1.0f, 1.0f}; // diffuse light intensity

	// Specular
	float light_specular[] = {1.0f, 1.0f, 1.0f, 1.0f}; // specular light intensity
	float light_shininess[] = {100, 100, 100, 100}; // shininess value

	/**
	 * Default constructor for the class. Builds the JFrame, assigns the passed-in data
	 * to the class variables, and configures the GLCanvas.
	 *
	 * @param x An array of the x-coordinates for each ellipse's center of mass.
	 * @param y An array of the y-coordinates for each ellipse's center of mass.
	 * @param minAxis An array of the lengths of each ellipse's minor axis (radius).
	 * @param rotAngle An array of the angles each ellipse has been rotated by to make them not axially-aligned.
	 * @param cylAngle An array of the computed angles for each cylinder.
	 * @param w The image's width in pixels.
	 * @param h The image's height in pixels.
	 * @param flag Specifies whether lighting should be turned on or off.
	 * @param perParts Percentage of cylinders to display.
	 */
	public CylinderWindow(double[] x, double[] y, double[] minAxis, double[] rotAngle, double[] cylAngle, int w, int h, boolean flag, int perParts)
	{
		// Sets the title for the JFrame
		super("Filament 3-D Representation");

		// Initializes the OpenGL variables
		glu = new GLU();
		glut = new GLUT();
		quad = glu.gluNewQuadric();

		// Assigns the passed-in data to the class variables
		imageW = w;
		imageH = h;
		xLocs = x;
		yLocs = y;
		minorA = minAxis;
		rotateAng = rotAngle;
		cylAng = cylAngle;
		lightOn = flag;
		percents = perParts;

		// Initializes the GLCapabilities object and specifies that we want double buffering
		caps = new GLCapabilities();
		caps.setDoubleBuffered(true);

		// Initializes the GLCanvas, using the GLCapabilities object
		canvas = new GLCanvas(caps);
		canvas.addGLEventListener(this); // Sets the Event Listener
		canvas.addKeyListener(this); // Sets the Keyboard Listener
		canvas.addMouseListener(this); // Sets the Mouse Listener
		canvas.addMouseMotionListener(this); // Sets the Mouse Motion Listener
		getContentPane().add(canvas); // Adds the GLCanvas to the JFrame
		
		if (percents != 100)
		{
			randomCylinders();
		}
	}
	
	

	/**
	 * The "main" method of the class. Sets properties for the JFrame and makes it visible.
	*/
	public void run()
	{
		// Sets the frame's size
		setSize(500, 500);
		// Sets the frame to appear in the center of the screen
		setLocationRelativeTo(null);
		// Closes only the Frame when the X (Close) button is pressed
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		// Makes the frame visible
		setVisible(true);
		// Requests focus to the window
		canvas.requestFocusInWindow();
	}

	/**
	 * Initialization method for the OpenGL aspects of the frame. Sets the lighting and
	 * orthographic projection for the 3-D view.
	 *
	 * @param drawable A GLAutoDrawable object. Used to keep the correct GL object.
	*/
	public void init(GLAutoDrawable drawable)
	{
		// Gets the current GL object
		GL gl = drawable.getGL();

		// Sets the background color for the 3-D view
		gl.glClearColor(0, 0, 0, 1);
		// Enables depth testing
		gl.glEnable(GL.GL_DEPTH_TEST);
		// Enables the normalization of normal vectors
		gl.glEnable(GL.GL_NORMALIZE);

		if (lightOn)
		{
			// Sets lighting properties
			gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lightPosition, 0);
			gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, light_ambient, 0);
			gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, light_diffuse, 0);
			gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, light_specular, 0);
			gl.glLightfv(GL.GL_LIGHT0, GL.GL_SHININESS, light_shininess, 0);
		}
		// Sets the matrix mode to Projection to set the orthographic view
		gl.glMatrixMode(GL.GL_PROJECTION);
		// Loads the identity matrix
		gl.glLoadIdentity();
		// Defines the orthographic view
		gl.glOrtho(-(imageW), (imageW), -(imageH), (imageH), -(imageW + imageH), (imageW + imageH));
		// Returns the matrix mode to the Model View for 3-D display
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}
	
	/**
	 * Decides (via Random) which cylinders should be drawn if the user
	 * specified that only a certain percentage should be in the rendering.
	 */
	public void randomCylinders()
	{
		Random RNG = new Random();
		
		chosenParts = new int[(int)(xLocs.length * (percents/100.0f))];
		
		for (int i = 0; i < chosenParts.length; i++)
		{
			boolean goAgain = false;
			do
			{
				goAgain = false;
				int tmp = RNG.nextInt(xLocs.length);
				
				for (int j = 0; j < i; j++)
				{
					if (chosenParts[j] == tmp)
					{
						goAgain = true;
					}
				}
				if (!goAgain)
				{
					chosenParts[i] = tmp;
				}
			}
			while (goAgain);
		}
	}

	/**
	 * Display method for OpenGL. Handles the drawing of objects and swaps the buffers on
	 * the canvas for fast 3-D rendering during trackball movement.
	 *
	 * @param drawable A GLAutoDrawable object. Used to keep the correct GL object.
	*/
	public void display(GLAutoDrawable drawable)
	{
		// Gets current GL object
		GL gl = drawable.getGL();

		// Clears the viewport and depth buffer
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		// Loads the identity matrix
		gl.glLoadIdentity();

		if (lightOn)
		{
			// Enables lighting and the specified light for the projection
			gl.glEnable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_LIGHT0);
		}

		// Pushes the current matrix to the stack
		gl.glPushMatrix();

		// Perform scene rotations based on user mouse input (trackball movement)
		gl.glRotatef(angle2, 1, 0, 0);
		gl.glRotatef(angle, 0, 1, 0);
		// Pushes current matrix to the stack
		gl.glPushMatrix();

		// Sets up the boundaries of the sample box
		drawBoundingBox(drawable);

		if (percents == 100)
		{
			// Draws each of the cylinders
			for(int i = 0; i < xLocs.length; i++)
			{
				drawCylinders(drawable, xLocs[i], yLocs[i], minorA[i], rotateAng[i], cylAng[i], lightOn);
			}
		}
		else
		{
			// Draws the randomly selected cylinders
			for(int i = 0; i < chosenParts.length; i++)
			{
				drawCylinders(drawable, xLocs[chosenParts[i]], yLocs[chosenParts[i]], minorA[chosenParts[i]], rotateAng[chosenParts[i]], cylAng[chosenParts[i]], lightOn);
			}
		}

		if (lightOn)
		{
			// Disables lighting
			gl.glDisable(GL.GL_LIGHTING);
		}

		// Pops the above matrix pushes off of the stack
		gl.glPopMatrix();
		gl.glPopMatrix();

		// Swaps buffers (back buffer -> front buffer, renders image)
		canvas.swapBuffers();
	}

	/**
	 * Draws the lines that make up the "wireframe" box.
	 *
	 * @param drawable A GLAutoDrawable object. Used to keep the correct GL object.
	*/
	void drawBoundingBox(GLAutoDrawable drawable)
	{
		// Gets current GL object
		GL gl = drawable.getGL();

		float zDepth = (float)(imageW+imageH) / 4;

		// Pushes the current matrix to the stack
		gl.glPushMatrix();
		// Draws the first side of the box
		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(0.33f, 0.33f, 0.33f); // Sets the color for the lines
		// Four vertices making up the first side, based on image dimensions
		// and max particle diameter
		gl.glVertex3f((-imageW / 2), (-imageH / 2), zDepth);
		gl.glVertex3f((-imageW / 2), (imageH / 2), zDepth);
		gl.glVertex3f((-imageW / 2), (imageH / 2), -zDepth);
		gl.glVertex3f((-imageW / 2), (-imageH / 2), -zDepth);
		gl.glEnd();

		// Second side of the box - since the first side looped around, we only need
		// to use connected strips now (to prevent overlap of the lines)
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glColor3f(0.33f, 0.33f, 0.33f); // Sets the color for the lines
		// Four vertices making up the second side
		gl.glVertex3f((-imageW / 2), (-imageH / 2), zDepth);
		gl.glVertex3f((imageW / 2), (-imageH / 2), zDepth);
		gl.glVertex3f((imageW / 2), (imageH / 2), zDepth);
		gl.glVertex3f((-imageW / 2), (imageH / 2), zDepth);
		gl.glEnd();

		// Third side of the box
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glColor3f(0.33f, 0.33f, 0.33f); // Sets the color for the lines
		// Four vertices making up the third side
		gl.glVertex3f((imageW / 2), (-imageH / 2), zDepth);
		gl.glVertex3f((imageW / 2), (-imageH / 2), -zDepth);
		gl.glVertex3f((imageW / 2), (imageH / 2), -zDepth);
		gl.glVertex3f((imageW / 2), (imageH / 2), zDepth);
		gl.glEnd();

		// Fourth and final side of the box - no need to do the top and bottom
		// as all of those edges have been make already
		gl.glBegin(GL.GL_LINE_STRIP);
		gl.glColor3f(0.33f, 0.33f, 0.33f); // Sets the color for the lines
		// Four vertices making up the fourth side
		gl.glVertex3f((imageW / 2), (-imageH / 2), -zDepth);
		gl.glVertex3f((-imageW / 2), (-imageH / 2), -zDepth);
		gl.glVertex3f((-imageW / 2), (imageH / 2), -zDepth);
		gl.glVertex3f((imageW / 2), (imageH / 2), -zDepth);
		gl.glEnd();

		// Pops the current matrix off of the stack
		gl.glPopMatrix();
	}

	/**
	 * Draws a cylinder, given its location, radius, and rotation angles.
	 *
	 * @param drawable A GLAutoDrawable object. Used to keep the correct GL object.
	 * @param x The x-coordinate of the ellipse's center of mass in the original 2-D image.
	 * @param y The y-coordinate of the ellipse's center of mass in the original 2-D image.
	 * @param rad The cylinder's radius. Comes from the minor axis length of the ellipse.
	 * @param XYRotation The angle measurement (in degrees) that the cylinder must be rotated by to line it up with the ellipse (removal of axial alignment).
	 * @param CylRotation The height angle measurement (in degrees) that the cylinder must be rotated to produce the correct ellipse in 2-D.
	 * @param light Boolean specifying whether lighting is on or off.
	*/
	void drawCylinders(GLAutoDrawable drawable, double x, double y, double rad, double XYRotation, double CylRotation, boolean light)
	{
		// Gets the current GL object
		GL gl = drawable.getGL();

		// Pushes the current matrix to the stack
		gl.glPushMatrix();

		// Translates the cylinder to the correct spatial location
		gl.glTranslatef((float) x-((float)(imageW/2)), 0, (float) y-((float)(imageH/2)));
		// Rotates the cylinder to remove axial alignment
		gl.glRotatef((float) XYRotation, 0, 1, 0);
		// Rotates the cylinder to be aligned with the x-axis
		gl.glRotatef(-90, 0, 1, 0);
		// Rotates the cylinder from being upright to pitched at the correct angle
		gl.glRotatef(-(float) CylRotation, 1, 0, 0);
		// Moves the cylinder so that the center is at the origin
		gl.glTranslatef(0, -height/2, 0);
		// Rotates the cylinder to be upright, base at y=0, top at y=height
		gl.glRotatef(-90, 1, 0, 0);
		
		if (light)
		{
			// Applies lighting properties
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, clear_mat, 0);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, mat_emission2, 0);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE, mat_emission2, 0);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, mat_emission2, 0);
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, light_shininess, 0);
		}
		else
		{
			float[] mat = {0.5f, 0.5f, 0.5f, 1.0f};
			gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, mat, 0);
		}
		
		if(!teapots)
		{
			// Builds the cylinder using the quadric object
			// Top and bottom of cylinder have same radius
			// cylinder is height units high
			glu.gluCylinder(quad, (float) rad, (float) rad, height, 10, 10);
		}
		else
		{
			glut.glutWireTeapot((float) rad);
		}

		// Pops the top matrix off of the stack
		gl.glPopMatrix();
	}

	/**
	 * The reshape function for OpenGL. Defines how to adjust the OpenGL contents if
	 * the JFrame is resized.
	 *
	 * @param drawable A GLAutoDrawable object. Used to keep the correct GL object.
	 * @param x Unused.
	 * @param y Unused.
	 * @param w The width of the JFrame.
	 * @param h The height of the JFrame.
	*/
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		// Gets the current GL objet
		GL gl = drawable.getGL();

		// Sets the viewport for the window
		gl.glViewport(0, 0, w, h);
		// Changes the matrix mode to Projection to adjust the orthographic projection
		gl.glMatrixMode(GL.GL_PROJECTION);
		// Loads the identity matrix
		gl.glLoadIdentity();
		// If the new height of the JFrame is larger than the width, scale the contents
		// of the 3-D view to maintain the aspect ratio
		if (w <= h)
		{
			gl.glOrtho(-(imageW), (imageW), -(imageH) * (float) h / w, (imageH) * (float) h / w, -(imageW + imageH), (imageW + imageH));
		}
		// Similarly, if width is larger than height, scale to maintain original
		// aspect ratio
		else
		{
			gl.glOrtho(-(imageW) * (float) w / h, (imageW) * (float) w / h, -(imageH), (imageH), -(imageW + imageH), (imageW + imageH));
		}
		// Return to model view
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}

	/**
	 * Event handler for if the display is changed. Empty.
	*/
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
	{
	}

	/**
	 * Event handler for if a keyboard key is typed. Empty.
	*/
	public void keyTyped(KeyEvent key)
	{
	}

	/**
	 * Event handler for when a key is pressed.
	 * If the Escape key was pressed, destroy window.
	 *
	 * @param key A KeyEvent object representing what key was pressed.
	*/
	public void keyPressed(KeyEvent key)
	{
		// Checks what key was pressed
		switch (key.getKeyChar())
		{
			// If Escape was pressed
			case KeyEvent.VK_ESCAPE:
				// Close JFrame and free up memory
				dispose();
				break;

			// Resets the 3-D view to its initial angle and positioning
			case KeyEvent.VK_EQUALS:
				angle = 20;
				angle2 = 15;
				canvas.display();
				break;
				
			// Not sure what this is for...
			case '`':
				if(!teapots)
				{
					teapots = true;
					canvas.display();
					break;
				}
				else
				{
					teapots = false;
					canvas.display();
					break;
				}
				
			default:
				break;
		}
	}

	/**
	 * Event handler for if key is released. Empty.
	 */
	public void keyReleased(KeyEvent key)
	{
	}

	/**
	 * Event handler for if mouse is clicked. Empty.
	 */
	public void mouseClicked(MouseEvent mouse)
	{
	}

	/**
	 * Event handler for if mouse is pressed. Grabs mouse location and stores in
	 * the global variables, startx and starty. This is used for trackball movement in
	 * model view.
	 *
	 * @param mouse A MouseEvent object that contains the location of the mouse click.
	*/
	public void mousePressed(MouseEvent mouse)
	{
		// Grabs the location of the mouse
		pickPoint = mouse.getPoint();
		int x = pickPoint.x;
		int y = pickPoint.y;

		// Stores x-position and y-position for movement
		startx = x;
		starty = y;

		// Updates display
		canvas.display();
	}

	/**
	 * Event handler for if mouse is released. Empty.
	*/
	public void mouseReleased(MouseEvent mouse)
	{
	}

	/**
	 * Event handler for mouse. Empty.
	*/
	public void mouseEntered(MouseEvent mouse)
	{
	}

	/**
	 * Event handler for mouse. Empty.
	*/
	public void mouseExited(MouseEvent mouse)
	{
	}

	/**
	 * Event handler for if mouse is moved, with buttons not depressed. Empty.
	*/
	public void mouseMoved(MouseEvent mouse)
	{
	}

	/**
	 * Event handler for when mouse is dragged across the screen, with left button
	 * depressed. Handles trackball movement in the model view.
	 *
	 * @param mouse A MouseEvent object that contains the location of the mouse.
	*/
	public void mouseDragged(MouseEvent mouse)
	{
		// Grabs the location of the mouse
		pickPoint = mouse.getPoint();
		int x = pickPoint.x;
		int y = pickPoint.y;

		// Computes the new angles to rotate the model view by using a comparison
		// between the old and new x-position and y-positions
		angle = angle + (x - startx);
		angle2 = angle2 + (y - starty);

		// Updates x-position and y-position
		startx = x;
		starty = y;

		// Updates display
		canvas.display();
	}
}
