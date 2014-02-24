package org.openlca.app.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

public class HtmlResource implements IHtmlResource {

	private Bundle bundle;
	private String internalPath;
	private String targetPath;

	public HtmlResource(Bundle bundle, String internalPath, String targetPath) {
		this.bundle = bundle;
		this.internalPath = internalPath;
		this.targetPath = targetPath;
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
	public String getTargetFilePath() {
		return targetPath;
	}

	@Override
	public List<IHtmlResource> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public InputStream openStream() throws IOException {
		return FileLocator.openStream(bundle, new Path(internalPath), false);
	}

}
