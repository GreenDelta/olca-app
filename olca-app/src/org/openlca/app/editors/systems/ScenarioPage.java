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
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Scenario;

class ScenarioPage extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ScrolledForm form;
	private final List<ScenarioSection> sections = new ArrayList<>();

	public ScenarioPage(ProductSystemEditor editor) {
		super(editor, "ScenarioPage", "Scenarios");
		this.editor = editor;
		editor.onSaved(() -> {
			sections.forEach(s -> s.update());
		});
	}

	private List<Scenario> scenarios() {
		return editor.getModel().scenarios;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);

		// render the baseline scenario
		Scenario baseLine = getModel().scenarios.stream()
				.filter(s -> s.isBaseline)
				.findFirst()
				.orElse(null);
		if (baseLine == null) {
			baseLine = new Scenario();
			baseLine.name = "Baseline";
			baseLine.isBaseline = true;
			getModel().scenarios.add(baseLine);
		}
		new ScenarioSection(baseLine)
				.render(tk, body);

		// create sections for existing scenarios
		for (Scenario s : scenarios()) {
			if (s.isBaseline)
				continue;
			ScenarioSection section = new ScenarioSection(s);
			section.render(tk, body);
			sections.add(section);
		}

		form.reflow(true);
	}

	private class ScenarioSection {

		Scenario scenario;
		Section section;
		ParameterRedefTable paramTable;
		FormToolkit tk;
		Composite body;

		ScenarioSection(Scenario scenario) {
			this.scenario = scenario;
		}

		ScenarioSection render(FormToolkit tk, Composite body) {
			this.tk = tk;
			this.body = body;
			section = UI.section(body, tk,
					scenario.name != null ? scenario.name : "");
			UI.gridData(section, true, false);
			Composite comp = UI.sectionClient(section, tk);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1);

			Composite textComp = tk.createComposite(comp);
			UI.gridData(textComp, true, false);
			UI.gridLayout(textComp, 2, 10, 0);

			// name
			Text nameText = UI.formText(textComp, tk, M.Name);
			if (scenario.name != null) {
				nameText.setText(scenario.name);
			}
			nameText.addModifyListener(e -> {
				scenario.name = nameText.getText();
				section.setText(scenario.name);
				editor.setDirty(true);
			});

			// description
			Text descrText = UI.formMultiText(
					textComp, tk, M.Description);
			if (scenario.description != null) {
				descrText.setText(scenario.description);
			}
			descrText.addModifyListener(e -> {
				scenario.description = descrText.getText();
				editor.setDirty(true);
			});

			// parameters
			Section paramSection = UI.section(
					comp, tk, M.Parameters);
			UI.gridData(paramSection, true, false);
			paramTable = new ParameterRedefTable(
					editor, () -> scenario.parameters);
			paramTable.create(tk,
					UI.sectionClient(paramSection, tk));
			paramTable.bindActions(paramSection);

			// only non-baseline scenarios can be removed
			Action onAdd = Actions.onAdd(this::onCopy);
			if (scenario.isBaseline) {
				Actions.bind(section, onAdd);
			}
			if (!scenario.isBaseline) {
				Actions.bind(section, onAdd,
						Actions.onRemove(this::onRemove));
			}
			return this;
		}

		void update() {
			// sync JPA state
			for (Scenario s : scenarios()) {
				if (Objects.equals(s, this.scenario)) {
					this.scenario = s;
					paramTable.update();
					break;
				}
			}
		}

		void onRemove() {
			sections.remove(this);
			editor.getModel().scenarios.remove(scenario);
			section.dispose();
			form.reflow(true);
			editor.setDirty(true);
		}

		void onCopy() {
			Scenario s = new Scenario();
			s.name = "New scenario";
			for (ParameterRedef redef : scenario.parameters) {
				s.parameters.add(redef.clone());
			}
			getModel().scenarios.add(s);
			ScenarioSection section = new ScenarioSection(s)
					.render(tk, body);
			sections.add(section);
			form.reflow(true);
			editor.setDirty(true);
		}
	}
}
