package org.openlca.app.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.components.ModelLink;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.util.Bean;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.core.model.RootEntity;

public class Widgets {

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
		editor.getBinding().onString(editor::getModel, property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text doubleText(Composite parent, String label, String property, ModelEditor<?> editor,
		FormToolkit toolkit) {
		Text text = UI.formText(parent, toolkit, label);
		editor.getBinding().onDouble(editor::getModel, property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text shortText(Composite parent, String label, String property, ModelEditor<?> editor,
		FormToolkit toolkit) {
		Text text = UI.formText(parent, toolkit, label);
		editor.getBinding().onShort(editor::getModel, property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text multiText(Composite parent, String label, String property, ModelEditor<?> editor,
		FormToolkit toolkit) {
		Text text = UI.formMultiText(parent, toolkit, label);
		editor.getBinding().onString(editor::getModel, property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Text multiText(Composite parent, String label, String property, ModelEditor<?> editor,
		FormToolkit toolkit, int heightHint) {
		Text text = UI.formMultiText(parent, toolkit, label, heightHint);
		editor.getBinding().onString(editor::getModel, property, text);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return text;
	}

	public static Button checkBox(Composite parent, String label, String property, ModelEditor<?> editor,
		FormToolkit toolkit) {
		Button button = UI.formCheckBox(parent, toolkit, label);
		editor.getBinding().onBoolean(editor::getModel, property, button);
		new CommentControl(parent, toolkit, property, editor.getComments());
		return button;
	}

	@SuppressWarnings("unchecked")
	public static ModelLink<?> modelLink(Composite parent, String label,
		String property, ModelEditor<?> editor, FormToolkit tk) {
		try {
			var type = (Class<RootEntity>) Bean.getType(
					editor.getModel(), property);
			var link = ModelLink.of(type)
				.setEditable(editor.isEditable())
				.renderOn(parent, tk, label);
			editor.getBinding().onModel(editor::getModel, property, link);
			new CommentControl(parent, tk, property, editor.getComments());
			return link;
		} catch (Exception e) {
			ErrorReporter.on("failed to create model link");
			return ModelLink.of(RootEntity.class);
		}
	}

}
