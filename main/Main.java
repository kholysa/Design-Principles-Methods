package main;
import java.io.IOException;
import java.util.HashMap;

import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

import wifi.*;

/**
 * Central block of the project. Most objects are created and stored in this class. Threads are also started here.
 * Follows the Singleton Pattern: its static reference is available through the method get()
 * 
 * @author Jerome
 * @version 1.0
 */
public class Main {
	
	/**
	 * The size of a side of a tile
	 */
	public static final double TILE_LENGTH = 30.48; //12 inches
	/**
	 * The total size of the map (assuming it is a square) in number of tiles. Note that the coordinate system goes from -1 to (this value - 1)
	 */
	public static final int MAP_TILE_SIZE = 12; //TODO change back to 12 tiles
	/**
	 * The number of tiles per board. Used to find where the cracks are
	 */
	public static final int BOARD_TILE_SIZE = 4;
	
	private static final String IP_ADDRESS = "132.206.67.203";
	private static final int TEAM_NUMBER = 5;
	
	//Static final variables of the specs of the ball tray
	private static final double TRAY_WIDTH = 7.62; //3 inches
	private static final double BALL_SPACING = 7.62; //3 inches
	private static final double BALL_HOLE_RADIUS = 2.54; //1 inch
	private static final double BALL_HOLE_MARGIN = 3.81; //1.5 inches
	
	private static final double X_DIST_BALL_HOLDER = 4.8; //Distance the ball should be from the center of rotation in order to be picked up
	private static final double Y_DIST_BALL_HOLDER = 11.6; //Distance of the ball holder from the center of rotation
	
	private HashMap<String, EV3LargeRegulatedMotor> motors;
	private SensorPoller sensorPoller;
	private Odometer odometer;
	private OdometryCorrection odometryCorrection;
	private Navigator navigator;
	private ObstacleDetector obstacleDetector;
	private Display display;
	private WifiConnection wifi;
	
	public Logger logger;
	
	private boolean forward;

	private static Main instance;
	
