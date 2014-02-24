package org.openlca.app.rcp.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

public class FileUtils {

	private static final int BUF_SIZE = 4096;

	/**
	 * As in the Guava original, just with work reporting: Opens input and
	 * output streams from the given suppliers, copies all bytes from the input
	 * to the output, and closes the streams. *
	 * 
	 * @return the number of bytes copied
	 * @throws IOException
	 *             if an I/O error occurs
	 */

	// since throwing of exception depends on successful operations, interfering
	// with resource managing is unnecessary, and not wanted
	public static long copy(InputSupplier<? extends InputStream> from,
			OutputSupplier<? extends OutputStream> to, WorkCallback cb)
			throws IOException {
		int successfulOps = 0;
		InputStream in = from.getInput();
		try {
			OutputStream out = to.getOutput();
			try {
				long count = copy(in, out, cb);
				successfulOps++;
				return count;
			} finally {
				Closeables.close(out, successfulOps < 1);
				successfulOps++;
			}
		} finally {
			Closeables.close(in, successfulOps < 2);
		}
	}

	/**
	 * Doesn't close or flush.
	 * 
	 * @return the number of bytes copied
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static long copy(InputStream from, OutputStream to, WorkCallback cb)
			throws IOException {
		byte[] buf = new byte[BUF_SIZE];
		long total = 0;
		while (true) {
			int r = from.read(buf);
			if (r == -1) {
				break;
			}
			to.write(buf, 0, r);
			if (cb != null) {
				cb.report(r);
			}
			total += r;
		}
		return total;
	}
}
