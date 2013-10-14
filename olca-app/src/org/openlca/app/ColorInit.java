package org.openlca.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Initialization of the colors for the charts in the application. */
class ColorInit {

	private Logger log = LoggerFactory.getLogger(getClass());

	public void run() {
		try {
			File workspace = Platform.getLocation().toFile();
			File colorDir = new File(workspace, "colors");
			if (!colorDir.exists())
				colorDir.mkdirs();
			log.trace("Initialize colors from {}", colorDir);
			initChartColors(colorDir);
		} catch (Exception e) {
			log.error("Failed to initialize colors", e);
		}
	}

	private void initChartColors(File colorDir) {
		File file = new File(colorDir, "chart_colors.txt");
		RGB[] defaultColors = FaviColor.getChartColors();
		FaviColor.setChartColors(sync(defaultColors, file));

	}

	private RGB[] sync(RGB[] defaultColors, File file) {
		if (!file.exists()) {
			writeFile(file, defaultColors);
			return defaultColors;
		}
		RGB[] fromFile = readFile(file);
		if (fromFile == null || fromFile.length == 0)
			return defaultColors;
		return syncColors(fromFile, defaultColors);
	}

	private RGB[] syncColors(RGB[] fromFile, RGB[] defaultColors) {
		int maxLength = Math.max(fromFile.length, defaultColors.length);
		RGB[] syncColors = new RGB[maxLength];
		for (int i = 0; i < maxLength; i++) {
			if (fromFile.length > i)
				syncColors[i] = fromFile[i];
			if (syncColors[i] == null && defaultColors.length > i)
				syncColors[i] = defaultColors[i];
		}
		return syncColors;
	}

	private void writeFile(File file, RGB[] chartColors) {
		log.trace("Write color defaults to file {}", file);
		try (FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
				BufferedWriter buffer = new BufferedWriter(writer)) {
			for (RGB rgb : chartColors) {
				buffer.write(FaviColor.toHex(rgb));
				buffer.newLine();
			}
		} catch (Exception e) {
			log.error("Failed to write color-defaults", e);
		}
	}

	private RGB[] readFile(File file) {
		log.trace("Read color defaults from file {}", file);
		try (FileInputStream fis = new FileInputStream(file);
				InputStreamReader reader = new InputStreamReader(fis, "UTF-8");
				BufferedReader buffer = new BufferedReader(reader)) {
			String line = null;
			List<RGB> rgbs = new ArrayList<>();
			while ((line = buffer.readLine()) != null) {
				String hex = line.trim();
				if (!hex.isEmpty())
					rgbs.add(FaviColor.fromHex(hex));
			}
			return rgbs.toArray(new RGB[rgbs.size()]);
		} catch (Exception e) {
			log.error("Failed to read color file ", e);
			return new RGB[0];
		}
	}

}
