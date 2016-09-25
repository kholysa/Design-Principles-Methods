package main;

import filters.Filter;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * Obstacle detector is a thread that runs in the background while the navigator is running in order to detect obstacles (blocks) in the way. This class is also responsible for moving around the obstacle.
 * To do so, it pauses the navigator and uses a wall follower algorithm to move around the obstacle until the robot points in the direction of the target position of the navigator.
 * 
 * @author Jerome
 * @version 1.0
 */
public class ObstacleDetector extends Thread {
	
	private static final long ITERATION_TIME = 25;
	private static final int WALL_DIST = 22; //distance from a wall at which robot will start moving around
	
	private boolean avoiding = false;
	
	private volatile boolean running = false;
	
	/**
	 * constructs a default obstacle detector
	 */
	public ObstacleDetector() {
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		long correctionStart, correctionEnd;

		while (true) {
			correctionStart = System.currentTimeMillis();
			
			if(running) {
				
				Main.get().getDisplay().addDisplayValue("Front", Main.get().getSensorPoller().getTopUsReading(Filter.MEAN));
				
				if(Main.get().getSensorPoller().getTopUsReading(Filter.MEAN) < WALL_DIST) { //An obstacle was detected in range
					
					avoiding = true;
					
					Main.get().getOdometryCorrection().setRunning(false); //Pause the odometry correction
					
					EV3LargeRegulatedMotor rightM = Main.get().getMotor("right");
					EV3LargeRegulatedMotor leftM  = Main.get().getMotor("left");
					
					Navigator n = Main.get().getNavigator();
					
					double targetX = n.getFinalTargetX(); //Save the target theta in order to tell the navigator to continue its previous course after the obstacle has been avoided
					double targetY = n.getFinalTargetY();
					
					n.travelTo(Main.get().getOdometer().getX(), Main.get().getOdometer().getY());
					
					n.setRunning(false); //Pause the navigator
					
					n.turnTo(Main.get().getOdometer().getTheta() + 1.35);
					n.setRunning(true);
					while(n.isTurning());
					n.setRunning(false);
					
					//Move forward a bit
					rightM.setSpeed(200);
					leftM.setSpeed(200);
					rightM.rotate(850, true);
					leftM.rotate(850);
					
					n.turnTo(Main.get().getOdometer().getTheta() - 0.45);
					n.setRunning(true);
					while(n.isTurning());
					n.setRunning(false);
					
					//Move forward a bit
					rightM.setSpeed(200);
					leftM.setSpeed(200);
					rightM.rotate(300, true);
					leftM.rotate(300);
					
					n.travelToSquare(targetX, targetY);
					n.setRunning(true); //Resume the navigator
					Main.get().getOdometryCorrection().setRunning(true); //Resume the odometry correction
					
					avoiding = false;
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
	 * Pause or resume the obstacle detector
	 * @param running whether to pause or resume the detector
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}
	
	/**
	 * Check if the robot is currently avoiding an obstacle
	 * @return whether the object is avoiding an obstacle or not
	 */
	public boolean isAvoiding() {
		return avoiding;
	}
}
