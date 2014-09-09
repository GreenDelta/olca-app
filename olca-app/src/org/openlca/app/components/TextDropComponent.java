package org.openlca.app.components;

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
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.app.util.Labels;
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
		if (content != null && content.getModelType() != modelType)
			throw new IllegalArgumentException("Descriptor must be of type "
					+ modelType);

		this.content = content;
		text.setData(content); // tooltip
		if (content == null) {
			text.setText("");
		} else {
			String label = Labels.getDisplayName(content);
			text.setText(label == null ? "" : label);
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
				BaseDescriptor descriptor = ModelSelectionDialog
						.select(modelType);
				if (descriptor != null)
					handleAdd(descriptor);
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
				.setToolTipText(Messages.RemoveObject);
		if (content == null)
			removeButton.setEnabled(false);
		removeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				setContent(null);
				if (handler != null)
					handler.handle(null);
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
					handleAdd(event.data);
				}
			}
		});
	}

	private void handleAdd(Object data) {
		BaseDescriptor descriptor = ModelTransfer.getDescriptor(data);
		if (descriptor == null || descriptor.getModelType() != modelType)
			return;
		setContent(descriptor);
		if (handler != null)
			handler.handle(descriptor);
	}

}