	/**
	 * private constructor for the Main class. Follows the Singleton Pattern. This object is created in the main method
	 * Central control point of the whole program.
	 * Steps:
	 * -Initialize the objects, threads and EV3 objects
	 * -Start the threads that need to run in the background
	 * -Retrieve data from server
	 * -Perform localization using the ultrasonic localizer
	 * -The chain of actions now differs depending on whether the robot is playing as forward or as defender:
	 * 
	 * As forward:
	 * -Navigate to the shooting zone (avoid obstacles on the path) without entering the blue zone
	 * -Navigate to the ball tray
	 * -Pick up a ball
	 * -Detect balls of the right color
	 * -Navigate to the shooting zone
	 * -Shoot the ball
	 * -Go back to the ball tray, repeat the procedure
	 * 
	 * As defender:
	 * -Navigate to the front of the goal
	 * -Position as to block an area of the goal as big as possible
	 * 
	 * In both cases, use the odometer along with the odometry correction running in a separate thread to keep track of the position of the robot
	 */
	protected Main() {
		instance = this;
		
		motors = new HashMap<String, EV3LargeRegulatedMotor>();
		motors.put("right", new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C")));
		motors.put("left", new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D")));
		motors.put("throw1", new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A")));
		motors.put("throw2", new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B")));
		
		sensorPoller = new SensorPoller();
		odometer = new Odometer();
		odometryCorrection = new OdometryCorrection();
		navigator = new Navigator();
		obstacleDetector = new ObstacleDetector();
		display = new Display();
		
		logger = new Logger(); //Used to debug and do sensor characterization
		
		//Start the threads
		sensorPoller.start();
		odometer.start();
		odometryCorrection.start();
		obstacleDetector.start();
		navigator.start();
		display.start();
		
		//Add an exit thread to be able to stop the robot at any point
		(new Thread() {
			@Override
			public void run() {
				while(Button.waitForAnyPress() != Button.ID_ESCAPE);
				logger.close();
				System.exit(0);
			}
		}).start();
		
		Button.waitForAnyPress(); //Wait before starting the program and getting the parameters via wifi

		//Retrieve parameters by Wifi
		//wifi = new WifiConnection();
		try {
			wifi = new WifiConnection(IP_ADDRESS, TEAM_NUMBER);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//TODO remove the following once the wifi works
		/*wifi.StartData = new HashMap<String, Integer>();
		wifi.StartData.put("SC", 4);
		wifi.StartData.put("ll-x", 5);
		wifi.StartData.put("ll-y", 5);
		wifi.StartData.put("ur-x", 6);
		wifi.StartData.put("ur-y", 6);
		wifi.StartData.put("Role", 0); //0 is forward, 1 is defense
		wifi.StartData.put("d1", 3); //Distance of the defense zone from the front wall
		wifi.StartData.put("d2", 3); //Distance of the forward zone from the back wall*/
		
		//DEMO PROCEDURE
		//Check if the zone y position minus 1 is on a crack, if so, change the d1 or d2 in order to avoid light localizing on that crack
		if((wifi.StartData.get("d1") - 1) % 4 == 0) {
			wifi.StartData.put("d1", wifi.StartData.get("d1") - 1);
		}
		if((wifi.StartData.get("d2") - 1) % 4 == 0) {
			wifi.StartData.put("d2", wifi.StartData.get("d2") - 1);
		}
		
		//Fix the wifi arguments being different
		if(wifi.StartData.get("DTN") == TEAM_NUMBER) {
			wifi.StartData.put("Role", 1);
			wifi.StartData.put("SC", wifi.StartData.get("DSC"));
		}
		else {
			wifi.StartData.put("Role", 0);
			wifi.StartData.put("SC", wifi.StartData.get("OSC"));
		}
		
		
		USLocalizer.doLocalization(wifi.StartData.get("SC")); //Localize
		
		odometryCorrection.setRunning(true);
		obstacleDetector.setRunning(true);
		
		if(wifi.StartData.get("Role") == 0) { //FORWARD
			
			//currentBall holds the number of the ball we are currently seeking or moving
			for(int currentBall = 0; currentBall < 4; currentBall++) { //There are 4 balls on the tray
				
				//Navigate to the forward zone.
				Actions.navigateToShooting(wifi.StartData.get("d2"));
				
				//Light localize only the first time here
				//if(currentBall == 0)
					//LightLocalizer.doLocalization(true);
				
				//Calculate the point to do the localization at, considering the ball tray can be rotated
				int llx = wifi.StartData.get("ll-x"); //Create easy access variables
				int lly = wifi.StartData.get("ll-y");
				int urx = wifi.StartData.get("ur-x");
				int ury = wifi.StartData.get("ur-y");
				int[] localizationLocation = new int[2];
				double[] ballLocation = new double[2];
				boolean yFirst = false; //Approach the light localization point by navigating on the y or x axis first
				double thetaBallX = TRAY_WIDTH/2 + X_DIST_BALL_HOLDER;
				double thetaBallY = BALL_HOLE_RADIUS + BALL_HOLE_MARGIN - Y_DIST_BALL_HOLDER + BALL_SPACING * currentBall;
				
				if(llx < urx) {
					if(lly < ury) { //ball tray on left of tile
						ballLocation = new double[] {llx * TILE_LENGTH + thetaBallX, lly * TILE_LENGTH + thetaBallY};
						localizationLocation = new int[] {llx, lly - 1};
					}
					else { //on top of tile
						ballLocation = new double[] {(odometer.getX() < llx)? (llx * TILE_LENGTH + thetaBallY) : (urx * TILE_LENGTH - thetaBallY), lly * TILE_LENGTH - thetaBallX};
						localizationLocation = new int[] {llx  + ((odometer.getX() < llx)? -1 : 2), lly};
						yFirst = true;
					}
				}
				else {
					if(lly < ury) { //on bottom of tile
						ballLocation = new double[] {(odometer.getX() < urx)? (urx * TILE_LENGTH + thetaBallY) : (llx * TILE_LENGTH - thetaBallY), lly * TILE_LENGTH + thetaBallX};
						localizationLocation = new int[] {llx + ((odometer.getX() < urx)? -2 : 1), lly};
						yFirst = true;
					}
					else { //on right of tile
						ballLocation = new double[] {llx * TILE_LENGTH - thetaBallX, ury * TILE_LENGTH + thetaBallY};
						localizationLocation = new int[] {llx, lly - 1};
					}
				}
				
				//Convert from tiles to centimeters
				localizationLocation[0] *= TILE_LENGTH;
				localizationLocation[1] *= TILE_LENGTH;
				
				//Go to a corner close by the ball tray first
				navigator.travelToSquare(localizationLocation[0], localizationLocation[1], false, yFirst);
				
				double backupX = navigator.getTargetX(); //Save a point the robot can travel backwards to after it has picked up a ball
				double backupY = navigator.getTargetY();
				
				navigator.setRunning(true);
				while(navigator.isNavigating() || obstacleDetector.isAvoiding());
				navigator.setRunning(false);
				
				//Localize here to make sure the ball approach will be smooth
				LightLocalizer.doLocalization();
				
				navigator.travelToSquare(ballLocation[0], ballLocation[1]);
				navigator.setRunning(true);
				while(navigator.isNavigating() || obstacleDetector.isAvoiding());
				
				navigator.turnTo(Math.PI/2);
				while(navigator.isTurning());
				navigator.setRunning(false);
				obstacleDetector.setRunning(false); //We got to the point we wanted we don't need to avoid obstacles anymore
				
				Actions.pickupBall();
				
				navigator.travelTo(backupX, backupY, true); //Travel backwards to leave the ball tray
				navigator.setRunning(true);
				while(navigator.isNavigating());
				
				//Navigate to shooting zone
				Actions.navigateToShooting(wifi.StartData.get("d2"));
				
				//Light localize here? Turn off obstacleDetector after since the light localizer leaves it on at the end of its procedure
				LightLocalizer.doLocalization(true);
				obstacleDetector.setRunning(false);
				
				//Turn towards the goal
				double targetTheta = Math.atan2(odometer.getY() - (Main.MAP_TILE_SIZE - 2) * Main.TILE_LENGTH, odometer.getX() - (Main.MAP_TILE_SIZE / 2 - 1) * Main.TILE_LENGTH) - Math.PI;
				targetTheta += Math.PI / 2; //Robot has to be sideways to shoot
				if(targetTheta < 0)
					targetTheta += 2*Math.PI;
				else if(targetTheta > 2*Math.PI)
					targetTheta -= 2*Math.PI;
				
				navigator.turnTo(targetTheta);
				navigator.setRunning(true);
				while(navigator.isTurning());
				navigator.setRunning(false);
				
				//Finally, throw the ball
				Actions.throwBall();
				
				//Resume the obstacle avoidance and odometry correction for the next ball
				obstacleDetector.setRunning(true);
			}
		}
		else { //DEFENSE
			//Navigate to the front of the net (use parameter d1)
			Actions.navigateToDefense(wifi.StartData.get("d1"));
			
			//Stop the odometryCorrection and obstacleDetector (stop using the ultrasonic sensor)
			odometryCorrection.setRunning(false);
			obstacleDetector.setRunning(false);
			
			//Shut down. This will turn off the sensors (most importantly the Ultrasonic sensor) and will save the robot's battery. The robot is in position and does not need to move anymore
			System.exit(0);
		}
	}
	
	/**
	 * Starting point of the program. Where the static reference of the Main class is created
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		new Main();
	}
	
	/**
	 * get the reference to the main object, following the Singleton Pattern
	 * @return the static reference of the Main object
	 */
	public static Main get() {
		return instance;
	}
	
	/**
	 * @param key Either "right", "left", "throw1" or "throw2", each representing a different motor
	 * @return the motor referenced by the key
	 */
	public EV3LargeRegulatedMotor getMotor(String key) {
		return motors.get(key);
	}
	
	/**
	 * get the reference to the sensor poller
	 * @return the sensor poller
	 */
	public SensorPoller getSensorPoller() {
		return sensorPoller;
	}
	
	/**
	 * get the reference to the odometer object
	 * @return the odometer
	 */
	public Odometer getOdometer() {
		return odometer;
	}
	
	/**
	 * get the reference to the odometry correction object
	 * @return the odometry correction
	 */
	public OdometryCorrection getOdometryCorrection() {
		return odometryCorrection;
	}
	
	/**
	 * get the reference to the navigator object
	 * @return the navigator
	 */
	public Navigator getNavigator() {
		return navigator;
	}
	
	/**
	 * get the reference to the obstacle detector object
	 * @return the obstacle detector
	 */
	public ObstacleDetector getObstacleDetector() {
		return obstacleDetector;
	}
	
	/**
	 * get the reference to the display object, used to display text and values on the EV3 Brick
	 * @return the display
	 */
	public Display getDisplay() {
		return display;
	}
	
	/**
	 * get the reference to the wifi connection object, containing the starting data
	 * @return the wifi connection
	 */
	public WifiConnection getWifi() {
		return wifi;
	}
	
	/**
	 * get the role of the robot
	 * @return whether the robot is playing the forward role or the defender role
	 */
	public boolean isForward() {
		return forward;
	}
}
