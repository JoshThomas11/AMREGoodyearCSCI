import java.util.*;

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
