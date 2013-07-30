/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.app.resources;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.openlca.core.application.plugin.Activator;

/**
 * Manager for the application images and icons.
 */
public class ImageManager {

	private static ImageRegistry imageRegistry = new ImageRegistry();

	public static Image getImage(IImageType imageType) {
		String path = imageType.getPath();
		Image image = imageRegistry.get(path);
		if (image == null || image.isDisposed()) {
			image = imageType.createImage();
			imageRegistry.put(path, image);
		}
		return image;
	}

	public static ImageDescriptor getImageDescriptor(IImageType imageType) {
		String path = imageType.getPath();
		ImageDescriptor d = imageRegistry.getDescriptor(path);
		if (d != null)
			return d;
		d = Activator.getImageDescriptor(path);
		imageRegistry.put(path, d);
		return d;
	}

}
