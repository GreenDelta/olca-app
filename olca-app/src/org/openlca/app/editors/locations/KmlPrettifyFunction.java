package org.openlca.app.editors.locations;

import java.util.function.Consumer;

import org.openlca.app.editors.processes.kml.KmlUtil;
import org.openlca.util.Strings;

class KmlPrettifyFunction {

	private Consumer<Boolean> validHandler;

	public KmlPrettifyFunction(Consumer<Boolean> validHandler) {
		this.validHandler = validHandler;
	}

	public String prettifyKML(String kml) {
		if (kml == null || kml.isEmpty()) {
			if (validHandler != null)
				validHandler.accept(true);
			return null;
		}
		try {
			String result = KmlUtil.prettyFormat(kml);
			if (validHandler != null)
				validHandler.accept(!Strings.nullOrEmpty(result));
			return result;
		} catch (Exception e) {
			if (validHandler != null)
				validHandler.accept(false);
			return null;
		}
	}
}