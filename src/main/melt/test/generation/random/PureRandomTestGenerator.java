package melt.test.generation.random;

import java.util.HashSet;

import melt.Config;
import melt.learn.PathLearner;
import melt.test.generation.TestGenerator;
import melt.test.generation.concolic.ConcolicTestGenerator;
import melt.test.util.TestCase;

public class PureRandomTestGenerator extends TestGenerator {

	public static long ceTime = 0;

	public PureRandomTestGenerator(PathLearner pathLearner) {
		super(pathLearner);
	}

	@Override
	public HashSet<TestCase> generate() throws Exception {
		if (Config.CE_ENABLED && pathLearner != null && pathLearner.getTarget().getAttempts() == Config.MAX_ATTEMPTS) {
			long t = System.currentTimeMillis();
			HashSet<TestCase> tcs = new ConcolicTestGenerator(pathLearner).generate();
			ceTime += System.currentTimeMillis() - t;
			return tcs;
		} else {
			return genPureRandomTests();
		}
	}
	
	// might get stuck when the constraints are too narrow
	private HashSet<TestCase> genPureRandomTests() throws Exception {
		HashSet<TestCase> testCases = new HashSet<TestCase>(Config.TESTS_SIZE);
		while (true) {
			TestCase testCase = new TestCase(melt.test.util.Util.randomTest());
			// check if the test is valid
			if (pathLearner == null || pathLearner.isValidTest(testCase)) {
				testCases.add(testCase);
				if (testCases.size() == Config.TESTS_SIZE) {
					return testCases;
				}
			}
		}
	}

}
