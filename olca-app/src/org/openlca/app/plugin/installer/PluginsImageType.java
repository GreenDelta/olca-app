package org.openlca.app.plugin.installer;

import java.io.File;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.RcpActivator;
import org.openlca.app.resources.IImageType;
import org.openlca.app.resources.ImageManager;

public enum PluginsImageType implements IImageType {
	NEWSEARCH_WIZ("newsearch_wiz.gif");

	private final String fileName;

	private PluginsImageType(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public String getPath() {
		return File.separator + "icons" + File.separator + this.fileName;
	}

	@Override
	public Image createImage() {
		return RcpActivator.getImageDescriptor(getPath()).createImage();
	}

	@Override
	public Image get() {
		return ImageManager.getImage(this);
	}
}
