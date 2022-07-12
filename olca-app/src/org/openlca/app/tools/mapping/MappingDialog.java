package org.openlca.app.tools.mapping;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Fn;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
import org.openlca.io.maps.MappingStatus;
import org.openlca.util.Categories;
import org.openlca.util.Strings;

class MappingDialog extends FormDialog {

	/**
	 * Opens a dialog for editing the given entry. When the returned status code
	 * is `OK`, the entry was updated (maybe modified). Otherwise, it is
	 * unchanged.
	 */
	static int open(MappingTool tool, FlowMapEntry entry) {
		if (tool == null || entry == null)
			return CANCEL;

		// the dialog works on a copy of the entry; only if
		// the user clicks on OK, the changes are applied
		var copy = entry.copy();
		var dialog = new MappingDialog(tool, copy);
		int state = dialog.open();
		if (state != OK)
			return state;

		Function<FlowRef, FlowRef> check = flowRef -> {
				var r = flowRef == null
					? new FlowRef()
					: flowRef;
				r.status = r.flow == null
					? MappingStatus.error("no flow set")
					: MappingStatus.ok("edited or checked manually");
				return r;
		};
		entry.factor(copy.factor())
			.sourceFlow(check.apply(copy.sourceFlow()))
			.targetFlow(check.apply(copy.targetFlow()));

		return state;
	}

	private final MappingTool tool;
	private final FlowMapEntry entry;

	private IManagedForm mform;

	private MappingDialog(MappingTool tool, FlowMapEntry entry) {
		super(UI.shell());
		this.tool = tool;
		this.entry = entry;
		if (entry.sourceFlow() == null) {
			entry.sourceFlow(new FlowRef());
		}
		if (entry.targetFlow() == null) {
			entry.targetFlow(new FlowRef());
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		shell.setText("Flow mapping");
		UI.center(UI.shell(), shell);
		super.configureShell(shell);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 650);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		this.mform = mform;
		FormToolkit tk = mform.getToolkit();
		Composite body = mform.getForm().getBody();
		UI.gridLayout(body, 1);
		Composite comp = tk.createComposite(body);
		UI.gridLayout(comp, 1);
		UI.gridData(comp, true, false);

		// source flow
		Fn.with(UI.formLabel(comp, tk, "Source flow"), label -> {
			label.setFont(UI.boldFont());
			UI.gridData(label, true, false);
		});
		RefPanel sourcePanel = new RefPanel(entry.sourceFlow(), true);
		sourcePanel.render(comp, tk);
		UI.gridData(tk.createLabel(
			comp, "", SWT.SEPARATOR | SWT.HORIZONTAL), true, false);

		// target flow
		Fn.with(UI.formLabel(comp, tk, "Target flow"), label -> {
			label.setFont(UI.boldFont());
			UI.gridData(label, true, false);
		});
		RefPanel targetPanel = new RefPanel(entry.targetFlow(), false);
		targetPanel.render(comp, tk);
		UI.gridData(tk.createLabel(
			comp, "", SWT.SEPARATOR | SWT.HORIZONTAL), true, false);

		// text with conversion factor
		Composite convComp = tk.createComposite(body);
		UI.gridLayout(convComp, 3);
		UI.gridData(convComp, true, false);
		Text convText = UI.formText(convComp, tk, M.ConversionFactor);
		convText.setText(Double.toString(entry.factor()));
		convText.addModifyListener(e -> {
			try {
				entry.factor(Double.parseDouble(convText.getText()));
			} catch (Exception ignored) {
			}
		});

		UI.gridData(convText, true, false);
		Label unitLabel = UI.formLabel(convComp, tk, "");
		Runnable updateUnit = () -> {
			String sunit = "?";
			String tunit = "?";
			if (entry.sourceFlow() != null
				&& entry.sourceFlow().unit != null
				&& entry.sourceFlow().unit.name != null) {
				sunit = entry.sourceFlow().unit.name;
			}
			if (entry.targetFlow() != null
				&& entry.targetFlow().unit != null
				&& entry.targetFlow().unit.name != null) {
				tunit = entry.targetFlow().unit.name;
			}
			unitLabel.setText(sunit + "/" + tunit);
			unitLabel.getParent().pack();
		};
		updateUnit.run();
		sourcePanel.onChange = updateUnit;
		targetPanel.onChange = updateUnit;
	}

	private class RefPanel {

		final FlowRef ref;
		final boolean forSource;

		Hyperlink flowLink;
		Label categoryLabel;
		Label propertyLabel;
		Label unitLabel;
		ProviderCombo providerCombo;

		Runnable onChange;

		RefPanel(FlowRef ref, boolean forSource) {
			this.ref = ref;
			this.forSource = forSource;
		}

		void render(Composite parent, FormToolkit tk) {
			Composite comp = tk.createComposite(parent);
			UI.gridLayout(comp, 2, 10, 5);
			UI.gridData(comp, true, false);

			UI.formLabel(comp, tk, M.Flow);
			flowLink = UI.formLink(comp, tk, "");
			Controls.onClick(flowLink, _e -> {
				IProvider p = forSource
					? tool.sourceSystem
					: tool.targetSystem;

				if (p == null) {
					MsgBox.error("Cannot select flow",
						"No data source for flows connected");
					return;
				}

				FlowRefDialog.open(p, o -> o.ifPresent(this::updateWith));
			});

			UI.formLabel(comp, tk, M.Category);
			categoryLabel = UI.formLabel(comp, tk, "");
			UI.formLabel(comp, tk, M.FlowProperty);
			propertyLabel = UI.formLabel(comp, tk, "");
			UI.formLabel(comp, tk, M.Unit);
			unitLabel = UI.formLabel(comp, tk, "");

			// the provider link
			if (!forSource && tool.targetSystem instanceof DBProvider) {
				Combo combo = UI.formCombo(comp, tk, M.Provider);
				UI.gridData(combo, false, false).widthHint = 400;
				this.providerCombo = new ProviderCombo(ref, combo);
			}
			updateLabels();
		}

