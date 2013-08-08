package org.openlca.app.update;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public class Utils {

	public static void copy(InputStream is, OutputStream outputStream)
			throws Exception {
		byte[] buf = new byte[4096];
		while (true) {
			int read = is.read(buf);
			if (read <= 0) {
				break;
			}
			outputStream.write(buf, 0, read);
		}
	}

	public static void invokeSwing(Runnable questioner)
			throws InterruptedException, InvocationTargetException {
		if (SwingUtilities.isEventDispatchThread()) {
			questioner.run();
		} else {
			SwingUtilities.invokeAndWait(questioner);
		}
	}

	public static boolean areEqual(Object obj1, Object obj2) {
		if (obj1 == obj2)
			return true;
		if (obj1 == null)
			return false;
		return obj1.equals(obj2);
	}

	public static void close(Closeable c, boolean swallowExc) throws Exception {
		try {
			if (c != null) {
				c.close();
			}
		} catch (Exception e) {
			if (!swallowExc) {
				throw e;
			}
		}
	}

	public static void copy(InputSupplier inputSupplier,
			FileOutputStream outputStream) throws IOException, Exception {
		boolean threw = true;
		try (InputStream is = inputSupplier.getInput()) {
			Utils.copy(is, outputStream);
			threw = false;
		} finally {
			Utils.close(outputStream, threw);
		}
	}

	public interface InputSupplier {
		InputStream getInput() throws IOException;
	}

}
