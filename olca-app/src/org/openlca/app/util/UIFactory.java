package org.openlca.app.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
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
	 * Creates a composite with a 2 column grid layout
	 * 
	 * @param parent
	 *            The parent composite
	 * @return A new composite
	 */
	public static Composite createContainer(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(createGridLayout(2, false, 10));
		return container;
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

	/**
	 * Creates a label and a text widget
	 * 
	 * @param parent
	 *            The parent composite
	 * @param labelText
	 *            The text of the label
	 * @param multiLine
	 *            Indicates if the text widget is multi line
	 * @return A new text widget
	 */
	public static Text createTextWithLabel(final Composite parent,
			final String labelText, final boolean multiLine) {
		return createTextWithLabel(parent, labelText, multiLine, SWT.NONE);
	}

	/**
	 * Creates a label and a text widget
	 * 
	 * @param parent
	 *            The parent composite
	 * @param labelText
	 *            The text of the label
	 * @param multiLine
	 *            Indicates if the text widget is multi line
	 * @param style
	 *            The style of the text widget
	 * @return A new text widget
	 */
	public static Text createTextWithLabel(final Composite parent,
			final String labelText, final boolean multiLine, final int style) {
		final Label nameLabel = new Label(parent, style);
		nameLabel.setText(labelText);
		return createText(parent, multiLine);
	}

	public static Text createText(Composite parent, boolean multiLine) {
		final Text text = new Text(parent, multiLine ? SWT.BORDER
				| SWT.V_SCROLL | SWT.WRAP | SWT.MULTI : SWT.BORDER);
		GridData gd_text = null;
		if (multiLine) {
			gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd_text.heightHint = 75;
		} else {
			gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false);
		}
		text.setLayoutData(gd_text);
		return text;
	}

}
