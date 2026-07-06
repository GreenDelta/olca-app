package org.openlca.app.tools.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import org.openlca.app.rcp.images.Images;
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
	private Button okButton;

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
	protected void createFormContent(IManagedForm mform) {
		var tk = mform.getToolkit();
		var body = UI.dialogBody(mform.getForm(), tk);
		UI.gridLayout(body, 1);

		createSourceGroup(body, tk);
		createTargetSection(body, tk);
	}

	private void createSourceGroup(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Current provider in source database");
		UI.gridData(section, true, false);
		var comp = UI.sectionClient(section, tk, 2);

		var source = match.source();
		nameRow(comp, tk, source);
		flowRow(comp, tk, source);
		locationRow(comp, tk, source);
	}

	private void nameRow(Composite comp, FormToolkit tk, ProviderInfo info) {
		UI.label(comp, tk, "Name");
		var provider = info.provider();
		var name = provider != null
			? Labels.name(provider)
			: "-";
		UI.label(comp, tk, name);
	}

	private void flowRow(Composite comp, FormToolkit tk, ProviderInfo info) {
		UI.label(comp, tk, "Flow");
		var flow = info.flow();
		var name = flow != null
			? Labels.name(flow)
			: "-";
		UI.label(comp, tk, name);
	}

	private void locationRow(Composite comp, FormToolkit tk, ProviderInfo info) {
		UI.label(comp, tk, "Location");
		var location = info.location();
		var text = location != null
			? location.code != null
				? location.code
				: location.name
			: "-";
		UI.label(comp, tk, text);
	}

	private void createTargetSection(Composite parent, FormToolkit tk) {
		var section = UI.section(parent, tk, "Matching provider in target database");
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);

		var searchText = UI.searchText(comp, tk);
		searchText.addModifyListener(_ -> doFilter(searchText.getText().trim()));

		table = Tables.createViewer(comp,
			"Name",
			"Location");
		table.setLabelProvider(new TargetLabel());
		table.setInput(allAlternatives);
		Tables.bindColumnWidths2(table, 0.7, 0.3);
		var gd = UI.gridData(table.getTable(), true, true);
		gd.heightHint = 1;
		gd.widthHint = 1;

		if (selected != null) {
			table.setSelection(new StructuredSelection(selected));
		}

		table.addSelectionChangedListener(_ -> {
			selected = Viewers.getFirstSelected(table);
			if (okButton != null) {
				okButton.setEnabled(selected != null);
			}
		});
	}

	private void doFilter(String term) {
		if (Strings.isBlank(term)) {
			table.setInput(allAlternatives);
		} else {
			var lower = term.toLowerCase();
			var filtered = allAlternatives.stream()
				.filter(info -> {
					var provider = info.provider();
					if (provider == null)
						return false;
					return provider.name != null
						&& provider.name.toLowerCase().contains(lower);
				})
				.toList();
			table.setInput(filtered);
		}
		table.setSelection(StructuredSelection.EMPTY);
		selected = null;
		if (okButton != null) {
			okButton.setEnabled(false);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(selected != null);
		}
	}

	@Override
	protected void okPressed() {
		if (selected != null && selected != match.selected()) {
			match.select(selected);
		}
		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 500);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		var loc = super.getInitialLocation(initialSize);
		int marginTop = (getParentShell().getSize().y - initialSize.y) / 3;
		if (marginTop < 0)
			marginTop = 0;
		return new Point(loc.x, loc.y + marginTop);
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
