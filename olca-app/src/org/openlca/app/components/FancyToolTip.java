/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.app.components;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.openlca.app.Messages;
import org.openlca.core.model.RootEntity;

/**
 * Creates a tool tip for controls containing model components, which shows the
 * name and description of the pointed model component in a pop-up section.
 */
public final class FancyToolTip extends DefaultToolTip {

	/**
	 * The form toolkit to use
	 */
	private final FormToolkit formToolkit;

	/**
	 * Creates a new instance.
	 * 
	 * @param control
	 *            the control on which this tool tip should occur
	 * @param toolkit
	 *            the form toolkit with which the tool tip should be painted
	 */
	public FancyToolTip(final Control control, final FormToolkit toolkit) {
		super(control);
		formToolkit = toolkit;
	}

	@Override
	protected Composite createToolTipContentArea(final Event event,
			final Composite parent) {
		Composite composite = null;

		// get the pointed component
		RootEntity descriptor = null;
		if (event.widget != null && event.widget instanceof Table) {
			final Table table = (Table) event.widget;
			final Item item = table.getItem(new Point(event.x, event.y));
			if (item != null && item.getData() != null
					&& item.getData() instanceof RootEntity) {
				descriptor = (RootEntity) item.getData();
			}

		} else if (event.widget != null && event.widget instanceof Tree) {
			final Tree tree = (Tree) event.widget;
			final Item item = tree.getItem(new Point(event.x, event.y));
			if (item != null && item.getData() != null
					&& item.getData() instanceof RootEntity) {
				descriptor = (RootEntity) item.getData();
			}

		} else if (event.widget != null && event.widget.getData() != null
				&& event.widget.getData() instanceof RootEntity) {
			descriptor = (RootEntity) event.widget.getData();
		}

		// create the tool tip
		if (descriptor != null) {
			composite = formToolkit.createComposite(parent);
			composite.setLayout(new TableWrapLayout());

			// create section
			final Section section = formToolkit.createSection(composite,
					ExpandableComposite.TITLE_BAR
							| ExpandableComposite.FOCUS_TITLE);
			final TableWrapData twd_section = new TableWrapData(
					TableWrapData.FILL, TableWrapData.FILL);
			twd_section.maxWidth = 280;
			twd_section.grabVertical = true;
			section.setLayoutData(twd_section);

			section.setText(descriptor.getName() != null ? descriptor.getName()
					: "");

			// create description composite
			final Composite descriptionComposite = formToolkit.createComposite(
					section, SWT.NONE);
			descriptionComposite.setLayout(new TableWrapLayout());
			section.setClient(descriptionComposite);
			formToolkit.paintBordersFor(descriptionComposite);

			// create text field for description
			final Text textField = formToolkit.createText(descriptionComposite,
					null, SWT.WRAP | SWT.MULTI);
			final TableWrapData twd_textField = new TableWrapData(
					TableWrapData.FILL, TableWrapData.FILL);
			twd_textField.grabVertical = true;
			twd_textField.grabHorizontal = true;
			textField.setLayoutData(twd_textField);
			final String nullText = Messages.NoDescription;
			textField.setText(descriptor.getDescription() != null ? descriptor
					.getDescription() : nullText);
		}
		return composite;
	}

}
