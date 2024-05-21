package org.openlca.app.tools.soda;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Popup;
import org.openlca.ilcd.descriptors.DataStock;
import org.openlca.ilcd.io.SodaClient;

class StockDownload {

	private final SodaClient client;
	private final DataStock stock;

	private StockDownload(SodaClient client, DataStock stock) {
		this.client = client;
		this.stock = stock;
	}

	static void run(SodaClient client, DataStock stock) {
		if (client == null || stock == null)
			return;
		new StockDownload(client, stock).run();
	}

	private void run() {

		var file = FileChooser.forSavingFile(
				"Download data stock as zip file",
				stock.getShortName() + ".zip");
		if (file == null)
			return;

		var err = new AtomicBoolean(false);
		App.run("Downloading data stock", () -> {
			try (var stream = client.exportDataStock(stock.getUUID())) {
				Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				ErrorReporter.on(e.getMessage());
				err.set(true);
			}
		}, () -> {
			if (!err.get()) {
				Popup.info("Downloaded data stock");
			}
		});
	}
}
