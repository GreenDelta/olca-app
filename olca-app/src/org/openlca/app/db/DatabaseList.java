package org.openlca.app.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.collections.ListUtils;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The list of registered databases which is stored in a configuration file.
 */
public class DatabaseList {

    private List<DerbyConfiguration> localDatabases = new ArrayList<>();
    private List<PostgresConfiguration> remoteDatabases = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public List<IDatabaseConfiguration> getDatabases() {
      return ListUtils.union(localDatabases, remoteDatabases);
    }

	public static DatabaseList read(File file) {
		Logger log = LoggerFactory.getLogger(DatabaseList.class);
		log.info("read database configurations from {}", file);
		try (FileInputStream in = new FileInputStream(file);
				Reader reader = new InputStreamReader(in, "utf-8")) {
			Gson gson = new Gson();
			return gson.fromJson(reader, DatabaseList.class);
		} catch (Exception e) {
			log.error("failed to read database configurations from " + file, e);
			return new DatabaseList();
		}
	}

	public void write(File file) {
		Logger log = LoggerFactory.getLogger(DatabaseList.class);
		log.info("write database configurations to {}", file);
		try (FileOutputStream out = new FileOutputStream(file);
				Writer writer = new OutputStreamWriter(out, "utf-8")) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String s = gson.toJson(this);
			writer.write(s);
		} catch (Exception e) {
			log.error("failed to write database configuration to " + file, e);
		}
	}

	public boolean contains(IDatabaseConfiguration config) {
		return localDatabases.contains(config) || remoteDatabases.contains(config);
	}

	/** Returns true if a database with the given name exists. */
	public boolean nameExists(String name) {
		if (Strings.nullOrEmpty(name))
			return false;
		String newName = name.trim().toLowerCase();
		Predicate<IDatabaseConfiguration> sameName = config -> {
			if (config == null || config.getName() == null)
				return false;
			return Strings.nullOrEqual(
					config.getName().toLowerCase(), newName);
		};
		for (IDatabaseConfiguration config : getDatabases()) {
			if (sameName.test(config))
				return true;
		}
		return false;
	}

  public void add(IDatabaseConfiguration config) {
    if (config instanceof DerbyConfiguration) {
      localDatabases.add((DerbyConfiguration)config);
    } else if (config instanceof PostgresConfiguration) {
      remoteDatabases.add((PostgresConfiguration)config);
    }
  }

  public void remove(IDatabaseConfiguration config) {
    if (config instanceof DerbyConfiguration) {
      localDatabases.remove(config);
    } else if (config instanceof PostgresConfiguration) {
      remoteDatabases.remove(config);
    }
  }

}
