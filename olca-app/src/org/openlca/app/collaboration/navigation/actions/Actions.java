package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.git.actions.GitProgressAction;
import org.openlca.git.actions.GitRemoteAction;
import org.openlca.git.model.Commit;
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
		var c = Repository.promptCredentials();
		if (c == null || Strings.nullOrEmpty(c.username()) || Strings.nullOrEmpty(c.password()))
			return null;
		var token = c.token();
		var password = c.password();
		if (token != null) {
			password += "&token=" + token;
		}
		return new UsernamePasswordCredentialsProvider(c.username(), password);
	}

	static <T> T run(GitRemoteAction<T> runnable)
			throws InvocationTargetException, InterruptedException, GitAPIException {
		var service = PlatformUI.getWorkbench().getProgressService();
		var runner = new GitRemoteRunner<>(runnable);
		service.run(true, false, runner::run);
		if (runner.exception != null)
			throw runner.exception;
		return runner.result;
	}

	static <T> T run(GitProgressAction<T> runnable)
			throws InvocationTargetException, InterruptedException, GitAPIException, IOException {
		var service = PlatformUI.getWorkbench().getProgressService();
		var runner = new GitProgressRunner<>(runnable);
		service.run(true, false, runner::run);
		if (runner.exception != null)
			if (runner.exception instanceof GitAPIException e)
				throw e;
			else if (runner.exception instanceof IOException e)
				throw e;
		return runner.result;
	}

	static Commit getStashCommit(FileRepository git) throws GitAPIException {
		var commits = Git.wrap(git).stashList().call();
		if (commits == null || commits.isEmpty())
			return null;
		return new Commit(commits.iterator().next());
	}

	private static class GitRemoteRunner<T> implements IRunnableWithProgress {

		private final GitRemoteAction<T> runnable;
		private T result;
		private GitAPIException exception;

		private GitRemoteRunner(GitRemoteAction<T> runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				runnable.withProgress(wrapped(monitor));
				result = runnable.run();
			} catch (GitAPIException e) {
				exception = e;
			}
		}

		private static ProgressMonitor wrapped(IProgressMonitor monitor) {
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

	}

	private static class GitProgressRunner<T> implements IRunnableWithProgress {

		private final GitProgressAction<T> runnable;
		private T result;
		private Exception exception;

		private GitProgressRunner(GitProgressAction<T> runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				runnable.withProgress(wrapped(monitor));
				result = runnable.run();
			} catch (GitAPIException | IOException e) {
				exception = e;
			}
		}

		private static org.openlca.git.util.ProgressMonitor wrapped(IProgressMonitor monitor) {
			return new org.openlca.git.util.ProgressMonitor() {

				@Override
				public void beginTask(String name, int totalWork) {
					monitor.beginTask(name, totalWork);
				}

				@Override
				public void subTask(String name) {
					monitor.subTask(name);
				}

				@Override
				public void worked(int work) {
					monitor.worked(work);
				}

			};
		}

	}

}
