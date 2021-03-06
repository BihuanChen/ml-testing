package melt.test.generation.search2;

import java.util.HashMap;
import java.util.HashSet;

import melt.Config;
import jmetal.core.Solution;
import jmetal.operators.mutation.Mutation;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;

public class GuidedMutation extends Mutation {

	private static final long serialVersionUID = -8186478343375466110L;

	private Double mutationProbability_h = null;
	private Double mutationProbability_l = null;
	
	public GuidedMutation(HashMap<String, Object> parameters) {
		super(parameters);
		
		if (parameters.get("probability_h") != null) {
	  		mutationProbability_h = (Double) parameters.get("probability_h");
		}
		if (parameters.get("probability_l") != null) {
	  		mutationProbability_l = (Double) parameters.get("probability_l");
		}
	}

	@Override
	public Object execute(Object object) throws JMException {
		//System.err.println("mutation");
		Solution solution = (Solution)object;		
		
		//printSolution(solution);
		TestVar tv = (TestVar)solution.getDecisionVariables()[0];
		// mutate with low probability if it satisfies the prefix path
		if (tv.getObjValue() != -1 && tv.getViolations() == null) {
			Object[] test = tv.getTest().getTest();
			for (int j = 0; j < test.length; j++) {
				if (PseudoRandom.randDouble() < mutationProbability_l) {
					doMutation(test, j);
				}
			}
		} else {
			Object[] test = tv.getTest().getTest();
			HashSet<Integer> depInputs = tv.computeDepInputs();
			for (int j = 0; j < test.length; j++) {
				if (depInputs.contains(j)) {
					if (PseudoRandom.randDouble() < mutationProbability_h) {
						doMutation(test, j);
					}
				} else {
					if (PseudoRandom.randDouble() < mutationProbability_l) {
						doMutation(test, j);
					}
				}
			}
		}
		tv.clear();
		
		//printSolution(solution);
		return solution;
	}
	
	// TODO more operators?
	private void doMutation(Object[] test, int index) {
		//System.err.println("mutated");
		@SuppressWarnings("rawtypes")
		Class cls = test[index].getClass();
		if (cls == Byte.class) {
			test[index] = (byte)PseudoRandom.randInt(Config.MIN_BYTE, Config.MAX_BYTE);
		} else if (cls == Short.class) {
			test[index] = (short)PseudoRandom.randInt(Config.MIN_SHORT, Config.MAX_SHORT);
		} else if (cls == Character.class) {
			test[index] = (char)PseudoRandom.randInt(Config.MIN_CHAR, Config.MAX_CHAR);
		} else if (cls == Integer.class) {
			Integer varMin = Config.varMinIntMap.get(Config.PARAMETERS[index]);
			Integer varMax = Config.varMaxIntMap.get(Config.PARAMETERS[index]);
			if (varMin != null && varMax != null) {
				test[index] = PseudoRandom.randInt(varMin, varMax);
			} else {
				test[index] = PseudoRandom.randInt(Config.MIN_INT, Config.MAX_INT);
			}
		} else if (cls == Long.class) {
			test[index] = (long)PseudoRandom.randDouble(Config.MIN_LONG, Config.MAX_LONG);
		} else if (cls == Float.class) {
			test[index] = (float)PseudoRandom.randDouble(Config.MIN_FLOAT, Config.MAX_FLOAT);
		} else if (cls == Double.class) {
			test[index] = PseudoRandom.randDouble(Config.MIN_DOUBLE, Config.MAX_DOUBLE);
		} else if (cls == Boolean.class) {
			test[index] = PseudoRandom.randInt(0, 1) == 0 ? false : true;
		}
	}
	
	public void printSolution(Solution sol) {
		for (int i = 0; i < sol.numberOfVariables(); i++) {
			System.err.println(((TestVar)sol.getDecisionVariables()[i]).getTest());
		}
		System.err.println();
	}

}
