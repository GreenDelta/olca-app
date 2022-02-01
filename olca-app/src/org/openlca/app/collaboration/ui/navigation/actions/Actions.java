package org.openlca.app.collaboration.ui.navigation.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.collaboration.ui.views.CompareView;
import org.openlca.app.collaboration.ui.views.HistoryView;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Actions {

	private static final Logger log = LoggerFactory.getLogger(Actions.class);

	static void refresh() {
		Navigator.refresh();
		HistoryView.refresh();
		CompareView.clear();
	}

	static void handleException(String message, Exception e) {
		log.error(message, e);
		MsgBox.error(e.getMessage());
	}

	static UsernamePasswordCredentialsProvider credentialsProvider() {
		var c = Repository.get().config.credentials;
		if (c == null || Strings.nullOrEmpty(c.username()) || Strings.nullOrEmpty(c.password()))
			return null;
		return new UsernamePasswordCredentialsProvider(c.username(), c.password());
	}

	static ProgressMonitor progressMonitor(IProgressMonitor monitor) {
		return new ProgressMonitor() {

			private int remaining;

			@Override
			public void update(int completed) {
				monitor.worked(completed);
			}

			@Override
			public void start(int totalTasks) {
				remaining += totalTasks;
			}

			@Override
			public boolean isCancelled() {
				return monitor.isCanceled();
			}

			@Override
			public void endTask() {
				remaining--;
				if (remaining == 0) {
					monitor.done();
				}
			}

			@Override
			public void beginTask(String title, int totalWork) {
				monitor.beginTask(title, totalWork);
			}
		};
	}

	static <T> T runWithProgress(GitRunnable<T> runnable) {
		try {
			var service = PlatformUI.getWorkbench().getProgressService();
			var runner = new ProgressRunner<>(runnable);
			service.run(true, false, runner::run);
			return runner.result;
		} catch (InvocationTargetException | InterruptedException e) {
			handleException("Error running git action", e);
			return null;
		}
	}

	private static class ProgressRunner<T> implements IRunnableWithProgress {

		private final GitRunnable<T> runnable;
		private T result;

		private ProgressRunner(GitRunnable<T> runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				result = runnable.run(monitor);
			} catch (GitAPIException e) {
				handleException("Error running git action", e);
			}
		}

	}

	interface GitRunnable<T> {

		T run(IProgressMonitor monitor) throws GitAPIException;

	}

}
