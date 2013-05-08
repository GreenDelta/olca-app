package org.openlca.updater;

import java.io.InputStream;
import java.util.concurrent.Callable;

public class ProcessExitCodeButler implements Callable<Integer> {
	private int exitcodeTestPeriodMs = 50;

	private final Process process;

	ProcessExitCodeButler(Process process) {
		this.process = process;
	}

	public void setExitcodeTestPeriodMs(int exitcodeTestPeriodMs) {
		this.exitcodeTestPeriodMs = exitcodeTestPeriodMs;
	}

	public int getExitcodeTestPeriodMs() {
		return exitcodeTestPeriodMs;
	}

	@Override
	public Integer call() throws Exception {
		int retval = -1;
		try (InputStream processOutput = process.getInputStream()) {
			int buflen = 4096;
			byte[] buf = new byte[buflen];
			while (true) {
				try {
					retval = process.exitValue();
					break;
				} catch (IllegalThreadStateException itse) {
					// not quit yet, ensure full stdout buffer not
					// blocking process:
					try {
						do {
							if (processOutput.available() > 0) {
								processOutput.read(
										buf,
										0,
										Math.min(buflen,
												processOutput.available()));
							}
							Thread.sleep(1);
						} while (processOutput.available() > 0);

						Thread.sleep(exitcodeTestPeriodMs);
					} catch (InterruptedException ie) {
						// ignore, loop
					}
				}
			}
		}
		return retval;
	}
}