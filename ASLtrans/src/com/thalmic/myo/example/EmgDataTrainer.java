package com.thalmic.myo.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;


import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.enums.StreamEmgType;

// Attempt at Data Trainer program
public class EmgDataTrainer {
	private static BufferedReader br;
	private static boolean del_first_row = true;
	private static boolean del_first_column = true;
	private static int records = 0;
	private static FileWriter writerN;
	private static FileWriter writerS;
	private static String first_line;
	private static ArrayList<Data> inner_list;
	private static HashMap<String, double[][]> training_data;
	private static ArrayList<HashMap<Integer, Integer>> confusion_matrix;
	private static int READING_FREQUENCY = 20;
	private static String[] words =  { "together", "you", "me", "teacher", "above"};//{ "table", "above", "below" , "thank you", "drink", "water", "i/me", "you", "friend", "teacher"};
	private static String[] wrodsDom = { "together", "you", "me", "teacher", "above"};
	private static int HOW_MANY_SETS = 5;
	private static int MAX_RECORDS = 200;
	private static boolean RIGHT_HAND_DOMINANT = true;
	private static boolean TIME_SERIES = true;
	private static Hub hub;
	private static DataCollector dataCollector;
	private static String MODE = "a"; // Automatic mode by default
	private static Instances data_train = null;
	private static Instances data_test = null;
	private static Instances data_train_Dom = null;
	private static Instances data_test_Dom = null;
	private static int input = 0;
	private static Classifier classifier = null;
	private static String classifier_name = "";
	

	public static void main(String args[]) {
		

		int lines = 0;
		String non_time_file = null;
		String time_series_file = null;
		long time = 0;
		try {
			// Create a new "Hub" which is used by the Myo API to keep track of
			// the Myo devices
			hub = new Hub("com.example.emg-data-sample");

			// hub.waitForMyo(ms) returns a Myo object if found within ms
			// milliseconds, or null otherwise
			System.out.println("Attempting to find a Myo...");
			Myo myo = hub.waitForMyo(10000);

			if (myo == null) {
				throw new RuntimeException("Unable to find a Myo!");
			}

			// setStreamEmg tells the Myo to send EMG data
			System.out.println("Connected to a Myo armband!");
			myo.setStreamEmg(StreamEmgType.STREAM_EMG_ENABLED);
			
			// create an instance of a DeviceListener to record the data, and tell
			// the hub to use it
			dataCollector = new DataCollector(RIGHT_HAND_DOMINANT, TIME_SERIES);
			
			// Add listener
			hub.addListener(dataCollector);

			// Greet
			System.out.println("Welcome to Data Trainer program - this program attempts to train a model using input from the Myo and later test it.\n");

			do {
				// User input
				greet();

				if (input == 1)
					train();
				else if (input == 2)
					test();
				else if (input == 3)
					saveload();
				
				else if(input == 4)
					chooseClassifier();
				else if(input == 5){
					saveCsv();
					
					System.out.println("Data Change to file set to true, Select train");
				}
				else{
					loadTrainData();
					System.out.println("loading Train data... ");
				}
					
			} while (true);
			
		} catch (Exception e) {
			System.err.println("Error: ");
			e.printStackTrace();
			System.exit(1);
		}
		finally {
			time = System.currentTimeMillis() - time;
			System.out.println("\nTime elapsed: " + time);
			System.out.println("bye!");
		}

		// try {
		// // Greet
		// greet();
		//
		//
		//
		// // br = new BufferedReader(new FileReader(args[0]));
		// // if (del_first_row) {
		// // first_line = br.readLine();
		// // if (del_first_column) {
		// // Scanner scan = new Scanner(first_line).useDelimiter(",");
		// // scan.next();
		// // String l = "";
		// // while (scan.hasNext()) {
		// // l += scan.next() + ",";
		// // }
		// // first_line = l.substring(0, l.length() - 1);
		// // }
		// // }
		// //
		// // time = System.currentTimeMillis();
		// // training_data = new HashMap<String, double[][]>();
		// // testing_data = new HashMap<String, double[][]>();
		// // confusion_matrix = new ArrayList<HashMap<Integer, Integer>>();
		// // int l = 0;
		// //
		// // System.out.println("Training set: ");
		// // do {
		// // inner_list = parse(br);
		// // double[][] data = Data.combineLists(inner_list);
		// //// data = Data.averageRecords(data);
		// // System.out.println("\t" + inner_list.get(0).getClassname());
		// // training_data.put(inner_list.get(0).getClassname(), data);
		// // } while (++l < num_of_words);
		// //
		// //// Data.print(training_data.get("Time"));
		// //
		// // System.out.println("Testing set:");
		// // names = new String[l];
		// // l = 0;
		// // do {
		// // inner_list = parse(br);
		// // double[][] data = Data.combineLists(inner_list);
		// //// data = Data.averageRecords(data);
		// // System.out.print("\t" + inner_list.get(0).getClassname());
		// // testing_data.put(inner_list.get(0).getClassname(), data);
		// // names[l] = inner_list.get(0).getClassname();
		// //// Data.print(testing_data.get("Time"));
		// // System.out.println(": "+test(data));
		// // } while (++l < num_of_words);
		// //
		// // System.out.println("\nConfusion Matrix:");
		// // for (String s : testing_data.keySet()) {
		// // System.out.print("\t"+s);
		// // }
		// // System.out.println();
		// // int _i = 0;
		// // for (HashMap<Integer, Integer> scores : confusion_matrix) {
		// // System.out.print(names[_i]);
		// // for (Integer i : scores.keySet()) {
		// // System.out.print("\t"+scores.get(i));
		// // }
		// // _i++;
		// // System.out.println();
		// // }
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// } finally {
		// // br.close();
		// time = System.currentTimeMillis() - time;
		// System.out.println("\nTime elapsed: " + time);
		// }
	}

