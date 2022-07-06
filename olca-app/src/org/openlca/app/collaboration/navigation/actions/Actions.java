package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.git.actions.GitProgressAction;
import org.openlca.git.actions.GitRemoteAction;
import org.openlca.git.actions.GitStashApply;
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

	static <T> T run(GitCredentialsProvider credentials, GitRemoteAction<T> runnable)
			throws InvocationTargetException, InterruptedException, GitAPIException {
		runnable.authorizeWith(credentials);
		var service = PlatformUI.getWorkbench().getProgressService();
		var runner = new GitRemoteRunner<>(runnable);
		service.run(true, false, runner::run);
		var repo = org.openlca.app.db.Repository.get();
		if (runner.exception == null) {
			if (Strings.nullOrEmpty(credentials.token)) {
				repo.useTwoFactorAuth(false);
			}
			return runner.result;
		}
		if (!(runner.exception instanceof TransportException))
			throw runner.exception;
		var m = runner.exception.getMessage();
		var notAuthorized = m.endsWith("not authorized");
		var tokenRequired = m.endsWith("400 null");
		var notPermitted = m.contains("not permitted on");
		if (notPermitted) {
			if (Strings.nullOrEmpty(credentials.token)) {
				repo.useTwoFactorAuth(false);
			}
			repo.invalidateCredentials();
			throw new TransportException("You do not have sufficient access to this repository");
		}
		if (!notAuthorized && !tokenRequired)
			throw runner.exception;
		if (notAuthorized) {
			repo.invalidateCredentials();
			credentials = AuthenticationDialog.forcePromptCredentials(repo);
		} else if (tokenRequired) {
			repo.useTwoFactorAuth(true);
			credentials = AuthenticationDialog.promptToken(repo);
		}
		if (credentials == null)
			return null;
		return run(credentials, runnable);
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

	static Commit getStashCommit(Repository git) throws GitAPIException {
		var commits = Git.wrap(git).stashList().call();
		if (commits == null || commits.isEmpty())
			return null;
		return new Commit(commits.iterator().next());
	}

	static void askApplyStash() throws InvocationTargetException, GitAPIException, IOException, InterruptedException {
		var answers = Arrays.asList("No", "Yes");
		var result = Question.ask("Apply stashed changes",
				"Do you want to apply the changes you stashed before the commit?",
				answers.toArray(new String[answers.size()]));
		if (result == 0)
			return;
		applyStash();
	}

	static void applyStash() throws GitAPIException, InvocationTargetException, IOException, InterruptedException {
		var repo = org.openlca.app.db.Repository.get();
		var libraryResolver = WorkspaceLibraryResolver.forStash();
		if (libraryResolver == null)
			return;
		var conflictResult = ConflictResolutionMap.forStash();
		if (conflictResult == null)
			return;
		Actions.run(GitStashApply.from(repo.git)
				.to(Database.get())
				.update(repo.workspaceIds)
				.resolveConflictsWith(conflictResult.resolutions())
				.resolveLibrariesWith(libraryResolver));
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
