package org.openlca.app.collaboration.navigation.actions;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.collaboration.Repository;
import org.openlca.app.collaboration.util.WebRequests;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Question;
import org.openlca.collaboration.model.LibraryInfo;
import org.openlca.collaboration.model.WebRequestException;
import org.openlca.core.database.IDatabase.DataPackage;
import org.openlca.core.library.LibraryPackage;
import org.openlca.util.Strings;

class Libraries {

	static void uploadTo(Repository repo) {
		if (!repo.isCollaborationServer())
			return;
		var libraries = repo.getInfo().dataPackages().stream()
				.filter(DataPackage::isLibrary)
				.collect(Collectors.toList());
		if (libraries.isEmpty())
			return;
		var serverLibraries = new HashSet<String>();
		App.runWithProgress(M.CheckingLibraries, () -> {
			WebRequests.execute(repo.client::listLibraries).stream()
					.map(LibraryInfo::name)
					.forEach(serverLibraries::add);
		});
		var toUpload = libraries.stream()
				.map(DataPackage::name)
				.filter(Predicate.not(serverLibraries::contains))
				.collect(Collectors.toList());
		var libraryDir = Workspace.getLibraryDir();
		if (toUpload.isEmpty())
			return;
		if (!Question.ask(M.UploadLibrariesTitle, M.UploadLibrariesMessage + "\n\n" + Strings.join(toUpload, '\n')))
			return;
		App.runWithProgress(M.UploadingLibraries, () -> {
			toUpload.forEach(id -> {
				try {
					var tmpFile = Files.createTempFile("olca-library", ".zip");
					LibraryPackage.zip(libraryDir.getLibrary(id).get(), tmpFile.toFile());
					try (var stream = new FileInputStream(tmpFile.toFile())) {
						repo.client.uploadLibrary(stream);
					}
					Files.delete(tmpFile);
				} catch (IOException e) {
					ErrorReporter.on("Error creating library zip file", e);
				} catch (WebRequestException e) {
					WebRequests.handleException("Error uploading library " + id, e);
				}
			});
		});
	}

}
