package main;

/**
 * Navigator is a thread that allows to easily get the robot to move to a specific point and/or to a specific angle.
 * It can be paused in order for the rest of the code to navigate the robot using different controllers
 * 
 * @author Jerome
 * @version 1.2
 */
public class Navigator extends Thread {
	private static final int RUN_PERIOD = 20; //Time between two iterations of the infinite loop in the thread
	private static final double DIST_TOLERANCE = 0.6; //Tolerance, in cm, from the target position
	private static final double ANGLE_TOLERANCE = Math.PI / 60; //Tolerance, in radians, from the target angle
	private static final int MOTOR_MAX_ROTATION_SPEED = 110; //Motor constants
	private static final int MOTOR_MIN_ROTATION_SPEED = 60;
	private static final int MOTOR_MAX_MOVE_SPEED = 300;
	private static final int MOTOR_MIN_MOVE_SPEED = 115;
	private static final double LEFT_ADJUSTMENT = 0.05; //Percent/100 correction of the left wheel speed to account for the heavier weight on the left side
	
	private Object lock;
	
	private volatile boolean running = false; //Whether the navigator is active or not
	
	private boolean navigating = false; //Whether the navigator is currently moving to a target point
	private boolean backwards = false; // Whether to navigate while the robot is facing forward or backwards
	private boolean turning = false;
	private double targetTheta; //Variables used to set a target position
	
	private double[][] targets = { {0, 0} };
	private int targetIndex = 0; //Specify whether

	/**
	 * construct a default navigator
	 */
	public Navigator() {	
		lock = new Object();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		long startTime, endTime;

		while(true) {
			if(running) {
				startTime = System.currentTimeMillis();
				
				//First, calculate the target theta based on the current position compared to the target position
				synchronized(lock) {
					if(!turning) { //Only calculate the target theta if a specific theta has not been given
						targetTheta = Math.atan2(Main.get().getOdometer().getY() - targets[targetIndex][1], Main.get().getOdometer().getX() - targets[targetIndex][0]) - Math.PI;
						if(backwards) //Add 180 degrees in order to have the back of the robot facing forward
							targetTheta += Math.PI;
						if(targetTheta < 0)
							targetTheta += 2*Math.PI;
						else if(targetTheta > 2*Math.PI)
							targetTheta -= 2*Math.PI;
					}
					
					Main.get().getDisplay().addDisplayValue("TX", targets[targetIndex][0]);
					Main.get().getDisplay().addDisplayValue("TY", targets[targetIndex][1]);
				
					double thetaVar = Main.get().getOdometer().getTheta() - targetTheta; //The difference between the current theta and the target one
						
					if(Math.abs(Main.get().getOdometer().getX() - targets[targetIndex][0]) > DIST_TOLERANCE || Math.abs(Main.get().getOdometer().getY() - targets[targetIndex][1]) > DIST_TOLERANCE || turning) { //Do not move if the target location has been reached
						if(Math.abs(thetaVar) > ANGLE_TOLERANCE && Math.abs(thetaVar) < 2*Math.PI - ANGLE_TOLERANCE) { //Check if the angle is okay
							//The angle is not okay, we need to adjust it
							int speed = Math.min(MOTOR_MAX_ROTATION_SPEED, (int) (Math.abs(thetaVar) / (Math.PI / 3) * (MOTOR_MAX_ROTATION_SPEED - MOTOR_MIN_ROTATION_SPEED) + MOTOR_MIN_ROTATION_SPEED));
							Main.get().getMotor("right").setSpeed(speed);
							Main.get().getMotor("left").setSpeed(speed);
							
							//Find which direction to go to turn by a minimal amount
							if(thetaVar > 0 && thetaVar < Math.PI || thetaVar < -Math.PI) { //Need to turn to the right
								Main.get().getMotor("right").backward();
								Main.get().getMotor("left").forward();
							}
							else { //Turn to the left
								Main.get().getMotor("right").forward();
								Main.get().getMotor("left").backward();
							}
							
							//We rotated and thus the readings of the light sensors for the odometry correction are unusable. Reset them:
							Main.get().getOdometryCorrection().reset();
							
						}
						else { //The angle is fine, we can thus move forward to reach the target position
							if(turning) { //Turning means we dont want to move anymore
								turning = false;
								targets[targetIndex] = new double[] {Main.get().getOdometer().getX(), Main.get().getOdometer().getY()}; //Prevent it from moving again
							}
							else {
								double distance2 = Math.pow(Main.get().getOdometer().getX() - targets[targetIndex][0], 2) + Math.pow(Main.get().getOdometer().getY() - targets[targetIndex][1], 2);
								int speed = Math.min(MOTOR_MAX_MOVE_SPEED, (int) (distance2 / 250 * (MOTOR_MAX_MOVE_SPEED - MOTOR_MIN_MOVE_SPEED) + MOTOR_MIN_MOVE_SPEED));
								Main.get().getMotor("right").setSpeed(speed);
								Main.get().getMotor("left").setSpeed((int) (speed + speed*LEFT_ADJUSTMENT));
								
								if(backwards) {
									Main.get().getMotor("right").backward();
									Main.get().getMotor("left").backward();
								}
								else {
									Main.get().getMotor("right").forward();
									Main.get().getMotor("left").forward();
								}
							}
						}
					}
					else { //We have reached the target position!
						
						if(targetIndex + 1 < targets.length) {
							targetIndex++;
						}
						else {
							Main.get().getMotor("right").stop(true);
							Main.get().getMotor("left").stop();
							navigating = false;
						}
					}
				}
				
				//Wait if the code above was performed faster than the RUN_PERIOD
				endTime = System.currentTimeMillis();
				if (endTime - startTime < RUN_PERIOD) {
					try {
						Thread.sleep(RUN_PERIOD - (endTime - startTime));
					} catch (InterruptedException e) {
						//Nothing to do here
					}
				}
			}
		}
		
	}
	
