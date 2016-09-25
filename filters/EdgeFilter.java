package filters;

import main.Main;
import main.SensorPoller;

/**
 * Class used to detect whether an Edge has been encountered based on the sensor readings.
 * @author Jack, Jerome
 * @version 1.1
 */
public class EdgeFilter implements Filter{
	private static final double THRESHOLD = 0.15;
	private static final int WINDOW = 5; //window for how many of the last values to read, this should be calibrated
	
	/**
	 * determines the presence of an edge within the data array
	 * Steps:
	 * -Determine what the minima is by finding the zero crossing that corresponds to the largest negative change in the derivative
	 * -Compare whether this rate of change represented by the minima variable is large enough to indicate an edge
	 * @param data the array holding sensor data
	 * @return If an edge is detected return 1 otherwise return -1
	 */
	@Override
	public float getFilteredData(float[] data) {
		float[] edgeData = new float[data.length];
		int index = Main.get().getSensorPoller().getIndex() % SensorPoller.COLOR_WINDOW; //Get the index of the last data put in the window
		
		//Put the array in the order the data was put in, based on that index
		System.arraycopy(data, index + 1, edgeData, 0, data.length - index - 1);
		System.arraycopy(data, 0, edgeData, data.length - index - 1, index + 1);
		
		//Check if a line was detected by computing the derivative and comparing to the threshold
		if(edgeData[edgeData.length - 1] - edgeData[edgeData.length - 1 - WINDOW] < THRESHOLD)
			return -1;
		else
			return 1;
	}
	
}
