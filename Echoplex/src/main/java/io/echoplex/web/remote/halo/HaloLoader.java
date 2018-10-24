package io.echoplex.web.remote.halo;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.echoplex.web.remote.NGContext;

/**
 * 
 * Simple custom class loader implementation
 * 
 */
public class HaloLoader extends ClassLoader {

	/**
	 * The HashMap where the classes will be cached
	 */
	private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private String clazz_name;

	public HaloLoader(String string) {
		super();
		this.clazz_name = string;
	}

	@Override
	public String toString() {
		return HaloLoader.class.getName();
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {

//		if (classes.containsKey(name)) {
//			return classes.get(name);
//		}

		byte[] classData;

		try {
			if (name.equals(clazz_name)) {
			classData = loadClassData(name);
			} else {
				return super.findClass(name);
			}
		} catch (IOException e) {
			throw new ClassNotFoundException("Class [" + name + "] could not be found", e);
		}

		Class<?> c = defineClass(name, classData, 0, classData.length);
		resolveClass(c);
		classes.put(name, c);

		return c;
	}

	/**
	 * Load the class file into byte array
	 * 
	 * @param name
	 *            The name of the class e.g. com.codeslices.test.TestClass}
	 * @return The class file as byte array
	 * @throws IOException
	 */
	private byte[] loadClassData(String name) throws IOException {
		File f = new File("/apps/apache-tomcat-7.0.54/webapps/acgm/WEB-INF/classes/" + name.replace(".", "/") + ".class");
		
	//	HaloLoader.getSystemResourceAsStream(name.replace(".", "/") + ".class")
		
		
		
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int i;

		while ((i = in.read()) != -1) {
			out.write(i);
		}

		in.close();
		byte[] classData = out.toByteArray();
		out.close();

		return classData;
	}

	/**
	 * Simple usage of the CustomClassLoader implementation
	 * 
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 */
	public static void nailMain(NGContext context)  throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException,
			InvocationTargetException {
		HaloLoader loader = new HaloLoader(context.getArgs()[0]);
		// This class should be in your application class path
		Class<?> c = loader.findClass(context.getArgs()[0]);
		Object o = c.newInstance();
		Method m = c.getMethod(context.getArgs()[1]);
		context.out.println(m.invoke(o));
	}

}