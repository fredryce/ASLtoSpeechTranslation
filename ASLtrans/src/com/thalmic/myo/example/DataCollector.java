package com.thalmic.myo.example;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.FirmwareVersion;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.enums.Arm;
import com.thalmic.myo.enums.StreamEmgType;
import com.thalmic.myo.enums.WarmupState;
import com.thalmic.myo.enums.XDirection;

public class DataCollector extends AbstractDeviceListener {
	private boolean RIGHT_HAND_DOMINANT = true;
	private boolean TIME_SERIES = true;
	private Myo myo_R, myo_L;
	private byte[] emgSamples;
	private double rollW_R, pitchW_R, yawW_R, rollW_L, pitchW_L, yawW_L;
	private long timestamp = 0;
	private Vector3 accel_R, gyro_R, accel_L, gyro_L;
	private Instances train_instances_data; // training instances data
	private Instances test_instances_data; // testing instances data
	private Instances train_instances_data_Dom; // training instances data Single hand Data
	private Instances test_instances_data_Dom; // testing instances data Single hand Data
	private Instances train_Cali; // calibrated data saved to instance
	public boolean cali;
	public static String wordtest[] = {"together", "name", "teacher", "me", "you"};

	// hashmap to track which file data String is associated with which file name String
	HashMap<String, String> fileNameAndData;

	public DataCollector(boolean hand, boolean time_series) {
		// test this stuff here
		rollW_R = pitchW_R = yawW_R = rollW_L = pitchW_L = yawW_L = 0;
		accel_R = accel_L = gyro_R = gyro_L = new Vector3();
		// test this stuff here
		fileNameAndData = new HashMap<String, String>();
		this.RIGHT_HAND_DOMINANT = hand;
		this.TIME_SERIES = time_series;
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
		if (myo_R == null) {
			myo_R = myo;
		} else if (myo_L == null) {
			myo_L = myo;
		} else {
			System.out.println("Only 2 Myo Armbands are supported at a time.");
		}
	}
	
	@Override
	public void onDisconnect(Myo myo, long timestamp) {
		if (myo == myo_R) {
			myo_R = null;
		} else if (myo == myo_L) {
			myo_L = null;
		}
	}
	
