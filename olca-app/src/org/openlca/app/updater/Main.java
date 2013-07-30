package org.openlca.app.updater;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;

import org.openlca.app.updater.Updater.UnzipRequest;

public class Main {

	private static final boolean DEBUG = false;

	public static void main(String[] args) {
		try {
			ArrayList<UnzipRequest> toUnzip = new ArrayList<>();
			ArrayList<String> toDelete = new ArrayList<>();
			ArrayList<String> toIgnore = new ArrayList<>();

			String toRun = parseArguments(args, toUnzip, toDelete, toIgnore);

			Updater updater = new Updater(toRun, toUnzip, toDelete, toIgnore);

			updater.setShowProgressBar(true);

			updater.setDeletionFailedCallback(new DeletionFailedCallback() {

				@Override
				public DeletionFailedResponse deletionFailed(final String path) {
					final String fileName = new File(path).getName();
					final AtomicInteger result = new AtomicInteger();
					try {
						Runnable questioner = new Runnable() {

							@Override
							public void run() {
								result.set(JOptionPane.showConfirmDialog(null,
										"Deletion of " + fileName
												+ " failed, retry?\n(Path:  "
												+ path + ")", "Retry question",
										JOptionPane.YES_NO_OPTION,
										JOptionPane.QUESTION_MESSAGE));
							}
						};
						Utils.invokeSwing(questioner);
					} catch (Exception e) {
						return DeletionFailedResponse.ERROR;
					}
					if (result.get() == JOptionPane.YES_OPTION) {
						return DeletionFailedResponse.REPEAT;
					}
					return DeletionFailedResponse.ERROR;
				}

			});

			updater.runInThisJVM();

		} catch (final Exception e) {
			final StringWriter sw = new StringWriter();
			try (PrintWriter s = new PrintWriter(sw)) {
				e.printStackTrace(s);
			}
			e.printStackTrace();
			// avoid showing anything on StdOut/StdErr:
			Runnable problemReporter = new Runnable() {

				@Override
				public void run() {
					JOptionPane.showMessageDialog(null,
							"Updater failure: " + e.getMessage()
									+ (DEBUG ? "\n" + sw.toString() : ""),
							"Updater problem", JOptionPane.ERROR_MESSAGE);
				}
			};
			try {
				Utils.invokeSwing(problemReporter);
			} catch (Exception e1) {
				// ignore
			}
			System.exit(1);
		}
		System.exit(0);
	}

	@SuppressWarnings("unchecked")
	public static String parseArguments(String[] args,
			ArrayList<UnzipRequest> toUnzip, ArrayList<String> toDelete,
			ArrayList<String> toIgnore) {
		if (args.length < 3) {
			throw new RuntimeException("Not enough arguments");
		}
		String toRun = args[0];

		// sort rest into UnzipRequests, toIgnore and toDelete:
		@SuppressWarnings("rawtypes")
		ArrayList toAddTo = toUnzip;
		for (int i = 1; i < args.length; i++) {
			String arg = args[i];

			if ("-ign".equals(arg)) {
				// rest is to be ignored
				toAddTo = toIgnore;
				continue;
			} else if ("-del".equals(arg)) {
				toAddTo = toDelete;
				continue;
			} else if ("-unzip".equals(arg)) {
				toAddTo = toUnzip;
				continue;
			}

			if (toAddTo == toUnzip) {
				if (args.length < i + 3) {
					throw new RuntimeException(
							"Need 3 arguments for unzipping at arg " + (1 + i));
				}
				String zip = args[i++];
				String targetDir = args[i++];
				int levelsToStripOnUnzip;
				try {
					levelsToStripOnUnzip = Integer.parseInt(args[i]);
				} catch (NumberFormatException nfe) {
					throw new RuntimeException("Argument " + i + " for "
							+ "levels to strip on unzip not a number");
				}
				toUnzip.add(new UnzipRequest(targetDir, zip,
						levelsToStripOnUnzip));
			} else {
				toAddTo.add(args[i]);
			}

		}
		return toRun;
	}

}
