package org.openlca.app.editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.editors.DataBinding.BindingType;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;

public abstract class ModelPage<T extends RootEntity> extends FormPage {

	private DataBinding binding;

	public ModelPage(ModelEditor<T> editor, String id, String title) {
		super(editor, id, title);
		this.binding = new DataBinding(editor);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ModelEditor<T> getEditor() {
		return (ModelEditor<T>) super.getEditor();
	}

	protected T getModel() {
		return getEditor().getModel();
	}

	public DataBinding getBinding() {
		return binding;
	}

	protected void createText(String label, String property, Composite parent) {
		Text text = UI.formText(parent, getManagedForm().getToolkit(), label);
		binding.on(getModel(), property, BindingType.STRING, text);
	}

	protected void createText(String label, String property, BindingType type,
			Composite parent) {
		Text text = UI.formText(parent, getManagedForm().getToolkit(), label);
		binding.on(getModel(), property, type, text);
	}

	protected void createDropComponent(String label, String property,
			ModelType modelType, Composite parent) {
		TextDropComponent text = UIFactory.createDropComponent(parent, label,
				getManagedForm().getToolkit(), modelType);
		binding.onModel(getModel(), property, text);
	}

}
