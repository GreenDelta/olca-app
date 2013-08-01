package org.openlca.app.component;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Images;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * A text field with an add and optional remove button which allows the drop of
 * descriptors of a specific model type into this field.
 */
public final class TextDropComponent extends Composite {

	private BaseDescriptor content;
	private Text text;
	private FormToolkit toolkit;
	private ModelType modelType;
	private Button removeButton;
	private ISingleModelDrop handler;

	public TextDropComponent(Composite parent, FormToolkit toolkit,
			ModelType modelType) {
		super(parent, SWT.FILL);
		this.toolkit = toolkit;
		this.modelType = modelType;
		createContent();
	}

	public void setHandler(ISingleModelDrop handler) {
		this.handler = handler;
	}

	public BaseDescriptor getContent() {
		return content;
	}

	public void setContent(BaseDescriptor content) {
		if (content.getModelType() != modelType)
			throw new IllegalArgumentException("Descriptor must be of type "
					+ modelType);

		this.content = content;
		text.setData(content); // tooltip
		if (content == null || content.getDisplayName() == null) {
			text.setText("");
		} else {
			text.setText(content.getDisplayName());
		}
		removeButton.setEnabled(content != null);
	}

	public ModelType getModelType() {
		return modelType;
	}

	private void createContent() {
		toolkit.adapt(this);
		TableWrapLayout layout = createLayout();
		setLayout(layout);
		// order of the method calls is important (fills from left to right)
		createAddButton();
		createTextField();
		addDropToText();
		createRemoveButton();
	}

	private TableWrapLayout createLayout() {
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 3;
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		return layout;
	}

	private void createAddButton() {
		Button addButton = toolkit.createButton(this, "", SWT.PUSH);
		addButton.setToolTipText(Messages.TextDropComponent_ToolTipText);
		addButton.setLayoutData(new TableWrapData());
		addButton.setImage(Images.getIcon(modelType));
		addButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				ObjectDialog dialog = new ObjectDialog(getShell(), modelType,
						false);
				int code = dialog.open();
				if (code == Window.OK && dialog.getSelection() != null)
					handleChange(dialog.getSelection());
			}
		});
	}

	private void createTextField() {
		text = toolkit.createText(this, "", SWT.BORDER);
		text.setEditable(false);
		TableWrapData layoutData = new TableWrapData(TableWrapData.FILL,
				TableWrapData.FILL);
		layoutData.grabHorizontal = true;
		text.setLayoutData(layoutData);
		if (content != null)
			text.setText(content.getName());
		new FancyToolTip(text, toolkit);
	}

	private void createRemoveButton() {
		removeButton = toolkit.createButton(this, "", SWT.PUSH);
		removeButton.setLayoutData(new TableWrapData());
		removeButton.setImage(ImageType.DELETE_ICON.get());
		removeButton
				.setToolTipText(Messages.TextDropComponent_RemoveButtonText);
		if (content == null)
			removeButton.setEnabled(false);
		removeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				handleChange(null);
			}
		});
	}

	private void addDropToText() {
		final Transfer transferType = ModelTransfer.getInstance();
		DropTarget dropTarget = new DropTarget(text, DND.DROP_COPY
				| DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transferType });
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (transferType.isSupportedType(event.currentDataType)) {
					handleChange(event.data);
				}
			}
		});
	}

	private void handleChange(Object data) {
		BaseDescriptor descriptor = null;
		if (data instanceof BaseDescriptor
				&& ((BaseDescriptor) data).getModelType() == modelType)
			descriptor = (BaseDescriptor) data;
		else if (data instanceof Object[]) {
			Object[] objects = (Object[]) data;
			if (objects.length > 0 && (objects[0] instanceof BaseDescriptor)
					&& ((BaseDescriptor) data).getModelType() == modelType)
				descriptor = (BaseDescriptor) objects[0];
		}
		setContent(descriptor);
		if (handler != null)
			handler.handle(descriptor);
	}

}
