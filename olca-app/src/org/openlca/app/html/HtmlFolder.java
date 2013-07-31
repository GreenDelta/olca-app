package org.openlca.app.html;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openlca.app.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlFolder {

	private static final Logger log = LoggerFactory.getLogger(HtmlFolder.class);
	private static final File htmlDirectory = new File(App.getWorkspace(),
			"html");
	private static final File versionFile = new File(htmlDirectory,
			"version.info");

	private static Set<String> baseFiles;

	static {
		try {
			initializeBaseFiles();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void initializeBaseFiles() throws IOException {
		boolean upToDate = readVersionInformation();
		if (!upToDate || !allBaseFilesExist()) {
			if (!upToDate) {
				FileUtils.deleteDirectory(htmlDirectory);
			}
			if (!htmlDirectory.exists())
				htmlDirectory.mkdirs();
			File zipFile = new File(htmlDirectory, "@temp.zip");
			try (FileOutputStream out = new FileOutputStream(zipFile)) {
				IOUtils.copy(
						HtmlFolder.class.getResourceAsStream("html_base.zip"),
						out);
				unzip(zipFile, htmlDirectory, !upToDate);
				if (!zipFile.delete())
					zipFile.deleteOnExit();
				if (!upToDate) {
					writeVersionInformation();
				}
			}
		}
	}

	private static boolean readVersionInformation() throws IOException {
		if (baseFiles == null) {
			baseFiles = new HashSet<>();
			if (!htmlDirectory.exists())
				htmlDirectory.mkdirs();
			if (!versionFile.exists())
				return false;
			try (BufferedReader reader = new BufferedReader(new FileReader(
					versionFile))) {
				String line = reader.readLine();
				if (line == null || !line.contains("Version:"))
					return false;
				else if (!line.substring(line.indexOf(":") + 1).trim()
						.equals(App.getVersion()))
					return false;
				else
					while ((line = reader.readLine()) != null)
						baseFiles.add(line);
			}
		}
		return true;
	}

	private static boolean allBaseFilesExist() {
		for (String file : baseFiles)
			if (!new File(htmlDirectory, file).exists())
				return false;
		return true;
	}

	private static void unzip(File zipFile, File baseDir, boolean hashFileNames)
			throws IOException {
		try (ZipFile zip = new ZipFile(zipFile)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File file = new File(baseDir, entry.getName());
				if (hashFileNames)
					baseFiles.add(entry.getName());
				if (!file.exists()) {
					if (entry.isDirectory()) {
						file.mkdirs();
						continue;
					}
					file.getParentFile().mkdirs();
					try (FileOutputStream fos = new FileOutputStream(file)) {
						IOUtils.copy(zip.getInputStream(entry), fos);
					}
				}
			}
		}
	}

	private static void writeVersionInformation() throws IOException {
		if (!versionFile.exists())
			versionFile.createNewFile();
		try (BufferedWriter versionWriter = new BufferedWriter(new FileWriter(
				versionFile))) {
			versionWriter.write("Version: " + App.getVersion());
			versionWriter.newLine();
			for (String fileName : baseFiles) {
				versionWriter.write(fileName);
				versionWriter.newLine();
			}
		}
	}

	public static String getUrl(IHtmlResource resource) throws IOException {
		initializeBaseFiles();
		register(resource);

		log.trace("get html page {}", resource.getFileName());
		File file = new File(htmlDirectory, resource.getBundleName()
				+ File.separator + resource.getBundleVersion() + File.separator
				+ resource.getFileName());
		URL url = file.toURI().toURL();
		String s = url.toString();
		log.trace("resolved it to {}", s);
		return s;
	}

	public static void register(IHtmlResource resource) throws IOException {
		if (!exists(resource))
			extract(resource);
	}

	private static boolean exists(IHtmlResource resource) {
		File bundleDirectory = new File(htmlDirectory, resource.getBundleName());
		if (!bundleDirectory.exists())
			return false;
		File versionDirectory = new File(bundleDirectory,
				resource.getBundleVersion());
		if (!versionDirectory.exists())
			return false;
		File file = new File(versionDirectory, resource.getFileName());
		if (!file.exists())
			return false;
		return true;
	}

	private static void extract(IHtmlResource resource) throws IOException {
		String path = getPath(resource);
		File file = new File(htmlDirectory, path);
		file.getParentFile().mkdirs();
		file.createNewFile();

		try (InputStream is = resource.openStream();
				OutputStream os = new FileOutputStream(file)) {
			IOUtils.copy(is, os);
		}
	}

	private static String getPath(IHtmlResource resource) {
		return resource.getBundleName() + File.separator
				+ resource.getBundleVersion() + File.separator
				+ resource.getFileName();
	}

}
