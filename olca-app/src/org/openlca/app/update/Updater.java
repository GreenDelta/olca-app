package org.openlca.app.update;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import org.openlca.app.update.DeletionFailedCallback.DeletionFailedResponse;

/**
 * Simple delete-before-unzip updater which does no logging (no access to stdout
 * or stderr) and sticks to JDK-provided tools. Keeping clear of stdout and
 * stderr is beneficial when running in a self-contained sub-process whose
 * parent might have quit already.
 */
public class Updater {

	private List<String> pathsToDelete;
	private Set<String> pathsToIgnore;
	private String toRun;
	private boolean showProgressBar = false;
	private DeletionFailedCallback deletionFailedCallback;
	private Boolean windows = null;
	private boolean attemptedDeleteBefore = false;
	private List<UnzipRequest> unzipRequests;

	private ExecutorService threadPool = Executors.newCachedThreadPool();
	private ProgressFrame progressFrame;

	public Updater(String toRun, List<UnzipRequest> unzipRequests,
			List<String> pathsToDelete, List<String> pathsToIgnore) {
		this.unzipRequests = new ArrayList<>(unzipRequests);
		this.toRun = toRun;
		this.pathsToDelete = new ArrayList<>(pathsToDelete);
		this.pathsToIgnore = new HashSet<>(pathsToIgnore);
	}

	public void setDeletionFailedCallback(DeletionFailedCallback cb) {
		this.deletionFailedCallback = cb;
	}

	public void setShowProgressBar(boolean showProgressBar) {
		this.showProgressBar = showProgressBar;
	}

	public boolean isShowProgressBar() {
		return showProgressBar;
	}

	public List<String> getPathsToDelete() {
		return pathsToDelete;
	}

	public List<UnzipRequest> getUnzipRequests() {
		return unzipRequests;
	}

	public Set<String> getPathsToIgnore() {
		return pathsToIgnore;
	}

