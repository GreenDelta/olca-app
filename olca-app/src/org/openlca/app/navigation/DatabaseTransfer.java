package org.openlca.app.navigation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.openlca.app.db.Database;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.database.config.MySqlConfig;
import org.openlca.jsonld.Json;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A simple drag-and-drop transfer type for database
 * configurations.
 */
class DatabaseTransfer extends ByteArrayTransfer {

	private static final String NAME = "database_config_transfer";
	private static final int ID = registerType(NAME);

	@Override
	protected int[] getTypeIds() {
		return new int[]{ID};
	}

	@Override
	protected String[] getTypeNames() {
		return new String[]{NAME};
	}

	@Override
	protected void javaToNative(Object object, TransferData data) {
		if (!validate(object) || !isSupportedType(data))
			return;
		var config = (DatabaseConfig) object;
		var obj = new JsonObject();
		Json.put(obj, "name", config.name());
		Json.put(obj, "remote", config instanceof MySqlConfig);
		var bytes = new Gson().toJson(obj)
				.getBytes(StandardCharsets.UTF_8);
		super.javaToNative(bytes, data);
	}

	@Override
	protected Object nativeToJava(TransferData data) {
		if (!isSupportedType(data))
			return null;
		if (!(super.nativeToJava(data) instanceof byte[] bytes))
			return null;
		try {
			var json = new String(bytes, StandardCharsets.UTF_8);
			var obj = new Gson().fromJson(json, JsonObject.class);
			var name = Json.getString(obj, "name");
			var isRemote = Json.getBool(obj, "remote", false);
			var configs = isRemote
					? Database.getConfigurations().getMySqlConfigs()
					: Database.getConfigurations().getDerbyConfigs();
			for (var config : configs) {
				if (Objects.equals(config.name(), name))
					return config;
			}
			return null;
		} catch (Exception e) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("failed to read db-config from dnd-transfer", e);
			return null;
		}
	}

	@Override
	protected boolean validate(Object object) {
		if (!(object instanceof DatabaseConfig config))
			return false;
		return Strings.notEmpty(config.name());
	}
}
