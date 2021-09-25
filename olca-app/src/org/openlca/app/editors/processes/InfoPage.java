package org.openlca.app.editors.processes;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.M;
import org.openlca.app.components.mapview.MapDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.InfoSection;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentControl;
import org.openlca.app.editors.processes.data_quality.DataQualityShell;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.LocationViewer;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.util.Strings;

class InfoPage extends ModelPage<Process> {

	private ImageHyperlink geoLink;

	InfoPage(ProcessEditor editor) {
		super(editor, "ProcessInfoPage", M.GeneralInformation);
		editor.getEventBus().register(this);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		var tk = mform.getToolkit();
		var body = UI.formBody(form, tk);
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
		var inner = tk.createComposite(comp);

		// we can only support direct calculations when no
		// libraries are bound to the database
		boolean withDirect = Database.get()
				.getLibraries()
				.isEmpty();
		int columns = withDirect ? 3 : 2;
		UI.gridLayout(inner, columns, 5, 0);

		// create product system
		var b = tk.createButton(inner, M.CreateProductSystem, SWT.NONE);
		b.setImage(Images.get(ModelType.PRODUCT_SYSTEM, Overlay.NEW));
		Controls.onSelect(b, e -> ProcessToolbar.createSystem(getModel()));

		// direct calculation
		if (withDirect) {
			b = tk.createButton(inner, "Direct calculation", SWT.NONE);
			b.setImage(Icon.RUN.get());
			Controls.onSelect(
					b, e -> ProcessToolbar.directCalculation(getModel()));
		}

		// export to Excel
		b = tk.createButton(inner, M.ExportToExcel, SWT.NONE);
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
				var date = new GregorianCalendar(
						widget.getYear(),
						widget.getMonth(),
						widget.getDay()).getTime();
				if (isStart) {
					process.documentation.validFrom = date;
				} else {
					process.documentation.validUntil = date;
				}
				getEditor().setDirty(true);
			}));
		};

		// start date
		tk.createLabel(comp, M.StartDate, SWT.NONE);
		var startBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		startBox.setEnabled(isEditable());
		UI.gridData(startBox, false, false).widthHint = 150;
		new CommentControl(comp, tk, "documentation.validFrom", getComments());
		setTime.accept(startBox, true);

		// end date
		tk.createLabel(comp, M.EndDate, SWT.NONE);
		var endBox = new DateTime(comp, SWT.DATE | SWT.DROP_DOWN);
		endBox.setEnabled(isEditable());
		UI.gridData(endBox, false, false).widthHint = 150;
		new CommentControl(comp, tk, "documentation.validUntil", getComments());
		setTime.accept(endBox, false);

		// the description text
		multiText(comp, M.Description, "documentation.time", 40);
	}

	private void createDqSection(Composite body, FormToolkit tk) {
		Composite comp = UI.formSection(body, tk, M.DataQuality, 3);
		createDqViewer(comp, tk, M.ProcessSchema, "dqSystem");
		createDqEntryRow(comp, tk);
		createDqViewer(comp, tk, M.FlowSchema, "exchangeDqSystem");
		createDqViewer(comp, tk, M.SocialSchema, "socialDqSystem");
	}

	private void createDqViewer(Composite comp, FormToolkit tk,
			String label, String property) {
		tk.createLabel(comp, label);
		DQSystemViewer dqViewer = new DQSystemViewer(comp);
		dqViewer.setNullable(true);
		dqViewer.setInput(Database.get());
		getBinding().onModel(this::getModel, property, dqViewer);
		dqViewer.setEnabled(isEditable());
		new CommentControl(comp, getToolkit(), property, getComments());
	}

	private Hyperlink createDqEntryRow(Composite parent, FormToolkit tk) {
		UI.formLabel(parent, tk, M.DataQualityEntry);
		Supplier<String> dqLabel = () -> {
			Process p = getModel();
			return p.dqSystem == null || Strings.nullOrEmpty(p.dqEntry)
					? "(not specified)"
					: p.dqSystem.applyScoreLabels(p.dqEntry);
		};
		Hyperlink link = UI.formLink(parent, tk, dqLabel.get());
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
		link.setEnabled(isEditable());
		new CommentControl(parent, getToolkit(), "dqEntry", getComments());
		return link;
	}

	private void createGeographySection(Composite body, FormToolkit tk) {
		Composite comp = UI.formSection(body, tk, M.Geography, 3);
		tk.createLabel(comp, M.Location);
		LocationViewer combo = new LocationViewer(comp);
		combo.setNullable(true);
		combo.setInput(Database.get());
		getBinding().onModel(this::getModel, "location", combo);
		combo.setEnabled(isEditable());
		combo.addSelectionChangedListener(loc -> {
			String linkText = loc != null && loc.geodata != null
					? "open in map"
					: "no data";
			geoLink.setText(linkText);
			geoLink.getParent().pack();
		});
		new CommentControl(comp, getToolkit(), "location", getComments());
		createGeoLink(comp, tk);
		multiText(comp, M.Description, "documentation.geography", 40);
	}

	private void createGeoLink(Composite parent, FormToolkit tk) {
		UI.formLabel(parent, "Geographic data");
		Composite comp = tk.createComposite(parent);
		UI.gridData(comp, true, true);
		GridLayout layout = UI.gridLayout(comp, 2);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		geoLink = tk.createImageHyperlink(comp, SWT.TOP);
		UI.gridData(geoLink, true, false).horizontalSpan = 2;
		Location loc = getModel().location;
		String linkText = loc != null && loc.geodata != null
				? "open in map"
				: "no data";
		geoLink.setText(linkText);
		UI.filler(parent);

		Controls.onClick(geoLink, e -> {
			Location location = getModel().location;
			if (location == null || location.geodata == null)
				return;
			FeatureCollection coll = GeoJSON.unpack(location.geodata);
			if (coll == null)
				return;
			MapDialog.show(location.name, map -> {
				map.addBaseLayers();
				map.addLayer(coll)
						.fillColor(Colors.get(173, 20, 87, 100))
						.borderColor(Colors.get(173, 20, 87, 100))
						.center();
			});
		});
	}
}
