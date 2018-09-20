package org.openlca.app.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Bean;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Widgets {

	private static final Logger log = LoggerFactory.getLogger(Widgets.class);

	public static ImageHyperlink link(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		new Label(parent, SWT.NONE).setText(label);
		ImageHyperlink link = new ImageHyperlink(parent, SWT.TOP);
		link.setForeground(Colors.linkBlue());
		try {
			Object value = Bean.getValue(editor.getModel(), property);
			if (value == null) {
				link.setText("-");
				return link;
			}
			if (!(value instanceof CategorizedEntity)) {
				link.setText(value.toString());
				return link;
			}
			CategorizedEntity entity = (CategorizedEntity) value;
			link.setText(Labels.getDisplayName(entity));
			link.setImage(Images.get(entity));
			link.addHyperlinkListener(new ModelLinkClickedListener(entity));
			new CommentControl(parent, toolkit, property, editor.getComments());
			return link;
		} catch (Exception e) {
			log.error("Could not get value " + property + " of " + editor.getModel(), e);
			return link;
		}
	}

	public static Label readOnly(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		UI.formLabel(parent, label);
		Label labelWidget = new Label(parent, SWT.NONE);
		GridData gridData = UI.gridData(labelWidget, false, false);
		gridData.verticalAlignment = SWT.TOP;
		gridData.verticalIndent = 2;
		editor.getBinding().readOnly(editor.getModel(), property, labelWidget);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return labelWidget;
	}

	public static CLabel readOnly(Composite parent, String label, Image image, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		UI.formLabel(parent, label);
		CLabel labelWidget = new CLabel(parent, SWT.NONE);
		GridData gridData = UI.gridData(labelWidget, false, false);
		gridData.verticalAlignment = SWT.TOP;
		gridData.verticalIndent = 2;
		labelWidget.setImage(image);
		editor.getBinding().readOnly(editor.getModel(), property, labelWidget);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return labelWidget;
	}

	public static Text text(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		Text text = UI.formText(parent, toolkit, label);
		editor.getBinding().onString(() -> editor.getModel(), property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text doubleText(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		Text text = UI.formText(parent, toolkit, label);
		editor.getBinding().onDouble(() -> editor.getModel(), property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text shortText(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		Text text = UI.formText(parent, toolkit, label);
		editor.getBinding().onShort(() -> editor.getModel(), property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text multiText(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		Text text = UI.formMultiText(parent, toolkit, label);
		editor.getBinding().onString(() -> editor.getModel(), property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text multiText(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit, int heightHint) {
		Text text = UI.formMultiText(parent, toolkit, label, heightHint);
		editor.getBinding().onString(() -> editor.getModel(), property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static DateTime date(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		toolkit.createLabel(parent, label, SWT.NONE);
		DateTime dateTime = new DateTime(parent, SWT.DATE | SWT.DROP_DOWN);
		GridData data = new GridData();
		data.widthHint = 150;
		dateTime.setLayoutData(data);
		editor.getBinding().onDate(() -> editor.getModel(), property, dateTime);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return dateTime;
	}

	public static Button checkBox(Composite parent, String label, String property, ModelEditor<?> editor,
			FormToolkit toolkit) {
		Button button = UI.formCheckBox(parent, toolkit, label);
		editor.getBinding().onBoolean(() -> editor.getModel(), property, button);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return button;
	}

	public static TextDropComponent dropComponent(Composite parent, String label, String property,
			ModelEditor<?> editor, FormToolkit toolkit) {
		ModelType modelType = getModelType(editor.getModel(), property);
		toolkit.createLabel(parent, label, SWT.NONE);
		TextDropComponent text = new TextDropComponent(
				parent, toolkit, modelType);
		UI.gridData(text, true, false);
		editor.getBinding().onModel(() -> editor.getModel(), property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	private static ModelType getModelType(Object model, String property) {
		try {
			Class<?> type = Bean.getType(model, property);
			return ModelType.forModelClass(type);
		} catch (Exception e) {
			log.error("Error determining model type", e);
			return null;
		}
	}

	private static class ModelLinkClickedListener extends HyperlinkAdapter {

		private Object model;

		public ModelLinkClickedListener(CategorizedEntity entity) {
			this.model = entity;
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			if (model instanceof CategorizedEntity)
				App.openEditor((CategorizedEntity) model);
			else if (model instanceof CategorizedDescriptor)
				App.openEditor((CategorizedDescriptor) model);
		}

	}

}
