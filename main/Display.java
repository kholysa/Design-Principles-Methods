package main;
import java.util.HashMap;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;

/**
 * Class that allows easy display of values along with their "key" (their name).
 * Useful for printing values from anywhere in the code in order to debug.
 * 
 * @author Jerome
 * @version 1.1
 */
public class Display extends Thread{
	private static final long ITERATION_TIME = 250;
	private TextLCD t;
	private HashMap<String, Double> displayValues;
	
	private Object lock;

	/**
	 * constructs a default display object
	 */
	public Display() {
		t = LocalEV3.get().getTextLCD();
		displayValues = new HashMap<String, Double>();
		lock = new Object();
	}

	// run method (required for Thread)
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		long displayStart, displayEnd;

		while (true) {
			displayStart = System.currentTimeMillis();
			
			//Clear the display
			t.clear();

			// Display the information
			synchronized(lock) {
				int line = 0;
				for(String key: displayValues.keySet()) {
					if(line < 8) { //The screen only has 6 rows
						t.drawString(key + ": " + formattedDoubleToString(displayValues.get(key), 2), 0, line);
						line++;
					}
					else break;
				}
			}
			
			// throttle the OdometryDisplay
			displayEnd = System.currentTimeMillis();
			if (displayEnd - displayStart < ITERATION_TIME) {
				try {
					Thread.sleep(ITERATION_TIME - (displayEnd - displayStart));
				} catch (InterruptedException e) {
					// there is nothing to be done here because it is not
					// expected that OdometryDisplay will be interrupted
					// by another thread
				}
			}
		}
	}
	
	/**
	 * set all display values. Note that only a maximum of 6 can be displayed on the EV3 LCD screen
	 * @param values hashmap of the values to display. The keys are used as a name and are displayed before the value
	 */
	public void setDisplayValues(HashMap<String, Double> values) {
		this.displayValues = values;
	}
	
	
	/**
	 * get all display values
	 * @return the hashmap containing the values along with their key (their name)
	 */
	public HashMap<String, Double> getDisplayValues() {
		return displayValues;
	}
	
	/**
	 * add a value to display, along with its key (its name)
	 * @param key the name of the value. Will be displayed next to it
	 * @param value the value corresponding to that key
	 */
	public void addDisplayValue(String key, double value) {
		synchronized(lock) {
			displayValues.put(key, value);
		}
	}
	
	/**
	 * remove a value from the display list
	 * @param key the key of the value to remove
	 * @return the value that was remove, or -1 if the key wasn't found
	 */
	public double removeDisplayValue(String key) {
		synchronized(lock) {
			if(displayValues.containsKey(key))
				return displayValues.remove(key);
			else
				return -1;
		}
	}
	
	/**
	 * converts a double to a string and keeps only a specified number of decimal places
	 * @param x the value to convert to string
	 * @param places the number of decimal places to keep in the final string
	 * @return
	 */
	private static String formattedDoubleToString(double x, int places) {
		String result = "";
		String stack = "";
		long t;
		
		// put in a minus sign as needed
		if (x < 0.0)
			result += "-";
		
		// put in a leading 0
		if (-1.0 < x && x < 1.0)
			result += "0";
		else {
			t = (long)x;
			if (t < 0)
				t = -t;
			
			while (t > 0) {
				stack = Long.toString(t % 10) + stack;
				t /= 10;
			}
			
			result += stack;
		}
		
		// put the decimal, if needed
		if (places > 0) {
			result += ".";
		
			// put the appropriate number of decimals
			for (int i = 0; i < places; i++) {
				x = Math.abs(x);
				x = x - Math.floor(x);
				x *= 10.0;
				result += Long.toString((long)x);
			}
		}
		
		return result;
	}
}