	/*
	 * HELPER METHODS::-
	 */

	// Record data (automatic stop) (Copied from EmgDataSampleMotion.java)
	// myo's event loop runs here and records the data
	private static void record(boolean train) {
		// IF its training mode then record initial training instances with labels FOR TRAINING
		if (train) {
			for (int j = 0; j < words.length; j++) {
				System.out.println("Please make hand gesture for **" + words[j]
						+ "** now.");
				countdown();
				dataCollector.insertName(words[j], "N/A");

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

					// Activate the recording here when detecting initial
					// motion
					if (activated == false) {
						distance = (Data.calcEuclideanDistance(data,
								dataCollector.getData()));
						if (distance > 3) {
							timestamp = dataCollector.getTimeStamp();
							activated = true;
						}
						data = dataCollector.getData();
					}

					// Deactivate the recording here by updating the data
					// every 1/2 seconds and detecting if motion has been
					// made or no
					else {
						dataCollector.saveDataToInstance(words[j], train);
						track++;

						elapsed_time += dataCollector.getTimeStamp()
								- timestamp;
						timestamp = dataCollector.getTimeStamp();

						// Update the data every 1/2 second
						if (elapsed_time > 200000) {
							elapsed_time = 0;
							distance = (Data.calcEuclideanDistance(data,dataCollector.getData()));
							// System.out.println(distance);
							if (distance < 0.5) {
								// System.out.println(track);
								track = 0;
								motion = false;
							}
							data = dataCollector.getData();
						}
						System.out.println(Arrays.toString(data));
					}
				}
				
				
				

			}
		}

