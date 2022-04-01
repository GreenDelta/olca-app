package org.openlca.app.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.collaboration.util.Comments;
import org.openlca.app.components.ModelLink;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Bean;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.core.model.RootEntity;

public abstract class ModelPage<T extends RootEntity> extends FormPage {

	public ModelPage(ModelEditor<T> editor, String id, String title) {
		super(editor, id, title);
		editor.onSaved(() -> {
			var mForm = getManagedForm();
			if (mForm == null || mForm.getForm() == null)
				return;
			mForm.getForm().setText(getFormTitle());
		});
	}

	public String getFormTitle() {
		return getTitle() + ": " + Labels.name(getModel());
	}

	@SuppressWarnings("unchecked")
	@Override
	public ModelEditor<T> getEditor() {
		return (ModelEditor<T>) super.getEditor();
	}

	public boolean isEditable() {
		return getEditor().isEditable();
	}

	protected T getModel() {
		return getEditor().getModel();
	}

	protected DataBinding getBinding() {
		return getEditor().getBinding();
	}

	protected Comments getComments() {
		return getEditor().getComments();
	}

	protected FormToolkit getToolkit() {
		return getManagedForm().getToolkit();
	}

	protected ImageHyperlink link(Composite parent, String label, String property) {
		new Label(parent, SWT.NONE).setText(label);
		var link = new ImageHyperlink(parent, SWT.TOP);
		link.setForeground(Colors.linkBlue());
		try {
			var value = Bean.getValue(getModel(), property);
			if (value == null) {
				link.setText("- none -");
				return link;
			}
			if (!(value instanceof RootEntity entity)) {
				link.setText(value.toString());
				return link;
			}
			link.setText(Labels.name(entity));
			link.setImage(Images.get(entity));
			Controls.onClick(link, $ -> App.open(entity));
			new CommentControl(parent, getToolkit(), property, getComments());
			return link;
		} catch (Exception e) {
			ErrorReporter.on("Failed to get '" + property + "' of " + getModel(), e);
			return link;
		}
	}

	protected Label readOnly(Composite parent, String label, String property) {
		return Widgets.readOnly(parent, label, property, getEditor(), getToolkit());
	}

	protected CLabel readOnly(Composite parent, String label, Image image, String property) {
		return Widgets.readOnly(parent, label, image, property, getEditor(), getToolkit());
	}

	protected Text text(Composite parent, String label, String property) {
		var text = Widgets.text(parent, label, property, getEditor(), getToolkit());
		text.setEditable(isEditable());
		return text;
	}

	protected Text doubleText(Composite parent, String label, String property) {
		var text = Widgets.doubleText(parent, label, property, getEditor(), getToolkit());
		text.setEditable(isEditable());
		return text;
	}

	protected Text shortText(Composite parent, String label, String property) {
		var text = Widgets.shortText(parent, label, property, getEditor(), getToolkit());
		text.setEditable(isEditable());
		return text;
	}

	protected Text multiText(Composite parent, String label, String property) {
		var text = Widgets.multiText(parent, label, property, getEditor(), getToolkit());
		text.setEditable(isEditable());
		return text;
	}

	protected Text multiText(Composite parent, String label, String property, int heightHint) {
		var text = Widgets.multiText(parent, label, property, getEditor(), getToolkit(), heightHint);
		text.setEditable(isEditable());
		return text;
	}

	protected Button checkBox(Composite parent, String label, String property) {
		var btn = Widgets.checkBox(parent, label, property, getEditor(), getToolkit());
		btn.setEnabled(isEditable());
		return btn;
	}

	protected ModelLink<?> modelLink(Composite parent, String label, String property) {
		var link = Widgets.modelLink(parent, label, property, getEditor(), getToolkit());
		link.setEditable(isEditable());
		return link;
	}
}
