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
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.components.TextDropComponent;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Bean;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ModelPage<T extends CategorizedEntity> extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private DataBinding binding;

	public ModelPage(ModelEditor<T> editor, String id, String title) {
		super(editor, id, title);
		this.binding = new DataBinding(editor);
		editor.onSaved(this::updateFormTitle);
	}

	protected abstract void updateFormTitle();

	@SuppressWarnings("unchecked")
	@Override
	public ModelEditor<T> getEditor() {
		return (ModelEditor<T>) super.getEditor();
	}

	protected T getModel() {
		return getEditor().getModel();
	}

	protected DataBinding getBinding() {
		return binding;
	}

	protected ImageHyperlink createLink(String label, String property,
			Composite parent) {
		new Label(parent, SWT.NONE).setText(label);
		ImageHyperlink link = new ImageHyperlink(parent, SWT.TOP);
		link.setForeground(Colors.linkBlue());
		try {
			Object value = Bean.getValue(getModel(), property);
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
			return link;
		} catch (Exception e) {
			log.error("Could not get value " + property + " of " + getModel(), e);
			return link;
		}
	}

	protected Label createReadOnly(String label, String property,
			Composite parent) {
		UI.formLabel(parent, label);
		Label labelWidget = new Label(parent, SWT.NONE);
		GridData gridData = UI.gridData(labelWidget, false, false);
		gridData.verticalAlignment = SWT.TOP;
		gridData.verticalIndent = 2;
		binding.readOnly(getModel(), property, labelWidget);
		return labelWidget;
	}

	protected CLabel createReadOnly(String label, Image image, String property,
			Composite parent) {
		UI.formLabel(parent, label);
		CLabel labelWidget = new CLabel(parent, SWT.NONE);
		GridData gridData = UI.gridData(labelWidget, false, false);
		gridData.verticalAlignment = SWT.TOP;
		gridData.verticalIndent = 2;
		labelWidget.setImage(image);
		binding.readOnly(getModel(), property, labelWidget);
		return labelWidget;
	}

	protected Text createText(String label, String property, Composite parent) {
		Text text = UI.formText(parent, getManagedForm().getToolkit(), label);
		binding.onString(() -> getModel(), property, text);
		return text;
	}

	protected Text createDoubleText(String label, String property, Composite parent) {
		Text text = UI.formText(parent, getManagedForm().getToolkit(), label);
		binding.onDouble(() -> getModel(), property, text);
		return text;
	}

	protected Text createMultiText(String label, String property,
			Composite parent) {
		Text text = UI.formMultiText(parent, getManagedForm().getToolkit(),
				label);
		binding.onString(() -> getModel(), property, text);
		return text;
	}

	protected DateTime createDate(String label, String property,
			Composite parent) {
		getManagedForm().getToolkit().createLabel(parent, label, SWT.NONE);
		DateTime dateTime = new DateTime(parent, SWT.DATE | SWT.DROP_DOWN);
		GridData data = new GridData();
		data.widthHint = 150;
		dateTime.setLayoutData(data);
		binding.onDate(() -> getModel(), property, dateTime);
		return dateTime;
	}

	protected Button createCheckBox(String label, String property,
			Composite parent) {
		Button button = UI.formCheckBox(parent, getManagedForm().getToolkit(),
				label);
		binding.onBoolean(() -> getModel(), property, button);
		return button;
	}

	protected TextDropComponent createDropComponent(String label,
			String property, ModelType modelType, Composite parent) {
		TextDropComponent text = UIFactory.createDropComponent(parent, label,
				getManagedForm().getToolkit(), modelType);
		binding.onModel(() -> getModel(), property, text);
		return text;
	}

	private class ModelLinkClickedListener extends HyperlinkAdapter {

		private Object model;

		public ModelLinkClickedListener(CategorizedEntity entity) {
			this.model = entity;
		}

		@Override
		public void linkActivated(HyperlinkEvent e) {
			if (model instanceof CategorizedEntity)
				App.openEditor((CategorizedEntity) model);
			else if (model instanceof BaseDescriptor)
				App.openEditor((BaseDescriptor) model);
		}

	}

}
