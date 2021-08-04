package com.thalmic.myo.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

// Data normalizer / standardizer
public class EmgDataNormalizer {
	private static boolean del_first_row = true;
	private static boolean del_first_column = true;
	private static int records = 0;
	private static BufferedReader br;
	private static FileWriter writerN;
	private static FileWriter writerS;
	private static ArrayList<Data> inner_list;
	private static String first_line;

	public static void main(String[] args) {
		if (args.length <1) {
			System.out.println("Restart the program providing the file name like: java data-normalizer filename.csv ");
			System.exit(1);
		}
		try {
			BufferedReader br1 = new BufferedReader(new FileReader(args[0]));
		} catch (FileNotFoundException e) {
			System.out.println(args[0] + " not found");
			System.exit(1);
		}
		
		// Greet
		greet();
		
		// Skip first line from processing and initialize reader
		try {
			br = new BufferedReader(new FileReader(args[0]));
			if (del_first_row)
				first_line = br.readLine();
			if (del_first_column) {
				Scanner scan = new Scanner(first_line).useDelimiter(",");
				scan.next();
				String l="";
				while (scan.hasNext()) {
					l += scan.next()+",";
				}
				first_line = l.substring(0, l.length()-1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String normalizedName = args[0].substring(0, args[0].length() - 4) + "_normalized.csv";
		String standardizedName = args[0].substring(0, args[0].length() - 4) + "_standardized.csv";
		
		// Initialize writers and write the first line
		try {
			writerN = new FileWriter(new File(normalizedName));
			writerS = new FileWriter(new File(standardizedName));
			if (del_first_row) {
				writerN.write(first_line + "\n");
				writerS.write(first_line + "\n");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		do {
			// Parse
			try {
				inner_list  = parse(br);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Write (normalized and standardized)
			if (!inner_list.isEmpty()) {
				try {
					WriteToFile(inner_list, writerN, writerS);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} while (!inner_list.isEmpty());
		
		// Close writers and reader
		try {
			writerN.flush();
			writerS.flush();
			writerN.close();
			writerS.close();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * HELPER METHDOS::-
	 */

	private static void greet() {
		System.out.println("Welcome to Data normalization/standardization program - this program takes as input a file name and creates two new files with the numeric values normalized and standardized.");
		String input = "";
		Scanner in = new Scanner(System.in);
		System.out.print("Is the first row the header? (y/n): ");
		input = in.nextLine();
		if (input.equals("n"))
			del_first_row = false;

		System.out.print("Is the first column the timestamp (will not be written to the new files)? (y/n): ");
		input = in.nextLine();
		if (input.equals("n"))
			del_first_column = false;

		System.out.print("Records per word/class? eg. 200: ");
		records = in.nextInt();
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
				// If its the first line, then populate the data instances since we
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

	private static void WriteToFile(ArrayList<Data> list, FileWriter writerN, FileWriter writerS)
			throws IOException {
		double[][] normalized = Data.combineListsNormalized(list);
		double[][] standardized = Data.combineListsStandardized(list);
		for (int i = 0; i < normalized[0].length; i++) {
			String line = "";
			String line2 = "";
			for (int j = 0; j < normalized.length; j++) {
				line += (normalized[j][i] + ",");
				line2 += (standardized[j][i] + ",");
			}
			line += list.get(normalized.length - 1).getClassname() + "\n";
			line2 += list.get(normalized.length - 1).getClassname() + "\n";
			writerN.write(line);
			writerS.write(line2);
		}
	}
}