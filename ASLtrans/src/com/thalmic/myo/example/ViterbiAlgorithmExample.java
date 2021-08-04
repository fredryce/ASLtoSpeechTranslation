package com.thalmic.myo.example;

import java.util.HashMap;

// Attempt at viterbi algorithm implementation for HMM based on https://en.wikipedia.org/wiki/Viterbi_algorithm#Example
public class ViterbiAlgorithmExample {
	private HashMap<State, Double> start_probabilities;
	private HashMap<State, HashMap<State, Double>> state_transitions; // State transition probabilities
	private HashMap<State, HashMap<Observation, Double>> observation_probability; // Observation probability of noticing an observation within a state
	
	private State[] viterbi() {
		State[] state_array = start_probabilities.keySet().toArray(new State[0]);
		Observation[] obs_array = observation_probability.get(state_array[0]).keySet().toArray(new Observation[0]);
		Double[] start_probabilities_array = start_probabilities.values().toArray(new Double[0]);
		Double[][] transition_probabilities_array = new Double[state_array.length][];
		Double[][] observation_probabilities_array = new Double[state_array.length][];
		double T1[][] = new double[state_array.length][obs_array.length]; // Delta
		int T2[][] = new int[state_array.length][obs_array.length]; // Psi
		int Z[] = new int[obs_array.length];
		State X[] = new State[obs_array.length];
		int temp = 0;
		for (State state : state_transitions.keySet()) {
			transition_probabilities_array[temp] = state_transitions.get(state).values().toArray(new Double[0]);
			temp++;
		}
		temp = 0;
		for (State state : observation_probability.keySet()) {
			observation_probabilities_array[temp] = observation_probability.get(state).values().toArray(new Double[0]);
			temp++;
		}
		
//		for (int i = 0 ; i < state_array.length; i++) {
//			System.out.println("For state: " + state_array[i] + " (" + start_probabilities_array[i] + ")");
//			for (int j = 0; j < state_array.length; j++) {
//				System.out.println("\tTo " + state_array[j] + ": " + transition_probabilities_array[i][j]);
//			}
//			for (int j = 0; j < obs_array.length; j++) {
//				System.out.println("\tObservation " + obs_array[j] + ": " + observation_probabilities_array[i][j]);
//			}			
//		}
//		
		int index = 0;
		for (State state : start_probabilities.keySet()) {
			T1[index][0] = start_probabilities.get(state) * observation_probability.get(state).values().iterator().next();
			T2[index][0] = 0;
			index++;
		}
		for (index = 1; index < obs_array.length; index++) {
			for (int si = 0; si < state_array.length; si++) {
				double local_max = 0;
				// Find max
				for (int k = 0; k < index; k++) {
					// TODO:
					double t = T1[k][index-1] * transition_probabilities_array[k][si] * observation_probabilities_array[si][index];
					if (t > local_max) {
						T1[si][index] = t;
						T2[si][index] = k;
					}
				}
			}
		}
		double max = -1;
		for (int k = 0; k < T1.length; k++) {
			if (T1[k][obs_array.length-1] > max) {
				max = T1[k][obs_array.length-1];
				Z[Z.length-1] = k;
				X[Z.length-1] = state_array[Z[Z.length-1]];
			}
		}
		for (int i = Z.length-1; i >= 1; i--) {
			Z[i-1] = T2[Z[i]][i];
			X[i-1] = state_array[Z[i-1]];
		}
		return X;
	}
	
	public ViterbiAlgorithmExample() {
		start_probabilities = new HashMap<State, Double>();
		start_probabilities.put(State.HEALTHY, 0.6);
		start_probabilities.put(State.FEVER, 0.4);
		state_transitions = new HashMap<State, HashMap<State, Double>>();
		HashMap<State, Double> state_prob = new HashMap<State, Double>();
		state_prob.put(State.HEALTHY, 0.7);
		state_prob.put(State.FEVER, 0.3);
		state_transitions.put(State.HEALTHY, state_prob);
		state_prob = new HashMap<State, Double>();
		state_prob.put(State.HEALTHY, 0.4);
		state_prob.put(State.FEVER, 0.6);
		state_transitions.put(State.FEVER, state_prob);
		observation_probability = new HashMap<State, HashMap<Observation, Double>>();
		HashMap<Observation, Double> observation_prob = new HashMap<Observation, Double>();
		observation_prob.put(Observation.NORMAL, 0.5);
		observation_prob.put(Observation.COLD, 0.4);
		observation_prob.put(Observation.DIZZY, 0.1);
		observation_probability.put(State.HEALTHY, observation_prob);
		observation_prob = new HashMap<Observation, Double>();
		observation_prob.put(Observation.NORMAL, 0.1);
		observation_prob.put(Observation.COLD, 0.3);
		observation_prob.put(Observation.DIZZY, 0.6);
		observation_probability.put(State.FEVER, observation_prob);
	}
	public static void main (String args[]) {
		ViterbiAlgorithmExample vi = new ViterbiAlgorithmExample();
		State[] ans = vi.viterbi();
		for (int i =0 ; i < ans.length; i++) {
			System.out.println(ans[i]);
		}
	}
}

enum State {
	HEALTHY, FEVER
}

enum Observation {
	NORMAL, COLD, DIZZY
}