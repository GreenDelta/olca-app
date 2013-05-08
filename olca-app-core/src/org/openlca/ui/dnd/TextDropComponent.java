/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.ui.dnd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Project;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.FancyToolTip;
import org.openlca.ui.IContentChangedListener;
import org.openlca.ui.SelectObjectDialog;

/**
 * An text field with an add and remove button which allows the drop of a
 * specific model component type into this field.
 * 
 * @author Michael Srocka
 * @since 1.1
 */
public final class TextDropComponent extends Composite {

	/** the add button of this component */
	private Button addButton;

	/** the clazz of this component */
	private Class<? extends IModelComponent> clazz;

	/** the content of this component */
	private IModelComponent content = null;

	/**
	 * Database
	 */
	private final IDatabase database;

	/**
	 * Content change listeners
	 */
	private List<IContentChangedListener> listeners = new ArrayList<>();

	/** the icon of the add button */
	private Image objectIcon;

	/** if isNecessary is true, no delete button will be created */
	private final boolean objectIsNecessary;

	/** the delete button of this component */
	private Button removeButton;

	/**
	 * The navigation root
	 */
	private final NavigationRoot root;

	/** the text field of this component */
	private Text text;

	/** the toolkit which paints the container content */
	private FormToolkit toolkit;

	/** the transfer type for which the drop function is valid */
	private Transfer transferType = ModelComponentTransfer.getInstance();

	/**
	 * Creates a new DropComponent object.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param toolkit
	 *            the form toolkit which paints the content
	 * @param clazz
	 *            The class of the model component that can be dropped
	 * @param modelComponent
	 *            The initial content
	 * @param isNecessary
	 *            Indicates if a content is necessary
	 * @param database
	 *            The database
	 * @param root
	 *            The navigation root
	 */
	public TextDropComponent(final Composite parent, final FormToolkit toolkit,
			final Class<? extends IModelComponent> clazz,
			final IModelComponent modelComponent, final boolean isNecessary,
			final IDatabase database, final NavigationRoot root) {
		super(parent, SWT.FILL);
		this.toolkit = toolkit;
		objectIsNecessary = isNecessary;
		content = modelComponent;
		this.root = root;
		this.database = database;
		this.clazz = clazz;
		if (clazz == Actor.class) {
			objectIcon = ImageType.ACTOR_ICON.get();
		} else if (clazz == Source.class) {
			objectIcon = ImageType.SOURCE_ICON.get();
		} else if (clazz == UnitGroup.class) {
			objectIcon = ImageType.UNIT_GROUP_ICON.get();
		} else if (clazz == Flow.class) {
			objectIcon = ImageType.FLOW_ICON.get();
		} else if (clazz == FlowProperty.class) {
			objectIcon = ImageType.FLOW_PROPERTY_ICON.get();
		} else if (clazz == LCIAMethod.class) {
			objectIcon = ImageType.LCIA_ICON.get();
		} else if (clazz == Project.class) {
			objectIcon = ImageType.PROJECT_ICON.get();
		} else if (clazz == Process.class) {
			objectIcon = ImageType.PROCESS_ICON.get();
		} else if (clazz == ProductSystem.class) {
			objectIcon = ImageType.PRODUCT_SYSTEM_ICON.get();
		} else {
			objectIcon = ImageType.SEARCH_ICON.get();
		}
		createContent();

	}

	public void setTextBackground(Color color) {
		text.setBackground(color);
	}

	public void setAddButtonToolTipText(String text) {
		if (addButton != null)
			addButton.setToolTipText(text);
	}

