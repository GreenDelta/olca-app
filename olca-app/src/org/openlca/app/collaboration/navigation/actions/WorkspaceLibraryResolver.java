package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.wizards.io.LibraryDialog;
import org.openlca.core.database.IDatabase.DataPackage;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.git.actions.LibraryResolver;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;

class WorkspaceLibraryResolver implements LibraryResolver {

	private final Repository repo;
	private final LibraryDir libDir;

	private WorkspaceLibraryResolver(Repository repo) {
		this.repo = repo;
		this.libDir = Workspace.getLibraryDir();
	}

	static WorkspaceLibraryResolver forRemote(Repository repo) {
		var commit = repo.commits.get(repo.commits.resolve(Constants.REMOTE_BRANCH));
		return forCommit(repo, commit);
	}

	static WorkspaceLibraryResolver forStash(Repository repo) throws GitAPIException {
		var commits = Git.wrap(repo).stashList().call();
		if (commits == null || commits.isEmpty())
			return null;
		var commit = new Commit(commits.iterator().next());
		return forCommit(repo, commit);
	}

	static WorkspaceLibraryResolver forCommit(Repository repo, Commit commit) {
		if (commit == null)
			return null;
		var resolver = new WorkspaceLibraryResolver(repo);
		if (!resolver.init(repo, commit))
			return null;
		return resolver;
	}
	
	// init before resolve is called in GitMerge, to avoid invalid thread access
	private boolean init(Repository repo, Commit commit) {
		var remoteLibs = repo.getDataPackages(commit).stream()
				.filter(DataPackage::isLibrary)
				.collect(Collectors.toList());
		var localLibs = Database.get().getDataPackages().getLibraries();
		for (var newLib : remoteLibs) {
			if (localLibs.contains(newLib.name()))
				continue;
			if (preresolve(newLib) == null)
				return false;
		}
		return true;
	}

	@Override
	public Library resolve(String newLib) {
		return libDir.getLibrary(newLib).orElse(null);
	}

	private Library preresolve(DataPackage newLib) {
		try {
			var lib = resolve(newLib.name());
			if (lib != null)
				return lib;
			lib = importFromCollaborationServer(newLib.name());
			if (lib != null)
				return lib;
			var dialog = new LibraryDialog(newLib);
			if (dialog.open() != LibraryDialog.OK)
				return null;
			if (dialog.isFileSelected())
				return App.exec(M.ExtractingLibrary + " - " + newLib,
						() -> Libraries.importFromFile(new File(dialog.getLocation())));
			return App.exec(M.DownloadingAndExtractingLibrary + " - " + newLib,
					() -> Libraries.importFromUrl(dialog.getLocation()));
		} catch (IOException e) {
			return null;
		}
	}

	private Library importFromCollaborationServer(String newLib) throws IOException {
		if (!repo.isCollaborationServer())
			return null;
		var stream = WebRequests.execute(
				() -> repo.client.downloadLibrary(newLib));
		if (stream == null)
			return null;
		return App.exec(M.DownloadingAndExtractingLibrary + " - " + newLib,
				() -> Libraries.importFromStream(stream));
	}

}
