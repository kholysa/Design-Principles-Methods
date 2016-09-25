package main;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import filters.EdgeFilter;
import filters.Filter;
import filters.MeanFilter;
import filters.MedianFilter;

/**
 * The sensor poller is a thread that continuously polls data from the sensors and stores them in an array of limited size called a window.
 * Filters can then be applied to that data to retrieve more accurate values.
 * Doesn't need to have a lock since no other thread can modify this object's fields.
 * Uses the red mode for the color sensor for more accurate line detection
 * 
 * @author Jerome
 * @version 1.0
 */
public class SensorPoller extends Thread {

	private static final int ITERATION_TIME = 25; //Time for this thread to run one iteration of the loop
	public static final int US_WINDOW = 10;
	public static final int COLOR_WINDOW = 50; //Allow initial time on startup to establish environment color
	
	private Filter meanFilter, medianFilter, edgeFilter;
	
	private float[] topUsData, leftFloorColorData, rightFloorColorData, centerFloorColorData;
	private int index; //Used to fill the windows
	
	private EV3UltrasonicSensor topUsSensor;
	private EV3ColorSensor leftFloorColorSensor, rightFloorColorSensor, centerFloorColorSensor;
	
	/**
	 * Constructs a default sensor poller and fills the windows with the initial value of each sensor
	 */
	public SensorPoller() {
		//Initialize the filters
		meanFilter = new MeanFilter();
		medianFilter = new MedianFilter();
		edgeFilter = new EdgeFilter();
		
		//Initialize the sensors
		topUsSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S3"));
		leftFloorColorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S2"));
		rightFloorColorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
		centerFloorColorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S4"));
		
		//Initialize the windows
		topUsData = new float[US_WINDOW];
		leftFloorColorData = new float[COLOR_WINDOW];
		rightFloorColorData = new float[COLOR_WINDOW];
		centerFloorColorData = new float[COLOR_WINDOW];
		
		//Fill the arrays with initial value
		for(int i = 0; i < US_WINDOW; i++) {
			topUsSensor.fetchSample(topUsData, i);
		}
		
		for(int i = 0; i < COLOR_WINDOW; i++) {
			leftFloorColorSensor.getRedMode().fetchSample(leftFloorColorData, i);
			rightFloorColorSensor.getRedMode().fetchSample(rightFloorColorData, i);
			centerFloorColorSensor.getRedMode().fetchSample(centerFloorColorData, i);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		long startTime, endTime;
		
		while(true) {
			startTime = System.currentTimeMillis();
			
			//Get the sensor values
			topUsSensor.fetchSample(topUsData, index % US_WINDOW);
			leftFloorColorSensor.getRedMode().fetchSample(leftFloorColorData, index % COLOR_WINDOW);
			rightFloorColorSensor.getRedMode().fetchSample(rightFloorColorData, index % COLOR_WINDOW);
			centerFloorColorSensor.getRedMode().fetchSample(centerFloorColorData, index % COLOR_WINDOW);
			index++;
			
			Main.get().getDisplay().addDisplayValue("US", topUsData[(index - 1) % US_WINDOW]);
			
			//This ensures that this loop is ran only once per period
			endTime = System.currentTimeMillis();
			if (endTime - startTime < ITERATION_TIME) {
				try {
					Thread.sleep(ITERATION_TIME - (endTime - startTime));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that the odometer will be interrupted by
					// another thread
				}
			}
		}
	}
	
	/**
	 * get the data from the ultrasonic sensor placed on the top of the robot after it has been filtered using a specific filter
	 * @param filter the filter to use (Use the constants specified in the Filter class)
	 * @return the filtered value
	 */
	public float getTopUsReading(int filter) {
		switch(filter) {
		case Filter.MEAN:
			return meanFilter.getFilteredData(topUsData) * 100;
		case Filter.MEDIAN:
			return medianFilter.getFilteredData(topUsData) * 100;
		case Filter.EDGE:
			return edgeFilter.getFilteredData(topUsData);
		default: //Filter not supported
			return -1;
		}
	}
	
	/**
	 * get the data from the color sensor on the left side of the robot pointing on the floor after it has been filtered using a specific filter
	 * @param filter the filter to use (Use the constants specified in the Filter class)
	 * @return the filtered value
	 */
	public float getLeftFloorColorReading(int filter) {
		switch(filter) {
		case Filter.MEAN:
			return meanFilter.getFilteredData(leftFloorColorData);
		case Filter.MEDIAN:
			return medianFilter.getFilteredData(leftFloorColorData);
		case Filter.EDGE:
			return edgeFilter.getFilteredData(leftFloorColorData);
		default: //Filter not supported
			return -1;
		}
	}
	
	/**
	 * get the data from the color sensor on the right side of the robot pointing on the floor after it has been filtered using a specific filter
	 * @param filter the filter to use (Use the constants specified in the Filter class)
	 * @return the filtered value
	 */
	public float getRightFloorColorReading(int filter) {
		switch(filter) {
		case Filter.MEAN:
			return meanFilter.getFilteredData(rightFloorColorData);
		case Filter.MEDIAN:
			return medianFilter.getFilteredData(rightFloorColorData);
		case Filter.EDGE:
			return edgeFilter.getFilteredData(rightFloorColorData);
		default: //Filter not supported
			return -1;
		}
	}
	
	/**
	 * get the data from the color sensor in the center of the robot pointing on the floor after it has been filtered using a specific filter
	 * @param filter the filter to use (Use the constants specified in the Filter class)
	 * @return the filtered value
	 */
	public float getCenterFloorColorReading(int filter) {
		switch(filter) {
		case Filter.MEAN:
			return meanFilter.getFilteredData(centerFloorColorData);
		case Filter.MEDIAN:
			return medianFilter.getFilteredData(centerFloorColorData);
		case Filter.EDGE:
			return edgeFilter.getFilteredData(centerFloorColorData);
		default: //Filter not supported
			return -1;
		}
	}
	
	/**
	 * get the index of the latest value that was put in an array
	 * @return the index of the last recorded value
	 */
	public int getIndex() {
		return index - 1;
	}
	
	/**
	 * get the lejos object for the ultrasonic sensor place on the top of the robot and facing forward
	 * @return the ultrasonic sensor
	 */
	public EV3UltrasonicSensor getTopUsSensor() {
		return topUsSensor;
	}
	
	/**
	 * get the lejos object for the color sensor on the left side of the robot pointing towards the floor
	 * @return the color sensor
	 */
	public EV3ColorSensor getLeftFloorColorSensor() {
		return leftFloorColorSensor;
	}
	
	/**
	 * get the lejos object for the color sensor on the left side of the robot pointing towards the floor
	 * @return the color sensor
	 */
	public EV3ColorSensor getRightFloorColorSensor() {
		return rightFloorColorSensor;
	}
	
	/**
	 * get the lejos object for the color sensor in the center of the robot pointing towards the floor
	 * @return the color sensor
	 */
	public EV3ColorSensor getCenterFloorColorSensor() {
		return centerFloorColorSensor;
	}
}
