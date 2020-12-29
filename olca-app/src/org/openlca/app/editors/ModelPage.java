package org.openlca.app.editors;

import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.util.Labels;
import org.openlca.cloud.model.Comments;
import org.openlca.core.model.CategorizedEntity;

public abstract class ModelPage<T extends CategorizedEntity> extends FormPage {

	public ModelPage(ModelEditor<T> editor, String id, String title) {
		super(editor, id, title);
		editor.onSaved(() -> {
			var mform = getManagedForm();
			if (mform == null || mform.getForm() == null)
				return;
			mform.getForm().setText(getFormTitle());
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
		return Widgets.link(parent, label, property, getEditor(), getToolkit());
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
		var text =  Widgets.shortText(parent, label, property, getEditor(), getToolkit());
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

	protected DateTime date(Composite parent, String label, String property) {
		var dt = Widgets.date(parent, label, property, getEditor(), getToolkit());
		dt.setEnabled(isEditable());
		return dt;
	}

	protected Button checkBox(Composite parent, String label, String property) {
		var btn =  Widgets.checkBox(parent, label, property, getEditor(), getToolkit());
		btn.setEnabled(isEditable());
		return btn;
	}

	protected TextDropComponent dropComponent(Composite parent, String label, String property) {
		var comp =  Widgets.dropComponent(parent, label, property, getEditor(), getToolkit());
		comp.setEnabled(isEditable());
		return comp;
	}

}
