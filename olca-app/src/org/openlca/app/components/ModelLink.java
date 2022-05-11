package org.openlca.app.components;

import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

public class ModelLink<T extends RootEntity> {

	private final IDatabase db;
	private final Class<T> type;
	private final ModelType modelType;

	private ImageHyperlink link;
	private Consumer<T> onChange;
	private T model;
	private boolean editable = true;

	private ModelLink(Class<T> type) {
		this.db = Objects.requireNonNull(Database.get());
		this.type = Objects.requireNonNull(type);
		var modelType = ModelType.UNKNOWN;
		for (var mt : ModelType.values()) {
			if (type.equals(mt.getModelClass())) {
				modelType = mt;
				break;
			}
		}
		this.modelType = modelType;
	}

	public static <T extends RootEntity> ModelLink<T> of(Class<T> type) {
		return new ModelLink<>(type);
	}

	/**
	 * Set if the model link is enabled or not. This has to be done before the
	 * model links is rendered.
	 */
	public ModelLink<T> setEditable(boolean editable) {
		this.editable = editable;
		return this;
	}

	public ModelLink<T> onChange(Consumer<T> fn) {
		this.onChange = fn;
		return this;
	}

	public ModelLink<T> renderOn(Composite parent, FormToolkit tk, String label) {
		UI.formLabel(parent, tk, label);
		return renderOn(parent, tk);
	}

	public ModelLink<T> renderOn(Composite parent, FormToolkit tk) {
		var comp = tk.createComposite(parent, SWT.FILL);
		UI.gridLayout(comp, 3, 10, 0);

		// the selection handler of this widget
		Runnable doSelect = () -> select(ModelSelector.select(modelType));

		// selection button
		var btn = tk.createImageHyperlink(comp, SWT.BORDER);
		btn.setToolTipText("Select a data set");
		btn.setImage(Images.get(modelType));
		btn.setEnabled(editable);
		Controls.onClick(btn, $ -> doSelect.run());

		// the link
		link = tk.createImageHyperlink(comp, SWT.NONE);
		Controls.onClick(link, $ -> {
			if (model != null) {
				App.open(model);
			} else if (editable) {
				doSelect.run();
			}
		});
		if (editable) {
			ModelTransfer.onDrop(link, ds -> {
				if (!ds.isEmpty()) {
					select(ds.get(0));
				}
			});
		}

		// the delete button
		var deleteBtn = tk.createImageHyperlink(comp, SWT.TOP);
		deleteBtn.setEnabled(editable);
		deleteBtn.setToolTipText(M.Remove);
		deleteBtn.setHoverImage(Icon.DELETE.get());
		deleteBtn.setImage(Icon.DELETE_DISABLED.get());
		Controls.onClick(deleteBtn, $ -> {
			model = null;
			updateLinkText();
			if (onChange != null) {
				onChange.accept(null);
			}
		});

		updateLinkText();
		return this;
	}

	private void select(Descriptor d) {
		if (d == null
			|| d.type == null
			|| !Objects.equals(d.type.getModelClass(), type))
			return;
		model = db.get(type, d.id);
		updateLinkText();
		if (onChange != null) {
			onChange.accept(model);
		}
	}

	/**
	 * Set the model of this link to the given value without firing
	 * an {@code onChange} event.
	 */
	public ModelLink<T> setModel(T model) {
		this.model = model;
		updateLinkText();
		return this;
	}

	private void updateLinkText() {
		if (link == null)
			return;
		var text = model == null
			? "- none -"
			: Labels.name(model);
		link.setText(Strings.cut(text, 120));
		link.setToolTipText(text);
		link.getParent().pack();
	}

}
