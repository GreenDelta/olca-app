package org.openlca.core.resources;

import org.eclipse.swt.graphics.Image;

public interface IImageType {

	String getPath();

	/**
	 * Creation - normally users should use {@link #get()}
	 * 
	 * @return
	 */
	Image createImage();

	/**
	 * Returns a (cached) image that is automatically disposed.
	 * 
	 * @return
	 */
	Image get();
}
