package com.thalmic.myo.example;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.FirmwareVersion;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;

public class EmgDataCollector extends AbstractDeviceListener {
	//private final List<Myo> myos = new ArrayList<Myo>();
	private Myo myo_1, myo_2;
	private byte[] emgSamples;
	public Vector3 accel_1, accel_2, gyro_1, gyro_2;

	public EmgDataCollector() {
		accel_1 = new Vector3();
		accel_2 = new Vector3();
		gyro_1 = new Vector3();
		gyro_2 = new Vector3();
	}

	@Override
	public void onPair(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
		if (emgSamples != null) {
			for (int i = 0; i < emgSamples.length; i++) {
				emgSamples[i] = 0;
			}
		}
	}
	
	@Override
	public void onConnect(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
		System.out.println(myo + " :(");
		if (myo_1 == null) {
			myo_1 = myo;
		} else if (myo_2 == null) {
			myo_2 = myo;
		} else {
			System.out.println("Max of 2 Myos only supported.");
		}
	}
	
	@Override
	public void onDisconnect(Myo myo, long timestamp) {
		if (myo == myo_1) {
			myo_1 = myo_2;
			myo_2 = null;
		} else if (myo == myo_2) {
			myo_2 = null;
		}
	}

	@Override
	public void onEmgData(Myo myo, long timestamp, byte[] emg) {
		this.emgSamples = emg;
	}

	/*@Override
	public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
		Quaternion normalized = rotation.normalized();

		double roll = Math.atan2(2.0f * (normalized.getW() * normalized.getX() + normalized.getY() * normalized.getZ()), 1.0f - 2.0f * (normalized.getX() * normalized.getX() + normalized.getY() * normalized.getY()));
		double pitch = Math.asin(2.0f * (normalized.getW() * normalized.getY() - normalized.getZ() * normalized.getX()));
		double yaw = Math.atan2(2.0f * (normalized.getW() * normalized.getZ() + normalized.getX() * normalized.getY()), 1.0f - 2.0f * (normalized.getY() * normalized.getY() + normalized.getZ() * normalized.getZ()));

		double rollW = ((roll + Math.PI) / (Math.PI * 2.0) * 360);
		double pitchW = ((pitch + Math.PI / 2.0) / Math.PI * 360);
		double yawW = ((yaw + Math.PI) / (Math.PI * 2.0) * 360);

		if (myo == myo_1) {
			rollW_1 = rollW;
			pitchW_1 = pitchW;
			yawW_1 = yawW;
		} else if (myo == myo_2) {
			rollW_2 = rollW;
			pitchW_2 = pitchW;
			yawW_2 = yawW;
		}
	}*/
	
	@Override
	public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
		if (myo == myo_1) {
			accel_1 = accel;
		} else if (myo == myo_2) {
			accel_2 = accel;
		}
	}
	
	@Override
	public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
		if (myo == myo_1) {
			gyro_1 = gyro;
		} else if (myo == myo_2) {
			gyro_2 = gyro;
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(emgSamples);
	}
	
//	@Override
	// Prepares a file for writing the data by writing the first line of headers which consist of sensor number followed by the time
//	public void prepareFile(File file, int time) {
//		FileWriter writer = null;
//		
//		try {
//			writer = new FileWriter(file, true);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		String text = "";
//		for (int i = 1; i<=time; i++) {
//			if (i == 1)
//				text = text+"S1_"+i+",S2_"+i+",S3_"+i+",S4_"+i+",S5_"+i+",S6_"+i+",S7_"+i+",S8_"+i;
//			else
//				text = text+",S1_"+i+",S2_"+i+",S3_"+i+",S4_"+i+",S5_"+i+",S6_"+i+",S7_"+i+",S8_"+i;
//		}
//		
//		try {
//			writer.write(text+"\n");
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	@Override
	// Carriage returns a file...
	public void carriageReturn(File file) {
		try {
			FileWriter writer = new FileWriter(file, true);
			writer.write("\n");
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Write each line of EMG data (motion) to a csv and return number of lines written
	@Override
	public int writeToFileMotion(File file, String name) {
		if (emgSamples == null) {
			return -1;
		}
		
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String text = "";
		for (int i = 0; i<emgSamples.length; i++) {
			if (i != 0)
				text = text+","+emgSamples[i];
			else
				text = ""+emgSamples[0];
		}
		try {
			writer.write(text+",");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return numberOfColumns(file);
	}
	
	// Write each line of EMG data (letter) to a csv and return number of lines written
	@Override
	public int writeToFileLetter(File file, int letter) {
		if (emgSamples == null) {
			return -1;
		}
		
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String text = "";
		for (int i = 0; i<emgSamples.length; i++) {
			if (i != 0)
				text = text+","+emgSamples[i];
			else
				text = ""+emgSamples[0];
		}
		text = text + "," + (char)letter;
		try {
			writer.write(text+"\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		};
		
		return numberOfLines(file);
	}
	
	// Returns number of lines in a file
	private int numberOfLines(File file) {
		BufferedReader reader = null;
		int lines = 0;
		try {
			reader = new BufferedReader(new FileReader(file));
			while (reader.readLine() != null) lines++;
			reader.close();
		} catch (Exception e) {}
		return lines;
	}
	
	// Returns number of columns in a file
	private int numberOfColumns(File file) {
		BufferedReader reader = null;
		int cols = 0;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				cols = line.length() - line.replace(",", "").length();
				line = reader.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return cols;
	}
	
}