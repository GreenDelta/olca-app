package org.openlca.app.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An HTML resource is an HTML, JavaScript, CSS, or image file. HTML files can
 * be shown in the embedded SWT browser widget. The resources for these files
 * are located in an HTML folder in the workspace directory. Some basic
 * libraries like jQuery or Bootstrap are directly provided within this
 * directory under the folder 'libs'. Other resources of specific plugins are
 * located under a folder with the name and version of the plugin in the HTML
 * folder. See the HTMLFolder class for more information about registering
 * resources.
 */
public interface IHtmlResource {

	/**
	 * Returns the name of the plugin / OSGi-bundle where this resource is
	 * located.
	 */
	String getBundleName();

	/**
	 * Returns the version of the plugin / OSGi-bundle where this resource is
	 * located.
	 */
	String getBundleVersion();

	/**
	 * Get the relative path of the resource in the target directory with the
	 * html folder (e.g. my_page.html or js/my_lib.js).
	 */
	String getTargetFilePath();

	/**
	 * Returns a list of dependencies of the resource. If there are no other
	 * dependencies this may be null or an empty list.
	 */
	List<IHtmlResource> getDependencies();

	/**
	 * Opens a stream to the resource in the respective plugin. This is used for
	 * copying the resource to the target location.
	 */
	InputStream openStream() throws IOException;

}
