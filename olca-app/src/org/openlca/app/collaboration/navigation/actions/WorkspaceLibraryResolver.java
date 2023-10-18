package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.wizards.io.LibraryDialog;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.git.actions.LibraryResolver;
import org.openlca.git.find.Commits;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Repositories;
import org.openlca.jsonld.LibraryLink;

class WorkspaceLibraryResolver implements LibraryResolver {

	private final LibraryDir libDir;

	private WorkspaceLibraryResolver() {
		this.libDir = Workspace.getLibraryDir();
	}

	static WorkspaceLibraryResolver forRemote() {
		var git = org.openlca.app.db.Repository.get().git;
		var commits = Commits.of(git);
		var commit = commits.get(commits.resolve(Constants.REMOTE_BRANCH));
		if (commit == null)
			return null;
		var resolver = new WorkspaceLibraryResolver();
		if (!resolver.init(git, commit))
			return null;
		return resolver;
	}

	static WorkspaceLibraryResolver forStash() throws GitAPIException {
		var git = org.openlca.app.db.Repository.get().git;
		var commits = Git.wrap(git).stashList().call();
		if (commits == null || commits.isEmpty())
			return null;
		var commit = new Commit(commits.iterator().next());
		var resolver = new WorkspaceLibraryResolver();
		if (!resolver.init(git, commit))
			return null;
		return resolver;
	}

	// init before resolve is called in GitMerge, to avoid invalid thread access
	private boolean init(Repository git, Commit commit) {
		var info = Repositories.infoOf(git, commit);
		if (info == null)
			return false;
		var remoteLibs = info.libraries();
		var localLibs = Database.get().getLibraries();
		for (var newLib : remoteLibs) {
			if (localLibs.contains(newLib.id()))
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

	private Library preresolve(LibraryLink newLib) {
		try {
			var lib = resolve(newLib.id());
			if (lib != null)
				return lib;
			lib = importFromCollaborationServer(newLib);
			if (lib != null)
				return lib;
			var dialog = new LibraryDialog(newLib);
			if (dialog.open() != LibraryDialog.OK)
				return null;
			if (dialog.isFileSelected())
				return App.exec("Extracting library " + newLib.id(),
						() -> Libraries.importFromFile(new File(dialog.getLocation())));
			return App.exec("Downloading and extracting library " + newLib.id(),
					() -> Libraries.importFromUrl(dialog.getLocation()));
		} catch (IOException e) {
			return null;
		}
	}

	private Library importFromCollaborationServer(LibraryLink newLib) throws IOException {
		var repo = org.openlca.app.db.Repository.get();
		if (!repo.isCollaborationServer())
			return null;
		var stream = repo.client.downloadLibrary(newLib.id());
		if (stream == null)
			return null;
		return App.exec("Downloading and extracting library " + newLib.id(),
				() -> Libraries.importFromStream(stream));
	}

}
