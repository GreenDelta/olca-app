package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.FileType;

/**
 * Label provider for files.
 */
class FileLabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof File)
			return getImage((File) element);
		return null;
	}

	private Image getImage(File file) {
		if (file.isDirectory())
			return Images.platformImage(ISharedImages.IMG_OBJ_FOLDER);
		return Images.get(FileType.of(file));
	}

	@Override
	public String getText(Object element) {
		String text = null;
		if (element instanceof File) {
			File file = (File) element;
			text = file.getAbsoluteFile().getName();
		}
		return text;
	}
}