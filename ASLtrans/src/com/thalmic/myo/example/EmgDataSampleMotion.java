package com.thalmic.myo.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.enums.StreamEmgType;

// Recording time-series data

public class EmgDataSampleMotion {

	public static int READING_FREQUENCY = 20;
	private static int HOW_MANY_WORDS = 5;
	private static int HOW_MANY_SETS = 5;
	private static int MAX_RECORDS = 200;
	private static boolean RIGHT_HAND_DOMINANT = true;
	private static boolean TIME_SERIES = true;
	private static String MODE = "";
	public static String[] words;
	private static Hub hub;
	private static DataCollector dataCollector;

	public static void main(String[] args) {
		int lines = 0;
		String non_time_file = null;
		String time_series_file = null;
		try {
			// Create a new "Hub" which is used by the Myo API to keep track of the Myo devices
			hub = new Hub("com.example.emg-data-sample");

			// hub.waitForMyo(ms) returns a Myo object if found within ms milliseconds, or null otherwise
			System.out.println("Attempting to find a Myo...");
			Myo myo = hub.waitForMyo(10000);

			if (myo == null) {
				throw new RuntimeException("Unable to find a Myo!");
			}

			// setStreamEmg tells the Myo to send EMG data
			System.out.println("Connected to a Myo armband!");
			myo.setStreamEmg(StreamEmgType.STREAM_EMG_ENABLED);
			
			// User input
			greet();
			
			// create an instance of a DeviceListener to record the data, and tell the hub to use it
			dataCollector = new DataCollector(RIGHT_HAND_DOMINANT, TIME_SERIES);
			hub.addListener(dataCollector);

			System.out.println("Recording each data (sentence/word) in " + HOW_MANY_SETS + " set(s). Reading once every " + READING_FREQUENCY + " ms.");
			System.out.println();
			createDirs();
			createReadme();
			// set the filenames outside the loop (only one file per recordings)
			non_time_file = checkFileName(false);
			time_series_file = checkFileName(true);
			dataCollector.prepareFile(non_time_file, MAX_RECORDS, !TIME_SERIES);
			dataCollector.prepareFile(time_series_file, MAX_RECORDS, TIME_SERIES);
			for (int i = 0; i < HOW_MANY_SETS; i++) {
				System.out.println("==============================");
				System.out.println("                         SET "+(i+1) + " (Press enter to start recording...)");
				System.out.println("==============================");
				// Start recording
				record(non_time_file, time_series_file);
			}
			dataCollector.saveDataToFile(non_time_file);
			dataCollector.saveDataToFile(time_series_file);
		} catch (Exception e) {
			System.err.println("Error: ");
			e.printStackTrace();
			System.exit(1);
		}

		finally {
			System.out.println("bye!");
		}
	}
	
	// myo's event loop runs here and records the data
	private static void record(String non_time_file, String time_series_file) {
		Scanner in = new Scanner(System.in);
		in.nextLine();
		int lines, columns;
		for (int j = 0; j < HOW_MANY_WORDS; j++) {
			System.out.println("Please make hand gesture for **" + words[j] + "** now.");
			countdown();
			lines = 0;
			columns = 0;
			dataCollector.insertName(words[j], time_series_file);
			
			// Recording MODES:			
			if (MODE.equals("u")) {
				// User enabled mode
				System.out.println("Press enter to stop...");
				Process process = new Process(hub, dataCollector, non_time_file, time_series_file, words[j]);
				process.start();
				
				Scanner ent = new Scanner(System.in);
				ent.nextLine();
				process.shutdown();
			}
			
			else if (MODE.equals("s")) {
				// Static mode
				while (!maxColumnsWritten(columns)) {
					// run the event loop for READING_FREQUENCY milliseconds and
					// record all values within this time
					hub.run(READING_FREQUENCY);
					lines = dataCollector.writeToFileMotion(non_time_file, words[j]);
					columns = dataCollector.writeToFileMotionTimeSeries(time_series_file);
				}
				dataCollector.carriageReturn(time_series_file);
			}
			
			else if (MODE.equals("a")) {
				// Automatic mode
				boolean motion = true;
				boolean activated = false;
				double distance = 0.0;
				long timestamp = -1;
				long elapsed_time = 0;
				int track = 0;
				double data[] = null;
				while (motion) {
					hub.run(READING_FREQUENCY);
					
					// Activate the recording here when detecting initial motion
					if (activated == false) {
						distance = (Data.calcEuclideanDistance(data, dataCollector.getData()));
						if (distance > 0.5) {
							timestamp = dataCollector.getTimeStamp();
							activated = true;
						}
						data = dataCollector.getData();
					}
					
					// Deactivate the recording here by updating the data every 1/2 seconds and detecting if motion has been made or no
					else {
						lines = dataCollector.writeToFileMotion(non_time_file, words[j]);
						columns = dataCollector.writeToFileMotionTimeSeries(time_series_file);
						track++;

						elapsed_time += dataCollector.getTimeStamp() - timestamp;
						timestamp = dataCollector.getTimeStamp();
						
						// Update the data every 1/2 second
						if (elapsed_time > 200000) {
							elapsed_time = 0;
							distance = (Data.calcEuclideanDistance(data, dataCollector.getData()));
//							System.out.println(distance);
							if (distance < 1) {
								System.out.println(track);
								track = 0;
								motion = false;
							}
							data = dataCollector.getData();
						}
					}
				}
			} else
				System.out.println("This shouldnt run!");
		}
	}
	
