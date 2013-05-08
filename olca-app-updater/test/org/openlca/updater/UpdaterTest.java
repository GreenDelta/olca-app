package org.openlca.updater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openlca.updater.Updater.UnzipRequest;

public class UpdaterTest {

	private File tempDir;
	private Updater updater;
	private File dirToDel;

	@Before
	public void setUp() throws Exception {
		tempDir = File.createTempFile("testtmpfile", "d");
		tempDir.delete();
		tempDir.mkdir();

		updater = new Updater(null,
				Arrays.asList(new UnzipRequest[] { new UnzipRequest(tempDir
						.getAbsolutePath(), createTempInputZip(), 1) }),
				new ArrayList<String>(), new ArrayList<String>());
	}

	private String createTempInputZip() throws Exception {
		File tempFile = File.createTempFile("tempinput", "f");
		try (InputStream resourceAsStream = getClass().getResourceAsStream(
				"/testinput.zip");
				FileOutputStream outputStream = new FileOutputStream(tempFile)) {
			Utils.copy(resourceAsStream, outputStream);
		}
		return tempFile.getAbsolutePath();
	}

	@After
	public void tearDown() throws Exception {
		try {
			if (updater != null && tempDir != null) {
				updater.delDir(tempDir);
			}
		} catch (Exception e) {
			log("Deletion failed somehow", e);
		}

		try {
			if (updater != null && dirToDel != null && dirToDel.exists()) {
				updater.delDir(dirToDel);
			}
		} catch (Exception e) {
			log("Deletion failed somehow", e);
		}
	}

	private void log(String string, Exception e) {
		System.err.println(string + (string.endsWith(" ") ? "" : " ") + e);
		e.printStackTrace(System.err);
	}

	@Test
	public void testRunNormallyWithSimpleDeletionsWithBlanks() throws Exception {
		dirToDel = new File(tempDir, "dirTo Del");
		assertTrue(dirToDel.mkdirs());
		File fileToDel = new File(tempDir, "fileTo Del.f");
		fileToDel.deleteOnExit();
		assertTrue(fileToDel.createNewFile());

		updater.getPathsToDelete().add(dirToDel.getAbsolutePath());
		updater.getPathsToDelete().add(fileToDel.getAbsolutePath());

		updater.runInThisJVM();

		assertFalse(fileToDel.exists());
		assertFalse(dirToDel.exists());
	}

	@Test
	public void testDeleteDeepDirs() throws Exception {
		dirToDel = new File(tempDir, "dirTo Del");
		assertTrue(dirToDel.mkdirs());
		File subDir = new File(dirToDel, "subDir");
		assertTrue(subDir.mkdir());
		File deepFileToDel = new File(subDir, "fileToDel.f");
		assertTrue(deepFileToDel.createNewFile());

		updater.getPathsToDelete().add(dirToDel.getAbsolutePath());

		updater.runInThisJVM();

		assertFalse(deepFileToDel.exists());
		assertFalse(dirToDel.exists());
	}

	@Test
	public void testStripLeadingParts() throws Exception {
		assertEquals("", updater.stripLeadingParts("", 0));
		assertEquals("asdf/asf", updater.stripLeadingParts("asdf/asf", 0));
		assertEquals("asf", updater.stripLeadingParts("asdf/asf", 1));
		assertEquals(null, updater.stripLeadingParts("asdf/asf", 2));
	}

	@Test
	public void testRunNormallyWithNoDeletions() throws Exception {
		updater.runInThisJVM();

		File contentTestFile = new File(tempDir, "dirfd/klsddkf.txt");
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(
				contentTestFile))) {
			String readLine = bufferedReader.readLine();
			assertTrue(readLine.startsWith("fdasdf"));
		}
	}

	@Test
	public void testRunWithDeletesOfNonExisting() throws Exception {
		dirToDel = new File(tempDir, "dirTo Del");
		File fileToDel = new File(tempDir, "fileTo Del.f");
		assertFalse(fileToDel.exists());
		assertFalse(dirToDel.exists());

		updater.getPathsToDelete().add(dirToDel.getAbsolutePath());
		updater.getPathsToDelete().add(fileToDel.getAbsolutePath());

		updater.runInThisJVM();
	}

}
