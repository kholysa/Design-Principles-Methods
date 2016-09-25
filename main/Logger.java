package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Logger {
	
	private PrintWriter out;
	
	private long startTime;
	
	public Logger() {
		startTime = System.currentTimeMillis();
		try {
			out = new PrintWriter(new File("log.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void addValue(float val) {
		out.println((System.currentTimeMillis() - startTime) + "\t" + val);
	}
	
	public void close() {
		out.close();
	}

}
