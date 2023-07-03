package org.openlca.app;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.collaboration.preferences.CollaborationPreference;
import org.openlca.app.db.Libraries;
import org.openlca.app.db.Repository;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelEditorInput;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.matrix.solvers.JavaSolver;
import org.openlca.core.matrix.solvers.MatrixSolver;
import org.openlca.core.matrix.solvers.NativeSolver;
import org.openlca.core.matrix.solvers.mkl.MKL;
import org.openlca.core.matrix.solvers.mkl.MKLSolver;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.nativelib.Module;
import org.openlca.nativelib.NativeLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class App {

	static Logger log = LoggerFactory.getLogger(App.class);

	private static MatrixSolver solver;

	private App() {
	}

	/**
	 * Get the folder where openLCA is installed. This is where our native math
	 * libraries and the openLCA.ini file are located. On macOS this is the
	 * folder `openLCA.app/Contents/Eclipse`.
	 */
	public static File getInstallLocation() {
		URL url = Platform.getInstallLocation().getURL();
		try {
			// url.toURI() does not work for URLs with specific characters
			// which is the case when the application is installed in
			// folders like C:\Program Files (x86)\openLCA; see
			// https://community.oracle.com/blogs/kohsuke/2007/04/25/how-convert-javaneturl-javaiofile
			return new File(url.toURI());
		} catch (URISyntaxException e) {
			return new File(url.getPath());
		}
	}

	public static MatrixSolver getSolver() {
		if (solver != null)
			return solver;

		synchronized (App.class) {
			if (solver != null)
				return solver;

			// try to load the native libraries, first try the workspace and
			// then the installation location
			var dirs = List.of(Workspace.root(), getInstallLocation());
			try {
				// check for MKL first
				for (var dir : dirs) {
					if (MKL.isLibraryDir(dir) && MKL.loadFrom(dir)) {
						log.info("loaded MKL libraries from {}", dir);
						solver = new MKLSolver();
						return solver;
					}
				}
				for (var dir : dirs) {
					if (NativeLib.isLibraryDir(dir)) {
						NativeLib.loadFrom(dir);
						if (NativeLib.isLoaded()) {
							log.info("loaded native libraries; with UMFPACK={}",
									NativeLib.isLoaded(Module.UMFPACK));
							solver = new NativeSolver();
							return solver;
						}
					}
				}
			} catch (Throwable err) {
				log.error("failed to load native solver", err);
				return null;
			}

			log.warn("could not load a high-performance library for calculations");
			solver = new JavaSolver();
			return solver;
		}
	}

	/**
	 * Returns the version of the openLCA application.
	 */
	public static String getVersion() {
		return RcpActivator.getDefault().getBundle().getVersion().toString();
	}

	public static boolean isCommentingEnabled() {
		return Repository.isConnected() && CollaborationPreference.commentsEnabled();
	}

	/**
	 * Indicates if the application runs in developer mode (for activation of
	 * experimental features and development tools).
	 */
	public static boolean runsInDevMode() {
		String val = AppArg.DEV_MODE.getValue();
		if (val == null)
			return false;
		return val.equals("true");
	}

	public static void open(RootEntity model) {
		open(Descriptor.of(model));
	}

	public static void open(RootDescriptor d) {
		// the model editor will try to load the thing from
		// the database, thus the ID has to be >= 0 here
		if (d == null || d.type == null || d.id <= 0) {
			log.error("model is null, could not open editor");
			return;
		}

		if (!Libraries.checkValidity(d))
			return;

		log.trace("open editor for {} ", d);
		String editorId = "editors." + d.type.getModelClass()
				.getSimpleName().toLowerCase();
		var input = new ModelEditorInput(d);
		Editors.open(input, editorId);
	}

	public static void close(RootEntity entity) {
		close(Descriptor.of(entity));
	}

	public static void close(Descriptor d) {
		IEditorReference ref = findEditor(d);
		if (ref == null)
			return;
		Editors.close(ref);
	}

	/**
	 * Returns true if the given data set is currently opened in an editor that
	 * has a dirty (= unsaved) state.
	 */
	public static boolean hasDirtyEditor(RootEntity e) {
		if (e == null)
			return false;
		return hasDirtyEditor(Descriptor.of(e));
	}

	/**
	 * Returns true if the given data set is currently opened in an editor that
	 * has a dirty (= unsaved) state.
	 */
	public static boolean hasDirtyEditor(Descriptor d) {
		IEditorReference ref = findEditor(d);
		if (ref == null)
			return false;
		return ref.isDirty();
	}

	private static IEditorReference findEditor(Descriptor d) {
		if (d == null)
			return null;
		for (var ref : Editors.getReferences()) {
			try {
				var inp = ref.getEditorInput();
				if (!(inp instanceof ModelEditorInput input))
					continue;
				if (Objects.equals(input.getDescriptor(), d))
					return ref;
			} catch (Exception e) {
				log.error("editor search failed", e);
			}
		}
		return null;
	}

	public static Job runInUI(String name, Runnable runnable) {
		WrappedUIJob job = new WrappedUIJob(name, runnable);
		job.setUser(true);
		job.schedule();
		return job;
	}

	/**
	 * Wraps a runnable in a job and executes it using the Eclipse jobs
	 * framework. No UI access is allowed for the runnable.
	 */
	public static Job run(String name, Runnable runnable) {
		return run(name, runnable, null);
	}

	/**
	 * See {@link App#run(String, Runnable)}. Additionally, this method allows
	 * to give a callback which is executed in the UI thread when the runnable
	 * is finished.
	 */
	public static Job run(String name, Runnable runnable, Runnable callback) {
		WrappedJob job = new WrappedJob(name, runnable);
		if (callback != null)
			job.setCallback(callback);
		job.setUser(true);
		job.schedule();
		return job;
	}

	public static void runWithProgress(String name, Runnable runnable) {
		var progress = PlatformUI.getWorkbench()
				.getProgressService();
		try {
			progress.run(true, false, (monitor) -> {
				monitor.beginTask(name, IProgressMonitor.UNKNOWN);
				runnable.run();
				monitor.done();
			});
		} catch (InvocationTargetException | InterruptedException e) {
			log.error("Error while running progress " + name, e);
		}
	}

	public static void runWithProgress(
			String name, Runnable fn, Runnable callback) {
		runWithProgress(name, fn, callback, null);
	}

	public static void runWithProgress(
			String name, Runnable fn, Runnable callback, Runnable onError) {
		var service = PlatformUI.getWorkbench().getProgressService();
		AtomicBoolean fnSucceeded = new AtomicBoolean(false);
		try {
			service.run(true, false, m -> {
				m.beginTask(name, IProgressMonitor.UNKNOWN);
				fn.run();
				fnSucceeded.set(true);
				m.done();
				if (callback != null) {
					WrappedUIJob uiJob = new WrappedUIJob(name, callback);
					uiJob.schedule();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			log.error("Error while running progress " + name, e);
			if (!fnSucceeded.get() && onError != null) {
				onError.run();
			}
		}
	}

	/**
	 * Shows a progress indicator while running the given function in a separate
	 * thread. The calling thread is blocked while the given function is
	 * executed. It returns the result of the given function or `null` when
	 * calling that function failed.
	 */
	public static <T> T exec(String task, Supplier<T> fn) {
		var ref = new AtomicReference<T>();
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile((monitor) -> {
						monitor.beginTask(task, IProgressMonitor.UNKNOWN);
						ref.set(fn.get());
						monitor.done();
					});
		} catch (Exception e) {
			ErrorReporter.on("exec " + task + " failed", e);
		}
		return ref.get();
	}

	/**
	 * Blocks the current thread while running the given function in a separate
	 * non-UI thread.
	 */
	public static void exec(String task, Runnable fn) {
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(monitor -> {
						monitor.beginTask(task, IProgressMonitor.UNKNOWN);
						fn.run();
						monitor.done();
					});
		} catch (Exception e) {
			ErrorReporter.on("Failed to execute task: " + task, e);
		}
	}

}
