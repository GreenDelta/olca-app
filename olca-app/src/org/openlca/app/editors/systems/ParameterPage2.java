package org.openlca.app.editors.systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;

class ParameterPage2 extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ScrolledForm form;
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
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);

		// render the baseline scenario
		ParameterRedefSet baseLine = getModel().parameterSets
				.stream()
				.filter(s -> s.isBaseline)
				.findFirst()
				.orElse(null);
		if (baseLine == null) {
			baseLine = new ParameterRedefSet();
			baseLine.name = "Baseline";
			baseLine.isBaseline = true;
			getModel().parameterSets.add(baseLine);
		}
		new ParameterSection(baseLine)
				.render(tk, body);

		// create sections for existing scenarios
		for (ParameterRedefSet s : paramSets()) {
			if (s.isBaseline)
				continue;
			ParameterSection section = new ParameterSection(s);
			section.render(tk, body);
			sections.add(section);
		}

		form.reflow(true);
	}

	private class ParameterSection {

		ParameterRedefSet paramSet;
		Section section;
		ParameterRedefTable paramTable;
		FormToolkit tk;
		Composite body;

		ParameterSection(ParameterRedefSet paramSet) {
			this.paramSet = paramSet;
		}

		ParameterSection render(FormToolkit tk, Composite body) {
			this.tk = tk;
			this.body = body;
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
					textComp, tk, M.Description);
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
			Action onAdd = Actions.onAdd(this::onCopy);
			if (paramSet.isBaseline) {
				Actions.bind(section, onAdd);
			}
			if (!paramSet.isBaseline) {
				Actions.bind(section, onAdd,
						Actions.onRemove(this::onRemove));
			}
			return this;
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
			s.name = "New parameter set";
			for (ParameterRedef redef : paramSet.parameters) {
				s.parameters.add(redef.clone());
			}
			getModel().parameterSets.add(s);
			ParameterSection section = new ParameterSection(s)
					.render(tk, body);
			sections.add(section);
			form.reflow(true);
			editor.setDirty(true);
		}
	}
}
