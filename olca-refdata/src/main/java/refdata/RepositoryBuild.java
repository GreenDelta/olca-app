package refdata;

import static org.openlca.core.model.ModelType.CURRENCY;
import static org.openlca.core.model.ModelType.DQ_SYSTEM;
import static org.openlca.core.model.ModelType.FLOW;
import static org.openlca.core.model.ModelType.FLOW_PROPERTY;
import static org.openlca.core.model.ModelType.LOCATION;
import static org.openlca.core.model.ModelType.UNIT_GROUP;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openlca.cloud.api.RepositoryClient;
import org.openlca.cloud.api.RepositoryConfig;
import org.openlca.cloud.model.data.FetchRequestData;
import org.openlca.cloud.model.data.FileReference;
import org.openlca.core.database.Derby;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.input.JsonImport;

public class RepositoryBuild {

	private static final String URL = "http://cloud.greendelta.com/ws";
	private static final String REPO = "refdata/refdata";

	public static void main(String[] args) throws Exception {
		System.out.println("  Create database from refdata repository");
		Util.clean();
		buildDb("empty");
		buildDb("units", FLOW_PROPERTY, UNIT_GROUP);
		buildDb("flows", FLOW, FLOW_PROPERTY, UNIT_GROUP, LOCATION, CURRENCY, DQ_SYSTEM);
		Util.zip();
		Util.copyToApp();
		System.out.println("  done");
	}

	private static void buildDb(String name, ModelType... modelTypes) throws Exception {
		System.out.println("  building " + name + " database");
		var db = new Derby(F("build/" + name));
		if (modelTypes != null && modelTypes.length > 0) {
			RepositoryClient client = createClient(db);
			Set<FileReference> descriptors = loadDescriptors(client, modelTypes);
			File data = downloadData(db, client, descriptors);
			Files.move(data.toPath(), F("download/" + name + ".zip").toPath());
		}
		db.close();
	}

	private static RepositoryClient createClient(IDatabase db) {
		RepositoryConfig config = new RepositoryConfig(db, URL, REPO);
		return new RepositoryClient(config);
	}

	private static Set<FileReference> loadDescriptors(RepositoryClient client, ModelType... modelTypes)
			throws Exception {
		if (modelTypes == null || modelTypes.length == 0)
			return new HashSet<>();
		List<ModelType> types = Arrays.asList(modelTypes);
		Set<FetchRequestData> all = client.list();
		Set<FileReference> filtered = new HashSet<>();
		for (FetchRequestData data : all) {
			if (!types.contains(data.type))
				continue;
			filtered.add(createRef(data));
		}
		return filtered;
	}

	private static FileReference createRef(FetchRequestData data) {
		FileReference ref = new FileReference();
		ref.type = data.type;
		ref.refId = data.refId;
		return ref;
	}

	private static File downloadData(IDatabase db, RepositoryClient client, Set<FileReference> descriptors)
			throws Exception {
		File tmp = client.downloadJson(descriptors);
		ZipStore store = ZipStore.open(tmp);
		JsonImport jsonImport = new JsonImport(store, db);
		jsonImport.run();
		store.close();
		return tmp;
	}

	private static File F(String path) {
		return Util.F(path);
	}

}
