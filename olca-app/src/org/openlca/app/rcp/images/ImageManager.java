package org.openlca.app.rcp.images;

import java.io.File;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.openlca.app.preferences.Theme;
import org.openlca.app.rcp.RcpActivator;

/**
 * Manager for the application images and icons.
 */
class ImageManager {

	private static final String ROOT = "icons";
	private static final ImageRegistry registry = new ImageRegistry();

	static Image get(Icon icon) {
		if (icon == null)
			return null;
		return get(icon.fileName);
	}

	static Image get(Icon icon, Overlay overlay) {
		if (icon == null || overlay == null)
			return null;
		return get(icon.fileName, overlay.fileName);
	}

	static Image get(ModelIcon icon) {
		if (icon == null)
			return null;
		return get(icon.fileName);
	}

	static Image get(String filename) {
		if (filename == null)
			return null;
		Image image = registry.get(filename);
		if (image != null && !image.isDisposed())
			return image;
		image = RcpActivator.getImageDescriptor(toPath(filename)).createImage();
		registry.put(filename, image);
		return image;
	}

	static Image get(ModelIcon icon, Overlay overlay) {
		if (icon == null || overlay == null)
			return null;
		return get(icon.fileName, overlay.fileName);
	}

	private static Image get(String filename, String overlay) {
		if (filename == null || overlay == null)
			return null;
		String id = filename + "-" + overlay;
		Image withOverlay = registry.get(id);
		if (withOverlay != null && !withOverlay.isDisposed())
			return withOverlay;
		var withIcon = new DecorationOverlayIcon(
				get(filename), descriptor(overlay), IDecoration.BOTTOM_RIGHT);
		withOverlay = withIcon.createImage();
		registry.put(id, withOverlay);
		return withOverlay;
	}

	static ImageDescriptor descriptor(Icon icon) {
		if (icon == null)
			return null;
		return descriptor(icon.fileName);
	}

	static ImageDescriptor descriptor(ModelIcon icon) {
		if (icon == null)
			return null;
		return descriptor(icon.fileName);
	}

	static ImageDescriptor descriptor(String filename) {
		if (filename == null)
			return null;
		ImageDescriptor d = registry.getDescriptor(filename);
		if (d != null)
			return d;
		d = RcpActivator.getImageDescriptor(toPath(filename));
		registry.put(filename, d);
		return d;
	}

	static ImageDescriptor descriptor(Icon icon, Overlay overlay) {
		if (icon == null || overlay == null)
			return null;
		return descriptor(icon.fileName, overlay.fileName);
	}

	static ImageDescriptor descriptor(ModelIcon icon, Overlay overlay) {
		if (icon == null || overlay == null)
			return null;
		return descriptor(icon.fileName, overlay.fileName);
	}

	private static ImageDescriptor descriptor(String filename, String overlay) {
		String id = filename + "-" + overlay;
		ImageDescriptor withOverlay = registry.getDescriptor(id);
		if (withOverlay != null)
			return withOverlay;
		withOverlay = new OverlayImageDescriptor(descriptor(filename), descriptor(overlay));
		registry.put(id, withOverlay);
		return withOverlay;
	}

	private static String toPath(String filename) {
		var mode = Theme.isDark() ? "dark" : "light";
		return ROOT + File.separator + mode + File.separator + filename;
	}

	private static class OverlayImageDescriptor extends CompositeImageDescriptor {

		private final ImageDescriptor image;
		private final ImageDescriptor overlay;
		private final Point size;
		private final Point overlaySize;

		private OverlayImageDescriptor(ImageDescriptor image, ImageDescriptor overlay) {
			this.image = image;
			this.overlay = overlay;

			// image bounds
			var _image = image.createImage();
			var imgBounds = _image.getBounds();
			_image.dispose();
			size = new Point(imgBounds.width, imgBounds.height);

			// overlay bounds
			var _overlay = overlay.createImage();
			var overlayBounds = _overlay.getBounds();
			_overlay.dispose();
			overlaySize = new Point(overlayBounds.width, overlayBounds.height);
		}

		@Override
		protected void drawCompositeImage(int width, int height) {
			drawImage(image::getImageData, 0, 0);
			int x = size.x - overlaySize.x;
			int y = size.y - overlaySize.y;
			drawImage(overlay::getImageData, x, y);
		}

		@Override
		protected Point getSize() {
			return size;
		}

	}

}
