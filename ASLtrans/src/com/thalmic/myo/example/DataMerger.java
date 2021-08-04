package com.thalmic.myo.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

// CSV File Merger
public class DataMerger {
	
	private static BufferedReader br;
	private static ArrayList<File> files;
	private static FileWriter writer;
	private static String filename;
	
	public static void main(String[] args) throws IOException {
		System.out.println("Welcome to File Merger - This program merges all .csv files in the current directory into one file with one header (taken from the first file)");
		files = getAllFiles();
		if (files.isEmpty()) {
			System.out.println("No .csv files found...");
			System.exit(1);
		} else if (files.size() < 2) {
			System.out.println("Only one .csv file. No need to merge.");
			System.exit(1);
		} else {
			System.out.println("Press enter to begin");
			new Scanner(System.in).nextLine();
			filename = checkFileName();
			try {
				writer = new FileWriter(new File(filename));
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (int i = 0; i<files.size(); i++) {
				try {
					br = new BufferedReader(new FileReader(files.get(i)));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				if (i == 0) {
					writer.write(br.readLine()+"\n");
				} else {
					br.readLine();
				}
				String line = br.readLine();
				while (line != null) {
					writer.write(line+"\n");
					line = br.readLine();
				}
				writer.flush();
			}
		}
		System.out.println("Files merged into "+ filename);
	}

	/*
	 * Helper Methods::
	 */
	
	// Get all files in the current dir and return the arraylist
	private static ArrayList<File> getAllFiles() {
		File curDir = new File(".");
		File[] filesList = curDir.listFiles();
		ArrayList<File> files = new ArrayList<File>();
		for (File f : filesList) {
			if (f.isFile()) {
				if (f.getName().substring(f.getName().length()-4, f.getName().length()).equals(".csv")) {
					files.add(f);
				}
			}
		}
		return files;
	}
	
	// Ensure that the file name doesn't exist. If it does, generate a unique one.
	public static String checkFileName() {
		int count = 0;
		String name;
		File data;
		do {
			count++;
			name = "data_merged" + count + ".csv";
			data = new File(name);
		} while (data.exists());
		return name;
	}
}