package org.openlca.app.editors.reports;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.editors.projects.ProjectEditor;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportComponent;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;

class SectionList {

	private Report report;
	private ProjectEditor editor;
	private DataBinding binding;
	private Composite parent;
	private FormToolkit toolkit;
	private ScrolledForm form;

	private List<Sec> sections = new ArrayList<>();

	SectionList(ProjectEditor editor, Composite parent, ScrolledForm form,
			FormToolkit toolkit) {
		this.editor = editor;
		this.binding = new DataBinding(editor);
		report = editor.getReport();
		this.parent = parent;
		this.toolkit = toolkit;
		this.form = form;
		report.getSections().sort((s1, s2) -> s1.getIndex() - s2.getIndex());
		report.getSections().forEach((model) -> sections.add(new Sec(model)));
	}

	private void swap(int i, int j) {
		Sec sec1 = sections.get(i);
		Sec sec2 = sections.get(j);
		ReportSection model1 = sec1.model;
		ReportSection model2 = sec2.model;
		sec1.setModel(model2);
		sec2.setModel(model1);
		model1.setIndex(j);
		model2.setIndex(i);
		report.getSections().sort((s1, s2) -> s1.getIndex() - s2.getIndex());
	}

	void addNew() {
		ReportSection model = new ReportSection();
		model.setIndex(report.getSections().size());
		report.getSections().add(model);
		model.setTitle("New section");
		model.setText("TODO: add text");
		Sec section = new Sec(model);
		sections.add(section);
		form.reflow(true);
		section.ui.setFocus();
		editor.setDirty(true);
	}

	private class Sec {

		ReportSection model;
		Section ui;
		Text titleText;
		Text descriptionText;
		ComboViewer componentViewer;

		Sec(ReportSection model) {
			this.model = model;
			createUi();
		}

		void setModel(ReportSection model) {
			this.model = model;
			String title = model.getTitle() != null ? model.getTitle() : "";
			String text = model.getText() != null ? model.getText() : "";
			ReportComponent component = ReportComponent.getForId(
					model.getComponentId());
			ui.setText(title);
			titleText.setText(title);
			descriptionText.setText(text);
			componentViewer.setSelection(new StructuredSelection(component));
		}

		private void createUi() {
			ui = UI.section(parent, toolkit, model.getTitle());
			Composite composite = UI.sectionClient(ui, toolkit);
			titleText = UI.formText(composite, toolkit, "Section");
			binding.onString(() -> model, "title", titleText);
			titleText.addModifyListener((e) -> {
				ui.setText(titleText.getText());
			});
			descriptionText = UI.formMultiText(composite, toolkit, "Text");
			binding.onString(() -> model, "text", descriptionText);
			createComponentViewer(composite);
			createActions();
		}

		private void createComponentViewer(Composite composite) {
			UI.formLabel(composite, toolkit, "Component");
			componentViewer = new ComboViewer(composite);
			UI.gridData(componentViewer.getControl(), false, false).widthHint = 250;
			componentViewer.setContentProvider(ArrayContentProvider
					.getInstance());
			componentViewer.setLabelProvider(new ComponentLabel());
			componentViewer.setInput(ReportComponent.values());
			componentViewer
					.addSelectionChangedListener((evt) -> {
						ReportComponent component = Viewers.getFirst(evt
								.getSelection());
						if (component == null
								|| component == ReportComponent.NONE)
							model.setComponentId(null);
						else
							model.setComponentId(component.getId());
						editor.setDirty(true);
					});
			if (model.getComponentId() != null)
				componentViewer.setSelection(new StructuredSelection(
						ReportComponent.getForId(model.getComponentId())));
		}

		private void createActions() {
			Action up = Actions.create(
					"Move up",
					ImageType.UP_16.getDescriptor(),
					() -> moveUp());
			Action down = Actions.create(
					"Move down",
					ImageType.DOWN_16.getDescriptor(),
					() -> moveDown());
			Action delete = Actions.create(
					"Delete section",
					ImageType.DELETE_ICON.getDescriptor(),
					() -> delete());
			Actions.bind(ui, up, down, delete);
		}

		private void delete() {
			boolean b = Question.ask("Delete section",
					"Do you really want to delete this report section?");
			if (!b)
				return;
			report.getSections().remove(model);
			sections.remove(this);
			ui.dispose();
			for (int i = model.getIndex(); i < sections.size(); i++) {
				Sec sec = sections.get(i);
				sec.model.setIndex(i);
			}
			form.reflow(true);
			editor.setDirty(true);
		}

		private void moveUp() {
			int i = model.getIndex();
			if (i <= 0)
				return;
			swap(i, i - 1);
		}

		private void moveDown() {
			int i = model.getIndex();
			if (i >= (sections.size() - 1))
				return;
			swap(i, i + 1);
		}
	}

	private class ComponentLabel extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (!(element instanceof ReportComponent))
				return null;
			ReportComponent component = (ReportComponent) element;
			return getLabel(component);
		}

		private String getLabel(ReportComponent component) {
			switch (component) {
			case NONE:
				return "None";
			case VARIANT_DESCRIPTION_TABLE:
				return "Variant description table";
			case INDICATOR_DESCRIPTION_TABLE:
				return "LCIA category description table";
			case PARAMETER_DESCRIPTION_TABLE:
				return "Parameter description table";
			case PARAMETER_VALUE_TABLE:
				return "Parameter value table";
			case IMPACT_RESULT_TABLE:
				return "LCIA result table";
			case PROCESS_CONTRIBUTION_CHARTS:
				return "Process contribution charts";
			case RESULT_CHART:
				return "Result chart";
			case NORMALISATION_RESULT_TABLE:
				return "Normalisation result table";
			case SINGLE_SCORE_TABLE:
				return "Single score table";
			default:
				return "unknown";
			}
		}
	}
}
