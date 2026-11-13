package refdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.openlca.util.Dirs;

class RefData {

	private static final int MAX_DOWNLOAD_ATTEMPTS = 4;

	enum Set {
		UNITS,
		FLOWS,
		NONE;

		List<String> files() {
			return switch (this) {
				case NONE -> List.of();
				case UNITS -> List.of(
					"currencies.csv",
					"flow_properties.csv",
					"unit_groups.csv",
					"units.csv"
				);
				case FLOWS -> List.of(
					"currencies.csv",
					"flow_properties.csv",
					"flows.csv",
					"unit_groups.csv",
					"units.csv",

					"mappings/EcoSpold1_Export_BAFU.csv",
					"mappings/EcoSpold1_Import_BAFU.csv",
					"mappings/EcoSpold1_Import_Default.csv",
					"mappings/ILCD_Import.csv"
				);
			};
		}
	}

	/**
	 * Creates a temporary import directory with files defined in the given
	 * reference data set.
	 */
	static Optional<File> createTempImportDir(Set refSet) {

		File tempDir;
		try {
			tempDir = Files.createTempDirectory(
				"_ref_data_build_" + refSet.name()).toFile();
			System.out.println("created import folder: " + tempDir);
		} catch (Exception e) {
			System.out.println("ERROR: failed to create import folder: "
				+ e.getMessage());
			return Optional.empty();
		}

		try {
			for (var file : refSet.files()) {
				var cached = fetchRefDataFile(file).orElse(null);
				if (cached == null)
					return Optional.empty();
				var target = new File(tempDir, file);
				Dirs.createIfAbsent(target.getParentFile());
				Files.copy(cached.toPath(), target.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			}
			return Optional.of(tempDir);
		} catch (Exception e) {
			System.out.println("ERROR: failed to copy resources to "
				+ tempDir + ": " + e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Fetches a reference data file from the GreenDelta/data repository. The
	 * file is locally cached in the `target` folder of this project. If it
	 * already exists there, it is not downloaded again. So in order to update
	 * a reference data file, first clear that folder.
	 *
	 * @param name the name of the file in the `refdata` folder.
	 * @return the file or an empty option if it failed to fetch the file.
	 */
	static Optional<File> fetchRefDataFile(String name) {

		var target = new File("target/" + name);
		if (target.exists())
			return Optional.of(target);

		try {
			Dirs.createIfAbsent(target.getParentFile());
		} catch (Exception e) {
			System.out.println("ERROR: failed to create download folder of: "
				+ name + e.getMessage());
			return Optional.empty();
		}

		var path = "https://raw.githubusercontent.com/GreenDelta/data" +
			"/master/refdata/" + name;
		var temp = new File(target.getParentFile(), target.getName() + ".part");
		for (int attempt = 1; attempt <= MAX_DOWNLOAD_ATTEMPTS; attempt++) {
			System.out.println("download: " + path
				+ " (attempt " + attempt + "/" + MAX_DOWNLOAD_ATTEMPTS + ")");
			try {
				var url = new URI(path).toURL();
				try (var input = url.openStream()) {
					Files.copy(input, temp.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
				}
				// only move complete downloads into the cache
				Files.move(temp.toPath(), target.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
				return Optional.of(target);
			} catch (FileNotFoundException e) {
				// HTTP 404; retrying will not help
				System.out.println("ERROR: file not found: " + path);
				break;
			} catch (Exception e) {
				System.out.println("WARNING: failed to download: "
					+ path + ": " + e.getMessage());
				if (attempt < MAX_DOWNLOAD_ATTEMPTS) {
					try {
						Thread.sleep(2000L * attempt);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}

		temp.delete();
		System.out.println("ERROR: failed to download: " + path);
		return Optional.empty();
	}

}