	/**
	 * Set a point to move to. If the navigator is running it will directly start moving in the direction of the point
	 * @param x the x position to navigate to
	 * @param y the y position to navigate to
	 */
	public void travelTo(double x, double y) {
		navigating = true;
		synchronized(lock) {
			targets = new double[][] { {x, y} };
			targetIndex = 0;
			backwards = false;
		}
	}
	/**
	 * Set a point to move to, and specify whether to approach it forward or backwards. If the navigator is running it will directly start moving in the direction of the point
	 * @param x the x position to navigate to
	 * @param y the y position to navigate to
	 * @param backwards whether the robot should approach the point facing backwards or forward
	 */
	public void travelTo(double x, double y, boolean backwards) {
		navigating = true;
		synchronized(lock) {
			targets = new double[][] { {x, y} };
			targetIndex = 0;
			this.backwards = backwards;
		}
	}
	
	/**
	 * Set a point to move to by moving on axes individually. Navigates on the x axis first. If the navigator is running it will directly start moving in the direction of the point
	 * @param x the x position to navigate to
	 * @param y the y position to navigate to
	 */
	public void travelToSquare(double x, double y) {
		navigating = true;
		synchronized(lock) {
			targets = new double[][] { {x, Main.get().getOdometer().getY()}, {x, y} };
			targetIndex = 0;
			backwards = false;
		}
	}
	
	/**
	 * Set a point to move to by moving on axes individually, specify whether to approach it forward or backwards and whether to move on the x or y axis first. If the navigator is running it will directly start moving in the direction of the point
	 * @param x the x position to navigate to
	 * @param y the y position to navigate to
	 * @param backwards whether the robot should approach the point facing backwards or forward
	 * @param yFirst whether to move on the y axis first or the x axis first
	 */
	public void travelToSquare(double x, double y, boolean backwards, boolean yFirst) {
		navigating = true;
		synchronized(lock) {
			if(yFirst)
				targets = new double[][] { {Main.get().getOdometer().getX(), y}, {x, y} };
			else
				targets = new double[][] { {x, Main.get().getOdometer().getY()}, {x, y} };	
			targetIndex = 0;
			this.backwards = backwards;
		}
	}
	
	/**
	 * Set an angle to rotate to. The robot will stop moving after reaching this angle
	 * @param theta the angle to rotate to (in radians from the positive x-axis)
	 */
	public void turnTo(double theta) {
		turning = true;
		navigating = true;
		synchronized(lock) {
			while(theta > 2*Math.PI)
				theta -= 2*Math.PI;
			targetTheta = theta;
		}
	}
	
	//Accessors
	/**
	 * check if the navigator has reached its destination
	 * @return whether the robot is in motion
	 */
	public boolean isNavigating() {
		return navigating;
	}
	
	/**
	 * Check if the robot has reached the angle specified in the turnTo() method
	 * @return whether the robot is rotating
	 */
	public boolean isTurning() {
		return turning;
	}
	
	/**
	 * Get the current target the robot is trying to reach
	 * @param position array to be filled by the x, y and theta values of the target position
	 * @param update array specifying which values to retrieve
	 */
	public void getTarget(double[] position, boolean[] update) {
		// ensure that the values don't change while the odometer is running
		synchronized (lock) {
			if (update[0])
				position[0] = targets[targetIndex][0];
			if (update[1])
				position[1] = targets[targetIndex][1];
			if (update[2])
				position[2] = targetTheta;
		}
	}
	
	/**
	 * Get the current target x postion the robot is trying to reach
	 * @return the target x position
	 */
	public double getTargetX() {
		double result;
		synchronized(lock) {
			result = targets[targetIndex][0];
		}
		return result;
	}
	
	/**
	 * Get the current target y postion the robot is trying to reach
	 * @return the target y position
	 */
	public double getTargetY() {
		double result;
		synchronized(lock) {
			result = targets[targetIndex][1];
		}
		return result;
	}
	
	/**
	 * Get the current target angle the robot is trying to reach
	 * @return the target theta (angle)
	 */
	public double getTargetTheta() {
		double result;
		synchronized(lock) {
			result = targetTheta;
		}
		return result;
	}
	
	/**
	 * Get the final target x postion the robot is trying to reach
	 * @return the final target x position
	 */
	public double getFinalTargetX() {
		double result;
		synchronized(lock) {
			result = targets[targets.length - 1][0];
		}
		return result;
	}
	
	/**
	 * Get the final target y postion the robot is trying to reach
	 * @return the final target y position
	 */
	public double getFinalTargetY() {
		double result;
		synchronized(lock) {
			result = targets[targets.length - 1][1];
		}
		return result;
	}
	
	/**
	 * pause or resume the navigator's thread
	 * @param running whether the navigator should pause or resume
	 */
	public void setRunning(boolean running) {
		synchronized(lock) {
			this.running = running;
			if(!running) { //Stop the motors if pausing the navigator
				Main.get().getMotor("right").stop(true);
				Main.get().getMotor("left").stop();
			}
		}
	}
	
}
