package org.openlca.app.editors.processes;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.editors.processes.data_quality.DataQualityShell;
import org.openlca.app.rcp.HtmlFolder;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.KmlUtil;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
		UI.gridLayout(inner, 3, 5, 0);

		// create product system
		Button b = toolkit.createButton(inner, M.CreateProductSystem, SWT.NONE);
		b.setImage(Images.get(ModelType.PRODUCT_SYSTEM, Overlay.NEW));
		Controls.onSelect(b, e -> ProcessToolbar.createSystem(getModel()));

		// direct calculation
		b = toolkit.createButton(inner, "Direct calculation", SWT.NONE);
		b.setImage(Icon.RUN.get());
		Controls.onSelect(b, e -> ProcessToolbar.directCalculation(getModel()));

		// export to Excel
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
		Supplier<String> dqLabel = () -> {
			Process p = getModel();
			return p.dqSystem == null || Strings.nullOrEmpty(p.dqEntry)
					? "(not specified)"
					: p.dqSystem.applyScoreLabels(p.dqEntry);
		};
		Hyperlink link = UI.formLink(parent, toolkit, dqLabel.get());
		Controls.onClick(link, e -> {
			if (getModel().dqSystem == null) {
				MsgBox.info("No data quality system is selected");
				return;
			}
			String oldVal = getModel().dqEntry;
			DQSystem system = getModel().dqSystem;
			String entry = getModel().dqEntry;
			DataQualityShell shell = DataQualityShell.withoutUncertainty(
					parent.getShell(), system, entry);
			shell.onOk = (_shell) -> {
				getModel().dqEntry = _shell.getSelection();
			};
			shell.onDelete = (_shell) -> {
				getModel().dqEntry = null;
			};
			shell.addDisposeListener(_e -> {
				if (Objects.equals(oldVal, getModel().dqEntry))
					return;
				link.setText(dqLabel.get());
				link.pack();
				getEditor().setDirty(true);
			});
			shell.open();
		});
		new CommentControl(parent, getToolkit(), "dqEntry", getComments());
		return link;
	}

	private void createGeographySection(Composite body) {
		Composite comp = UI.formSection(body, toolkit, M.Geography, 3);
		toolkit.createLabel(comp, M.Location);
		LocationViewer combo = new LocationViewer(comp);
		combo.setNullable(true);
		combo.setInput(Database.get());
		getBinding().onModel(() -> getModel(), "location", combo);
		combo.addSelectionChangedListener((s) -> {
			kmlLink.setText(KmlUtil.getDisplayText(getModel()));
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
		UI.gridData(kmlLink, true, false).horizontalSpan = 2;
		kmlLink.setText(KmlUtil.getDisplayText(getModel()));
		UI.filler(parent);

		// open the KML feature when the link is clicked
		Controls.onClick(kmlLink, e -> {
			Process p = getModel();
			String kml = null;
			if (p.location != null && p.location.kmz != null) {
				kml = KmlUtil.toKml(p.location.kmz);
			}
			if (Strings.nullOrEmpty(kml)) {
				MsgBox.info("No KML assigned",
						"The process has no location with KML assigned.");
				return;
			}

			// set the shell flags explicitly, otherwise the thing
			// cannot be resized on macOS
			Shell pshell = parent.getShell();
			Shell shell = new Shell(pshell, SWT.CLOSE
					| SWT.MAX | SWT.MIN | SWT.RESIZE);
			shell.setText(kmlLink.getText());
			int width = (int) (pshell.getSize().x * 2. / 3.);
			int height = (int) (width * 9. / 16.);
			shell.setSize(width, height);
			UI.center(pshell, shell);
			shell.setLayout(new FillLayout());
			Browser browser = new Browser(shell, SWT.NONE);
			browser.setJavascriptEnabled(true);
			shell.open();
			String _kml = kml;
			UI.onLoaded(browser, HtmlFolder.getUrl("kml_results.html"), () -> {
				JsonObject obj = new JsonObject();
				obj.addProperty("amount", 1.0);
				obj.addProperty("kml", _kml);
				browser.execute("addFeature(" + new Gson().toJson(obj) + ")");
			});

		});
	}
}
