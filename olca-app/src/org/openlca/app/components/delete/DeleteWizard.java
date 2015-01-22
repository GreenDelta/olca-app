package org.openlca.app.components.delete;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.Messages;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard for displaying warnings and errors while deleting an object
 * 
 * @see ProblemWizard
 * 
 */
public class DeleteWizard<T extends BaseDescriptor> extends ProblemWizard {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final T model;
	private IUseSearch<T> search;

	public DeleteWizard(IUseSearch<T> search, T model) {
		this.model = model;
		this.search = search;
		initializeProblems();
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.Delete);
	}

	private void initializeProblems() {
		if (search == null || model == null)
			return;
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new Runner());
		} catch (final Exception e) {
			log.error("Initialize delete wizard failed", e);
		}
	}

	private class Runner implements IRunnableWithProgress {

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.AnalyzingForProblems,
					IProgressMonitor.UNKNOWN);
			List<Problem> problems = new ArrayList<>();
			for (BaseDescriptor descriptor : search.findUses(model))
				createProblem(problems, descriptor);
			setProblems(problems.toArray(new Problem[problems.size()]));
		}

		private void createProblem(List<Problem> problems,
				BaseDescriptor descriptor) {
			Problem problem = new Problem(Problem.ERROR, getMessage(model,
					descriptor)) {
				@Override
				public void solve() {
				}
			};
			problems.add(problem);
		}

		private String getMessage(T used, BaseDescriptor user) {
			String s1 = used.getName();
			String s2 = user.getModelType().getModelClass().getSimpleName()
					+ " " + user.getName();
			return s1 + " is used by " + s2;
		}
	}

}
