package org.openlca.app.editors.graphical_legacy.view;

import org.eclipse.draw2d.GridData;
import org.eclipse.swt.SWT;

class GridPos {

	private GridPos() {
	}

	static GridData leftTop() {
		return new GridData(SWT.LEFT, SWT.TOP, false, false);
	}

	static GridData leftCenter() {
		return new GridData(SWT.LEFT, SWT.CENTER, false, false);
	}

	static GridData rightTop() {
		return new GridData(SWT.RIGHT, SWT.TOP, false, false);
	}

	static GridData rightCenter() {
		return new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	}

	static GridData fillTop() {
		return new GridData(SWT.FILL, SWT.TOP, true, false);
	}

	static GridData fill() {
		return new GridData(SWT.FILL, SWT.FILL, true, true);
	}
}
