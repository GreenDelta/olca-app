package org.openlca.updater;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.openlca.updater.Updater.UnzipRequest;

public class MainTest {

	private ArrayList<UnzipRequest> toUnzip;
	private ArrayList<String> toDelete;
	private ArrayList<String> toIgnore;

	@Before
	public void setUp() throws Exception {
		toUnzip = new ArrayList<>();
		toDelete = new ArrayList<>();
		toIgnore = new ArrayList<>();
	}

	@Test
	public void shouldWorkWithSingleUnzipReq() {
		assertEquals(
				"exec",
				Main.parseArguments(new String[] { "exec", "-unzip", "azip",
						"adir", "2" }, toUnzip, toDelete, toIgnore));
		assertEquals(new UnzipRequest("adir", "azip", 2), toUnzip.get(0));
		assertEquals(0, toDelete.size());
		assertEquals(0, toIgnore.size());
	}

	@Test
	public void shouldWorkWithMultipleUnzipReq() {
		assertEquals(
				"exec",
				Main.parseArguments(new String[] { "exec", "-unzip", "azip",
						"adir", "2", "azip2", "adir2", "0", "azip3", "adir3",
						"1" }, toUnzip, toDelete, toIgnore));
		assertEquals(new UnzipRequest("adir", "azip", 2), toUnzip.get(0));
		assertEquals(new UnzipRequest("adir2", "azip2", 0), toUnzip.get(1));
		assertEquals(new UnzipRequest("adir3", "azip3", 1), toUnzip.get(2));
		assertEquals(0, toDelete.size());
		assertEquals(0, toIgnore.size());
	}

	@Test
	public void shouldWorkWithAllKinds() {
		assertEquals("exec", Main.parseArguments(new String[] { "exec",
				"-unzip", "azip", "adir", "2", "azip2", "adir2", "0", "-del",
				"del1", "del2", "-ign", "ign1", "ign2" }, toUnzip, toDelete,
				toIgnore));
		assertEquals(new UnzipRequest("adir", "azip", 2), toUnzip.get(0));
		assertEquals(new UnzipRequest("adir2", "azip2", 0), toUnzip.get(1));
		assertEquals(Arrays.asList(new String[] { "del1", "del2" }), toDelete);
		assertEquals(Arrays.asList(new String[] { "ign1", "ign2" }), toIgnore);
	}

	@Test
	public void shouldWorkWithRepeatedArgs() {

		assertEquals("exec", Main.parseArguments(new String[] { "exec",
				"-unzip", "azip", "adir", "2", "azip2", "adir2", "0", "-del",
				"del1", "-ign", "ign1", "-ign", "ign2", "-del", "del2" },
				toUnzip, toDelete, toIgnore));
		assertEquals(new UnzipRequest("adir", "azip", 2), toUnzip.get(0));
		assertEquals(new UnzipRequest("adir2", "azip2", 0), toUnzip.get(1));
		assertEquals(Arrays.asList(new String[] { "del1", "del2" }), toDelete);
		assertEquals(Arrays.asList(new String[] { "ign1", "ign2" }), toIgnore);
	}
}
