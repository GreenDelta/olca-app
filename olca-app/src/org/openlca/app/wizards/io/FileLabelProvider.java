package org.openlca.app.wizards.io;

import java.io.File;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.openlca.app.rcp.ImageType;

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
			return ImageType
					.getPlatformImage(ISharedImages.IMG_OBJ_FOLDER);
		if (hasExtension(file, ".xml"))
			return ImageType.XML_ICON.get();
		if (hasExtension(file, ".zip"))
			return ImageType.ZIP_ICON.get();
		if (hasExtension(file, ".xls") || hasExtension(file, ".xlsx"))
			return ImageType.FILE_EXCEL_SMALL.get();
		else
			return ImageType.FILE_SMALL.get();
	}

	private boolean hasExtension(File file, String extension) {
		if (file == null || file.getName() == null)
			return false;
		return file.getName().toLowerCase().trim().endsWith(extension);
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