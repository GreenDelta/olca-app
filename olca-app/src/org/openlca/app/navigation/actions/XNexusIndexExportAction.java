package org.openlca.app.navigation.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.Version;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.CategoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Exports an file with the process meta-data of the currently activated
 * database for the search index in openLCA Nexus (http://nexus.openlca.org).
 */
public class XNexusIndexExportAction extends Action implements
		INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());

	public XNexusIndexExportAction() {
		setImageDescriptor(ImageType.EXTENSION_ICON.getDescriptor());
		setText("Export Nexus Index");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		return Database.isActive(e.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		IDatabase db = Database.get();
		if (db == null) {
			log.trace("no database activated");
			return;
		}
		String defaultName = db.getName() + "_nexus_index.json";
		File file = FileChooser.forExport("*.json", defaultName);
		if (file == null)
			return;
		App.run("Export Nexus index", new Runner(file, db));
	}

	private class Runner implements Runnable {

		private File file;
		private IDatabase db;

		public Runner(File file, IDatabase db) {
			this.file = file;
			this.db = db;
		}

		@Override
		public void run() {
			log.trace("run Nexus index export");
			try {
				List<IndexEntry> entries = new ArrayList<>();
				ProcessDao dao = new ProcessDao(db);
				for (ProcessDescriptor descriptor : dao.getDescriptors()) {
					log.trace("index process {}", descriptor);
					Process process = dao.getForId(descriptor.getId());
					IndexEntry entry = new IndexEntry(process);
					entries.add(entry);
				}
				writeEntries(entries);
			} catch (Exception e) {
				log.error("failed to write index entries", e);
			}
		}

		private void writeEntries(List<IndexEntry> entries) throws Exception {
			log.trace("write {} entries to file {}", entries.size(), file);
			try (FileOutputStream out = new FileOutputStream(file);
					OutputStreamWriter writer = new OutputStreamWriter(out,
							"utf-8");
					BufferedWriter buffer = new BufferedWriter(writer)) {

				Gson gson = new GsonBuilder().setDateFormat(
						"yyyy-MM-dd'T'HH:mm:ssZ").create();
				gson.toJson(entries, buffer);
			}
		}
	}

	@SuppressWarnings("unused")
	private class IndexEntry {

		private String id;
		private String name;
		private String categoryPath;
		private String version;
		private String description;
		private String technology;
		private String location;
		private String owner;
		private String documentor;
		private String generator;
		private String reviewer;
		private Date created;
		private Date validityTimeStart;
		private Date validityTimeEnd;

		private IndexEntry(org.openlca.core.model.Process process) {
			id = process.getRefId();
			name = process.getName();
			categoryPath = CategoryPath.getFull(process.getCategory());
			description = process.getDescription();
			version = Version.asString(process.getVersion());
			if (process.getLocation() != null)
				location = process.getLocation().getCode();
			ProcessDocumentation doc = process.getDocumentation();
			if (doc != null) {
				writeDocValues(doc);
			}
		}

		private void writeDocValues(ProcessDocumentation doc) {
			technology = doc.getTechnology();
			if (doc.getDataSetOwner() != null)
				owner = doc.getDataSetOwner().getName();
			if (doc.getDataGenerator() != null)
				generator = doc.getDataGenerator().getName();
			if (doc.getReviewer() != null)
				reviewer = doc.getReviewer().getName();
			if (doc.getDataDocumentor() != null)
				documentor = doc.getDataDocumentor().getName();
			created = doc.getCreationDate();
			validityTimeStart = doc.getValidFrom();
			validityTimeEnd = doc.getValidUntil();
		}
	}
}
