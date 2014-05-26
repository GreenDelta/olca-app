package org.openlca.app.editors.reports;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.editors.DataBinding;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;

class ReportInfoPage extends FormPage {

	private Report report;
	private ReportEditor editor;
	private DataBinding binding;

	private FormToolkit toolkit;

	public ReportInfoPage(ReportEditor editor, Report report) {
		super(editor, "ReportInfoPage", "Report");
		this.editor = editor;
		this.report = report;
		this.binding = new DataBinding(editor);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Report for project: "
				+ Labels.getDisplayName(report.getProject()));
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		for (ReportSection reportSection : report.getSections())
			createSection(reportSection, body);
		createParameternamesSection(body);
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeneralInformation);
		Text titleText = UI.formText(composite, toolkit, "Title");
		binding.onString(() -> report, "title", titleText);
		UI.formLabel(composite, toolkit, Messages.Project);
		ImageHyperlink link = toolkit.createImageHyperlink(composite, SWT.TOP);
		link.setText(Labels.getDisplayName(report.getProject()));
		link.setForeground(Colors.getLinkBlue());
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				App.openEditor(report.getProject());
			}
		});
		createPreviewButton(composite);
	}

	private void createPreviewButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, "Preview", SWT.NONE);
		button.setImage(ImageType.SEARCH_ICON.get());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ReportViewer.open(report);
			}
		});
	}

	private void createSection(ReportSection reportSection, Composite body) {
		final Section section = UI.section(body, toolkit,
				reportSection.getTitle());
		Composite composite = UI.sectionClient(section, toolkit);
		final Text titleText = UI.formText(composite, toolkit, "Section");
		binding.onString(() -> reportSection, "title", titleText);
		titleText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				section.setText(titleText.getText());
			}
		});
		Text descriptionText = UI.formMultiText(composite, toolkit, "Text");
		binding.onString(() -> reportSection, "text", descriptionText);
		createComponentViewer(reportSection, composite);
	}

	private void createComponentViewer(final ReportSection reportSection,
			Composite composite) {
		UI.formLabel(composite, toolkit, "Component");
		ComboViewer viewer = new ComboViewer(composite);
		UI.gridData(viewer.getControl(), false, false).widthHint = 250;
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new ComponentLabel());
		viewer.setInput(ReportComponent.values());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent evt) {
				ReportComponent component = Viewers.getFirst(evt.getSelection());
				if (component == null || component == ReportComponent.NONE)
					reportSection.setComponentId(null);
				else
					reportSection.setComponentId(component.getId());
				editor.setDirty(true);
			}
		});
	}

	private void createParameternamesSection(Composite body) {
		Section section = UI.section(body, toolkit, "Parameter mapping");
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		String[] properties = { Messages.Parameter, Messages.Name };
		TableViewer viewer = Tables.createViewer(composite, properties);
		Tables.bindColumnWidths(viewer, 0.5, 0.5);
		UI.gridData(viewer.getTable(), true, true).minimumHeight = 150;
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
			case PARAMETER_TABLE:
				return "Parameter table";
			case RESULT_CHART:
				return "Result chart";
			case RESULT_TABLE:
				return "Result table";
			case VARIANT_TABLE:
				return "Project variant table";
			default:
				return "unknown";
			}
		}
	}
}
