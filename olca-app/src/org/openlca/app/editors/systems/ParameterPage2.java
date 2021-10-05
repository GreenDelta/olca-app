package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

class ParameterPage2 extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ScrolledForm form;
	private FormToolkit tk;
	private Composite body;
	private AddButton addButton;
	private final List<ParameterSection> sections = new ArrayList<>();

	public ParameterPage2(ProductSystemEditor editor) {
		super(editor, "ParameterPage2", "Parameters");
		this.editor = editor;
		editor.onSaved(() -> {
			sections.forEach(s -> s.update());
		});
	}

	private List<ParameterRedefSet> paramSets() {
		return editor.getModel().parameterSets;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(this);
		tk = mform.getToolkit();
		body = UI.formBody(form, tk);

		// create the baseline scenario if necessary
		ParameterRedefSet base = getModel().parameterSets
				.stream()
				.filter(s -> s.isBaseline)
				.findFirst()
				.orElse(null);
		if (base == null) {
			base = new ParameterRedefSet();
			base.name = "Baseline";
			base.isBaseline = true;
			getModel().parameterSets.add(base);
		}

		// sort and render the parameter sets
		List<ParameterRedefSet> sets = paramSets();
		sets.sort((s1, s2) -> {
			if (s1.isBaseline)
				return -1;
			if (s2.isBaseline)
				return 1;
			return Strings.compare(s1.name, s2.name);
		});
		for (ParameterRedefSet s : paramSets()) {
			sections.add(new ParameterSection(s));
		}

		// render the add button at the end of the list
		addButton = new AddButton();
		form.reflow(true);
	}

	private void addNew(ParameterRedefSet s) {
		getModel().parameterSets.add(s);
		sections.add(new ParameterSection(s));
		addButton.render();
		form.reflow(true);
		editor.setDirty(true);
	}

	private class ParameterSection {

		ParameterRedefSet paramSet;
		Section section;
		ParameterRedefTable paramTable;

		ParameterSection(ParameterRedefSet paramSet) {
			this.paramSet = paramSet;
			render();
		}

		void render() {
			section = UI.section(body, tk,
					paramSet.name != null ? paramSet.name : "");
			UI.gridData(section, true, false);
			Composite comp = UI.sectionClient(section, tk);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1);

			Composite textComp = tk.createComposite(comp);
			UI.gridData(textComp, true, false);
			UI.gridLayout(textComp, 2, 10, 0);

			// name
			Text nameText = UI.formText(textComp, tk, M.Name);
			if (paramSet.name != null) {
				nameText.setText(paramSet.name);
			}
			nameText.addModifyListener(e -> {
				paramSet.name = nameText.getText();
				section.setText(paramSet.name);
				editor.setDirty(true);
			});

			// description
			Text descrText = UI.formMultiText(
					textComp, tk, M.Description, 40);
			if (paramSet.description != null) {
				descrText.setText(paramSet.description);
			}
			descrText.addModifyListener(e -> {
				paramSet.description = descrText.getText();
				editor.setDirty(true);
			});

			// parameters
			Section paramSection = UI.section(
					comp, tk, M.Parameters);
			UI.gridData(paramSection, true, false);
			paramTable = new ParameterRedefTable(
					editor, () -> paramSet.parameters);
			paramTable.create(tk,
					UI.sectionClient(paramSection, tk));
			paramTable.bindActions(paramSection);

			// only non-baseline scenarios can be removed
			Action onCopy = Actions.create(
					"Copy parameter set",
					Icon.COPY.descriptor(),
					this::onCopy);
			if (paramSet.isBaseline) {
				Actions.bind(section, onCopy);
			}
			if (!paramSet.isBaseline) {
				Actions.bind(section, onCopy,
						Actions.onRemove(this::onRemove));
			}
		}

		void update() {
			// sync JPA state
			for (ParameterRedefSet s : paramSets()) {
				if (Objects.equals(s, this.paramSet)) {
					this.paramSet = s;
					paramTable.update();
					break;
				}
			}
		}

		void onRemove() {
			sections.remove(this);
			editor.getModel().parameterSets.remove(paramSet);
			section.dispose();
			form.reflow(true);
			editor.setDirty(true);
		}

		void onCopy() {
			ParameterRedefSet s = new ParameterRedefSet();
			s.name = paramSet.name + " - Copy";
			s.description = paramSet.description;
			for (ParameterRedef redef : paramSet.parameters) {
				s.parameters.add(redef.copy());
			}
			addNew(s);
		}
	}

	class AddButton {

		Composite comp;

		AddButton() {
			render();
		}

		void render() {
			if (comp != null) {
				comp.dispose();
			}
			comp = tk.createComposite(body);
			GridLayout btnGrid = UI.gridLayout(comp, 1);
			btnGrid.marginLeft = 0;
			btnGrid.marginTop = 0;
			btnGrid.marginBottom = 10;
			Button addButton = tk.createButton(
					comp, "Add parameter set", SWT.NONE);
			addButton.setImage(Icon.ADD.get());
			Controls.onSelect(addButton, e -> {
				ParameterRedefSet s = new ParameterRedefSet();
				s.name = "New parameter set";
				addNew(s);
			});
		}
	}

}
