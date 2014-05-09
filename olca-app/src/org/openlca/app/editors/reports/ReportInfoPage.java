package org.openlca.app.editors.reports;

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
import org.openlca.app.editors.DataBinding.TextBindType;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;

class ReportInfoPage extends FormPage {

	private Report report;
	private DataBinding binding;

	private FormToolkit toolkit;

	public ReportInfoPage(ReportEditor editor, Report report) {
		super(editor, "ReportInfoPage", "Report");
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
		binding.on(report, "title", DataBinding.TextBindType.STRING, titleText);
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
		binding.on(reportSection, "title", TextBindType.STRING, titleText);
		titleText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				section.setText(titleText.getText());
			}
		});
		Text descriptionText = UI.formMultiText(composite, toolkit, "Text");
		binding.on(reportSection, "text", TextBindType.STRING, descriptionText);
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

}