		private void updateWith(FlowRef newRef) {
			if (newRef == null) {
				ref.flow = null;
				ref.flowCategory = null;
				ref.flowLocation = null;
				ref.property = null;
				ref.unit = null;
				if (providerCombo != null) {
					providerCombo.update();
				}
				return;
			}
			ref.flow = newRef.flow;
			ref.flowCategory = newRef.flowCategory;
			ref.flowLocation = newRef.flowLocation;
			ref.property = newRef.property;
			ref.unit = newRef.unit;
			updateLabels();
			if (providerCombo != null) {
				providerCombo.update();
			}
			if (onChange != null) {
				onChange.run();
			}
		}

		private void updateLabels() {
			int maxLen = 80;

			// flow name
			if (ref.flow == null) {
				flowLink.setText("- none -");
			} else {
				String t = ref.flow.name;
				if (t == null) {
					t = ref.flow.refId;
				}
				if (t == null) {
					t = "?";
				}
				if (Strings.notEmpty(ref.flowLocation)) {
					t += " - " + ref.flowLocation;
				}
				flowLink.setText(Strings.cut(t, maxLen));
				flowLink.setToolTipText(t);
			}

			// category path
			if (Strings.nullOrEmpty(ref.flowCategory)) {
				categoryLabel.setText("");
			} else {
				categoryLabel.setText(
					Strings.cutLeft(ref.flowCategory, maxLen));
				categoryLabel.setToolTipText(ref.flowCategory);
			}

			// flow property
			if (ref.property == null || ref.property.name == null) {
				propertyLabel.setText("");
			} else {
				propertyLabel.setText(ref.property.name);
				propertyLabel.setToolTipText(ref.property.name);
			}

			// unit
			if (ref.unit == null || ref.unit.name == null) {
				unitLabel.setText("");
			} else {
				unitLabel.setText(ref.unit.name);
				unitLabel.setToolTipText(ref.unit.name);
			}

			flowLink.getParent().getParent().pack();
			flowLink.getParent().pack();
			mform.reflow(true);
		}
	}

	private static class ProviderCombo {

		private final FlowRef ref;
		private final Combo combo;
		private final ArrayList<ProcessDescriptor> providers = new ArrayList<>();

		ProviderCombo(FlowRef ref, Combo combo) {
			this.ref = ref;
			this.combo = combo;
			Controls.onSelect(combo, e -> {
				int i = combo.getSelectionIndex() - 1;
				if (i < 0 || i >= providers.size()) {
					setProvider(null);
				} else {
					setProvider(providers.get(i));
				}
				combo.setToolTipText(combo.getItems()[i + 1]);
			});
			update();
		}

		void update() {

			if (ref.flow == null
				|| ref.flow.refId == null
				|| ref.flow.flowType == FlowType.ELEMENTARY_FLOW) {
				providers.clear();
				combo.setItems();
				combo.setEnabled(false);
				setProvider(null);
				return;
			}

			IDatabase db = Database.get();
			combo.setEnabled(true);

			// collect providers
			providers.clear();
			long flowID = ref.flow.id;
			FlowDao fdao = new FlowDao(db);
			if (flowID == 0L) {
				Flow flow = fdao.getForRefId(ref.flow.refId);
				flowID = flow != null ? flow.id : 0L;
			}
			if (flowID > 0L) {
				Set<Long> ids = ref.flow.flowType == FlowType.WASTE_FLOW
					? fdao.getWhereInput(flowID)
					: fdao.getWhereOutput(flowID);
				providers.addAll(new ProcessDao(db).getDescriptors(ids));
				providers.sort(
					(p1, p2) -> Strings.compare(Labels.name(p1), Labels.name(p2)));
			}

			// fill the provider combo
			String[] items = new String[providers.size() + 1];
			items[0] = "- none -";
			int selected = 0;
			for (int i = 0; i < providers.size(); i++) {
				ProcessDescriptor p = providers.get(i);
				items[i + 1] = Labels.name(p);
				if (ref.provider != null
					&& Objects.equals(p.refId, ref.provider.refId)) {
					selected = i + 1;
				}
			}
			combo.setItems(items);
			combo.setToolTipText(items[selected]);
			combo.select(selected);
			if (ref.provider != null && selected == 0) {
				// the provider is not in the database
				// => we set it to null
				setProvider(null);
			}
		}

		private void setProvider(ProcessDescriptor p) {
			if (p == null) {
				ref.provider = null;
				ref.providerCategory = null;
				ref.providerLocation = null;
				return;
			}
			ref.provider = p;
			if (p.category == null) {
				ref.providerCategory = "";
			} else {
				ref.providerCategory = Categories.pathsOf(
					Database.get()).pathOf(p.category);
			}
			if (p.location == null) {
				ref.providerLocation = "";
			} else {
				Location loc = new LocationDao(
					Database.get()).getForId(p.location);
				ref.providerLocation = loc != null
					? loc.code
					: "";
			}
		}

	}
}
