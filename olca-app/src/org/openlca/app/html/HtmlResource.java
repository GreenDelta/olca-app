package org.openlca.app.html;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

public class HtmlResource implements IHtmlResource {

	private Bundle bundle;
	private String internalPath;
	private String fileName;

	public HtmlResource(Bundle bundle, String internalPath, String fileName) {
		this.bundle = bundle;
		this.internalPath = internalPath;
		this.fileName = fileName;
	}

	@Override
	public String getBundleName() {
		return bundle.getSymbolicName();
	}

	@Override
	public String getBundleVersion() {
		return bundle.getVersion().toString();
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public InputStream openStream() throws IOException {
		return FileLocator.openStream(bundle, new Path(internalPath), false);
	}

}
