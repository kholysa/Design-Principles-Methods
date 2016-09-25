package main;

import java.util.HashMap;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * This class contains only static methods used to perform basic actions. These actions are:
 * - Pick up a ball
 * - Throw a ball
 * - Navigate to the shooting zone
 * @author Jerome
 * @version 1.1
 */
public abstract class Actions {
	
	/**
	 * Throw a ball. Assumes there is a ball in the ball holder
	 */
	public static void throwBall() {
		
		EV3LargeRegulatedMotor motor1 = Main.get().getMotor("throw1");
		EV3LargeRegulatedMotor motor2 = Main.get().getMotor("throw2");
		
		motor1.setSpeed(90);
		motor2.setSpeed(90);
		
		motor1.rotate(-300, true);
		motor2.rotate(-300);
		
		motor1.setSpeed(100000); //Max speed
		motor2.setSpeed(100000);
		
		motor1.rotate(360, true);
		motor2.rotate(360);
		
		//Put the arm straight up again in order for the next pickUpBall() call to work
		motor1.setSpeed(40);
		motor2.setSpeed(40);
		motor1.rotateTo(360, true);
		motor2.rotateTo(360);
	}
	
	/**
	 * Make the robot pick up a ball. Assumes the ball is already positioned (between the ball holder and the rotating arm) to be picked up
	 */
	public static void pickupBall() {
		EV3LargeRegulatedMotor motor1 = Main.get().getMotor("throw1");
		EV3LargeRegulatedMotor motor2 = Main.get().getMotor("throw2");
		
		motor1.setSpeed(40);
		motor2.setSpeed(40);
		//from a vertical starting position, the arm turns 205 degrees to place the ball on the launcher
		motor1.rotate(205, true);
		motor2.rotate(205);
	}
	
	/**
	 * Navigate to the center of the shooting zone, 1 tile behind the forward line
	 * @param d2 the distance of the forward line from the back wall (should be retrieved via Wifi)
	 */
	public static void navigateToShooting(int d2) {
		Navigator navigator = Main.get().getNavigator();
		ObstacleDetector obstacleDetector = Main.get().getObstacleDetector();
		
		//Navigate to the forward zone. Move in the y-axis first in order to avoid the blue and defender zone
		navigator.travelToSquare((Main.MAP_TILE_SIZE / 2 - 1) * Main.TILE_LENGTH, (d2 - 2) * Main.TILE_LENGTH, false, true);
		navigator.setRunning(true);
		while(navigator.isNavigating() || obstacleDetector.isAvoiding());
		navigator.setRunning(false);
	}
	
	/**
	 * Navigate to the center of the defense zone, 1 tile behind the defense line
	 * @param d1 the distance of the defense line from the front wall (should be retrieved via Wifi)
	 */
	public static void navigateToDefense(int d1) {
		Navigator navigator = Main.get().getNavigator();
		ObstacleDetector obstacleDetector = Main.get().getObstacleDetector();
		
		//Navigate to the middle of the defense zone. Move in the y-axis first in order to avoid the blue and forward zone
		navigator.travelToSquare((Main.MAP_TILE_SIZE / 2 - 1) * Main.TILE_LENGTH, (Main.MAP_TILE_SIZE - d1) * Main.TILE_LENGTH, false, true);
		navigator.setRunning(true);
		while(navigator.isNavigating() || obstacleDetector.isAvoiding());
		navigator.setRunning(false);
	}

}
