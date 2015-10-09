package mlt.learn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import mlt.Config;
import mlt.test.Pair;
import mlt.test.Profiles;

public class ProfileAnalyzer {

	private PredicateNode root;
	private ArrayList<PredicateNode> nodes;
	
	private HashMap<Integer, HashSet<Integer>> leveledNodes; // the level of the nodes is the key	
	private HashMap<Integer, HashSet<Integer>> predicatedNodes; // the corresponding predicate is the key
	
	public ProfileAnalyzer() {
		root = new PredicateNode();
		root.setLevel(0);
		nodes = new ArrayList<PredicateNode>();
		nodes.add(root);
		
		leveledNodes = new HashMap<Integer, HashSet<Integer>>();
		predicatedNodes = new HashMap<Integer, HashSet<Integer>>();
	}

	public void update() {
		int size = Profiles.executedPredicates.size();
		if (size == 0) { return; }
		int testIndex = Profiles.tests.size() - 1;
		
		PredicateNode current = root;
		Stack<PredicateNode> loopBranchStack = new Stack<PredicateNode>();
		for (int i = 0; i < size; i++) {
			// get the branch predicate information
			Pair p = Profiles.executedPredicates.get(i);
			int index = p.getPredicateIndex();
			boolean value = p.isPredicateValue();

			// set the predicate index if not yet set
			if (current.getPredicate() == -1) {
				current.setPredicate(index);
				addToLeveledNodes(current.getLevel(), nodes.size() - 1);
				addToPredicatedNodes(current.getPredicate(), nodes.size() - 1);
			} else if (current.getPredicate() != p.getPredicateIndex()) {
				System.err.println("[ml-testing] error in creating the tree structure");
			}
			
			// check if the current branch is a loop branch;
			// if yes, push the loop branch into the stack for later references
			boolean isLoopBranch = false;
			String type = Profiles.predicates.get(index).getType();
			if (type.equals("for") || type.equals("do") || type.equals("while")) {
				isLoopBranch = true;
				if (loopBranchStack.size() == 0 || loopBranchStack.peek().getPredicate() != index) {
					loopBranchStack.push(current);
				}
			}
			
			// pop if exit the loop by taking its false branch
			if (isLoopBranch && !value) {
				loopBranchStack.pop();
			}
			
			// check if the next branch is a loop branch
			PredicateNode next = null;
			if (loopBranchStack.size() > 0 && i + 1 < size) {
				next = loopBranchStack.peek();
				if (Profiles.executedPredicates.get(i + 1).getPredicateIndex() != next.getPredicate()) {
					next = null;
				}
			}
			
			// set either the true branch or the false branch
			PredicateArc branch;
			if (value) {
				if (current.getSourceTrueBranch() == null) {
					if (next == null) {
						next = new PredicateNode();
						int l = current.getLevel() + 1;
						next.setLevel(l);
						nodes.add(next);
					}
					PredicateArc arc = new PredicateArc(current, next);
					current.setSourceTrueBranch(arc);
					next.addTargetTrueBranch(arc);
				}
				branch = current.getSourceTrueBranch();
				current = current.getSourceTrueBranch().getTarget();
			} else {
				if (current.getSourceFalseBranch() == null) {
					if (next == null) {
						next = new PredicateNode();
						int l = current.getLevel() + 1;
						next.setLevel(l);
						nodes.add(next);
					}
					PredicateArc arc = new PredicateArc(current, next);
					current.setSourceFalseBranch(arc);
					next.addTargetFalseBranch(arc);
				}
				branch = current.getSourceFalseBranch();
				current = current.getSourceFalseBranch().getTarget();
			}
			
			// avoid associating a test input to a loop branch for multiple times
			if (branch.getTests() == null || branch.getTests().get(branch.getTests().size() - 1) != testIndex) {
				if (!(Profiles.predicates.get(branch.getSource().getPredicate()).getType().equals("do") && value && isOneIterationDoLoop(i, size))) {
					branch.addTest(testIndex);
				}
			}
		}
		Profiles.executedPredicates.clear();
	}
	
	public PredicateNode findUnexploredBranch() {
		if (Config.MODE == Config.Mode.RANDOM) {
			return random();
		} else { // mode == Mode.SYSTEMATIC
			return systematic();
		}
	}
	
	// TODO find an unexplored branch randomly
	private PredicateNode random() {
		return null;
	}
	
	// find an unexplored branch systematically
	private PredicateNode systematic() {
		// locate the level and corresponding unexplored branches for further selection
		ArrayList<PredicateNode> pSet = null;
		int ls = leveledNodes.size();
		for (int i = 0; i < ls; i++) {
			pSet = new ArrayList<PredicateNode>();
			Iterator<Integer> iterator = leveledNodes.get(i).iterator();
			while (iterator.hasNext()) {
				PredicateNode node = nodes.get(iterator.next());
				if (Profiles.predicates.get(node.getPredicate()).getDepInputs() != null && node.getAttempts() < Config.MAX_ATTEMPTS) {
					String type = Profiles.predicates.get(node.getPredicate()).getType();
					if (type.equals("if")) {
						if (node.getSourceTrueBranch() == null || node.getSourceFalseBranch() == null) {
							pSet.add(node);
						}
					} else if (type.equals("for") || type.equals("do") || type.equals("while")) {
						if (node.getSourceTrueBranch() == null || node.getSourceTrueBranch().getTests().size() == node.getSourceFalseBranch().getTests().size()) {
							pSet.add(node);
						}
					} else {
						System.err.println("[ml-testing] unknown conditional statement");
					}
				}
			}
			if (pSet.size() > 0) {
				break;
			}
		}
		// TODO apply other heuristics (e.g., using predicatedNodes) rather than randomly
		if (pSet.size() > 0) {
			int ran = (int)Math.random() * pSet.size();
			pSet.get(ran).incAttempts();
			return pSet.get(ran);
		} else {
			return null;
		}
	}
	
	public void printNodes() {
		for (int i = 0; i < nodes.size(); i++) {
			PredicateNode node = nodes.get(i);
			System.out.println("[ml-testing] " + node);
		}
		System.out.println();
	}

	private void addToLeveledNodes(int level, int index) {
		if (leveledNodes.get(level) == null) {
			leveledNodes.put(level, new HashSet<Integer>());
		}
		leveledNodes.get(level).add(index);
	}
	
	private void addToPredicatedNodes(int predicate, int index) {
		if (predicatedNodes.get(predicate) == null) {
			predicatedNodes.put(predicate, new HashSet<Integer>());
		}
		predicatedNodes.get(predicate).add(index);
	}
	
	private boolean isOneIterationDoLoop (int start, int end) {
		Pair p1 = Profiles.executedPredicates.get(start);
		for (int i = start + 1; i < end; i++) {
			Pair p2 = Profiles.executedPredicates.get(i);
			if (p2.getPredicateIndex() == p1.getPredicateIndex() && p2.isPredicateValue()) {
				return false;
			} else if (p2.getPredicateIndex() == p1.getPredicateIndex() && !p2.isPredicateValue()) {
				break;
			}
		}
		return true;
	}

	public PredicateNode getRoot() {
		return root;
	}

	public void setRoot(PredicateNode root) {
		this.root = root;
	}

	public ArrayList<PredicateNode> getNodes() {
		return nodes;
	}

	public void setNodes(ArrayList<PredicateNode> nodes) {
		this.nodes = nodes;
	}
	
}
