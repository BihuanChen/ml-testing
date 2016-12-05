package melt.test.run;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import melt.Config;
import melt.core.Profile;

public class TaintRunner {
	
	private static BufferedReader reader;
	private static BufferedWriter writer;
	
	private static ObjectOutputStream oos;
	
	public static void run(Object[] test) throws IOException, InterruptedException {
		// write the test to be taint-analyzed
		setTest1(test);
		// read the taint results
		if (reader == null) {
			reader = new BufferedReader(new FileReader(new File(System.getProperty("java.io.tmpdir") + "/taint-result")));
		}
		String line = null;
		while ((line = reader.readLine()) == null) {
			Thread.sleep(5);
		}
		do {
			//System.out.println(line);
			String[] str = line.split(" ");
			String srcLoc = str[0].replace("/", ".");
			int tag = Integer.valueOf(str[1]);			
			Profile.taints.put(srcLoc, new HashSet<Integer>());
			for (int j = 0; j < Config.CLS.length; j++) {
				int bit = (int)Math.pow(2, j);
				if ((tag & bit) == bit) {
					Profile.taints.get(srcLoc).add(j);
				}
			}
		} while ((line = reader.readLine()) != null);
		System.out.println();
	}
	
	public static void setTest1(Object[] test) throws IOException {
		if (writer == null) {
			writer = new BufferedWriter(new FileWriter(new File(System.getProperty("java.io.tmpdir") + "/taint-test"), true));
		}
		for (int i = 0; i < test.length; i++) {
			writer.write(test[i] + " ");
		}
		writer.write("\n");
		writer.flush();
	}
	
	public static void setTest2(Object[] test) throws IOException {
		if (oos == null) {
			oos = new ObjectOutputStream(new FileOutputStream(new File(System.getProperty("java.io.tmpdir") + "/taint-test"), true));
		}
		oos.writeObject(test);
		oos.flush();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		Config.loadProperties("/home/bhchen/workspace/testing/benchmark4-siemens/src/replace/Replace.melt");
		for (int i = 0; i < 1; i++) {
			long t1 = System.currentTimeMillis();
			TaintRunner.run(new Object[]{'a', '1', '&', 'a', 'a'});
			long t2 = System.currentTimeMillis();
			System.out.println(t2 - t1);
			TaintRunner.run(new Object[]{'a', '1', '&', 'a', 'a'});
			long t3 = System.currentTimeMillis();
			System.out.println(t3 - t2);
			TaintRunner.run(new Object[]{'a', '1', '&', 'a', 'a'});
			System.out.println(System.currentTimeMillis() - t3);
		}
	}

}