		// Otherwise just record to Instances without applying labels for TESTING
		else {
			countdown();
//			// Automatic mode+
			System.out.println("Reading Calibrating Data.......");
			dataCollector.startCalibration(hub, READING_FREQUENCY); // let the user start at idle position, record data and reset data to zero.
			System.out.println(("Calibration Data Reading Completed!"));
			int columns = 0;
			while (columns++ < 300) {
				// run the event loop for READING_FREQUENCY milliseconds and
				// record all values within this time
				hub.run(READING_FREQUENCY);
				dataCollector.saveDataToInstance(null, train);
				//dataCollector.saveDataToInstanceDom(null, train);
			}
			
		}
//			boolean motion = true;
//			boolean activated = false;
//			double distance = 0.0;
//			long timestamp = -1;
//			long elapsed_time = 0;
//			int track = 0;
//			double data[] = null;
//			while (motion) {
//				hub.run(READING_FREQUENCY);
//				// Activate the recording here when detecting initial motion
//				if (activated == false) {
//					distance = (Data.calcEuclideanDistance(data,dataCollector.getData()));
//					if (distance > 3) {
//						timestamp = dataCollector.getTimeStamp();
//						activated = true;
//					}
//					data = dataCollector.getData();
//				}
//
//				// Deactivate the recording here by updating the data
//				// every 1/2 seconds and detecting if motion has been
//				// made or no
//				else {
//					dataCollector.saveDataToInstance(null, train);
////					System.out.println(dataCollector.getData());
//					track++;
//
//					elapsed_time += dataCollector.getTimeStamp() - timestamp;
//					timestamp = dataCollector.getTimeStamp();
//
//					// Update the data every 1/2 second
//					if (elapsed_time > 200000) {
//						elapsed_time = 0;
//						distance = (Data.calcEuclideanDistance(data,dataCollector.getData()));
//						// System.out.println(distance);
//						if (distance < 0.5) {
//							// System.out.println(track);
//							track = 0;
//							motion = false;
//						}
//						data = dataCollector.getData();
//					}
//				}
//			}
		}

