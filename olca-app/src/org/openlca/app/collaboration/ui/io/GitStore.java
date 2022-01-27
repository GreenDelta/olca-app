package org.openlca.app.collaboration.ui.io;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.openlca.core.model.ModelType;
import org.openlca.git.find.Datasets;
import org.openlca.git.find.Entries;
import org.openlca.git.find.References;
import org.openlca.git.model.Reference;
import org.openlca.git.util.GitUtil;
import org.openlca.jsonld.EntityStore;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class GitStore implements EntityStore {

	private static final Logger log = LoggerFactory.getLogger(GitStore.class);
	private static final Gson gson = new Gson();
	private static final JsonObject defaultContext;
	private final References references;
	private final Datasets datasets;
	private final String headCommitId;
	private final String remoteCommitId;
	private final Categories categories;

	static {
		defaultContext = new JsonObject();
		defaultContext.addProperty("@vocab", "http://openlca.org/schema/v1.1/");
	}

	public GitStore(FileRepository repo, String headCommitId, String remoteCommitId) {
		this.categories = new Categories(new Entries(repo), remoteCommitId);
		this.references = new References(repo);
		this.datasets = new Datasets(repo);
		this.headCommitId = headCommitId;
		this.remoteCommitId = remoteCommitId;
	}

	@Override
	public boolean contains(ModelType type, String refId) {
		if (type == ModelType.CATEGORY)
			return true;
		return references.get(type, refId, remoteCommitId) != null;
	}

	@Override
	public byte[] get(String path) {
		var binDir = GitUtil.findBinDir(path);
		if (binDir == null && !path.endsWith(GitUtil.DATASET_SUFFIX))
			return categories.getForPath(path);
		if (binDir == null)
			return getDataset(path);
		return getBinary(binDir, path);
	}

	private byte[] getDataset(String path) {
		var ref = getRef(path);
		return datasets.getBytes(ref.objectId);
	}

	private Reference getRef(String path) {
		var refs = references.find().path(path).commit(remoteCommitId).all();
		if (refs.isEmpty())
			return null;
		if (refs.size() > 1)
			throw new IllegalArgumentException("Ambigious path, returned more then 1 reference");
		return refs.get(0);
	}

	private byte[] getBinary(String binDir, String path) {
		var i = binDir.lastIndexOf(GitUtil.BIN_DIR_SUFFIX);
		var refPath = binDir.substring(0, i) + GitUtil.DATASET_SUFFIX;
		var filepath = path.substring(i + GitUtil.BIN_DIR_SUFFIX.length() + 1);
		var ref = getRef(refPath);
		return datasets.getBinary(ref, filepath);
	}

	@Override
	public JsonObject get(ModelType type, String refId) {
		if (type == ModelType.CATEGORY)
			return categories.getForRefId(refId);
		var ref = references.get(type, refId, remoteCommitId);
		if (ref == null)
			return null;
		var data = datasets.get(ref.objectId);
		return parse(data);
	}

	@Override
	public List<String> getBinFiles(ModelType type, String refId) {
		if (type == ModelType.CATEGORY)
			return Collections.emptyList();
		var ref = references.get(type, refId, remoteCommitId);
		if (ref == null)
			return Collections.emptyList();
		return references.getBinaries(ref).stream()
				.map(binary -> ref.getBinariesPath() + "/" + binary)
				.toList();
	}

	@Override
	public List<String> getRefIds(ModelType type) {
		return getChanges(type).stream()
				.map(r -> r.refId)
				.toList();
	}

	public List<Reference> getChanges(ModelType type) {
		if (type == ModelType.CATEGORY)
			return Collections.emptyList();
		return references.find()
				.type(type)
				.commit(remoteCommitId)
				.changedSince(headCommitId)
				.all();
	}

	@Override
	public JsonObject getContext() {
		var data = get("context.json");
		if (data == null)
			return defaultContext;
		try {
			return parse(new String(data, "utf-8"));
		} catch (Exception e) {
			log.error("Error reading context.json", e);
			return defaultContext;
		}
	}

	private JsonObject parse(String data) {
		if (Strings.nullOrEmpty(data))
			return null;
		return gson.fromJson(data, JsonObject.class);
	}

	@Override
	public void put(ModelType type, JsonObject model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void put(String path, byte[] data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putBin(ModelType type, String refId, String filename, byte[] data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putMetaInfo(JsonObject model) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws IOException {

	}

}
