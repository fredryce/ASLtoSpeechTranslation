package com.thalmic.myo.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/*
 * POJO that holds the normalized and standardized values for each feature and
 * provides supporting getters and setters.
 */
class Data {

	public Data() {
		this.max = Double.MIN_VALUE;
		this.min = Double.MAX_VALUE;
		this.sum = 0;
		this.count = 0;
		this.mean = 0;
		this.sd = Double.MIN_VALUE;
		this.list = new ArrayList<Double>();
		this.normalized_values = null;
		this.standardized_values = null;
		this.values = null;
		this.classname = "";
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public double getMean() {
		return mean;
	}

	public double getSd() {
		if (this.sd == Double.MIN_VALUE)
			return calcSd();
		else
			return sd;
	}

	public int getCount() {
		return this.count;
	}

	public String getClassname() {
		return this.classname;
	}

	public void setClassname(String classname) {
		this.classname = classname;
	}

	public double[] getList() {
		if (this.values == null) {
			double[] arr = new double[this.count];
			int c = 0;
			for (Double d : this.list) {
				arr[c] = d;
				c++;
			}
			this.values = arr;
			return arr;
		} else
			return this.values;
	}

	public double[] getNormalizedValues() {
		if (this.normalized_values == null) {
			double[] arr = new double[this.count];
			int c = 0;
			for (Double d : this.list) {
				arr[c] = (d - this.min) / (this.max - this.min);
				c++;
			}
			this.normalized_values = arr;
			return arr;
		} else
			return this.normalized_values;
	}

	public double[] getStandardizedValues() {
		if (this.standardized_values == null) {
			double[] arr = new double[this.count];
			double sd = getSd();
			int c = 0;
			for (Double d : this.list) {
				arr[c] = (d - this.mean) / (this.sd);
				c++;
			}
			this.standardized_values = arr;
			return arr;
		} else
			return this.standardized_values;
	}

	public void insert(Double n) {
		list.add(n);
		count++;
		sum += n;
		if (n > this.max)
			this.max = n;
		if (n < this.min)
			this.min = n;
		this.mean = (this.sum / this.count);
	}

	// Rotate a 2d array so that MxN converts to NxM
	public static double[][] rotate(double data[][]) {
		double[][] arr = new double[data[0].length][data.length];
		for (int i = 0; i < data[0].length; i++) {
			for (int j = 0; j < data.length; j++) {
				arr[i][j] = data[j][i];
			}
		}
		return arr;
	}
	
	// Calculate and return the euclidean distance between two n-dimensional planes
	public static double calcEuclideanDistance(double[] a, double[] b) {
		if (a == null || b == null) {
			return -1;
		}
		double dist = 0.0;
		for (int i = 0; i < a.length; i++) {
			dist += Math.pow(a[i] - b[i], 2);
		}
		return Math.sqrt(dist);
	}

	// Print a matrix in the form of recorded 2d array
	public static void print(double data[][]) {
		for (int i = 0; i < data[0].length; i++) {
			for (int j = 0; j < data.length; j++) {
				System.out.print(data[j][i] + " ");
			}
			System.out.println();
		}
	}

	// Average every x number of records into one record to reduce the size of data
	public static double[][] averageRecords(double[][] data/* , int interval */) {
		double[][] arr = new double[data.length][data[0].length / 2];
		for (int i = 0; i < arr.length; i++) {
			int c = 0;
			for (int j = 0; j < (arr[0].length); j++, c = c + 2) {
				arr[i][j] = (data[i][c] + data[i][c + 1]) / 2;
			}
		}
		return arr;
	}

	// Combine the array lists in the form of a 2d array of all values
	public static double[][] combineLists(ArrayList<Data> list) {
		if (list.isEmpty())
			return new double[1][];
		double[][] arr = new double[list.size()][list.get(0).getCount()];
		for (int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i).getList();
		}
		return arr;
	}

	// Combine the array lists in the form of a 2d array of all normalized
	// values for easier writing
	public static double[][] combineListsNormalized(ArrayList<Data> list) {
		if (list.isEmpty())
			return new double[1][];
		double[][] arr = new double[list.size()][list.get(0).getCount()];
		for (int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i).getNormalizedValues();
		}
		return arr;
	}

	// Combine the array lists in the form of a 2d array of all standardized
	// values for easier writing
	public static double[][] combineListsStandardized(ArrayList<Data> list) {
		if (list.isEmpty())
			return new double[1][];
		double[][] arr = new double[list.size()][list.get(0).getCount()];
		for (int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i).getStandardizedValues();
		}
		return arr;
	}

	private double calcSd() {
		double sum = 0;
		for (Double d : this.list) {
			double n = Math.pow((d - this.mean), 2);
			sum += n;
		}
		sum = sum / this.count;
		this.sd = Math.sqrt(sum);
		return Math.sqrt(sum);
	}

	private double max;
	private double min;
	private double mean;
	private double sd;
	private double sum;
	private int count;
	private String classname;
	private double[] normalized_values;
	private double[] standardized_values;
	private double[] values;
	private ArrayList<Double> list;
}