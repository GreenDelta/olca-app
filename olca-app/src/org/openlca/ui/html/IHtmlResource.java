package org.openlca.ui.html;

import java.io.IOException;
import java.io.InputStream;

public interface IHtmlResource {

	// create folder for bundle, check if exists
	String getBundleName();

	// create folder for version, check if exists
	String getBundleVersion();

	String getFileName();

	InputStream openStream() throws IOException;

}
