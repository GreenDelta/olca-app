package org.openlca.app.tools.soda;

import org.openlca.ilcd.descriptors.DataStock;
import org.openlca.ilcd.io.SodaClient;

import java.util.List;

record Connection(
		SodaClient client,
		List<DataStock> stocks,
		String url,
		String user,
		boolean hasEpds,
		String error
) {

	static Connection error(String error) {
		return new Connection(null, null, null, null, false, error);
	}

	boolean hasError() {
		return error != null;
	}

	@Override
	public String toString() {
		return user + "@" + url;
	}

}
