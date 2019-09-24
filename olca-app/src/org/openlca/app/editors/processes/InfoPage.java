package org.openlca.app.editors.processes;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.editors.processes.data_quality.DataQualityShell;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.KmlUtil;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

class InfoPage extends ModelPage<Process> {

	private FormToolkit toolkit;
	private ImageHyperlink kmlLink;
	private ScrolledForm form;

	InfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.GeneralInformation);
		editor.getEventBus().register(this);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		InfoSection infoSection = new InfoSection(getEditor());
		infoSection.render(body, toolkit);
		checkBox(infoSection.getContainer(), M.InfrastructureProcess, "infrastructureProcess");
		createButtons(infoSection.getContainer());
		createTimeSection(body);
		createGeographySection(body);
		createTechnologySection(body);
		new ImageSection(getEditor(), toolkit, body);
		createDqSection(body);
		body.setFocus();
		form.reflow(true);
	}

	private void createButtons(Composite comp) {
		UI.filler(comp, toolkit);
		Composite inner = toolkit.createComposite(comp);
		UI.gridLayout(inner, 2, 5, 0);
		Button b = toolkit.createButton(inner, M.CreateProductSystem, SWT.NONE);
		b.setImage(Images.get(ModelType.PRODUCT_SYSTEM, Overlay.NEW));
		Controls.onSelect(b, e -> ProcessToolbar.createSystem(getModel()));
		b = toolkit.createButton(inner, M.ExportToExcel, SWT.NONE);
		b.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(b, e -> ProcessToolbar.exportToExcel(getModel()));
	}

	private void createTechnologySection(Composite body) {
		Composite composite = UI.formSection(body, toolkit, M.Technology, 3);
		multiText(composite, M.Description, "documentation.technology", 40);
	}

	private void createTimeSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit, M.Time, 3);
		date(composite, M.StartDate, "documentation.validFrom");
		date(composite, M.EndDate, "documentation.validUntil");
		multiText(composite, M.Description, "documentation.time", 40);
	}

	private void createDqSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit, M.DataQuality, 3);
		createDqViewer(composite, M.ProcessSchema, "dqSystem");
		createDqEntryRow(composite);
		createDqViewer(composite, M.FlowSchema, "exchangeDqSystem");
		createDqViewer(composite, M.SocialSchema, "socialDqSystem");
	}

	private void createDqViewer(Composite parent, String label, String property) {
		toolkit.createLabel(parent, label);
		DQSystemViewer processSystemViewer = new DQSystemViewer(parent);
		processSystemViewer.setNullable(true);
		processSystemViewer.setInput(Database.get());
		getBinding().onModel(() -> getModel(), property, processSystemViewer);
		new CommentControl(parent, getToolkit(), property, getComments());
	}

	private Hyperlink createDqEntryRow(Composite parent) {
		UI.formLabel(parent, toolkit, M.DataQualityEntry);
		Hyperlink link = UI.formLink(parent, toolkit, getDqLabel());
		Controls.onClick(link, e -> {
			if (getModel().dqSystem == null) {
				MsgBox.error("Please select a data quality system first");
				return;
			}
			String oldVal = getModel().dqEntry;
			DQSystem system = getModel().dqSystem;
			String entry = getModel().dqEntry;
			DataQualityShell shell = DataQualityShell.withoutUncertainty(parent.getShell(), system, entry);
			shell.onOk = InfoPage.this::onDqEntryDialogOk;
			shell.onDelete = InfoPage.this::onDqEntryDialogDelete;
			shell.addDisposeListener(e2 -> {
				if (Objects.equals(oldVal, getModel().dqEntry))
					return;
				link.setText(getDqLabel());
				link.pack();
				getEditor().setDirty(true);
			});
			shell.open();
		});
		new CommentControl(parent, getToolkit(), "dqEntry", getComments());
		return link;
	}

	private void onDqEntryDialogOk(DataQualityShell shell) {
		getModel().dqEntry = shell.getSelection();
	}

	private void onDqEntryDialogDelete(DataQualityShell shell) {
		getModel().dqEntry = null;
	}

	private String getDqLabel() {
		Process p = getModel();
		if (p.dqEntry == null)
			return "(not specified)";
		if (p.dqSystem == null)
			return p.dqEntry;
		int[] vals = p.dqSystem.toValues(p.dqEntry);
		if (vals == null)
			return p.dqEntry;
		String label = "(";
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == 0) {
				label += "n.a.";
			} else {
				String e = p.dqSystem.getScoreLabel(vals[i]);
				if (e == null)
					return p.dqEntry;
				label += e;
			}
			if (i < (vals.length - 1)) {
				label += "; ";
			}
		}
		return label + ")";
	}

	private void createGeographySection(Composite body) {
		Composite comp = UI.formSection(body, toolkit, M.Geography, 3);
		toolkit.createLabel(comp, M.Location);
		LocationViewer combo = new LocationViewer(comp);
		combo.setNullable(true);
		combo.setInput(Database.get());
		getBinding().onModel(() -> getModel(), "location", combo);
		combo.addSelectionChangedListener((s) -> {
			kmlLink.setText(kmlLabel());
			kmlLink.getParent().pack();
		});
		new CommentControl(comp, getToolkit(), "location", getComments());
		createKmlSection(comp);
		multiText(comp, M.Description, "documentation.geography", 40);
	}

	private void createKmlSection(Composite parent) {
		UI.formLabel(parent, "KML");
		Composite comp = toolkit.createComposite(parent);
		UI.gridData(comp, true, true);
		GridLayout layout = UI.gridLayout(comp, 2);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		kmlLink = toolkit.createImageHyperlink(comp, SWT.TOP);
		Controls.onClick(kmlLink, e -> {
			Process p = getModel();
			if (p.location != null) {
				App.openEditor(p.location);
			}
		});
		UI.gridData(kmlLink, true, false).horizontalSpan = 2;
		kmlLink.setText(kmlLabel());
		UI.filler(parent);
	}

	private String kmlLabel() {
		Location loc = getModel().location;
		if (loc == null || loc.kmz == null)
			return "none";
		return KmlUtil.getDisplayText(loc.kmz);
	}

}
