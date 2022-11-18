package refdata;

import org.openlca.util.Dirs;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

class RefData {

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
						"locations.csv",
						"unit_groups.csv",
						"units.csv",

						"mappings/EcoSpold1_Import.csv",
						"mappings/EcoSpold2_Export.csv",
						"mappings/EcoSpold2_Import.csv",
						"mappings/ILCD_Import.csv",
						"mappings/SimaPro_Import.csv"
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
	private static Optional<File> fetchRefDataFile(String name) {
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
		System.out.println("download: " + path);
		try {
			var url = new URL(path);
			try (var input = url.openStream()) {
				Files.copy(input, target.toPath());
			}
			return Optional.of(target);
		} catch (Exception e) {
			System.out.println(
					"ERROR: failed to download: " + path + e.getMessage());
			return Optional.empty();
		}
	}

}