	/**
	 * Creates the graphical content of this component.
	 * 
	 */
	protected void createContent() {
		if (toolkit != null)
			toolkit.adapt(this);
		final TableWrapLayout layout = new TableWrapLayout();
		if (objectIsNecessary) {
			layout.numColumns = 2;
		} else {
			layout.numColumns = 3;
		}
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		setLayout(layout);

		// create the text field & tool tip
		// create the add button
		if (toolkit != null)
			addButton = toolkit.createButton(this, "", SWT.PUSH);
		else
			addButton = new Button(this, SWT.PUSH);
		addButton.setToolTipText(Messages.TextDropComponent_ToolTipText);
		addButton.setLayoutData(new TableWrapData());
		addButton.setImage(objectIcon);
		addButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				final SelectObjectDialog dialog = new SelectObjectDialog(
						addButton.getShell(), root, false, database, clazz);
				dialog.open();
				final int code = dialog.getReturnCode();
				if (code == Window.OK && dialog.getSelection() != null) {
					setData(dialog.getSelection());
				}
			}

		});

		if (toolkit != null)
			text = toolkit.createText(this, "", SWT.BORDER);
		else
			text = new Text(this, SWT.BORDER);
		text.setEditable(false);
		final TableWrapData layoutData = new TableWrapData(TableWrapData.FILL,
				TableWrapData.FILL);
		layoutData.grabHorizontal = true;
		text.setLayoutData(layoutData);
		if (content != null) {
			text.setText(content.getName());
		}
		if (toolkit != null)
			new FancyToolTip(text, toolkit);

		// creates a drop listener for the text field
		final DropTarget dropTarget = new DropTarget(text, DND.DROP_COPY
				| DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transferType });
		dropTarget.addDropListener(new DropTargetAdapter() {

			@Override
			public void dragEnter(final DropTargetEvent event) {
				// do nothing
			}

			@Override
			public void drop(final DropTargetEvent event) {
				if (transferType.isSupportedType(event.currentDataType)
						&& event.data != null) {
					final Object[] data = (Object[]) event.data;
					if (data[data.length - 1] instanceof String
							&& data[data.length - 1].equals(database.getUrl())) {
						final IModelComponent object = (IModelComponent) data[0];
						if (object.getClass() == clazz) {
							setData(object);
						}
					}
				}
			}

		});

		// create the delete button
		if (!objectIsNecessary) {
			if (toolkit != null)
				removeButton = toolkit.createButton(this, "", SWT.PUSH);
			else
				removeButton = new Button(this, SWT.PUSH);
			removeButton.setLayoutData(new TableWrapData());
			removeButton.setImage(ImageType.DELETE_ICON.get());
			removeButton
					.setToolTipText(Messages.TextDropComponent_RemoveButtonText);
			if (text.getText().equals("")) {
				removeButton.setEnabled(false);
			}
			removeButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(final MouseEvent e) {
					setData(null);
				}
			});
		}
	}

	/**
	 * Sets the reference to the selected {@link IModelComponent} and informs
	 * the selection changed listeners.
	 * 
	 * @param content
	 *            The new content
	 */
	protected void fireContentChange(final IModelComponent content) {
		for (final IContentChangedListener l : listeners) {
			l.contentChanged(this, content);
		}
	}

	/**
	 * Adds a content changed listener
	 * 
	 * @param listener
	 *            The listener to add
	 */
	public void addContentChangedListener(final IContentChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public void dispose() {
		super.dispose();
		text = null;
		addButton = null;
		removeButton = null;
		transferType = null;
		content = null;
		if (listeners != null) {
			listeners.clear();
			listeners = null;
		}
		toolkit = null;
		clazz = null;
		objectIcon = null;
	}

	@Override
	public Object getData() {
		return content;
	}

	/**
	 * Removes a listener from this container which is informed when the add
	 * button was pressed.
	 * 
	 * @param listener
	 *            the listener to be removed
	 */
	public void removeAddHandler(final MouseListener listener) {
		addButton.removeMouseListener(listener);
	}

	/**
	 * Removes a content changed listener
	 * 
	 * @param listener
	 *            The listener to remove
	 */
	public void removeContentChangedListener(
			final IContentChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setData(final Object data) {
		if (data == null) {
			content = null;
			text.setText("");
		} else if (data instanceof IModelComponent) {
			final IModelComponent descriptor = (IModelComponent) data;
			if (descriptor.getClass() == clazz) {
				content = descriptor;
				if (descriptor.getName() == null) {
					text.setText("");
				} else {
					text.setText(descriptor.getName());
				}
			}
		}
		text.setData(content); // for the tool tip
		if (!objectIsNecessary) {
			removeButton.setEnabled(content != null);
		}
		fireContentChange(content);
	}

	@Override
	public void setData(final String key, final Object value) {
		setData(value);
	}

}
