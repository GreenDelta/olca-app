package org.openlca.app.tools.graphics.figures;

import org.eclipse.draw2d.GridData;
import org.eclipse.swt.SWT;

public class GridPos {

	private GridPos() {
	}

	public static GridData leadTop() {
		return new GridData(SWT.LEAD, SWT.TOP, false, false);
	}

	public static GridData leadCenter() {
		return new GridData(SWT.LEAD, SWT.CENTER, false, false);
	}

	public static GridData trailTop() {
		return new GridData(SWT.TRAIL, SWT.TOP, false, false);
	}

	public static GridData trailCenter() {
		return new GridData(SWT.TRAIL, SWT.CENTER, false, false);
	}

	public static GridData fillTop() {
		return new GridData(SWT.FILL, SWT.TOP, true, false);
	}

	public static GridData fill() {
		return new GridData(SWT.FILL, SWT.FILL, true, true);
	}
}