//	}
	

	private static void train() throws Exception {
		
		// Populate Training Instances Data
		dataCollector.createInstances(words, true);

		// Notify user
		System.out.println("Words that you will be recording: "
				+ Arrays.toString(words)
				+ ". Press enter to start recording...");
		Scanner in = new Scanner(System.in);
		in.nextLine();

		// Record initially
		record(true);

		// Notify user
		System.out.println("\nPlease re-record data for accuracy. Press enter to start recording...");
		in.nextLine();

		// Rerecord for more data accuracy
		record(true);

		// Get recorded instances data
		data_train = dataCollector.getInstances_data(true);
		
		// Notify
		System.out.println("Data recorded. Please choose a classifier..\n");

		// Display the recorded instances
		// System.out.println(data.toString());

//		// Train Model
//		System.out.println("\nTraining Model...");
//		classifier = new J48();
//		classifier.buildClassifier(data_train);
//		System.out.println("Finished training...");

		// Evaluate Model
		// Evaluation eval = new Evaluation(data);
		// eval.crossValidateModel(j48, data, 10, new Random(1));
		// System.out.println(eval.toSummaryString("\nResults\n=======", true));
	}

	private static void test() throws Exception {
		if (classifier == null) {
			System.out.println("You must provide a classifier first...\n");
			return;
		}

		// Populate Test Instances Data
		dataCollector.createInstances(words, false);
		

		// Notify user
//		System.out.println("Press enter to start recording...");
//		Scanner in = new Scanner(System.in);
//		in.nextLine();

		// Record initially
		record(false);

		// Get recorded instances data
		data_test = dataCollector.getInstances_data(false);
		
		//System.out.println(data_test.toString());

		// Display the recorded instances
		// System.out.println(data_test.toString());
		

		// Evaluate Model
		 Evaluation eval = new Evaluation(data_test);
	
		 double[] predictions = eval.evaluateModel(classifier, data_test);
		 
		 int[] scores = new int[words.length];
		 
		 int winner = 0, max = -1;
	
		 for (double d : predictions) {
			 scores[(int)d]++;
		 }
		 System.out.println("Scores: ");
		 for (int i = 0; i< words.length; i++) {
			 if (scores[i] > max) {
				 max = scores[i];
				 winner = i;
			 }
			 System.out.println(words[i] +": " + scores[i]);
		 }
//		 System.out.println(Arrays.toString(predictions));
//		 System.out.println("Predicted word: " + words[winner]);
		 System.out.println();
		 
//		 System.out.println("\nEnter to display summary stats...");
//		 in.nextLine();
//		 System.out.println(eval.toSummaryString());
//		 Evaluation evalDom = new Evaluation(data_test_Dom);
//		 double[] predictionsDom = evalDom.evaluateModel(classifier, data_test_Dom);
//		 
//		 int[] scoresDom = new int[words.length];
//		 winner = 0; 
//		 max = -1;
//		 for (double d : predictionsDom) {
//			 scoresDom[(int)d]++;
//		 }
//		 System.out.println("Scores: ");
//		 for (int i = 0; i< words.length; i++) {
//			 if (scoresDom[i] > max) {
//				 max = scoresDom[i];
//				 winner = i;
//			 }
//			 System.out.println(words[i] +": " + scoresDom[i]);
//		 }
//		 
		 
		 
		 
		 
		 
		 
		 
		 
		 
		 
		 
		 
		 
		 
	}
	
	private static void saveload() throws Exception {
		Scanner in = new Scanner(System.in);
		System.out.print("(1) Save Model (2) Load Model: ");
		int inp = Integer.parseInt(in.nextLine());
		while (true) {
			if (inp == 1 || inp == 2)
				break;
			System.out.print("(1) Save Model (2) Load Model: ");
			inp = Integer.parseInt(in.nextLine());
		}
		
		// Save
		if (inp == 1) {
			if (classifier == null) {
				System.out.println("You must provide a classifier first...\n");
				return;
			}
//			String modelname = System.currentTimeMillis() + "_"+ classifier_name +"_" + Arrays.toString(words)+".model";
			String modelname = "xin.model";
			ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(modelname));
			oos.writeObject(classifier);
			oos.flush();
			oos.close();
			
			System.out.println("Model saved to " + System.getProperty("user.dir")+"\\"+modelname+"\n");
		}
		// Load
		else {
			boolean loaded = true;
			System.out.println("Working directory: " + System.getProperty("user.dir"));
			System.out.print("Model file name: ");
			String sinp = in.nextLine();
			try {
				classifier = (Classifier) weka.core.SerializationHelper.read(sinp);
			} catch (FileNotFoundException fe) {
				System.out.println(sinp + " not found...\n");
				loaded = false;
			}
			
			if (loaded) {
				System.out.println("Model loaded...\n");
			}
		}
	}
	private static void saveCsv(){
			CSVSaver saver = new CSVSaver();
			saver.setInstances(data_train);
			try{
			saver.setFile(new File("Test.csv"));
			
			saver.writeBatch();
			
			}
			catch(IOException e){
				e.printStackTrace();
			}
			
			
			
		
		
	}
	
	private static void chooseClassifier() throws Exception {
		if (data_train == null) {
			System.out.println("You must record training data first before choosing a classifier...\n");
			return;
		}
		Scanner in = new Scanner(System.in);
		System.out.print("(1) J48 Decision Trees (2) MultiLayer Perceptron (3) IBk K-nearest neighbours classifier (4) Naive Bayes classifier: ");
		int inp = Integer.parseInt(in.nextLine());
		while (true) {
			if (inp == 1 || inp == 2 || inp == 3 || inp == 4)
				break;
			System.out.print("(1) J48 Decision Trees (2) MultiLayer Perceptron (3) IBk K-nearest neighbours classifier (4) Naive Bayes classifier: ");
			inp = Integer.parseInt(in.nextLine());
		}
		
		switch (inp) {
		case 1:
			System.out.println("\nTraining Model...");
			classifier = new J48();
			classifier.buildClassifier(data_train);
			classifier_name = "J48";
			System.out.println("Finished training...");
			break;
		case 2:
			System.out.println("\nTraining Model...");
			classifier = new MultilayerPerceptron();
			classifier.buildClassifier(data_train);
			classifier_name = "MultiLayer Perceptron";
			System.out.println("Finished training...");
			break;
		case 3:
			System.out.println("\nTraining Model...");
			classifier = new IBk();
			classifier.buildClassifier(data_train);
			classifier_name = "IBk";
			System.out.println("Finished training...");
			break;
		case 4:
			System.out.println("\nTraining Model...");
			classifier = new NaiveBayes();
			classifier.buildClassifier(data_train);
			classifier_name = "Naive Bayes";
			System.out.println("Finished training...");
			break;
		default:
			System.out.println("\nTraining Model...");
			classifier = new J48();
			classifier.buildClassifier(data_train);
			classifier_name = "J48";
			System.out.println("Finished training...");
			break;
		}
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
		System.out.println("Start recording!\n");
	}

	// Return the closest class in the training data to the provided test data
	private static String test(double[][] test_data) {

		// Holds score for each word
		HashMap<Integer, Integer> scores = new HashMap<Integer, Integer>();

		// Euclidean distances hashamp for <Word, Distances> for each row
		// HashMap<String, double[]> distances = new HashMap<String,
		// double[]>();
		double[][] distances = new double[training_data.size()][test_data[0].length];

		// Just class names
		String[] classnames = training_data.keySet().toArray(new String[1]);
		// names = classnames;
		// System.out.println(Arrays.toString(classnames));

		// Initialize scores for each word to be 0
		for (int i = 0; i < classnames.length; i++) {
			scores.put(i, 0);
		}

		// Convert MxN test data matrix to NxM for ease of use
		test_data = Data.rotate(test_data);

		// Calculate scores for each row and populate the distances matrix
		for (int i = 0; i < test_data.length; i++) {
			double min = Double.MAX_VALUE;
			int min_index = 0;
			int c = 0;
			for (String name : training_data.keySet()) {
				double[][] train = training_data.get(name);
				train = Data.rotate(train);
				// System.out.println("Testing " + name);
				// System.out.println("Calculating euc distance between: " +
				// Arrays.toString(test_data[i]));
				// System.out.println("                             and: " +
				// Arrays.toString(train[i]));
				distances[c][i] = Data.calcEuclideanDistance(test_data[i],
						train[i]);
				// System.out.println("Distance: " + distances[c][i]);
				// new Scanner(System.in).nextLine();
				if (min > distances[c][i]) {
					min = distances[c][i];
					min_index = c;
				}
				c++;
			}
			// System.out.print("\n"+classnames[min_index] + " won with " +
			// min);
			scores.put(min_index, scores.get(min_index) + 1);
		}

		// for (int i =0; i < distances.length; i++) {
		// for (int j = 0; j < distances[0].length; j++) {
		// System.out.print(distances[i][j] + " ");
		// }
		// System.out.print("\n");
		// }

		// Word with the highest score wins
		double max = Double.MIN_VALUE;
		int index = 0;
		for (Integer i : scores.keySet()) {
			// System.out.print(scores.get(i) + ",");
			if (max < scores.get(i)) {
				max = scores.get(i);
				index = i;
			}
		}
		confusion_matrix.add(scores);
		return classnames[index];
	}

	private static ArrayList<Data> parse(BufferedReader br) throws IOException {
		Scanner in;
		ArrayList<Data> list = new ArrayList<Data>();
		for (int i = 0; i < records; i++) {
			String line = br.readLine();
			if (line != null) {
				in = new Scanner(line).useDelimiter(",");
				// Skip first column if needed
				if (del_first_column)
					in.next();
				// If its the first line, then populate the data instances since
				// we
				// don't know the length of a record (or how many features each
				// record has)
				if (i == 0) {
					while (in.hasNextDouble()) {
						Data data = new Data();
						data.insert(in.nextDouble());
						list.add(data);
					}
				} else {
					for (Data d : list) {
						d.insert(in.nextDouble());
					}
					String classname = in.next();
					for (Data d : list) {
						d.setClassname(classname);
					}
				}
			} else
				break;
		}
		return list;
	}
	
	private static void greet() {
		Scanner in = new Scanner(System.in);
		System.out.print("(1) Train (2) Test (3) Save/Load Model (4) Choose Classifier (5) Save training to CSV (6) to Save CSV file into Instance: ");
		int inp = in.nextInt();
		while (true) {
			if (inp == 1 || inp == 2 || inp == 3 || inp == 4 || inp == 5 || inp == 6)
				break;
			else {
				System.out.print("(1) Train (2) Test (3) Save/Load Model (4) Choose Classifier (5) Save training to CSV (6) to Save CSV file into Instance: ");
				inp = in.nextInt();
			}
		}
		input = inp;
	}
	public static void loadTrainData() throws FileNotFoundException, IOException{
		Scanner fileName = new Scanner(System.in);
		System.out.println(("file name: "));
		String name = fileName.nextLine();
		data_train = new Instances(new FileReader(name));
		data_train.setClassIndex(data_train.numAttributes() - 1);
		//System.out.println(data_train.toString());
		
	}
}

class C {
	public C(String classname) {
		this.classname = classname;

	}

	private String classname;
	private HashMap<String, ArrayList<Double>> features;
}