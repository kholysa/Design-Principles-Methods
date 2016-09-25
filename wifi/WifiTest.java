/*
* @author Sean Lawlor
* @date November 3, 2011
* @class ECSE 211 - Design Principle and Methods
* 
* Modified by F.P. Ferrie
* February 28, 2014
* Changed parameters for W2014 competition
* 
* Modified by Francois OD
* November 11, 2015
* Ported to EV3 and wifi (from NXT and bluetooth)
* Changed parameters for F2015 competition
*/

package wifi;

import java.io.IOException;
import java.util.HashMap;

import wifi.WifiConnection;
//import wifi.StartCorner;
//import wifi.Transmission;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;

public class WifiTest {
	// example call of the transmission protocol
	// The print function is just for debugging to make sure data is received correctly

	// *** INSTRUCTIONS ***
	// There are two variables to set manually on the EV3 client:
	// 1. SERVER_IP: the IP address of the computer running the server application
	// 2. TEAM_NUMBER: your project team number
	
	private static final String SERVER_IP = "192.168.43.204";
	private static final int TEAM_NUMBER = 5;
	
	
	private static TextLCD LCD = LocalEV3.get().getTextLCD();

	public static void main(String [] args) {
		
		WifiConnection conn = null;
		try {
			conn = new WifiConnection(SERVER_IP, TEAM_NUMBER);
		} catch (IOException e) {
			LCD.drawString("Connection failed", 0, 8);
		}
		
		//example use of StartData
		LCD.clear();
		if (conn != null){
			HashMap<String,Integer> t = conn.StartData;
			if (t == null) {
				LCD.drawString("Failed to read transmission", 0, 5);
			} else {
				LCD.drawString("Transmission read", 0, 5);
				LCD.drawString(t.toString(), 0, 6);
			
			}
		} else {
			LCD.drawString("Connection failed", 0, 5);
		}
		
		Button.ESCAPE.waitForPress();
	}
}
