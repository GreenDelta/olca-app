package org.openlca.app.collaboration.navigation.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog.GitCredentialsProvider;
import org.openlca.app.collaboration.util.CredentialStore;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.collaboration.views.CompareView;
import org.openlca.app.collaboration.views.HistoryView;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.collaboration.model.WebRequestException;
import org.openlca.git.Compatibility.UnsupportedClientVersionException;
import org.openlca.git.actions.GitProgressAction;
import org.openlca.git.actions.GitRemoteAction;
import org.openlca.git.actions.GitStashApply;
import org.openlca.git.util.Constants;

class Actions {

	static void refresh() {
		Navigator.refresh();
		HistoryView.refresh();
		CompareView.clear();
	}

	static void handleException(String message, Exception e) {
		handleException(message, null, e);
	}

	static void handleException(String message, String url, Exception e) {
		if (e instanceof UnsupportedClientVersionException) {
			message = "The repository was created by a newer openLCA client, please download the latest openLCA version to proceed.";
			MsgBox.error(message);
			return;
		}
		if (e instanceof WebRequestException we) {
			WebRequests.handleException(message, we);
		} else {
			WebRequests.handleException(message, new WebRequestException(url, e));
		}
	}

	static <T> T run(GitCredentialsProvider credentials, GitRemoteAction<T> runnable)
			throws InvocationTargetException, InterruptedException, GitAPIException {
		runnable.authorizeWith(credentials);
		var service = PlatformUI.getWorkbench().getProgressService();
		var runner = new GitRemoteRunner<>(runnable);
		service.run(true, false, runner::run);
		var repo = org.openlca.app.db.Repository.CURRENT;
		if (runner.exception == null)
			return runner.result;
		if (!(runner.exception instanceof TransportException))
			throw runner.exception;
		var m = runner.exception.getMessage();
		var notAuthorized = m.endsWith("not authorized");
		var tokenRequired = m.endsWith("400 null");
		var passwordMissing = m.endsWith("424 null");
		var notPermitted = m.contains("not permitted on");
		if (!tokenRequired) {
			CredentialStore.clearPassword(repo.serverUrl, repo.user());
		}
		if (notPermitted) {
			throw new TransportException(M.NoSufficientRights);
		}
		if (passwordMissing) {
			throw new TransportException(
					"We have updated our password encryption. Since we only store encrypted passwords, we are not able to migrate your current password. Please use the 'Forgot your password?' link on the website to request a new password being sent to your email address.");
		}
		if (!notAuthorized && !tokenRequired)
			throw runner.exception;
		if (notAuthorized) {
			credentials = repo.promptCredentials();
		} else if (tokenRequired) {
			credentials = repo.promptToken();
		}
		if (credentials == null)
			return null;
		return run(credentials, runnable);
	}

	static <T> T run(GitProgressAction<T> runnable)
			throws InvocationTargetException, InterruptedException, GitAPIException, IOException {
		return run(runnable, false);
	}

	static <T> T runWithCancel(GitProgressAction<T> runnable)
			throws InvocationTargetException, InterruptedException, GitAPIException, IOException {
		return run(runnable, true);
	}

	static <T> T run(GitProgressAction<T> runnable, boolean cancelable)
			throws InvocationTargetException, InterruptedException, GitAPIException, IOException {
		var service = PlatformUI.getWorkbench().getProgressService();
		var runner = new GitProgressRunner<>(runnable);
		service.run(true, cancelable, runner::run);
		if (runner.exception != null)
			if (runner.exception instanceof GitAPIException e)
				throw e;
			else if (runner.exception instanceof IOException e)
				throw e;
		return runner.result;
	}

	static void askApplyStash() throws InvocationTargetException, GitAPIException, IOException, InterruptedException {
		var answers = new String[] { M.No, M.Yes };
		var result = Question.ask(M.ApplyStashedChanges,
				M.ApplyStashedChangesQuestion,
				answers);
		if (result == 0)
			return;
		applyStash();
	}

	static boolean applyStash() throws GitAPIException, InvocationTargetException, IOException, InterruptedException {
		var repo = org.openlca.app.db.Repository.CURRENT;
		var libraryResolver = WorkspaceLibraryResolver.forStash();
		if (libraryResolver == null)
			return false;
		var conflictResult = ConflictResolver.resolve(Constants.STASH_REF);
		if (conflictResult == null)
			return false;
		Actions.run(GitStashApply.on(repo)
				.resolveConflictsWith(conflictResult.resolutions())
				.resolveLibrariesWith(libraryResolver));
		return true;
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

				@Override
				public void showDuration(boolean arg0) {

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
					monitor.subTask("");
				}

				@Override
				public void subTask(String name) {
					monitor.subTask(name);
				}

				@Override
				public void worked(int work) {
					monitor.worked(work);
				}

				@Override
				public boolean isCanceled() {
					return monitor.isCanceled();
				}

			};
		}

	}

}
