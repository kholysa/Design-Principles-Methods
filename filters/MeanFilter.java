package filters;

/**
 * Class used to return the Mean value in a data set of sensor readings.
 * 
 * @author Jack, Jerome
 * @version 1.1
 */
public class MeanFilter implements Filter {
	/**
	 * gets the filtered data from the data array that is passed to it
	 * Steps:
	 * -Find the sum of all values in the data array that has been passed
	 * -Divide by the sum by the size of the data array
	 * @param data the array holding sensor data
	 * @return the Mean value as per the above calculation
	 */
	@Override
	public float getFilteredData(float[] data) {
		float mean;
		float sum = 0;
		
		int count = 0;
		for(int i = 0; i < data.length; i++) {
			if(data[i] < 2.55f) {
				count++;
				sum += data[i];
			}
		}
	
		mean = (count > 0)? sum / count: 2.55f;
			
		return mean;
	}

}
