package main;

import filters.Filter;
import lejos.hardware.Sound;

/**
 * Light Localizer. Performs localization of the robot anywhere on the field. Robot rotates on itself and color sensor values are retrieved to find at what angles lines were detected.
 * Calculates the angle as well as the position of the robot and updates the odometer accordingly.
 * @author Jerome, Jack
 * @version 1.0
 */
public abstract class LightLocalizer {
	
	private static final int WAIT_TIME = 400; //Time to wait after a line has been detected to make sure it is not seen twice
	private static final double COLOR_SENSOR_DIST = 16.3; //Distance of the color sensor from the center of the two wheels
	
	/**
	 * Localizes (calculates the angle as well as the position). Does not move after it is done localizing
	 */
	public static void doLocalization() {
		doLocalization(false);
	}
	
	/**
	 * Localizes (calculates the angle as well as the position) and has the option to move to the nearest corner afterwards
	 * @param goToPoint whether or not to go to the nearest corner after localizing
	 */
	public static void doLocalization(boolean goToPoint) {
		
		Navigator navigator = Main.get().getNavigator();
		Odometer odometer = Main.get().getOdometer();
		
		//Pause the odometry correction and the obstacle avoidance:
		Main.get().getObstacleDetector().setRunning(false);
		Main.get().getOdometryCorrection().setRunning(false);
		
		double[] angles = new double[4]; //The 4 angles at which lines were found
		int currentAngle = 0;
		long lastTime = 0;
		
		//Record the position of the nearest corner:
		double cornerX = Math.round(odometer.getX() / Main.TILE_LENGTH) * Main.TILE_LENGTH;
		double cornerY = Math.round(odometer.getY() / Main.TILE_LENGTH) * Main.TILE_LENGTH;
		
		//Turn to an angle of 45 degrees to make sur the 4 lines a properly detected
		navigator.turnTo(Math.PI/4);
		navigator.setRunning(true);
		while(navigator.isNavigating());
		navigator.setRunning(false);
		
		//Turn until 4 lines are detected
		Main.get().getMotor("left").setSpeed(200);
		Main.get().getMotor("right").setSpeed(200);
		Main.get().getMotor("left").backward(); //Turn counterclockwise
		Main.get().getMotor("right").forward();
		
		//Get the first 4 lines detected and save the angle reported by the odometer at those time
		while(currentAngle < 4) {
			if(Main.get().getSensorPoller().getCenterFloorColorReading(Filter.EDGE) > 0 && System.currentTimeMillis() - lastTime > WAIT_TIME) {
				lastTime = System.currentTimeMillis();
				angles[currentAngle] = odometer.getTheta();
				currentAngle++;
				Sound.beep();
			}
		}
		
		Main.get().getMotor("left").stop(true); //Stop moving
		Main.get().getMotor("right").stop();
		
		//Calculation of the position using trig
		//Even-index angles are on the y axis, whereas odd-index angles are on the x axis
		double thetaY = (angles[0] - angles[2])/2;
		double thetaX = (angles[1] - angles[3])/2;
		double x = cornerX - COLOR_SENSOR_DIST * Math.cos(thetaY); //X has to be flipped
		double y = cornerY - COLOR_SENSOR_DIST * Math.cos(thetaX);
		
		//Calculation of the angle
		double thetaCorrection = Math.PI - (angles[0] + angles[2])/2;
		
		//Prevent correcting if some lines were skipped or some dark spot was detected
		if(Math.abs(thetaCorrection) > Math.PI) { //Expected to be 3pi/2 between the first and last line
			//Resume the odometry correction and the obstacle avoidance:
			Main.get().getObstacleDetector().setRunning(true);
			Main.get().getOdometryCorrection().setRunning(true);
			Sound.buzz();
			return;
		}
		
		double theta = odometer.getTheta() + thetaCorrection;
		
		odometer.setPosition(new double[] {x, y, theta}, new boolean[] {true, true, true});
		
		//Move to the nearest corner, if needed
		if(goToPoint) {
			navigator.travelTo(cornerX, cornerY);
			navigator.setRunning(true);
			while(navigator.isNavigating());
			navigator.setRunning(false); //Stop the navigator to make sure it does not want to correct itself after turning
		}
		
		//Resume the odometry correction and the obstacle avoidance:
		Main.get().getObstacleDetector().setRunning(true);
		Main.get().getOdometryCorrection().setRunning(true);
	}

}