	/*
	 * HELPER METHODS BELOW::
	 */
	
	// Creates Readme
	private static void createReadme() {
		String mdata = "DATA";
		File rdme = new File(mdata+"\\README.txt");
		if (!rdme.exists()) {
			FileWriter writer = null;
			try {
				writer = new FileWriter(rdme, true);
			} catch (IOException e) {
				e.printStackTrace();
			}
			StringBuilder sb = new StringBuilder("");
			sb.append("Data is formatted as follows:\r\n");
			sb.append("Reading Frequency: " + READING_FREQUENCY + "\r\n");
			sb.append("Readings per word: " + MAX_RECORDS + "\r\n");
			sb.append("Words: " + Arrays.toString(words) + "\r\n");			
			sb.append("Dominant hand: " + (RIGHT_HAND_DOMINANT ? "Right" : "Left") + "\r\n");
			
			try {
				writer.write(sb.toString());
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Check if motion is still present by calculating the euclidean distance between every nth recording
	public static boolean checkMotion() {
		return true;
	}
	
	// 3 second countdown
	private static void countdown() {
		try {
			System.out.println("Get ready...");
			System.out.println("3");
			Thread.sleep(1000);
			System.out.println("2");
			Thread.sleep(1000);
			System.out.println("1");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// never mind
		}
		System.out.println("Start recording!");
	}
	
	// Initial prompt
	private static void greet() {
		Scanner in = new Scanner(System.in);
		try {
			int freq, words, sets, records = 0;
			String input, mode = "";
			System.out.println("\nWelcome to Myo Data Collector for Motion");

			System.out.print("Use default values? ");
			System.out.print("Dominant Myo: Right hand, Frequency: "
					+ READING_FREQUENCY + ", Words: " + HOW_MANY_WORDS
					+ ", Sets: " + HOW_MANY_SETS + " (y/n): ");
			input = in.nextLine();
			if (input.equals("n")) {
				System.out.print("What hand is the dominant Myo (r/l)? ");
				input = in.nextLine();
				if (input.equals("r")) {
					RIGHT_HAND_DOMINANT = true;
				} else if (input.equals("l")) {
					RIGHT_HAND_DOMINANT = false;
				} else {
					RIGHT_HAND_DOMINANT = true;
				}
				System.out.print("Frequency? (20 recommended) ");
				freq = in.nextInt();
				System.out.print("How many Words per set? ");
				words = in.nextInt();
				System.out.print("How many sets? ");
				sets = in.nextInt();
				System.out.println("What recording mode? ('u' = user enabled (start and stop recording by pressing enter, NonTS only), 's' = static (record x amount of records per word), 'a' = automatic (stop recording when no motion is detected, NonTS only)");
				while (true) {
					if (mode.equals("u") || mode.equals("s") || mode.equals("a"))
						break;
					System.out.print("Please make a correct selection: ");
					mode = in.nextLine();
				}
				if (mode.equals("s")) {
					System.out.print("How many readings/records per word? ");
					records = in.nextInt();
				}	
				READING_FREQUENCY = freq;
				HOW_MANY_WORDS = words;
				HOW_MANY_SETS = sets;
				MAX_RECORDS = records;
				MODE = mode;
				askForWords(in);
			} else if (input.equals("y")) {
				System.out.print("What recording mode? ('u' = user enabled (start and stop recording by pressing enter), 's' = static (record x amount of records per word), 'a' = automatic (stop recording when no motion is detected)");
				System.out.println();
				while (true) {
					if (mode.equals("u") || mode.equals("s") || mode.equals("a"))
						break;
					System.out.print("Please make a correct selection: ");
					mode = in.nextLine();
				}
				if (mode.equals("s")) {
					System.out.print("How many readings/records per word? ");
					records = in.nextInt();
				}
				MAX_RECORDS = records;
				MODE = mode;
				askForWords(in);
			} else {
				System.out.println("Please restart the program with valid input..");
				System.exit(1);
			}
		} catch (Exception e) {
			System.out.println("An error occured.. Please restart the program. " + e);
			System.exit(1);
		}
	}
	
	// Ask for the names of the gestures (classes)
	public static void askForWords(Scanner input) {
		System.out.println();
		words = new String[HOW_MANY_WORDS];
		input = new Scanner(System.in);
		for (int i = 0; i < HOW_MANY_WORDS; i++) {
			System.out.print("Please enter name for word " + (i+1) + ": ");
			words[i] = input.nextLine();
		}
	}
	
	// returns true if and only if the maximum number of lines has been written to the file
	public static boolean maxLinesWritten(int lines) {
		if (lines == 0)
			return false;
		return (lines % (MAX_RECORDS)+1) == 0;
	}
	
	// returns true if and only if the maximum number of columns has been written to the file
	public static boolean maxColumnsWritten(int columns) {
//		if (columns == 0) return false;
		return columns >= (26 * MAX_RECORDS);
	}

	// Ensure that the file name doesn't exist. If it does, generate a unique one.
	public static String checkFileName(boolean time) {
		String mdata = "DATA";
		String time_dir = mdata + "\\TIME_SERIES_DATA";
		String non_time_dir = mdata + "\\NON_TIME_SERIES_DATA";
		int count = 0;
		String name;
		File data;
		do {
			count++;
			name = "" + (time ? time_dir : non_time_dir) + "\\data" + count + ".csv";
			data = new File(name);
		} while (data.exists());
		System.out.println("Creating " + data.getAbsolutePath());
		return name;
	}
	
	// Create directories
	public static void createDirs() {
		String data = "DATA";
		String time_dir = data + "\\TIME_SERIES_DATA";
		String non_time_dir = data + "\\NON_TIME_SERIES_DATA";

		File theDir = new File(time_dir);
		File theDir2 = new File(non_time_dir);
		File theDir3 = new File(data);
		// if the directories does not exist, create it
		if (!theDir3.exists()) {
			try {
				theDir3.mkdir();
			} catch (SecurityException se) {
				System.err.println(se);
			}
		}
		
		if (!theDir.exists()) {
			try {
				theDir.mkdir();
			} catch (SecurityException se) {
				System.err.println(se);
			}
		}
		
		if (!theDir2.exists()) {
			try {
				theDir2.mkdir();
			} catch (SecurityException se) {
				System.err.println(se);
			}
		}
	}
}

class Process extends Thread {
	private boolean running;
	private Hub hub;
	private DataCollector dataCollector;
	private String non_time_file;
	private String time_series_file;
	private String word;
	
	public Process(Hub hub, DataCollector dataCollector, String non_time_file, String time_series_file, String word) {
		this.running = true;
		this.hub = hub;
		this.dataCollector = dataCollector;
		this.non_time_file = non_time_file;
		this.time_series_file = time_series_file;
		this.word = word;
	}
	
	public void shutdown() {
		this.running = false;
		dataCollector.carriageReturn(time_series_file);
	}
	
	public void run() {
			while (running) {
				// run the event loop for READING_FREQUENCY milliseconds and
				// record all values within this time
				hub.run(EmgDataSampleMotion.READING_FREQUENCY);
				int lines = dataCollector.writeToFileMotion(non_time_file, word);
				int columns = dataCollector.writeToFileMotionTimeSeries(time_series_file);
				try {
					Thread.sleep(1);
				} catch(Exception e) {
					System.out.println(e);
				}
			}
		}
	}