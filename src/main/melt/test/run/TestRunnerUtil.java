package melt.test.run;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import melt.Config;

public class TestRunnerUtil {

	private Class<?> c;
	private Method m;
	
	public TestRunnerUtil() {
		try {
			URL[] cp = new URL[Config.CLASSPATH.length];
			for (int i = 0; i < Config.CLASSPATH.length; i++) {
				File f = new File(Config.CLASSPATH[i]);
				cp[i] = f.toURI().toURL();
			}
			URLClassLoader cl = new URLClassLoader(cp);
			c = cl.loadClass(Config.MAINCLASS);
			m = c.getMethod(Config.METHOD, Config.CLS);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	public void run(Object[] test) throws MalformedURLException {
		try {
			m.invoke(c.newInstance(), test);
		} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}
	}

}
