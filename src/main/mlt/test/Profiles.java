package mlt.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import mlt.instrument.Predicate;
import mlt.instrument.Predicate.TYPE;

public class Profiles {
	
	public static ArrayList<Predicate> predicates = new ArrayList<Predicate>();
	public static ArrayList<TestCase> tests = new ArrayList<TestCase>();
	
	// dynamic info
	public static PairArrayList executedPredicates = new PairArrayList();
	public static HashMap<String, HashSet<Integer>> taints = new HashMap<String, HashSet<Integer>>();
	
	private static Stack<Integer> loopIndexStack = new Stack<Integer>();
	private static Stack<Pair> loopPairStack = new Stack<Pair>();
	// for instrumentation
	public static void add(int index, boolean value) {
		Pair p = new Pair(index, value);
		
		if (index == -1) {
			int ret_i = loopIndexStack.pop();
			loopPairStack.pop();
			Predicate ret_pre_i = Profiles.predicates.get(ret_i);
			boolean mark = false;
			while (loopIndexStack.size() > 0) {
				loopPairStack.peek().addToInnerPairs(p);
				int ret_j = loopIndexStack.peek();
				Predicate ret_pre_j = Profiles.predicates.get(ret_j);
				if (ret_pre_i.getClassName().equals(ret_pre_j.getClassName()) &&
						ret_pre_i.getMethodName().equals(ret_pre_j.getMethodName()) &&
						ret_pre_i.getSignature().equals(ret_pre_j.getSignature())) {
					loopIndexStack.pop();
					loopPairStack.pop();
				} else {
					mark = true;
					break;
				}
			}
			if (!mark) {
				executedPredicates.add(p);
			}
			return; // the last iteration before the return statement could be redundant
		}
		
		TYPE type = predicates.get(index).getType();
		if (loopIndexStack.size() == 0) {
			if (type == TYPE.IF) {
				executedPredicates.add(p);
			} else if (type != TYPE.IF) {
				executedPredicates.add(p);
				loopIndexStack.push(index);
				loopPairStack.push(p);
			}
		} else {
			if (type != TYPE.IF && index != loopIndexStack.peek()) {
				loopPairStack.peek().addToInnerPairs(p);
				loopIndexStack.push(index);
				loopPairStack.push(p);
			} else if (type != TYPE.IF && index == loopIndexStack.peek()) {
				loopPairStack.pop();
				
				PairArrayList ps = loopPairStack.empty() ? executedPredicates : loopPairStack.peek().getInnerPairs();				
				int size = ps.size();
				if ((type == TYPE.FOR || type == TYPE.WHILE) && size >= 2){
					Pair p1 = ps.get(size - 1);
					Pair p2 = ps.get(size - 2);
					if (p1.getPredicateIndex() == index && p1.isPredicateValue() 
							&& p2.getPredicateIndex() == index && p2.isPredicateValue()) {
						for (int i = size - 2; i >= 0; i--) {
							Pair pt = ps.get(i);
							if (pt.getPredicateIndex() == index && pt.isPredicateValue()) {
								if (p1.equals(pt)) {
									ps.remove(size - 1);
									break;
								}
							} else {
								break;
							}
						}
					}
				} else if (type == TYPE.DO && size >= 3) {
					Pair p1 = ps.get(size - 1);
					Pair p2 = ps.get(size - 2);
					Pair p3 = ps.get(size - 3);
					if (p1.getPredicateIndex() == index && p1.isPredicateValue() 
							&& p2.getPredicateIndex() == index && p2.isPredicateValue() 
							&& p3.getPredicateIndex() == index && p3.isPredicateValue()) {
						for (int i = size - 2; i >= 0; i--) {
							Pair pt = ps.get(i);
							if (pt.getPredicateIndex() == index && pt.isPredicateValue()) {
								if (p1.equals(pt)) {
									ps.remove(size - 1);
									break;
								}
							} else {
								break;
							}
						}
					}
				}
				
				ps.add(p);
				if (value) {
					loopPairStack.push(p);
				} else {
					loopIndexStack.pop();
				}
			} else {
				loopPairStack.peek().addToInnerPairs(p);
			}
		}
	}
		
	public static void printPredicates() {
		int size = predicates.size();
		for (int i = 0; i < size; i++) {
			System.out.println("[ml-testing] " + predicates.get(i));
		}
		System.out.println();
	}
	
	public static void printTests() {
		int size = tests.size();
		System.out.println("[ml-testing] tests");
		for (int i = 0; i < size; i++) {
			System.out.println("[ml-testing] " + tests.get(i).getTest());
		}
		System.out.println();
	}
	
	public static void printExecutedPredicates() {
		int size = executedPredicates.size();
		System.out.println("[ml-testing] predicates");
		for (int i = 0; i < size; i++) {
			System.out.println(executedPredicates.get(i).getPredicateIndex() + " (" + executedPredicates.get(i).isPredicateValue() + ")");
			print(executedPredicates.get(i).getInnerPairs());
		}
	}
	
	public static void print(PairArrayList list) {
		if (list != null) {
			int size = list.size();
			for (int i = 0; i < size; i++) {
				System.out.println(list.get(i).getPredicateIndex() + " (" + list.get(i).isPredicateValue() + ")");
				print(list.get(i).getInnerPairs());
			}
		}
	}
	
	public static void printTaints() {
		Iterator<String> iterator = taints.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			System.out.println("[ml-testing] " + key + " taints are " + taints.get(key));
		}
	}
	
}