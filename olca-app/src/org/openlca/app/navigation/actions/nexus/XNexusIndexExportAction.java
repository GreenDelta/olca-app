package org.openlca.app.navigation.actions.nexus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;
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
public class XNexusIndexExportAction extends Action implements INavigationAction {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public XNexusIndexExportAction() {
		setImageDescriptor(Icon.EXTENSION.descriptor());
		setText("Export Nexus JSON Index");
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement))
			return false;
		var e = (DatabaseElement) first;
		return Database.isActive(e.getContent());
	}

	@Override
	public void run() {
		IDatabase db = Database.get();
		if (db == null) {
			log.trace("no database activated");
			return;
		}
		String defaultName = db.getName() + "_nexus_index.json";
		File file = FileChooser.forSavingFile(M.Export, defaultName);
		if (file == null)
			return;
		App.run("Export Nexus JSON index", new Runner(file, db));
	}

	private class Runner implements Runnable {

		private final File file;
		private final IDatabase db;

		public Runner(File file, IDatabase db) {
			this.file = file;
			this.db = db;
		}

		@Override
		public void run() {
			log.trace("run nexus index export");
			try {
				List<IndexEntry> entries = new ArrayList<>();
				ProcessDao dao = new ProcessDao(db);
				for (ProcessDescriptor descriptor : dao.getDescriptors()) {
					log.trace("index process {}", descriptor);
					Process process = dao.getForId(descriptor.id);
					entries.add(new IndexEntry(process));
				}
				IndexEntry.writeEntries(entries, file);
			} catch (Exception e) {
				log.error("failed to write index entries", e);
			}
		}

	}

	@SuppressWarnings("unused")
	static class IndexEntry {

		private String id;
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
		String name;
		Set<String> systemModel = new HashSet<>();

		IndexEntry(org.openlca.core.model.Process process) {
			id = process.refId;
			name = process.name;
			categoryPath = CategoryPath.getFull(process.category);
			description = process.description;
			version = Version.asString(process.version);
			if (process.location != null)
				location = process.location.code;
			ProcessDocumentation doc = process.documentation;
			if (doc != null) {
				writeDocValues(doc);
			}
		}

		private void writeDocValues(ProcessDocumentation doc) {
			technology = doc.technology;
			if (doc.dataSetOwner != null)
				owner = doc.dataSetOwner.name;
			if (doc.dataGenerator != null)
				generator = doc.dataGenerator.name;
			if (doc.reviewer != null)
				reviewer = doc.reviewer.name;
			if (doc.dataDocumentor != null)
				documentor = doc.dataDocumentor.name;
			created = doc.creationDate;
			validityTimeStart = doc.validFrom;
			validityTimeEnd = doc.validUntil;
		}

		static void writeEntries(Collection<IndexEntry> entries, File file) throws Exception {
			try (FileOutputStream out = new FileOutputStream(file);
					OutputStreamWriter writer = new OutputStreamWriter(out, "utf-8");
					BufferedWriter buffer = new BufferedWriter(writer)) {
				Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
				gson.toJson(entries, buffer);
			}
		}

	}
}
