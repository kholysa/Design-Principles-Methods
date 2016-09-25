package filters;

/**
 * Interface specifying what methods all filters should implement
 * 
 * @author Jerome
 * @version 1.0
 */
public interface Filter {
	
	/**
	 * Constant to specify to use the mean filter
	 */
	public static final int MEAN = 0;
	/**
	 * Constant to specify to use the median filter
	 */
	public static final int MEDIAN = 1;
	/**
	 * Constant to specify to use the edge filter
	 */
	public static final int EDGE = 2;

	/**
	 * filter data using a specific filter
	 * @param data the data to filter
	 * @return the filtered result
	 */
	public float getFilteredData(float[] data);
	
}