	@Override
	public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection, float rotation, WarmupState warmupState) {
		// Ensure that myo_1 refers to the armband on the right arm.
		if ((myo == myo_R && arm == Arm.ARM_LEFT) || 
			(myo == myo_L && arm == Arm.ARM_RIGHT) ) {
			// swap the two variables
			Myo temp = myo_L;
			myo_L = myo_R;
			myo_R = temp;
		}
		if (myo_L != null && myo_R != null) {
			if (RIGHT_HAND_DOMINANT) {
				myo_L.setStreamEmg(StreamEmgType.STREAM_EMG_DISABLED);
				myo_R.setStreamEmg(StreamEmgType.STREAM_EMG_ENABLED);
			} else {
				myo_R.setStreamEmg(StreamEmgType.STREAM_EMG_DISABLED);
				myo_L.setStreamEmg(StreamEmgType.STREAM_EMG_ENABLED);
			}
		}
	}

	@Override
	public void onEmgData(Myo myo, long timestamp, byte[] emg) {
		if ((myo == myo_R && RIGHT_HAND_DOMINANT) ||
			(myo == myo_L && !RIGHT_HAND_DOMINANT)) {
			this.emgSamples = emg;
		}
	}

	@Override
	public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
		Quaternion normalized = rotation.normalized();

		double roll = Math.atan2(2.0f * (normalized.getW() * normalized.getX() + normalized.getY() * normalized.getZ()), 1.0f - 2.0f * (normalized.getX() * normalized.getX() + normalized.getY() * normalized.getY()));
		double pitch = Math.asin(2.0f * (normalized.getW() * normalized.getY() - normalized.getZ() * normalized.getX()));
		double yaw = Math.atan2(2.0f * (normalized.getW() * normalized.getZ() + normalized.getX() * normalized.getY()), 1.0f - 2.0f * (normalized.getY() * normalized.getY() + normalized.getZ() * normalized.getZ()));

		double rollW = ((roll + Math.PI) / (Math.PI * 2.0) * 360);
		double pitchW = ((pitch + Math.PI / 2.0) / Math.PI * 360);
		double yawW = ((yaw + Math.PI) / (Math.PI * 2.0) * 360);

		if (myo == myo_R) {
			rollW_R = rollW;
			pitchW_R = pitchW;
			yawW_R = yawW;
		} else if (myo == myo_L) {
			rollW_L = rollW;
			pitchW_L = pitchW;
			yawW_L = yawW;
		}
		this.timestamp = timestamp;
	}
	public static void startCalibration(Hub hub, int freq){
		System.out.println("Keep both arms on the side of your body....");
		int frames = 0;
		while(frames++ < 100){
			
			hub.run(freq);
			for(int j =0; j < wordtest.length; j++ ){
				dataCollector.writeToFileMotion("test.csv", wordtest[j]);
			}
			
			
			
		}
		
	}

	@Override
	public String toString() {
		return Arrays.toString(emgSamples);
	}
	
	//@Override
	// Prepares a file for writing the data by writing the first line of headers which consist of sensor number followed by the time
	public void prepareFile(String filename, int time, boolean time_series) {
		String data = "";
		if (time_series) {
			data = "Class";
			for (int i = 1; i <= time; i++) {
					data += ",RollRight_"+i+",PitchRight_"+i+",YawRight_"+i+",RollAccelRight_"+i+",PitchAccelRight_"+i+",YawAccelRight_"+i+",GyroRollRight_"+i+",GyroPitchRight_"+i+",GyroYawRight_"+i+
							",RollLeft_"+i+",PitchLeft_"+i+",YawLeft_"+i+",RollAccelLeft_"+i+",PitchAccelLeft_"+i+",YawAccelLeft_"+i+",GyroRollLeft_"+i+",GyroPitchLeft_"+i+",GyroYawLeft_"+i+
							",S1_"+i+",S2_"+i+",S3_"+i+",S4_"+i+",S5_"+i+",S6_"+i+",S7_"+i+",S8_"+i;
			}
			data += "\r\n";
		} else {
			data = "Timestamp,RollRight,PitchRight,YawRight,RollAccelRight,PitchAccelRight,YawAccelRight,GyroRollRight,GyroPitchRight,GyroYawRight," + 
					"RollLeft,PitchLeft,YawLeft,RollAccelLeft,PitchAccelLeft,YawAccelLeft,GyroRollLeft,GyroPitchLeft,GyroYawLeft," + 
					"S1,S2,S3,S4,S5,S6,S7,S8,Class\r\n";
		}
		fileNameAndData.put(filename, data);
	}
	
	public void saveDataToFile(String filename) {
		String data = (String) fileNameAndData.get(filename);
		if (data != "") {
			File file = new File(filename);
			FileWriter writer = null;
			try {
				writer = new FileWriter(file, true);
				writer.write(data);
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
		if (myo == myo_R) {
			accel_R = accel;
		} else if (myo == myo_L) {
			accel_L = accel;
		}
	}
	
	@Override
	public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
		if (myo == myo_R) {
			gyro_R = gyro;
		} else if (myo == myo_L) {
			gyro_L = gyro;
		}
	}
	
	//@Override
	// Carriage returns a file...
	public void carriageReturn(String filename) {
		String data = (String) fileNameAndData.get(filename);
		fileNameAndData.put(filename, data + "\r\n");
	}
	
	// Insert the name of the word in the first column for time series data
	//@Override
	public void insertName(String name, String filename) {
		String oldValue = (String) fileNameAndData.get(filename);
		fileNameAndData.put(filename, oldValue + name + ",");
	}
	
	//@Override
	// Write each line of EMG data (motion) to a csv and return number of lines written in non-time series format
	public int writeToFileMotion(String filename, String name) {
		if (emgSamples == null) {
			return -1;
		}
		
		String data = timestamp + "," +
				rollW_R + "," + pitchW_R + "," + yawW_R + "," +
				accel_R.getX() + "," + accel_R.getY() + "," + accel_R.getZ() + "," +
				gyro_R.getX() + "," + gyro_R.getY() + "," + gyro_R.getZ() + "," +
				rollW_L + "," + pitchW_L + "," + yawW_L + "," +
				accel_L.getX() + "," + accel_L.getY() + "," + accel_L.getZ() + "," +
				gyro_L.getX() + "," + gyro_L.getY() + "," + gyro_L.getZ();
		for (int i = 0; i < emgSamples.length; i++) {
			data += "," + emgSamples[i];
		}
		data += "," + name + "\r\n";
		
		String oldValue = (String) fileNameAndData.get(filename);
		fileNameAndData.put(filename, oldValue + data);
		
		return numberOfLines(filename);
	}
	
	// Create initial Weka instances
//	public void createInstances(String[] class_names, boolean train) {
//		String header = "RollRight,PitchRight,YawRight,RollLeft,PitchLeft,YawLeft";
//				/*"RollRight,PitchRight,YawRight,RollAccelRight,PitchAccelRight,YawAccelRight,GyroRollRight,GyroPitchRight,GyroYawRight," + 
//				"RollLeft,PitchLeft,YawLeft,RollAccelLeft,PitchAccelLeft,YawAccelLeft,GyroRollLeft,GyroPitchLeft,GyroYawLeft," + 
//				"S1,S2,S3,S4,S5,S6,S7,S8";
//				*/
//		
//		Scanner scan = new Scanner(header).useDelimiter(",");
//		ArrayList<Attribute> alist = new ArrayList<Attribute>();
//		while (scan.hasNext()) {
//			Attribute att = new Attribute(scan.next());
//			alist.add(att);
//		}
//		List<String> ls = new ArrayList<String>();
//		for (String cs : class_names) {
//			ls.add(cs);
//		}
//		Attribute att = new Attribute("Class", ls);
//		alist.add(att);
//		
//		if (train) {
//			train_instances_data = new Instances("Train", alist, 0);
//			train_instances_data.setClass(att);
//		} else {
//			test_instances_data = new Instances("Test", alist, 0);
//			test_instances_data.setClass(att);
//		}
//	}
	public void createInstances(String[] class_names, boolean train) {
		String header =
				"RollRight,PitchRight,YawRight,RollAccelRight,PitchAccelRight,YawAccelRight,GyroRollRight,GyroPitchRight,GyroYawRight," + 
				"RollLeft,PitchLeft,YawLeft,RollAccelLeft,PitchAccelLeft,YawAccelLeft,GyroRollLeft,GyroPitchLeft,GyroYawLeft," + 
				"S1,S2,S3,S4,S5,S6,S7,S8";
		
				
		
		Scanner scan = new Scanner(header).useDelimiter(",");
		
		ArrayList<Attribute> alist = new ArrayList<Attribute>();
		
		while (scan.hasNext()) {
			Attribute att = new Attribute(scan.next());
			alist.add(att);
		}
		
		
		List<String> ls = new ArrayList<String>();
	
		for (String cs : class_names) {
			ls.add(cs);
			
		}
		
		
		Attribute att = new Attribute("Class", ls);
		alist.add(att);
	
		
		if (train) {
			train_instances_data = new Instances("Train", alist, 0);
			train_instances_data.setClass(att);
			
		} else {
			test_instances_data = new Instances("Test", alist, 0);
			test_instances_data.setClass(att);
		
		}
	}
	
	// Save a singular data to an instances object
	public boolean saveDataToInstance(String class_name, boolean train) {
		if (emgSamples == null) {
			return false;
		}
		
		DenseInstance di = new DenseInstance(27);
		di.setDataset(train_instances_data);
		
		if (train){
			di.setClassValue(class_name);
		}
			
		int index = 0;
		di.setValue(index++, rollW_R);
		di.setValue(index++, pitchW_R);
		di.setValue(index++, yawW_R);
		di.setValue(index++, accel_R.getX());
		di.setValue(index++, accel_R.getY());
		di.setValue(index++, accel_R.getZ());
		di.setValue(index++, gyro_R.getX());
		di.setValue(index++, gyro_R.getY());
		di.setValue(index++, gyro_R.getZ());
		di.setValue(index++, rollW_L);
		di.setValue(index++, pitchW_L);
		di.setValue(index++, yawW_L);	
		di.setValue(index++, accel_L.getX());
		di.setValue(index++, accel_L.getY());
		di.setValue(index++, accel_L.getZ());
		di.setValue(index++, gyro_L.getX());
		di.setValue(index++, gyro_R.getY());
		di.setValue(index++, gyro_R.getZ());
		
		for (int i = 0; i < emgSamples.length; i++) {
			di.setValue(index++, emgSamples[i]);
		}
		
		if (train){
			train_instances_data.add(di);
			}
		else{
			test_instances_data.add(di);
			
		}
			
		return true;
	}
	public boolean saveDataToInstanceDom(String class_name, boolean train) {
		if (emgSamples == null) {
			return false;
		}
		
		
		DenseInstance diDom = new DenseInstance(4);
		
		diDom.setDataset(train_instances_data_Dom); 
		
		if (train){
			
			diDom.setClassValue(class_name);	
		}
		
		int index = 0;
		diDom.setValue(index++, rollW_R);
		diDom.setValue(index++, pitchW_R);
		diDom.setValue(index++, yawW_R);

		
		if (train){
			
			train_instances_data_Dom.add(diDom);}
		else{
			test_instances_data_Dom.add(diDom);
		}
			
		return true;
	}
	
	// Return Instances data
	public Instances getInstances_data(boolean train) {
		return train ? train_instances_data : test_instances_data;
	}
	

	// Get the IMU data in a double array format for euc distance calculations (used mainly for detecting whitespace)
	public double[] getData() {
		if (emgSamples == null) {
			return null;
		}
		double data[] = new double[6];
		data[0] = rollW_R;
		data[1] = pitchW_R;
		data[2] = yawW_R;
		data[3] = rollW_L;
		data[4] = pitchW_L;
		data[5] = yawW_L;
		return data;
	}
	
	// Get the timestamp
	public long getTimeStamp() {
		return this.timestamp;
	}
	
	//@Override
	// Write each line of EMG data (motion) to a csv and return number of columns written in time series format
	public int writeToFileMotionTimeSeries(String filename) {
		if (emgSamples == null) {
			return -1;
		}
		
		StringBuilder sb = new StringBuilder("");
		sb.append(rollW_R+",");
		sb.append(pitchW_R+",");
		sb.append(yawW_R+",");
		sb.append(accel_R.getX()+",");
		sb.append(accel_R.getY()+",");
		sb.append(accel_R.getZ()+",");
		sb.append(gyro_R.getX()+",");
		sb.append(gyro_R.getY()+",");
		sb.append(gyro_R.getZ()+",");
		sb.append(rollW_L+",");
		sb.append(pitchW_L+",");
		sb.append(yawW_L+",");
		sb.append(accel_L.getX()+",");
		sb.append(accel_L.getY()+",");
		sb.append(accel_L.getZ()+",");
		sb.append(gyro_L.getX()+",");
		sb.append(gyro_L.getY()+",");
		sb.append(gyro_L.getZ());
		for (int i = 0; i < emgSamples.length; i++) {
			sb.append("," + emgSamples[i]);
		}
		sb.append(",");
		
		String oldValue = (String) fileNameAndData.get(filename);
		fileNameAndData.put(filename, oldValue + sb.toString());
		
		return numberOfColumns(filename);
	}
	
	// Returns number of lines in a file
	private int numberOfLines(String filename) {
		String data = (String) fileNameAndData.get(filename);
		return data.length() - data.replaceAll("\r\n", "").length();
	}
	
	// Returns number of columns in a file
	private int numberOfColumns(String filename) {
		String data = (String) fileNameAndData.get(filename);
		// get the last line
		data = data.substring(data.lastIndexOf("\r\n"));
		return data.length() - data.replaceAll(",", "").length();
	}
	
}