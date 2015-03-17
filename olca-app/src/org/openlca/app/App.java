package org.openlca.app;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.openlca.app.editors.ModelEditorInput;
import org.openlca.app.preferencepages.FeatureFlag;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.Editors;
import org.openlca.core.math.IMatrixSolver;
import org.openlca.core.math.JavaSolver;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.eigen.NativeLibrary;
import org.openlca.eigen.solvers.BalancedSolver;
import org.openlca.eigen.solvers.DenseSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

public class App {

	static Logger log = LoggerFactory.getLogger(App.class);

	private static EventBus eventBus = new EventBus();
	private static IMatrixSolver solver;

	private App() {
	}

	public static IMatrixSolver getSolver() {
		if (solver != null)
			return solver;
		if (!NativeLibrary.isLoaded()) {
			log.warn("could not load a high-performance library for calculations");
			solver = new JavaSolver();
			return solver;
		}
		if (FeatureFlag.USE_SPARSE_MATRICES.isEnabled())
			solver = new BalancedSolver();
		// else if (FeatureFlag.USE_SINGLE_PRECISION.isEnabled())
		// solver = new DenseFloatMatrixFactory();
		else
			solver = new DenseSolver();
		return solver;
	}

	/**
	 * Returns the version of the openLCA application. If there is a version
	 * defined in the ini-file (-olcaVersion argument) this is returned.
	 * Otherwise the version of the application bundle is returned.
	 */
	public static String getVersion() {
		String version = CommandArgument.VERSION.getValue();
		if (version != null)
			return version;
		return RcpActivator.getDefault().getBundle().getVersion().toString();
	}

	/**
	 * Indicates if the application runs in developer mode (for activation of
	 * experimental features and development tools).
	 */
	public static boolean runsInDevMode() {
		String val = CommandArgument.DEV_MODE.getValue();
		if (val == null)
			return false;
		return val.equals("true");
	}

	public static EventBus getEventBus() {
		return eventBus;
	}

	public static void openEditor(CategorizedEntity model) {
		BaseDescriptor descriptor = Descriptors.toDescriptor(model);
		openEditor(descriptor);
	}

	public static void openEditor(BaseDescriptor modelDescriptor) {
		if (modelDescriptor == null) {
			log.error("model is null, could not open editor");
			return;
		}
		log.trace("open editor for {} ", modelDescriptor);
		String editorId = getEditorId(modelDescriptor.getModelType());
		if (editorId == null)
			log.error("could not find editor for model {}", modelDescriptor);
		else {
			ModelEditorInput input = new ModelEditorInput(modelDescriptor);
			Editors.open(input, editorId);
		}
	}

	public static void closeEditor(CategorizedEntity entity) {
		BaseDescriptor descriptor = Descriptors.toDescriptor(entity);
		closeEditor(descriptor);
	}

	public static void closeEditor(BaseDescriptor descriptor) {
		if (descriptor == null) {
			log.error("model is null, could not close editor");
			return;
		}
		for (IEditorReference ref : Editors.getReferences())
			try {
				if (new ModelEditorInput(descriptor).equals(ref
						.getEditorInput())) {
					Editors.close(ref);
					break;
				}
			} catch (PartInitException e) {
				log.error("Error closing editor", e);
			}
	}

	private static String getEditorId(ModelType type) {
		if (type == null)
			return null;
		return "editors." + type.getModelClass().getSimpleName().toLowerCase();
	}

	public static void runInUI(String name, Runnable runnable) {
		WrappedUIJob job = new WrappedUIJob(name, runnable);
		job.setUser(true);
		job.schedule();
	}

	/**
	 * Wraps a runnable in a job and executes it using the Eclipse jobs
	 * framework. No UI access is allowed for the runnable.
	 */
	public static void run(String name, Runnable runnable) {
		run(name, runnable, null);
	}

	/**
	 * See {@link App#run(String, Runnable)}. Additionally, this method allows
	 * to give a callback which is executed in the UI thread when the runnable
	 * is finished.
	 */
	public static void run(String name, Runnable runnable, Runnable callback) {
		WrappedJob job = new WrappedJob(name, runnable);
		if (callback != null)
			job.setCallback(callback);
		job.setUser(true);
		job.schedule();
	}

	public static void runWithProgress(String name, Runnable runnable) {
		IProgressService progress = PlatformUI.getWorkbench()
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

	/**
	 * Returns the workspace directory where databases and other resources are
	 * stored (native libraries, HTML resources, etc.).
	 */
	public static File getWorkspace() {
		return Workspace.getDir();
	}
}
