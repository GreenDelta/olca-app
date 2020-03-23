package org.openlca.app.editors.systems;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
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
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Scenario;

class ScenarioPage extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ScrolledForm form;

	public ScenarioPage(ProductSystemEditor editor) {
		super(editor, "ScenarioPage", "Scenarios");
		this.editor = editor;
	}

	private List<Scenario> scenarios() {
		return editor.getModel().scenarios;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);

		Button addButton = tk.createButton(
				body, "Add scenario", SWT.NONE);
		addButton.setImage(Icon.ADD.get());
		Controls.onSelect(addButton, e -> {
			Scenario s = new Scenario();
			s.name = "New scenario";
			getModel().scenarios.add(s);
			new ScenarioSection(s).render(tk, body);
		});

		for (Scenario s : scenarios()) {
			new ScenarioSection(s).render(tk, body);
		}

		form.reflow(true);
	}

	private class ScenarioSection {

		Scenario scenario;
		Section section;

		ScenarioSection(Scenario scenario) {
			this.scenario = scenario;
			editor.onSaved(() -> {
				// sync JPA state
				for (Scenario s : scenarios()) {
					if (Objects.equals(s, this.scenario)) {
						this.scenario = s;
						// TODO: push parameter redefs
						break;
					}
				}
			});
		}

		void render(FormToolkit tk, Composite body) {
			section = UI.section(body, tk,
					scenario.name != null ? scenario.name : "");
			Composite comp = UI.sectionClient(section, tk);

			// name
			Text nameText = UI.formText(comp, tk, M.Name);
			if (scenario.name != null) {
				nameText.setText(scenario.name);
			}
			nameText.addModifyListener(e -> {
				scenario.name = nameText.getText();
				section.setText(scenario.name);
				editor.setDirty(true);
			});

			// description
			Text descriptionText = UI.formMultiText(comp, tk);
			if (scenario.description != null) {
				descriptionText.setText(scenario.description);
			}
			descriptionText.addModifyListener(e -> {
				scenario.description = descriptionText.getText();
				editor.setDirty(true);
			});

			Action delete = Actions.onRemove(() -> {
				editor.getModel().scenarios.remove(scenario);
				section.dispose();
				form.reflow(true);
				editor.setDirty(true);
			});
			Actions.bind(section, delete);
		}

	}

}
