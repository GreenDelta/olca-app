package org.openlca.app.editors.processes.kml;

import java.util.function.Consumer;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.python.google.common.base.Strings;

public class KmlPrettifyFunction extends BrowserFunction {

	private Consumer<Boolean> validHandler;

	public KmlPrettifyFunction(Browser browser, Consumer<Boolean> validHandler) {
		super(browser, "prettifyKML");
		this.validHandler = validHandler;
	}

	@Override
	public Object function(Object[] arguments) {
		String kml = getArg(arguments, 0);
		if (kml == null || kml.isEmpty()) {
			if (validHandler != null)
				validHandler.accept(true);
			return null;
		}
		try {
			String result = KmlUtil.prettyFormat(kml);
			if (validHandler != null)
				validHandler.accept(!Strings.isNullOrEmpty(result));
			return result;
		} catch (Exception e) {
			if (validHandler != null)
				validHandler.accept(false);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getArg(Object[] args, int index) {
		if (args.length <= index)
			return null;
		return (T) args[index];
	}

}