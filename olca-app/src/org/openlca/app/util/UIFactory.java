package org.openlca.app.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.TextDropComponent;
import org.openlca.core.model.ModelType;

/**
 * A factory for basic UI components in the openLCA framework.
 */
public final class UIFactory {

	private UIFactory() {
	}

	/**
	 * Creates a composite with the given layout
	 * 
	 * @param parent
	 *            The parent composite
	 * @param layout
	 *            The layout of the composite
	 * @return A new composite
	 */
	public static Composite createContainer(final Composite parent,
			final Layout layout) {
		final Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(layout);
		return container;
	}

	public static TextDropComponent createDropComponent(Composite parent,
			String labelText, FormToolkit toolkit, ModelType modelType) {
		toolkit.createLabel(parent, labelText, SWT.NONE);
		TextDropComponent dropComponent = new TextDropComponent(parent,
				toolkit, modelType);
		dropComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		return dropComponent;
	}

	/**
	 * Creates a new grid layout with the given vertical spacing and the given
	 * number of columns
	 * 
	 * @param numColumns
	 *            the number of columns of the layout
	 * @param vSpacing
	 *            The vertical spacing value
	 * @param makeColumnsEqual
	 *            Indicates if the columns should have equal size
	 * @return The grid layout
	 */
	public static Layout createGridLayout(final int numColumns,
			final boolean makeColumnsEqual, final int vSpacing) {
		final GridLayout layout = new GridLayout(numColumns, makeColumnsEqual);
		layout.verticalSpacing = vSpacing;
		return layout;
	}

}
