package org.openlca.app.editors.projects.reports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.projects.reports.model.Report;
import org.openlca.app.editors.projects.reports.model.ReportComponent;
import org.openlca.app.editors.projects.reports.model.ReportSection;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;

class SectionList {

	private Report report;
	private Composite parent;
	private FormToolkit toolkit;
	private ScrolledForm form;

	private final List<SectionWidget> sections = new ArrayList<>();

	SectionList(ReportEditor editor, Composite parent, ScrolledForm form,
							FormToolkit toolkit) {
		this.parent = parent;
		this.toolkit = toolkit;
		this.form = form;
		report = editor.report;
		report.sections.sort(Comparator.comparingInt(s -> s.index));
		report.sections.forEach((model) -> sections.add(
			new SectionWidget(model, parent, toolkit)));
	}

	private void swapSection(int i, int j) {
		var sec1 = sections.get(i);
		var sec2 = sections.get(j);
		var model1 = sec1.model;
		var model2 = sec2.model;
		sec1.setModel(model2);
		sec2.setModel(model1);
		model1.index = j;
		model2.index = i;
		report.sections.sort(Comparator.comparingInt(s -> s.index));
	}

	void addNew() {
		var model = new ReportSection();
		model.index = report.sections.size();
		report.sections.add(model);
		model.title = M.NewSection;
		model.text = "";
		var widget = new SectionWidget(model, parent, toolkit);
		sections.add(widget);
		form.reflow(true);
		widget.ui.setFocus();
		// TODO: editor.setDirty(true);
	}

	private class SectionWidget {

		ReportSection model;
		final Section ui;
		Text titleText;
		Text descriptionText;
		ComboViewer componentViewer;

		SectionWidget(ReportSection model, Composite parent, FormToolkit tk) {
			this.model = Objects.requireNonNull(model);
			ui = UI.section(parent, tk, model.title);
			var comp = UI.sectionClient(ui, tk);
			createTitleText(comp, tk);
			createDescriptionText(comp, tk);
			createComponentViewer(comp, tk);
			createActions();
		}

		void setModel(ReportSection model) {
			this.model = model;
			String title = model.title != null ? model.title : "";
			String text = model.text != null ? model.text : "";
			ui.setText(title);
			titleText.setText(title);
			descriptionText.setText(text);

			var component = ReportComponent.getForId(model.componentId);
			componentViewer.setSelection(new StructuredSelection(component));
		}

		private void createTitleText(Composite comp, FormToolkit tk) {
			titleText = UI.formText(comp, tk, M.Section);
			if (model.title != null)
				titleText.setText(model.title);
			titleText.addModifyListener(e -> {
				String t = titleText.getText();
				model.title = t;
				ui.setText(t);
				// TODO: editor.setDirty(true);
			});
		}

		private void createDescriptionText(Composite comp, FormToolkit tk) {
			descriptionText = UI.formMultiText(comp, tk, M.Text);
			if (model.text != null)
				descriptionText.setText(model.text);
			descriptionText.addModifyListener(e -> {
				model.text = descriptionText.getText();
				// TODO: editor.setDirty(true);
			});
		}

		private void createComponentViewer(Composite comp, FormToolkit tk) {
			UI.formLabel(comp, tk, M.Component);
			componentViewer = new ComboViewer(comp);
			UI.gridData(componentViewer.getControl(), false, false).widthHint = 250;
			componentViewer.setContentProvider(ArrayContentProvider
				.getInstance());
			componentViewer.setLabelProvider(new ReportComponentLabel());
			componentViewer.setInput(ReportComponent.values());
			componentViewer.addSelectionChangedListener(e -> {
				ReportComponent c = Selections.firstOf(e);
				if (c == null || c == ReportComponent.NONE)
					model.componentId = null;
				else
					model.componentId = c.getId();
				// TODO: editor.setDirty(true);
			});
			if (model.componentId != null)
				componentViewer.setSelection(new StructuredSelection(
					ReportComponent.getForId(model.componentId)));
		}

		private void createActions() {
			var up = Actions.create(
				M.MoveUp, Icon.UP.descriptor(), this::moveUp);
			var down = Actions.create(
				M.MoveDown, Icon.DOWN.descriptor(), this::moveDown);
			var delete = Actions.create(
				M.DeleteSection, Icon.DELETE.descriptor(), this::delete);
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
				SectionWidget sec = sections.get(i);
				sec.model.index = i;
			}
			form.reflow(true);
			// TODO: editor.setDirty(true);
		}

		private void moveUp() {
			int i = model.index;
			if (i <= 0)
				return;
			swapSection(i, i - 1);
		}

		private void moveDown() {
			int i = model.index;
			if (i >= (sections.size() - 1))
				return;
			swapSection(i, i + 1);
		}
	}
}
