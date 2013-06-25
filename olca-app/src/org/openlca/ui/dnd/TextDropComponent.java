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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.model.RootEntity;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.FancyToolTip;
import org.openlca.ui.IContentChangedListener;
import org.openlca.ui.Images;
import org.openlca.ui.SelectObjectDialog;

/**
 * An text field with an add and remove button which allows the drop of a
 * specific model type into this field.
 */
public final class TextDropComponent extends Composite {

	private Button addButton;
	private RootEntity content = null;
	private List<IContentChangedListener> listeners = new ArrayList<>();
	private final boolean objectIsNecessary;
	private Button removeButton;
	private final NavigationRoot root;
	private Text text;
	private FormToolkit toolkit;
	private Transfer transferType = ModelComponentTransfer.getInstance();

	public TextDropComponent(Composite parent, FormToolkit toolkit,
			RootEntity content, boolean isNecessary, NavigationRoot root) {
		super(parent, SWT.FILL);
		this.toolkit = toolkit;
		objectIsNecessary = isNecessary;
		this.content = content;
		this.root = root;
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
		addButton.setImage(Images.getIcon(content));
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
