package org.openlca.app.collaboration.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.collaboration.client.CSClient;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Datasets {

	private static final Logger log = LoggerFactory.getLogger(Datasets.class);

	public static void download(CSClient client, String repositoryId, String type, String refId) {
		App.runWithProgress(M.DownloadingData, () -> {
			File tmp = null;
			ZipStore store = null;
			try {
				tmp = Files.createTempFile("cs-json-", ".zip").toFile();
				var jsonStream = WebRequests.execute(() -> client.downloadJson(repositoryId, type, refId));
				if (jsonStream == null)
					return;
				Files.copy(jsonStream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
				store = ZipStore.open(tmp);
				var jsonImport = new JsonImport(store, Database.get());
				jsonImport.run();
			} catch (IOException ex) {
				log.error("Error during json import", ex);
			} finally {
				if (store != null) {
					try {
						store.close();
					} catch (IOException ex) {
						log.error("Error closing store", ex);
					}
				}
				if (tmp != null) {
					tmp.delete();
				}
			}
		}, Navigator::refresh);

	}

}
