package org.openlca.app.editors.processes;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.editors.processes.data_quality.DataQualityShell;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;

class InfoPage extends ModelPage<Process> {

	InfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.GeneralInformation);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var info = new InfoSection(getEditor());
		info.render(body, tk);
		checkBox(info.composite(),
				M.InfrastructureProcess, "infrastructureProcess");
		createButtons(info.composite(), tk);
		createTimeSection(body, tk);
		createGeographySection(body, tk);
		createTechnologySection(body, tk);
		new ImageSection(getEditor(), tk, body);
		createDqSection(body, tk);
		body.setFocus();
		form.reflow(true);
	}

	private void createButtons(Composite comp, FormToolkit tk) {
		UI.filler(comp, tk);
		var inner = UI.composite(comp, tk);
		UI.gridLayout(inner, 5);

		// we can only support direct calculations when no
		// libraries are bound to the database
		boolean withDirect = Database.get()
				.getLibraries()
				.isEmpty();
		int columns = withDirect ? 3 : 2;
		UI.gridLayout(inner, columns, 5, 0);

		// create a product system
		var b = UI.button(inner, tk, M.CreateProductSystem);
		b.setImage(Images.get(ModelType.PRODUCT_SYSTEM, Overlay.NEW));
		Controls.onSelect(b, e -> ProcessToolbar.createSystem(getModel()));

		// direct calculation
		if (withDirect) {
			b = UI.button(inner, tk, M.DirectCalculation);
			b.setImage(Icon.RUN.get());
			Controls.onSelect(
					b, e -> ProcessToolbar.directCalculation(getModel()));
		}

		// export to Excel
		b = UI.button(inner, tk, M.ExportToExcel);
		b.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(b, e -> ProcessToolbar.exportToExcel(getModel()));
	}

	private void createTechnologySection(Composite body, FormToolkit tk) {
		Composite comp = UI.formSection(body, tk, M.Technology, 3);
		multiText(comp, M.Description, "documentation.technology");
	}

	private void createTimeSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Time, 3);

		// start date
		var startBox = UI.date(comp, tk, M.StartDate, getModel().documentation.validFrom, selected -> {
			var doc = getModel().documentation;
			if (Objects.equals(doc.validFrom, selected))
				return;
			doc.validFrom = selected;
			getEditor().setDirty(true);
		});
		startBox.setEnabled(isEditable());
		new CommentControl(comp, tk, "documentation.validFrom", getComments());

		// end date
		var endBox = UI.date(comp, tk, M.EndDate, getModel().documentation.validUntil, selected -> {
			var doc = getModel().documentation;
			if (Objects.equals(doc.validUntil, selected))
				return;
			doc.validUntil = selected;
			getEditor().setDirty(true);
		});
		endBox.setEnabled(isEditable());
		new CommentControl(comp, tk, "documentation.validUntil", getComments());

		// the description text
		multiText(comp, M.Description, "documentation.time", 40);
	}

	private void createDqSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.DataQuality, 3);
		modelLink(comp, M.ProcessSchema, "dqSystem");
		createDqEntryRow(comp, tk);
		modelLink(comp, M.FlowSchema, "exchangeDqSystem");
		modelLink(comp, M.SocialSchema, "socialDqSystem");
	}

	private void createDqEntryRow(Composite parent, FormToolkit tk) {
		UI.label(parent, tk, M.DataQualityEntry);
		Supplier<String> dqLabel = () -> {
			Process p = getModel();
			return p.dqSystem == null || Strings.isBlank(p.dqEntry)
					? "(not specified)"
					: p.dqSystem.applyScoreLabels(p.dqEntry);
		};
		Hyperlink link = UI.hyperLink(parent, tk, dqLabel.get());
		Controls.onClick(link, e -> {
			if (getModel().dqSystem == null) {
				MsgBox.info(M.NoDataQualitySystemSelected);
				return;
			}
			String oldVal = getModel().dqEntry;
			DQSystem system = getModel().dqSystem;
			String entry = getModel().dqEntry;
			DataQualityShell shell = DataQualityShell.withoutUncertainty(
					parent.getShell(), system, entry);
			shell.onOk = s -> getModel().dqEntry = s.getSelection();
			shell.onDelete = s -> getModel().dqEntry = null;
			shell.addDisposeListener($ -> {
				if (Objects.equals(oldVal, getModel().dqEntry))
					return;
				link.setText(dqLabel.get());
				link.pack();
				getEditor().setDirty(true);
			});
			shell.open();
		});
		link.setEnabled(isEditable());
		new CommentControl(parent, getToolkit(), "dqEntry", getComments());
	}

	private void createGeographySection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.Geography, 3);
		modelLink(comp, M.Location, "location");
		multiText(comp, M.Description, "documentation.geography", 40);
	}
}
