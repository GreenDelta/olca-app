/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.core.database.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

import org.openlca.core.database.ConnectionData;

/**
 * A class for running a SQL scripts with insert, update, delete, and DDL
 * statements on a database connection.
 */
public class ScriptRunner implements ScriptHandler {

	private Statement statement;
	private ConnectionData data;

	public ScriptRunner(ConnectionData data) {
		this.data = data;
	}

	public void run(InputStream scriptStream, String encoding)
			throws ScriptException {
		try (Connection connection = data.createDatabaseConnection()) {
			statement = connection.createStatement();
			InputStreamReader reader = new InputStreamReader(scriptStream,
					encoding);
			ScriptParser parser = new ScriptParser(this);
			parser.parse(reader);
			statement.close();
		} catch (Exception e) {
			throw new ScriptException("Cannot execute script: "
					+ e.getMessage(), e);
		}
	}

	@Override
	public void statement(String query) throws ScriptException {
		try {
			statement.executeUpdate(query);
		} catch (Exception e) {
			throw new ScriptException("Cannot execute statement: " + query, e);
		}
	}
}
