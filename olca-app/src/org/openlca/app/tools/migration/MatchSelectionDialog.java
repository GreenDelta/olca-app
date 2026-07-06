package org.openlca.app.tools.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Strings;
import org.openlca.io.olca.migration.ProviderInfo;
import org.openlca.io.olca.migration.ProviderMatch;

class MatchSelectionDialog extends FormDialog {

	private final ProviderMatch match;
	private final List<ProviderInfo> allAlternatives;
	private ProviderInfo selected;
	private TableViewer table;
	private Button okBtn;

	private MatchSelectionDialog(ProviderMatch match) {
		super(UI.shell());
		this.match = Objects.requireNonNull(match);
		this.selected = match.selected();
		this.allAlternatives = new ArrayList<>(Util.sortedInfos(match.alternatives()));
	}

	static int open(ProviderMatch match) {
		if (match == null)
			return CANCEL;
		return new MatchSelectionDialog(match).open();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Select a matching provider");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 500);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 1);
		createSourceGroup(body, tk);
		createTargetSection(body, tk);
	}

	private void createSourceGroup(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Current provider in source database");
		UI.stretchX(section);
		var comp = UI.sectionClient(section, tk, 2);

		var p = match.source();
		if (p == null)
			return;

		var name = UI.labeledText(comp, tk, M.Name);
		Controls.set(name, Labels.name(p.provider()));
		name.setEditable(false);

		var flow = UI.labeledText(comp, tk, M.Flow);
		Controls.set(flow, Labels.name(p.flow()));
		flow.setEditable(false);

		var location = UI.labeledText(comp, tk, M.Location);
		Controls.set(location, Labels.name(p.location()));
		location.setEditable(false);
	}

	private void createTargetSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk,
			"Matching provider in target database");
		UI.stretchXY(section);
		var comp = UI.sectionClient(section, tk, 1);

		var searchText = UI.searchText(comp, tk);
		searchText.addModifyListener(_ -> doFilter(searchText.getText()));

		table = Tables.createViewer(comp, "Name", "Location");
		table.setLabelProvider(new TargetLabel());
		table.setInput(allAlternatives);
		Tables.bindColumnWidths2(table, 0.7, 0.3);
		UI.stretchXY(table.getTable());

		if (selected != null) {
			table.setSelection(new StructuredSelection(selected));
		}
		table.addSelectionChangedListener(_ -> {
			selected = Viewers.getFirstSelected(table);
			if (okBtn != null) {
				okBtn.setEnabled(selected != null);
			}
		});
	}

	private void doFilter(String query) {
		var filtered = applyQuery(query);
		table.setInput(filtered);
		table.setSelection(StructuredSelection.EMPTY);
		selected = null;
		if (okBtn != null) {
			okBtn.setEnabled(false);
		}
	}

	private List<ProviderInfo> applyQuery(String query) {
		if (Strings.isBlank(query))
			return allAlternatives;

		var parts = query.trim()
			.toLowerCase(Locale.ROOT)
			.split("\\s+");

		return allAlternatives.stream()
			.filter(info -> {
				var n = Labels.name(info.provider());
				if (Strings.isBlank(n))
					return false;
				var name = n.toLowerCase(Locale.ROOT);
				return Arrays.stream(parts).allMatch(name::contains);
			})
			.collect(Collectors.toList());
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		okBtn = getButton(IDialogConstants.OK_ID);
		if (okBtn != null) {
			okBtn.setEnabled(selected != null);
		}
	}

	@Override
	protected void okPressed() {
		if (selected != null && selected != match.selected()) {
			match.select(selected);
		}
		super.okPressed();
	}

	private static final class TargetLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			if (!(o instanceof ProviderInfo info))
				return null;
			if (col == 0 && info.provider() != null)
				return Images.get(info.provider());
			return null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ProviderInfo info))
				return null;
			return switch (col) {
				case 0 -> {
					var provider = info.provider();
					yield provider != null && provider.name != null
						? provider.name
						: "-";
				}
				case 1 -> {
					var location = info.location();
					if (location == null) {
						yield "";
					}
					yield location.code != null
						? location.code
						: location.name != null
							? location.name
							: "";
				}
				default -> null;
			};
		}
	}
}
