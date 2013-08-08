/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.database;

import java.io.File;
import java.io.InputStream;

import org.openlca.util.OS;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MySQL server application.
 * 
 * @author Michael Srocka
 */
public class ServerApp {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final String SEP = System.getProperty("file.separator");
	private final String ADMIN_EXE = SEP + "bin" + SEP + "mysqladmin.exe";
	private final String SERVER_EXE = SEP + "bin" + SEP + "mysqld-nt.exe";

	private String appDir;
	private String dataDir;
	private int port;

	public ServerApp(File appDir, File dataDir, int port) throws Exception {
		checkArgs(appDir);
		log.trace("Init MySQL Server App");
		log.trace("App - directory {}", appDir);
		log.trace("Data directory {}", dataDir);
		log.trace("Can write in data dir.: {}", dataDir.canWrite());
		this.appDir = appDir.getAbsolutePath();
		this.dataDir = dataDir.getAbsolutePath();
		this.port = port;
	}

	private void checkArgs(File appDir) {
		File admin = new File(appDir, ADMIN_EXE);
		File server = new File(appDir, SERVER_EXE);
		if (!admin.exists() || !server.exists()) {
			throw new IllegalArgumentException("The folder " + appDir
					+ " is not a MySQL application directory.");
		}
	}

	public void shutdown() throws Exception {
		assertWindows();
		log.info("Shutdown server via {}", exe(ADMIN_EXE));
		String[] command = new String[] { exe(ADMIN_EXE), "-u", "root",
				"shutdown", "--port=" + port };
		ProcessBuilder builder = new ProcessBuilder(command);
		String response = startProcess(builder);
		log.info("Server response: {}", response);
	}

	private String exe(String exe) {
		return appDir + exe;
	}

	public boolean ping() throws Exception {
		log.trace("Ping server via {}", exe(ADMIN_EXE));
		boolean ping = false;
		String[] command = new String[] { exe(ADMIN_EXE), "ping",
				"--port=" + port };
		ProcessBuilder builder = new ProcessBuilder(command);
		String response = startProcess(builder);
		log.trace("Server response: {}", response);
		ping = response.equalsIgnoreCase("mysqld is alive");
		return ping;
	}

	public void start() throws Exception {
		assertWindows();
		if (ping())
			return; // already alive
		log.info("Start MySQL Server {}.", exe(SERVER_EXE));
		log.info("Using data directory {}", dataDir);
		String[] command = new String[] { exe(SERVER_EXE), "--port=" + port,
				"--datadir=" + dataDir, "--max_connections=200" };
		ProcessBuilder builder = new ProcessBuilder(command);
		String response = startProcess(builder);
		log.info("Server response: {}", response);
		waitForServer();
	}

	private void waitForServer() throws Exception {
		int c = 0;
		boolean b = ping();
		while (!b && c < 10) {
			try {
				log.trace("Wait for server: {} second(s)", c);
				Thread.sleep(1000);
			} catch (Exception e) {
				log.error("Waiting for server failed", e);
			}
			b = ping();
			c++;
		}
	}

	private String startProcess(ProcessBuilder builder) throws Exception {
		builder.redirectErrorStream(true);
		Process process = builder.start();
		String[] result = null;
		try (InputStream is = process.getInputStream()) {
			result = Strings.readLines(is);
		}
		String res = null;
		if (result != null && result.length > 0) {
			res = result[0];
		}
		return res == null ? "" : res;
	}

	private void assertWindows() throws Exception {
		OS os = OS.getCurrent();
		if (os != OS.Windows)
			throw new Exception(
					"Start and shut-down of MySQL server currently not supported "
							+ "on non-Windows platforms.");
	}
}
