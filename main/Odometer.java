package main;

/**
 * The odometer is a thread that, once started, keeps track of the robot's position by continuously calculating the distance traveled and the change in angle
 *
 * @author Jerome
 * @version 1.0
 */
public class Odometer extends Thread {

	// odometer update period, in ms
	private static final long ITERATION_TIME = 15;
	private static final double WHEEL_RADIUS = 2.02; //Used to be 2.01
	private static final double TRACK = 15.56;
	
	// robot position
	private double x, y, theta;

	// lock object for mutual exclusion
	private Object lock;
	
	private int previousRightTacho, previousLeftTacho; //Need to store the previous tacho counts to calculate the variation in the tacho values

	/**
	 * construct a default odometer
	 */
	public Odometer() {
		x = 0.0;
		y = 0.0;
		theta = Math.PI / 2;
		lock = new Object();
		
		previousRightTacho = Main.get().getMotor("right").getTachoCount(); //Get the initial tacho values
		previousLeftTacho = Main.get().getMotor("left").getTachoCount();
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		long updateStart, updateEnd;

		while (true) {
			updateStart = System.currentTimeMillis();
			
			//Retrieve the wheel readings
			int rightTacho = Main.get().getMotor("right").getTachoCount();
			int leftTacho = Main.get().getMotor("left").getTachoCount();
			int dRightTacho = rightTacho - previousRightTacho; //Change in the wheels' angle
			int dLeftTacho = leftTacho - previousLeftTacho;
			previousRightTacho = rightTacho;
			previousLeftTacho = leftTacho;
			
			double leftDistance = dLeftTacho/360d * 2 * Math.PI * WHEEL_RADIUS; //Distance traveled by each wheels
			double rightDistance = dRightTacho/360d * 2 * Math.PI * WHEEL_RADIUS;
			double distance = (rightDistance + leftDistance) / 2; //Distance traveled by the center of the robot
			double deltaTheta = (rightDistance - leftDistance) / TRACK; //Angle by which the robot turned in the last time interval

			synchronized (lock) {
				// don't use the variables x, y, or theta anywhere but here!
				x += distance * Math.cos(theta + deltaTheta/2); //Update the x and y coordinate values of the odometer
				y += distance * Math.sin(theta + deltaTheta/2);
				theta += deltaTheta; //The total angle also changes
				
				//Keep theta between 0 and 2 pi
				theta %= 2*Math.PI;
				if(theta < 0)
					theta += 2*Math.PI;
				
				Main.get().getDisplay().addDisplayValue("X", x);
				Main.get().getDisplay().addDisplayValue("Y", y);
				Main.get().getDisplay().addDisplayValue("Theta", theta / (2*Math.PI) * 360);
			}

			// this ensures that the odometer only runs once every period
			updateEnd = System.currentTimeMillis();
			if (updateEnd - updateStart < ITERATION_TIME) {
				try {
					Thread.sleep(ITERATION_TIME - (updateEnd - updateStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that the odometer will be interrupted by
					// another thread
				}
			}
		}
	}

	// accessors
	/**
	 * get the x and y position (in centimeters) of the robot as tracked by the odometer, as well as the angle in radians (from the positive x-axis)
	 * @param position the array to be filled with the values of x, y and theta
	 * @param update the array specifying which values to retrieve
	 */
	public void getPosition(double[] position, boolean[] update) {
		// ensure that the values don't change while the odometer is running
		synchronized (lock) {
			if (update[0])
				position[0] = x;
			if (update[1])
				position[1] = y;
			if (update[2])
				position[2] = theta;
		}
	}

	/**
	 * get the x component of the position as tracked by the odometer
	 * @return the position of the robot in the x-axis (in centimeters)
	 */
	public double getX() {
		double result;

		synchronized (lock) {
			result = x;
		}

		return result;
	}

	/**
	 * get the y component of the position as tracked by the odometer
	 * @return the position of the robot in the y-axis (in centimeters)
	 */
	public double getY() {
		double result;

		synchronized (lock) {
			result = y;
		}

		return result;
	}

	/**
	 * get the angle component of the position as tracked by the odometer
	 * @return the orientation of the robot (in radians from the positive x-axis)
	 */
	public double getTheta() {
		double result;

		synchronized (lock) {
			result = theta;
		}

		return result;
	}

	// mutators
	/**
	 * set the current x and y position (in centimeters) of the robot, as well as the angle in radians (from the positive x-axis)
	 * @param position the array of values specifying x, y and theta
	 * @param update the array specifying which values to update
	 */
	public void setPosition(double[] position, boolean[] update) {
		// ensure that the values don't change while the odometer is running
		synchronized (lock) {
			if (update[0] && !Double.isNaN(position[0]))
				x = position[0];
			if (update[1] && !Double.isNaN(position[1]))
				y = position[1];
			if (update[2] && !Double.isNaN(position[2]))
				theta = position[2];
		}
	}

	/**
	 * set the current x component of the position of the robot
	 * @param x the position of the robot in the x-axis (in centimeters)
	 */
	public void setX(double x) {
		synchronized (lock) {
			if(!Double.isNaN(x))
				this.x = x;
		}
	}

	/**
	 * set the current y component of the position of the robot
	 * @param y the position of the robot in the y-axis (in centimeters)
	 */
	public void setY(double y) {
		synchronized (lock) {
			if(!Double.isNaN(y))
				this.y = y;
		}
	}

	/**
	 * set the current angle of the robot
	 * @param theta the angle of the robot (in radians from the positive x-axis)
	 */
	public void setTheta(double theta) {
		if(theta >= Math.PI * 2)
			theta -= Math.PI * 2;
		synchronized (lock) {
			if(!Double.isNaN(theta))
				this.theta = theta;
		}
	}
		
}
