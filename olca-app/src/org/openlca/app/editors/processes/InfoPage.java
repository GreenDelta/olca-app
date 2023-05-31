package org.openlca.app.editors.processes;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
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
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;

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

		// create product system
		var b = UI.button(inner, tk, M.CreateProductSystem);
		b.setImage(Images.get(ModelType.PRODUCT_SYSTEM, Overlay.NEW));
		Controls.onSelect(b, e -> ProcessToolbar.createSystem(getModel()));

		// direct calculation
		if (withDirect) {
			b = UI.button(inner, tk, "Direct calculation");
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

		// the handler for setting the start or end time
		BiConsumer<DateTime, Boolean> setTime = (widget, isStart) -> {
			var current = isStart
				? getModel().documentation.validFrom
				: getModel().documentation.validUntil;
			if (current != null) {
				var cal = new GregorianCalendar();
				cal.setTime(current);
				widget.setDate(
					cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH),
					cal.get(Calendar.DAY_OF_MONTH));
			}

			widget.addSelectionListener(Controls.onSelect(_e -> {
				var process = getModel();
				var selected = new GregorianCalendar(
					widget.getYear(), widget.getMonth(), widget.getDay()).getTime();
				var date = isStart
					? process.documentation.validFrom
					: process.documentation.validUntil;
				if (Objects.equals(date, selected))
					return;
				if (isStart) {
					process.documentation.validFrom = selected;
				} else {
					process.documentation.validUntil = selected;
				}
				getEditor().setDirty(true);
			}));
		};

		// start date
		UI.label(comp, tk, M.StartDate);
		var startBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		UI.gridData(startBox, false, false).minimumWidth = 150;
		new CommentControl(comp, tk, "documentation.validFrom", getComments());
		setTime.accept(startBox, true);

		// end date
		UI.label(comp, tk, M.EndDate);
		var endBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);		endBox.setEnabled(isEditable());
		UI.gridData(endBox, false, false).minimumWidth = 150;
		new CommentControl(comp, tk, "documentation.validUntil", getComments());
		setTime.accept(endBox, false);

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
			return p.dqSystem == null || Strings.nullOrEmpty(p.dqEntry)
				? "(not specified)"
				: p.dqSystem.applyScoreLabels(p.dqEntry);
		};
		Hyperlink link = UI.hyperLink(parent, tk, dqLabel.get());
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