	/**
	 * This relaunches the updater in an external JVM so that it can update the
	 * executing application. This works around file-locking problems.
	 * 
	 * @param javaExecutable
	 *            a java.exe, java or the like
	 * @param updaterJarPath
	 *            path to this jar
	 * @return executed process - output is consumed by a thread started in this
	 *         method to avoid lockups by accidental output if there is no
	 *         console.
	 * @throws Exception
	 */
	public Process runInNewJVM(File javaExecutable, String updaterJarPath)
			throws Exception {
		File tempUpdaterJar = createTempUpdaterJar(updaterJarPath);
		List<String> cmd = new ArrayList<>();
		cmd.addAll(Arrays.asList(new String[] {
				javaExecutable.getAbsolutePath(), "-jar",
				tempUpdaterJar.getAbsolutePath(),
				// the updater takes the executable and lists
				toRun }));
		// the paths to delete BEFORE unzipping
		cmd.add("-del");
		cmd.addAll(getPathsToDelete());

		cmd.add("-unzip");
		for (UnzipRequest req : unzipRequests) {
			cmd.add(req.getZipFile());
			cmd.add(req.getTargetDir());
			cmd.add(Integer.toString(req.getLevelsToStripOnUnzip()));
		}

		// and finally the paths to ignore (no overwriting, no deletion)
		cmd.add("-ign");
		cmd.addAll(pathsToIgnore);

		ProcessBuilder processBuilder = new ProcessBuilder(cmd);
		processBuilder.redirectErrorStream(true);
		final Process exec = processBuilder.start();

		Thread bufferEmptier = new Thread(new Runnable() {

			@Override
			public void run() {
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new InputStreamReader(
							exec.getInputStream()));

					while (reader.readLine() != null) {
						// updater should not write to console
					}
				} catch (IOException e) {
					// ignore
				} finally {
					try {
						if (reader != null)
							reader.close();
					} catch (Exception e) {
						// ignore
					}
				}
			}
		});
		bufferEmptier.setDaemon(true);
		bufferEmptier.start();
		return exec;
	}

	/**
	 * Should only be called by {@link Main}.
	 * 
	 * @throws Exception
	 */
	public void runInThisJVM() throws Exception {
		// first the checks then start deleting

		createAndShowProgressBar();

		checkZipsAndTargetDirs();

		deletePathsToDelete();

		for (UnzipRequest req : unzipRequests) {
			extract(req);
		}

		runBinary();

	}

	// impacts of closing output stream not clear, maybe used later
	protected File createTempUpdaterJar(final String updaterJarPath)
			throws Exception {
		final File tempUpdater = File.createTempFile("updaterTemp", ".f");
		Utils.copy(new Utils.InputSupplier() {
			@Override
			public InputStream getInput() throws IOException {
				return new FileInputStream(new File(updaterJarPath));
			}
		}, new FileOutputStream(tempUpdater));
		return tempUpdater;
	}

	private void extract(UnzipRequest req) throws Exception {
		File targetDir = new File(req.getTargetDir());
		try (ZipFile zipFile = new ZipFile(req.getZipFile())) {
			extract(zipFile, targetDir, req.getLevelsToStripOnUnzip());
		}
	}

	public void checkZipsAndTargetDirs() throws IOException {
		for (UnzipRequest req : unzipRequests) {
			try (ZipFile zipFile = new ZipFile(req.getZipFile())) {

			}

			File targetDir = new File(req.getTargetDir());
			if (!targetDir.exists()) {
				if (!targetDir.mkdirs()) {
					throw new RuntimeException("Target dir creation failed: "
							+ targetDir);
				}
			}

			if (!tryWriteToDir(targetDir)) {
				throw new RuntimeException("Cannot write to target dir: "
						+ targetDir);
			}
		}
	}

	protected void createAndShowProgressBar() {
		if (isShowProgressBar()) {
			Runnable progressBarCreator = new Runnable() {

				@Override
				public void run() {
					progressFrame = new ProgressFrame("openLCA update");
					progressFrame.init();
				}
			};
			try {
				Utils.invokeSwing(progressBarCreator);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	protected void setCurrentTask(String task) {
		if (progressFrame != null) {
			progressFrame.getProgressBorder().setTitle(task);
		}
	}

	protected void setProgress(int progress) {
		if (progressFrame != null) {
			progressFrame.getProgressBar().setValue(progress);
			progressFrame.getProgressBar().setIndeterminate(false);
		}
	}

	protected void setProgressTotal(int progressTotal) {
		if (progressFrame != null) {
			progressFrame.getProgressBar().setMaximum(progressTotal);
			progressFrame.getProgressBar().setIndeterminate(false);

		}
	}

	protected int getProgressTotal() {
		if (progressFrame != null) {
			return progressFrame.getProgressBar().getMaximum();
		}
		return 0;
	}

	protected int getProgress() {
		if (progressFrame != null) {
			return progressFrame.getProgressBar().getValue();
		}
		return 0;
	}

	private void runBinary() throws IOException {
		if (!isNullOrEmpty(toRun)) {

			File binary = new File(toRun);
			if (binary.exists()) {
				setCurrentTask("Running openLCA and quitting updater.");
				binary.setExecutable(true);
				ProcessBuilder processBuilder = new ProcessBuilder(
						binary.getAbsolutePath());
				processBuilder.redirectErrorStream(true);
				processBuilder.start();

			}
		}
	}

	private void extract(ZipFile zipFile, File dir, int stripLeadingLevels)
			throws Exception {
		setCurrentTask("Extracting...");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		setProgressTotal(getProgressTotal() + zipFile.size());
		int progress = getProgress();
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry.isDirectory()) {
				extractDirectory(dir, entry, stripLeadingLevels);
			} else {
				extractFile(dir, zipFile, entry, stripLeadingLevels);
			}
			setProgress(++progress);
		}
	}

	// impacts of closing output stream not clear, maybe used later
	private void extractFile(File dir, final ZipFile zipFile,
			final ZipEntry entry, int stripLeadingLevels) throws Exception {
		String name = stripLeadingParts(entry.getName(), stripLeadingLevels);
		if (isNullOrEmpty(name)) {
			return;
		}
		if (isPathToIgnore(name)) {
			return;
		}
		File file = new File(dir, name);

		FileOutputStream outputStream = null;
		int tryCounter = 20;
		while (outputStream == null) {
			try {
				outputStream = new FileOutputStream(file);
			} catch (FileNotFoundException fnfe) {
				// this regularly happens on win platform when the
				// prior process wasn't cleaned up: retry
				if (--tryCounter < 0) {
					throw new Exception("Could not open '" + file
							+ "' for writing, probably locked "
							+ "by an open application.", fnfe);
				}
				Thread.sleep(200);
			}
		}
		Utils.copy(new Utils.InputSupplier() {

			@Override
			public InputStream getInput() throws IOException {
				return zipFile.getInputStream(entry);
			}
		}, outputStream);
	}

	protected void extractDirectory(File target, ZipEntry entry,
			int stripLeadingLevels) {

		String name = stripLeadingParts(entry.getName(), stripLeadingLevels);
		if (isNullOrEmpty(name)) {
			return;
		}
		if (isPathToIgnore(name)) {
			return;
		}
		File theDir = new File(target, name);
		if (theDir.exists()) {
			if (!theDir.isDirectory()) {
				throw new RuntimeException("Trying to overwrite directory, "
						+ "but a file with the same name exists: " + theDir);
			}
		} else if (!theDir.mkdirs()) {
			throw new RuntimeException("Cannot create " + name + " in "
					+ target);
		}
	}

	private boolean isPathToIgnore(String name) {
		if (pathsToIgnore != null) {
			return pathsToIgnore.contains(name);
		}
		return false;
	}

	protected static boolean isNullOrEmpty(String name) {
		return name == null || name.equals("");
	}

	protected String stripLeadingParts(String name, int stripLeadingLevels) {
		if (stripLeadingLevels <= 0 || isNullOrEmpty(name)) {
			return name;
		}
		int[] stripCounter = new int[] { stripLeadingLevels };
		File part = stripParts(new File(name), stripCounter);
		return part == null ? null : part.getPath();
	}

	private File stripParts(File path, int[] stripCounter) {
		if (path == null)
			return null;
		File parent = path.getParentFile();
		File newParent = stripParts(parent, stripCounter);
		if (newParent == null) {
			if (stripCounter[0] > 0) {
				stripCounter[0]--;
				return null;
			}
			return new File(path.getName());
		}
		return new File(newParent, path.getName());
	}

	protected void deletePathsToDelete() throws Exception {
		setCurrentTask("Cleaning old version...");
		for (String path : pathsToDelete) {
			if (isPathToIgnore(path)) {
				continue;
			}
			boolean repeat = true;
			while (repeat) {
				DeletionFailedCallback tempCB = null;
				// special treatment of first deletion - allow time for shutting
				// down
				if (!attemptedDeleteBefore) {
					tempCB = deletionFailedCallback;
					deletionFailedCallback = new DeletionFailedCallback() {
						@Override
						public DeletionFailedResponse deletionFailed(String path) {
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// ignore
							}
							return DeletionFailedResponse.REPEAT;
						}
					};
				}
				repeat = tryDelete(path);
				attemptedDeleteBefore = true;
				if (tempCB != null) {
					this.deletionFailedCallback = tempCB;
				}
			} // while repeat
		}
	}

	protected boolean tryDelete(String path) throws Exception {
		boolean repeat = false;
		try {
			File pathFile = new File(path);

			if (pathFile.exists()) {
				if (pathFile.isDirectory()) {
					delDir(pathFile);
				} else {
					delFile(pathFile);
				}
			}
		} catch (Exception e) {
			if (this.deletionFailedCallback == null) {
				throw e;
			}
			DeletionFailedResponse failedResponse = this.deletionFailedCallback
					.deletionFailed(path);
			switch (failedResponse) {
			case ERROR:
				throw e;

			case REPEAT:
				repeat = true;
				break;
			case IGNORE:
				break;

			default:
				throw new RuntimeException("Unknown DeletionFailedResponse: "
						+ failedResponse);
			}
		}
		return repeat;
	}

	protected void delDir(File pathFile) throws Exception {
		if (isWindows()) {
			delDirWindows(pathFile);
		} else {
			delDirUnix(pathFile);
		}
	}

	protected void delDirUnix(File pathFile) throws Exception {
		execute(new String[] { "/bin/rm", "-rf", pathFile.getAbsolutePath() });
	}

	protected void delDirWindows(File pathFile) throws Exception {
		execute(new String[] { "cmd", "/D", "/Q", "/C", "rd", "/S", "/Q",
				pathFile.getAbsolutePath() });
	}

	private void execute(String[] command) throws Exception {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		final Process process = processBuilder.start();

		FutureTask<Integer> task = new FutureTask<>(new ProcessExitCodeButler(
				process));
		threadPool.execute(task);
		Integer exitCode = task.get();
		if (exitCode != 0) {
			throw new RuntimeException("Exit code " + exitCode
					+ " from command " + Arrays.asList(command));
		}
	}

	private boolean isWindows() {
		if (this.windows == null) {
			boolean winLocal = false;
			String osName = System.getProperty("os.name");
			if (osName != null && osName.toLowerCase().contains("win")) {
				winLocal = true;
			}
			this.windows = winLocal;
		}
		return this.windows;
	}

	private void delFile(File pathFile) {
		if (!pathFile.delete()) {
			throw new RuntimeException("Cannot delete: " + pathFile);
		}
	}

	public static boolean tryWriteToDir(File dir) {
		try {
			File tempFile = File.createTempFile("testfile", "tmp", dir);
			tempFile.delete();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private static class ProgressFrame extends JFrame {

		private static final long serialVersionUID = -3388807758370534484L;
		private TitledBorder progressBorder;
		private JProgressBar progressBar;

		public ProgressFrame(String title) {
			super(title);
		}

		public JProgressBar getProgressBar() {
			return progressBar;
		}

		public TitledBorder getProgressBorder() {
			return progressBorder;
		}

		public void init() {
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					if (JOptionPane.YES_OPTION == JOptionPane
							.showConfirmDialog(
									ProgressFrame.this,
									"Close and abort update? "
											+ "You may need to do a fresh reinstall "
											+ "of openLCA if you choose Yes.")) {
						System.exit(1);
					}
				}
			});
			Container content = getContentPane();
			progressBar = new JProgressBar();
			progressBorder = BorderFactory
					.createTitledBorder("Initializing...");
			progressBar.setBorder(progressBorder);
			progressBar.setIndeterminate(true);
			content.add(progressBar, BorderLayout.NORTH);
			if (progressBar.getPreferredSize() != null) {
				progressBar.setPreferredSize(new Dimension(370,
						(int) progressBar.getPreferredSize().getHeight()));
			}
			pack();
			toFront();
			// center:
			setLocationRelativeTo(null);
			setVisible(true);
		}
	}

	public static class UnzipRequest {
		private String targetDir;

		private String zipFile;

		private int levelsToStripOnUnzip;

		public UnzipRequest(String targetDir, String zipFile,
				int levelsToStripOnUnzip) {
			super();
			this.targetDir = targetDir;
			this.zipFile = zipFile;
			this.levelsToStripOnUnzip = levelsToStripOnUnzip;
		}

		public String getTargetDir() {
			return targetDir;
		}

		public void setTargetDir(String targetDir) {
			this.targetDir = targetDir;
		}

		public String getZipFile() {
			return zipFile;
		}

		public void setZipFile(String zipFile) {
			this.zipFile = zipFile;
		}

		public int getLevelsToStripOnUnzip() {
			return levelsToStripOnUnzip;
		}

		public void setLevelsToStripOnUnzip(int levelsToStripOnUnzip) {
			this.levelsToStripOnUnzip = levelsToStripOnUnzip;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (!(obj instanceof UnzipRequest)) {
				return false;
			}
			UnzipRequest other = (UnzipRequest) obj;
			return Utils.areEqual(getTargetDir(), other.getTargetDir())
					&& Utils.areEqual(getZipFile(), other.getZipFile())
					&& Utils.areEqual(getLevelsToStripOnUnzip(),
							other.getLevelsToStripOnUnzip());
		}

		@Override
		public String toString() {
			return "UnzipRequest(" + getTargetDir() + ", " + getZipFile()
					+ ", " + getLevelsToStripOnUnzip() + ")";
		}
	}

}
