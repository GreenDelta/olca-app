package org.openlca.plugin.installer;

import static org.junit.Assert.*;

import org.junit.Test;

public class PluginsServiceTest {

	@Test
	public void isNewerShouldWork() {
		assertFalse(PluginsService.isNewer(null, null));
		assertFalse(PluginsService.isNewer("", ""));
		assertFalse(PluginsService.isNewer("", null));
		assertFalse(PluginsService.isNewer(null, ""));
		assertFalse(PluginsService.isNewer("1", ""));
		assertFalse(PluginsService.isNewer("1", "1"));
		assertFalse(PluginsService.isNewer("1.1", "1.1"));
		assertFalse(PluginsService.isNewer("1.1", "1.2"));
		assertFalse(PluginsService.isNewer("1.1.snapshot", "1.1.snapshot"));
		assertFalse(PluginsService.isNewer("1.1.2", "1.1.snapshot"));

		assertTrue(PluginsService.isNewer("2.1", "1.2"));
		assertTrue(PluginsService.isNewer("2", "1.2"));
		assertTrue(PluginsService.isNewer("1.2.1", "1.2"));
		assertTrue(PluginsService.isNewer("1.2.1", "1.2"));
		assertTrue(PluginsService.isNewer("1.2", "1.1.snapshot"));
		assertTrue(PluginsService.isNewer("1.2.snapshot", "1.1.snapshot"));
	}
}
