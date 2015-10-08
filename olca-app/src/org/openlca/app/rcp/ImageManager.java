package org.openlca.app.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;

/**
 * Manager for the application images and icons.
 */
public class ImageManager {

	private static ImageRegistry imageRegistry = new ImageRegistry();

	public static Image getImage(ImageType imageType) {
		String path = imageType.getPath();
		Image image = imageRegistry.get(path);
		if (image != null && !image.isDisposed())
			return image;
		image = imageType.createImage();
		imageRegistry.put(path, image);
		return image;
	}

	public static ImageDescriptor getImageDescriptor(ImageType imageType) {
		String path = imageType.getPath();
		ImageDescriptor d = imageRegistry.getDescriptor(path);
		if (d != null)
			return d;
		d = RcpActivator.getImageDescriptor(path);
		imageRegistry.put(path, d);
		return d;
	}

	public static Image getImageWithOverlay(ImageType imageType,
			ImageType overlayType) {
		String path = imageType.getPath() + "-" + overlayType.getPath();
		Image imageWithOverlay = imageRegistry.get(path);
		if (imageWithOverlay != null && !imageWithOverlay.isDisposed())
			return imageWithOverlay;
		Image image = getImage(imageType);
		ImageDescriptor overlay = getImageDescriptor(overlayType);
		DecorationOverlayIcon imageWithIcon = new DecorationOverlayIcon(image,
				overlay, IDecoration.BOTTOM_RIGHT);
		imageWithOverlay = imageWithIcon.createImage();
		imageRegistry.put(path, imageWithOverlay);
		return imageWithOverlay;
	}
}
