package main;

import filters.Filter;
import lejos.hardware.Sound;

/**
 * Odometry Correction is a thread that periodically checks the two color sensor pointing the ground to detect lines. Once two lines have been detected, the odometer is updated by correcting the x, y and theta position
 * 
 * @author Jerome
 * @version 1.0
 */
public class OdometryCorrection extends Thread {
	
	private static final int ITERATION_TIME = 25;
	private static final double X_SENSOR_DIST = 8.6; // Distance in x of both sensors from the center of the robot's wheels
	private static final double Y_SENSOR_DIST = 11.9; // Distance in x of both sensors from the center of the robot's wheels
	private static final double DISPLACEMENT_THRESHOLD = 6; //Maximum distance between two light sensor readings that will trigger the odometry correction
	private static final double CORRECTION = 3.5; // Distance calculated to make the robot end perfectly on lines when moving straight
	private static final double OVERCORRECTION = 0.27; // Add this value to the angle theta to over-correct. Has proven to work better with this than without
	private static final int COOLDOWN = 1000; //1 second delay between two corrections
	
	private double[] distances = new double[2]; //Holds the distance recorded when a line is crossed. If the left light sensor is triggered the 0 index value will be changed and the 1 index will be changed for the right light sensor 
	
	private Object lock;
	
	private long lastCorrectionTime = 0;
	
	private volatile boolean running = false;

	/**
	 * Constructs a default odometry correction object
	 */
	public OdometryCorrection() {
		lock = new Object();
		reset();
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		long correctionStart, correctionEnd;
		
		Odometer odometer = Main.get().getOdometer();
		SensorPoller sensorPoller = Main.get().getSensorPoller();

		while (true) {
			correctionStart = System.currentTimeMillis();
			
			if(running) {
				
				boolean horizontal = Math.round(odometer.getTheta() / (Math.PI / 2)) % 2 == 0;
				
				//FIRST, find the position of the point in-between the two floor light sensors
				
				//Whenever a light sensor sees a line, record the distance traveled as told by the odometer. Prevents a light sensor from recording the same line twice
				if(sensorPoller.getLeftFloorColorReading(Filter.EDGE) > 0) {
					if(horizontal && (Double.isNaN(distances[0]) || Math.abs(odometer.getX() - distances[0]) > Main.TILE_LENGTH / 2)) //Robot is moving horizontally
						distances[0] = odometer.getX();
					else if(Double.isNaN(distances[0]) || Math.abs(odometer.getY() - distances[0]) > Main.TILE_LENGTH / 2) //Robot is moving vertically
						distances[0] = odometer.getY();
				}
				
				if(sensorPoller.getRightFloorColorReading(Filter.EDGE) > 0) {
					if(Math.round(odometer.getTheta() / (Math.PI / 2)) % 2 == 0 && (Double.isNaN(distances[1]) || Math.abs(odometer.getX() - distances[1]) > Main.TILE_LENGTH / 2)) //Robot is moving horizontally
						distances[1] = odometer.getX();
					else if(Double.isNaN(distances[1]) || Math.abs(odometer.getY() - distances[1]) > Main.TILE_LENGTH / 2) //Robot is moving vertically
						distances[1] = odometer.getY();
				}
				
				//SECONDLY, check if we got two consecutive readings. If so, perform the odometry correction
				if(Math.abs(distances[1] - distances[0]) < DISPLACEMENT_THRESHOLD && System.currentTimeMillis() - lastCorrectionTime > COOLDOWN) {
					Sound.beep();
					
					lastCorrectionTime = System.currentTimeMillis();
					
					synchronized(lock) {
						double oppositeOverAdj = (distances[1] - distances[0]) / (2* X_SENSOR_DIST); //Used for tan calculations
					
						//Trig calculations. Draw a triangle using the tile lines and the position of the two light sensors and assume the robot is moving on the x-axis only
						double deltaTheta = -Math.atan(oppositeOverAdj);
						double deltaPos = Math.abs(X_SENSOR_DIST * Math.sin(deltaTheta)) + OVERCORRECTION; //From 2 * X_SENSOR_DIST * Math.sin(deltaTheta) / 2
						
						int sign = ((Math.round(odometer.getTheta() / (Math.PI / 2)) % 4) < 2)? 1: -1; //If the angle is either PI or 3*PI/2 we need to flip the signs
						
						double x, y, theta;
						theta = odometer.getTheta() + deltaTheta * sign;
						
						if(horizontal) { //Robot is moving horizontally
							double lineDist = Y_SENSOR_DIST * Math.cos(theta) + sign * deltaPos; //Y distance of the detected line from the point in between the two wheels
							x = Math.round((odometer.getX() - lineDist) / Main.TILE_LENGTH) * Main.TILE_LENGTH + lineDist + sign * CORRECTION; //Round to the nearest line and add the error from the angle
							y = odometer.getY(); // + deltaY;
						}
						else { //Robot is moving vertically
							double lineDist = Y_SENSOR_DIST * Math.sin(theta) + sign * deltaPos; //Y distance of the detected line from the point in between the two wheels
							x = odometer.getX(); // + deltaY;
							y = Math.round((odometer.getY() - lineDist) / Main.TILE_LENGTH) * Main.TILE_LENGTH + lineDist + sign * CORRECTION; //Round to the nearest line and add the error from the angle
						}
						
						//FINALLY, update the odometer with the calculated position of the center point between the two wheels
						odometer.setPosition(new double[] {x,  y, theta}, new boolean[] {true, true, true});
					}
					
					reset(); //Reset the positions so this correction isn't applied more than once
				}
			}

			// this ensure the odometry correction occurs only once every period
			correctionEnd = System.currentTimeMillis();
			if (correctionEnd - correctionStart < ITERATION_TIME) {
				try {
					Thread.sleep(ITERATION_TIME - (correctionEnd - correctionStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that the odometry correction will be
					// interrupted by another thread
				}
			}
		}
	}
	
	/**
	 * pause or resume the odometry correction
	 * @param running whether to pause or resume the odometry correction
	 */
	public void setRunning(boolean running) {
		synchronized(lock) {
			this.running = running;
		}
		reset();
	}
	
	/**
	 * Reset the distances array so that it doesn't use previous values and tries to correct the robot's position
	 */
	public void reset() {
		synchronized(lock) {
			distances = new double[] {Double.NaN, Double.NaN};
		}
	}
}
