package org.openlca.app.editors.systems;

import org.eclipse.jface.action.Action;
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
import org.openlca.core.model.ParameterRedefSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.List;

class ParameterPage extends ModelPage<ProductSystem> {

	private final ProductSystemEditor editor;
	private ScrolledForm form;
	private FormToolkit tk;
	private Composite body;
	private AddButton addButton;
	private final List<ParameterSection> sections = new ArrayList<>();

	public ParameterPage(ProductSystemEditor editor) {
		super(editor, "ParameterPage2", "Parameters");
		this.editor = editor;
		editor.onSaved(() -> sections.forEach(ParameterSection::update));
	}

	private List<ParameterRedefSet> paramSets() {
		return editor.getModel().parameterSets;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.header(this);
		tk = mform.getToolkit();
		body = UI.body(form, tk);

		// create the baseline scenario if necessary
		var base = getModel().parameterSets
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
		var sets = paramSets();
		sets.sort((s1, s2) -> {
			if (s1.isBaseline)
				return -1;
			if (s2.isBaseline)
				return 1;
			return Strings.compare(s1.name, s2.name);
		});
		for (int i = 0; i < sets.size(); i++) {
			var s = sets.get(i);
			sections.add(new ParameterSection(i, s));
		}

		// render the add button at the end of the list
		addButton = new AddButton();
		form.reflow(true);
	}

	private void addNew(ParameterRedefSet s) {
		var sets = paramSets();
		var pos = sets.size();
		sets.add(s);
		sections.add(new ParameterSection(pos, s));
		addButton.render();
		form.reflow(true);
		editor.setDirty(true);
	}

	private class ParameterSection {

		int pos;
		ParameterRedefSet paramSet;
		Section section;
		ParameterRedefTable paramTable;

		ParameterSection(int pos, ParameterRedefSet set) {
			this.pos = pos;
			this.paramSet = set;
			render();
		}

		void render() {
			section = UI.section(body, tk,
					paramSet.name != null ? paramSet.name : "");
			UI.gridData(section, true, false);
			Composite comp = UI.sectionClient(section, tk);
			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 1);

			Composite textComp = UI.composite(comp, tk);
			UI.gridData(textComp, true, false);
			UI.gridLayout(textComp, 2, 10, 0);

			// name
			Text nameText = UI.labeledText(textComp, tk, M.Name);
			if (paramSet.name != null) {
				nameText.setText(paramSet.name);
			}
			nameText.addModifyListener(e -> {
				paramSet.name = nameText.getText();
				section.setText(paramSet.name);
				editor.setDirty(true);
			});

			// description
			Text descrText = UI.labeledMultiText(
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
			paramTable.create(UI.sectionClient(paramSection, tk));
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
			var sets = paramSets();
			for (int i = 0; i < sets.size(); i++) {
				if (i == pos) {
					this.paramSet = sets.get(i);
					paramTable.update();
					break;
				}
			}
		}

		void onRemove() {
			var sets = paramSets();
			sets.remove(pos);
			sections.remove(pos);
			for (var section : sections) {
				if (section.pos > pos) {
					section.pos--;
				}
			}
			section.dispose();
			form.reflow(true);
			editor.setDirty(true);
		}

		void onCopy() {
			var s = new ParameterRedefSet();
			s.name = paramSet.name + " - Copy";
			s.description = paramSet.description;
			for (var redef : paramSet.parameters) {
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
			comp = UI.composite(body, tk);
			var grid = UI.gridLayout(comp, 1);
			grid.marginLeft = 0;
			grid.marginTop = 0;
			grid.marginBottom = 10;
			var btn = UI.button(comp, tk, "Add parameter set");
			btn.setImage(Icon.ADD.get());
			Controls.onSelect(btn, e -> {
				var s = new ParameterRedefSet();
				s.name = "New parameter set";
				addNew(s);
			});
		}
	}
}
