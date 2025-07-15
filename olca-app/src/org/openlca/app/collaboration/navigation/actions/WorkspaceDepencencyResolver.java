package org.openlca.app.collaboration.navigation.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.dialogs.AuthenticationDialog;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.wizards.io.LibraryDialog;
import org.openlca.core.database.DataPackage;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.git.actions.DependencyResolver;
import org.openlca.git.actions.GitFetch;
import org.openlca.git.model.Commit;
import org.openlca.git.repo.ClientRepository;
import org.openlca.git.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkspaceDepencencyResolver implements DependencyResolver {

	private static final Logger log = LoggerFactory.getLogger(WorkspaceDepencencyResolver.class);
	private final Map<DataPackage, IResolvedDependency<?>> resolved = new HashMap<>();
	private final Repository repo;
	private final LibraryDir libDir;

	private WorkspaceDepencencyResolver(Repository repo) {
		this.repo = repo;
		this.libDir = Workspace.getLibraryDir();
	}

	static WorkspaceDepencencyResolver forRemote(Repository repo) {
		var commit = repo.commits.get(repo.commits.resolve(Constants.REMOTE_BRANCH));
		return forCommit(repo, commit);
	}

	static WorkspaceDepencencyResolver forStash(Repository repo) throws GitAPIException {
		var commits = Git.wrap(repo).stashList().call();
		if (commits == null || commits.isEmpty())
			return null;
		var commit = new Commit(commits.iterator().next());
		return forCommit(repo, commit);
	}

	static WorkspaceDepencencyResolver forCommit(Repository repo, Commit commit) {
		if (commit == null)
			return null;
		var resolver = new WorkspaceDepencencyResolver(repo);
		if (!resolver.init(repo, commit))
			return null;
		return resolver;
	}

	// init before resolve is called in GitMerge, to avoid invalid thread access
	private boolean init(Repository repo, Commit commit) {
		var remotePackages = repo.getDataPackages(commit).stream()
				.collect(Collectors.toList());
		var localPackages = Database.get().getDataPackages();
		for (var newPackage : remotePackages) {
			if (localPackages.contains(newPackage.name()))
				continue;
			var resolved = resolve(newPackage);
			if (resolved == null)
				return false;
		}
		return true;
	}

	@Override
	public IResolvedDependency<?> resolve(DataPackage dataPackage) {
		if (resolved.containsKey(dataPackage))
			return resolved.get(dataPackage);
		if (dataPackage.isLibrary()) {
			var lib = resolveLibrary(dataPackage);
			if (lib == null)
				return null;
			var dependency = IResolvedDependency.library(dataPackage, lib);
			resolved.put(dataPackage, dependency);
			return dependency;
		}
		if (dataPackage.isRepository()) {
			var current = Repository.get();
			if (current != null && current.url.equals(dataPackage.url()))
				// allow cyclic references by skipping own repository
				return IResolvedDependency.repository(dataPackage, null);
			var repo = resolveRepository(dataPackage);
			if (repo == null)
				return null;
			var dependency = IResolvedDependency.repository(dataPackage, repo);
			resolved.put(dataPackage, dependency);
			var dataPackages = repo.getDataPackages();
			for (var subDependency : dataPackages) {
				var resolved = resolve(subDependency);
				if (resolved == null)
					return null;
			}
			return dependency;
		}
		return null;
	}

	private Library resolveLibrary(DataPackage newPackage) {
		try {
			var library = libDir.getLibrary(newPackage.name());
			if (library.isPresent())
				return library.get();
			var lib = importFromCollaborationServer(newPackage.name());
			if (lib != null)
				return lib;
			var dialog = new LibraryDialog(newPackage);
			if (dialog.open() != LibraryDialog.OK)
				return null;
			if (dialog.isFileSelected())
				return App.exec(M.ExtractingLibrary + " - " + newPackage.name(),
						() -> Libraries.importFromFile(new File(dialog.getLocation())));
			return App.exec(M.DownloadingAndExtractingLibrary + " - " + newPackage.name(),
					() -> Libraries.importFromUrl(dialog.getLocation()));
		} catch (IOException e) {
			log.error("Error resolving library " + newPackage.name(), e);
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

	private ClientRepository resolveRepository(DataPackage newPackage) {
		var repo = Repository.get(newPackage);
		if (repo != null)
			return repo;
		repo = Repository.initialize(Database.get(), newPackage, newPackage.url());
		if (repo == null)
			return null;
		var credentials = AuthenticationDialog.promptCredentials(repo.serverUrl);
		if (credentials == null)
			return null;
		repo.user(credentials.user);
		try {
			Actions.run(repo, credentials, GitFetch.to(repo));
		} catch (InvocationTargetException | InterruptedException | GitAPIException e) {
			log.error("Error resolving library " + newPackage.name(), e);
			return null;
		}
		return repo;
	}
}
