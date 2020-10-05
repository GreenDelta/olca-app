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
import org.openlca.app.M;
import org.openlca.app.editors.projects.ProjectEditor;
import org.openlca.app.editors.reports.model.Report;
import org.openlca.app.editors.reports.model.ReportComponent;
import org.openlca.app.editors.reports.model.ReportSection;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;

class SectionList {

	private Report report;
	private ProjectEditor editor;
	private Composite parent;
	private FormToolkit toolkit;
	private ScrolledForm form;

	private List<Sec> sections = new ArrayList<>();

	SectionList(ProjectEditor editor, Composite parent, ScrolledForm form,
			FormToolkit toolkit) {
		this.editor = editor;
		report = editor.getReport();
		this.parent = parent;
		this.toolkit = toolkit;
		this.form = form;
		report.sections.sort((s1, s2) -> s1.index - s2.index);
		report.sections.forEach((model) -> sections.add(new Sec(model)));
	}

	private void swap(int i, int j) {
		Sec sec1 = sections.get(i);
		Sec sec2 = sections.get(j);
		ReportSection model1 = sec1.model;
		ReportSection model2 = sec2.model;
		sec1.setModel(model2);
		sec2.setModel(model1);
		model1.index = j;
		model2.index = i;
		report.sections.sort((s1, s2) -> s1.index - s2.index);
	}

	void addNew() {
		ReportSection model = new ReportSection();
		model.index = report.sections.size();
		report.sections.add(model);
		model.title = M.NewSection;
		model.text = "";
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
			String title = model.title != null ? model.title : "";
			String text = model.text != null ? model.text : "";
			ReportComponent component = ReportComponent.getForId(model.componentId);
			ui.setText(title);
			titleText.setText(title);
			descriptionText.setText(text);
			componentViewer.setSelection(new StructuredSelection(component));
		}

		private void createUi() {
			ui = UI.section(parent, toolkit, model.title);
			Composite comp = UI.sectionClient(ui, toolkit);
			createTitleText(comp);
			createDescriptionText(comp);
			createComponentViewer(comp);
			createActions();
		}

		private void createTitleText(Composite comp) {
			titleText = UI.formText(comp, toolkit, M.Section);
			if (model.title != null)
				titleText.setText(model.title);
			titleText.addModifyListener(e -> {
				String t = titleText.getText();
				model.title = t;
				ui.setText(t);
				editor.setDirty(true);
			});
		}

		private void createDescriptionText(Composite comp) {
			descriptionText = UI.formMultiText(comp, toolkit, M.Text);
			if (model.text != null)
				descriptionText.setText(model.text);
			descriptionText.addModifyListener(e -> {
				model.text = descriptionText.getText();
				editor.setDirty(true);
			});
		}

		private void createComponentViewer(Composite composite) {
			UI.formLabel(composite, toolkit, M.Component);
			componentViewer = new ComboViewer(composite);
			UI.gridData(componentViewer.getControl(), false, false).widthHint = 250;
			componentViewer.setContentProvider(ArrayContentProvider
					.getInstance());
			componentViewer.setLabelProvider(new ComponentLabel());
			componentViewer.setInput(ReportComponent.values());
			componentViewer.addSelectionChangedListener((evt) -> {
				ReportComponent c = Viewers.getFirst(evt.getSelection());
				if (c == null || c == ReportComponent.NONE)
					model.componentId = null;
					else
						model.componentId = c.getId();
					editor.setDirty(true);
				});
			if (model.componentId != null)
				componentViewer.setSelection(new StructuredSelection(
						ReportComponent.getForId(model.componentId)));
		}

		private void createActions() {
			Action up = Actions.create(
					M.MoveUp,
					Icon.UP.descriptor(),
					() -> moveUp());
			Action down = Actions.create(
					M.MoveDown,
					Icon.DOWN.descriptor(),
					() -> moveDown());
			Action delete = Actions.create(
					M.DeleteSection,
					Icon.DELETE.descriptor(),
					() -> delete());
			Actions.bind(ui, up, down, delete);
		}

		private void delete() {
			boolean b = Question.ask(M.DeleteSection,
					M.DeleteReportSectionQuestion);
			if (!b)
				return;
			report.sections.remove(model);
			sections.remove(this);
			ui.dispose();
			for (int i = model.index; i < sections.size(); i++) {
				Sec sec = sections.get(i);
				sec.model.index = i;
			}
			form.reflow(true);
			editor.setDirty(true);
		}

		private void moveUp() {
			int i = model.index;
			if (i <= 0)
				return;
			swap(i, i - 1);
		}

		private void moveDown() {
			int i = model.index;
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
				return M.None;
			case VARIANT_DESCRIPTION_TABLE:
				return M.VariantDescriptionTable;
			case INDICATOR_DESCRIPTION_TABLE:
				return M.LciaCategoryDescriptionTable;
			case PARAMETER_DESCRIPTION_TABLE:
				return M.ParameterDescriptionTable;
			case PARAMETER_VALUE_TABLE:
				return M.ParameterValueTable;
			case IMPACT_RESULT_TABLE:
				return M.LciaResultTable;
			case PROCESS_CONTRIBUTION_CHART:
				return M.ProcessContributionChart;
			case NORMALISATION_RESULT_TABLE:
				return M.NormalisationResultTable;
			case SINGLE_SCORE_TABLE:
				return M.SingleScoreTable;
			case INDICATOR_BAR_CHART:
				return M.IndicatorBarChart;
			case NORMALISATION_BAR_CHART:
				return M.NormalisationBarChart;
			case NORMALISATION_RADAR_CHART:
				return M.NormalisationRadarChart;
			case RELATIVE_INDICATOR_BAR_CHART:
				return M.RelativeLciaResultsBarChart;
			case RELATIVE_INDICATOR_RADAR_CHART:
				return M.RelativeLciaResultsRadarChart;
			case SINGLE_SCORE_BAR_CHART:
				return M.SingleScoreBarChart;
			case LCC_ADDED_VALUES_TABLE:
				return M.LCCAddedValuesTable;
			case LCC_NET_COSTS_TABLE:
				return M.LCCNetcostsTable;
			default:
				return M.Unknown;
			}
		}
	}
}
