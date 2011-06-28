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
final class Alg
{
	/**
	 * This empty constructor is private in order to make the class noninstantiable (static).
	 */
	private Alg()
	{
		throw new AssertionError("This constructor should not be called because this is a static class.");
	}

	/**
	 * Finds the maximum value in an array of doubles.
	 *
	 * @param data The array to search
	 *
	 * @return The maximum value in the array
	 */
	public static double arrayMax(double[] data)
	{
		double max = data[0];
		for (int i = 1; i < data.length; i++)
		{
			if (data[i] > max)
			{
				max = data[i];
			}
		}
		return max;
	}

	/**
	 * Finds the minimum value in an array of doubles.
	 *
	 * @param data The array to search
	 *
	 * @return The maximum value in the array
	 */
	public static double arrayMin(double[] data)
	{
		double min = data[1];
		for (int i = 1; i < data.length; i++)
		{
			if (data[i] < min)
			{
				min = data[i];
			}
		}
		return min;
	}

	/**
	 * Finds the Alpha value for the given data.
	 *
	 * @param NaNv The combined NaNv for the data
	 * @param delta The width of each bin (binWidth)
	 * @param parts The number of particles
	 * @param index The index specifying which element of NaNv[i] to access
	 *
	 * @return The alpha value
	 */
	public static double getAlpha(double[][] NaNv, double delta, int parts, int index)
	{
		double top = 0;
		double bottom = 0;

		for (int i = 0; i < NaNv.length; i++)
		{
			top += (NaNv[i][index] / parts) * (i + 0.5) * delta;
			bottom += (NaNv[i][0] / parts) * (i + 0.5) * delta;
		}

		return top / bottom;
	}

	/**
	 * Finds Na for the given data.
	 *
	 * @param data Input data from ImageJ, consisting of the diameters of each particle in pixels
	 * @param nBins The number of bins
	 * @param delta The width of each bin (binWidth)
	 * @param area The area of the (cropped) image
	 *
	 * @return Na
	 */
	public static double[] getNa(double[] data, int nBins, double delta, double area)
	{
		// Constructs Na, the 2-D particle distribution
		double[] Na = new double[nBins+1];

		// "counter" variable used to determine the values in Na
		int sum = 0;

		// Nested looping structure that determines sum for each entry in Na and assigns (sum/area) to each entry in Na
		for(int i = 0; i <= nBins; i++)
		{
			for(int j = 0; j < data.length; j++)
			{
				// Checks to see if the diameter in data falls within the range [i*delta, (i+1)*delta]
				if((data[j] > delta*i) && (data[j] <= delta*(i+1)))
				{
					sum++;
				}
			}
			// Stores the value in Na
			Na[i] = sum/area;
			sum = 0;
		}

		return Na;
	}


	/**
	 * The main function of the class. Computes the Schwartz-Saltikov algorithm from
	 * the given input parameters.
	 *
	 * @param data Input data from ImageJ, consisting of the diameters of each particle in pixels
	 * @param nBins The number of bins computed by ImageJ that the particles are placed into
	 * @param thick The thickness of the slice/cross-section in pixels
	 * @param area The area of the (cropped) image
	 *
	 * @return Nv The 3-D particle distribution, stored as an array of doubles
	*/
	public static double[] SSAlg(double[] data, int nBins, double delta, int thick, double area)
	{
		// Finds the minimum and maximum diameters from the data
		double max = arrayMax(data);
		double min = arrayMin(data);

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

		double[] Na = getNa(data, nBins, delta, area);

		// Constructs Nv, the 3-D distribution of the particles
		double[] Nv = new double[nBins+1];

		// Back-substitution implementation
		Nv[Nv.length-1] = Na[Na.length-1] / A[A.length-1][A[A.length-1].length-1]; // Assigns the last element of Nv to be Na's last element divided by A[last][last]

		double sum;

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
			Nv[k] = (Na[k] - sum) / A[k][k];
		}

		// Returns the 3-D distribution
		return Nv;
	}
}
