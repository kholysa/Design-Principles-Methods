package filters;

import java.util.Arrays;

/**
 * Class used to return the Median value in a data set of sensor readings.
 * 
 * @author Jack
 * @version 1.1
 */
public class MedianFilter implements Filter {
	
	/**
	 * gets the filtered data from the data array that is passed to it
	 * Steps:
	 * -Order the data array in ascending order of values
	 * -Takes the values at the 2 center indexes if the size of the array is even
	 * -Or takes the value at the center index if the size of the array is odd
	 * @param data the array holding sensor data
	 * @return the Median value based on the values found at the indexes
	 */
	@Override
	public float getFilteredData(float[] data) {
		float[] medianData = Arrays.copyOf(data, data.length);
		float median;
		
		//order the medianData in ascending order using insertion sort
		for(int j = 0; j < medianData.length; j++) {
			//while the start of the array has not been reached and a value is larger than its proceeding value in the array
			while(j > 0 && (medianData[j] < medianData[j-1])) {
				//swap the values around to place them back into ascending order using a temp value
				float temp = medianData[j];
				medianData[j] = medianData[j-1];
				medianData[j-1] = temp;

				//j is subtracted because previous values of j must now be considered again due to changes done above
				j = j-1;
			}			
		}
	
		if(medianData.length%2 == 0) {
			//if the size of the array is even then take the 2 middle values and find their average to return the median
			median = (medianData[(medianData.length/2)-1] + medianData[(medianData.length/2)])/2;
		} else {
			//if the size of the array is an odd value then we need just a singular value to represent the median
			median = medianData[(medianData.length - 1)/2];
		}
			
		return median;
	}

}
