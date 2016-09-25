package main;

import filters.Filter;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * Ultrasonic Localizer. Performs localization of the robot if placed in a corner tile. Robot rotates on itself and ultrasonic sensor values are retrieved to find at what angles were walls detected (using the rising edge method).
 * Once the angle has been calculated, robot rotates again to reach 180 degrees and 270 degrees from the positive x-axis and finds its x and y position that way. The odometer is updated with these values
 * 
 * @author Jerome
 * @version 1.0
 */
public abstract class USLocalizer {
	private static final double CORRECTION = 0.008;
	private static final int COOLDOWN = 1000; //In milliseconds, time where it will not detect a wall after seeing one
	private static final int WALL_DISTANCE = 40;
	private static final double US_SENSOR_DIST = 2.6; //the position of the sensor from the middle of the 2 wheels
	
	/**
	 * Performs the ultrasonic localization (assumes the robot is on the diagonal of a corner tile)
	 * Steps:
	 * -Rotate the robot until it sees no wall
	 * -Keep rotating until the robot sees a wall, then latch the angle
	 * -Switch direction and wait until it sees no wall
	 * -Keep rotating until the robot sees a wall, then latch the angle
	 * -Stop the motors and calculate the angle from the positive x-axis
	 * -Rotate to 180 degrees, use the ultrasonic sensor reading to find the x position of the robot
	 * -Rotate to 270 degrees, use the same procedure to find the y position of the robot
	 * -Update the odometer
	 */
	public static void doLocalization(int startingCorner) {
		double angleA, angleB;
		
		EV3LargeRegulatedMotor leftMotor = Main.get().getMotor("left");
		EV3LargeRegulatedMotor rightMotor = Main.get().getMotor("right");
		SensorPoller sensorPoller = Main.get().getSensorPoller();
		Navigator navigator = Main.get().getNavigator();
		Odometer odometer = Main.get().getOdometer();
		
		//The filter to use:
		int filter = Filter.MEAN;
		
		// rotate the robot until it sees no wall
		leftMotor.setSpeed(100);
		rightMotor.setSpeed(100);
		leftMotor.backward(); //Turn counterclockwise
		rightMotor.forward();
		while(sensorPoller.getTopUsReading(filter) < WALL_DISTANCE);
		
		// keep rotating until the robot sees a wall, then latch the angle
		while(sensorPoller.getTopUsReading(filter) > WALL_DISTANCE);
		angleA = odometer.getTheta();
		
		// switch direction and wait until it sees no wall
		leftMotor.forward(); //Turn clockwise
		rightMotor.backward();
		try{ Thread.sleep(COOLDOWN); } catch(Exception e) {} //Prevent detecting the same wall twice
		while(sensorPoller.getTopUsReading(filter) < WALL_DISTANCE);
		
		// keep rotating until the robot sees a wall, then latch the angle
		while(sensorPoller.getTopUsReading(filter) > WALL_DISTANCE);
		angleB = odometer.getTheta();
		
		//Stop the motors
		leftMotor.stop(true);
		rightMotor.stop();
		
		// angleA is clockwise from angleB, so assume the average of the
		// angles to the right of angleB is 45 degrees past 'north'
		double theta;
		if(angleA < angleB)
			theta = 225d/180*Math.PI - (angleA + angleB) / 2;
		else
			theta = 45d/180*Math.PI - (angleA + angleB) / 2;
		
		// update the odometer position
		odometer.setTheta(theta + odometer.getTheta() + CORRECTION + (startingCorner-1) * Math.PI/2);
	
		double x = 0, y = 0;
		
		switch(startingCorner) {
		case 1:
			//Get the y position (approximate)
			navigator.turnTo(Math.PI * 3 / 2);
			navigator.setRunning(true);
			while(navigator.isTurning());
			y = -(Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST));
			
			//Get the x position (approximate)
			navigator.turnTo(Math.PI);
			while(navigator.isTurning());
			x = -(Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST));
			
			break;
		case 2:
			//Get the x position (approximate)
			navigator.turnTo(0);
			navigator.setRunning(true);
			while(navigator.isTurning());
			x = (Main.MAP_TILE_SIZE - 1) * Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST);
			
			//Get the y position (approximate)
			navigator.turnTo(Math.PI * 3 / 2);
			while(navigator.isTurning());
			y = -(Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST));
			
			break;
		case 3:
			//Get the y position (approximate)
			navigator.turnTo(Math.PI / 2);
			navigator.setRunning(true);
			while(navigator.isTurning());
			y = (Main.MAP_TILE_SIZE - 1) * Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST);
			
			//Get the x position (approximate)
			navigator.turnTo(0);
			while(navigator.isTurning());
			x = (Main.MAP_TILE_SIZE - 1) * Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST);
			
			break;
		case 4:
			//Get the x position (approximate)
			navigator.turnTo(Math.PI);
			navigator.setRunning(true);
			while(navigator.isTurning());
			x = -(Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST));
			
			//Get the y position (approximate)
			navigator.turnTo(Math.PI / 2);
			while(navigator.isTurning());
			y = (Main.MAP_TILE_SIZE - 1) * Main.TILE_LENGTH - (sensorPoller.getTopUsReading(filter) + US_SENSOR_DIST);
			
			break;
		}
		
		//Stop the navigator
		navigator.setRunning(false);
		//Update the odometer position. Since navigator is already trying to be at 0, 0
		odometer.setPosition(new double[] {x, y, 0},  new boolean[] {true, true, false});
		
		Sound.beep(); //Signal the localization is completed
		
		//Navigate to the closest corner
		switch(startingCorner) {
		case 1:
			navigator.travelTo(0, 0);
			break;
		case 2:
			navigator.travelTo((Main.MAP_TILE_SIZE - 2) * Main.TILE_LENGTH, 0);
			break;
		case 3:
			navigator.travelTo((Main.MAP_TILE_SIZE - 2) * Main.TILE_LENGTH, (Main.MAP_TILE_SIZE - 2) * Main.TILE_LENGTH);
			break;
		case 4:
			navigator.travelTo(0, (Main.MAP_TILE_SIZE - 2) * Main.TILE_LENGTH);
			break;
		}
		
		navigator.setRunning(true);
		while(navigator.isNavigating());
		navigator.setRunning(false);
	}

}
