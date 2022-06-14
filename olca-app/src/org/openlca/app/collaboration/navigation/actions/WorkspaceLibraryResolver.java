package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.openlca.app.App;
import org.openlca.app.collaboration.dialogs.LibraryDialog;
import org.openlca.app.collaboration.util.WebRequests.WebRequestException;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.MsgBox;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.core.library.LibraryPackage;
import org.openlca.git.actions.LibraryResolver;
import org.openlca.git.find.Commits;
import org.openlca.git.model.Commit;
import org.openlca.git.util.Constants;
import org.openlca.git.util.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkspaceLibraryResolver implements LibraryResolver {

	private static final Logger log = LoggerFactory.getLogger(WorkspaceLibraryResolver.class);
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
			if (localLibs.contains(newLib))
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

	private Library preresolve(String newLib) {
		try {
			var lib = resolve(newLib);
			if (lib != null)
				return lib;
			lib = importFromCollaborationServer(newLib, libDir);
			if (lib != null)
				return lib;
			var dialog = new LibraryDialog(newLib);
			if (dialog.open() != LibraryDialog.OK)
				return null;
			if (dialog.isFileSelected())
				return App.exec("Extracting library " + newLib,
						() -> importFromFile(new File(dialog.getLocation()), libDir));
			try (var stream = new URL(dialog.getLocation()).openStream()) {
				return App.exec("Downloading and extracting library " + newLib,
						() -> importFromStream(stream, libDir));
			}
		} catch (IOException e) {
			return null;
		}
	}

	private Library importFromCollaborationServer(String newLib, LibraryDir libDir) throws IOException {
		var repo = org.openlca.app.db.Repository.get();
		if (!repo.isCollaborationServer())
			return null;
		try {
			var stream = repo.client.downloadLibrary(newLib);
			if (stream == null)
				return null;
			return App.exec("Downloading and extracting library " + newLib,
					() -> importFromStream(stream, libDir));
		} catch (WebRequestException e) {
			Actions.handleException("Error downloading library " + newLib, e);
			return null;
		}
	}

	private Library importFromFile(File file, LibraryDir libDir) {
		if (file == null)
			return null;
		var info = LibraryPackage.getInfo(file);
		if (info == null) {
			MsgBox.error(file.getName() + " is not a valid library package.");
			return null;
		}
		LibraryPackage.unzip(file, libDir);
		return resolve(info.name());
	}

	private Library importFromStream(InputStream stream, LibraryDir libDir) {
		var file = (Path) null;
		var library = (Library) null;
		try {
			file = Files.createTempFile("olca-library", ".zip");
			Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
			library = importFromFile(file.toFile(), libDir);
			return library;
		} catch (IOException e) {
			log.error("Error copying library from stream", e);
			return null;
		} finally {
			if (file != null && file.toFile().exists()) {
				try {
					Files.delete(file);
				} catch (IOException e) {
					log.trace("Error deleting tmp file", e);
					return library;
				}
			}
		}
	}

}
