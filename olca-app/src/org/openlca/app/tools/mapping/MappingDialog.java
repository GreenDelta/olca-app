package org.openlca.app.tools.mapping;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.openlca.app.M;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Error;
import org.openlca.app.util.Fn;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowType;
import org.openlca.io.maps.FlowMapEntry;
import org.openlca.io.maps.FlowRef;
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
		MappingDialog d = new MappingDialog(tool, entry.clone());
		int state = d.open();
		if (state != OK)
			return state;
		entry.factor = d.entry.factor;
		entry.sourceFlow = d.entry.sourceFlow;
		entry.targetFlow = d.entry.targetFlow;
		return state;
	}

	private final MappingTool tool;
	private final FlowMapEntry entry;

	private IManagedForm mform;

	private MappingDialog(MappingTool tool, FlowMapEntry entry) {
		super(UI.shell());
		this.tool = tool;
		this.entry = entry;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		this.mform = mform;
		FormToolkit tk = mform.getToolkit();
		Composite body = mform.getForm().getBody();
		body.setLayout(new FillLayout());
		Composite comp = tk.createComposite(body);
		UI.gridLayout(comp, 2).makeColumnsEqualWidth = true;

		Fn.with(UI.formLabel(comp, tk, "Source flow"), label -> {
			label.setFont(UI.boldFont());
			UI.gridData(label, true, false);
		});
		Fn.with(UI.formLabel(comp, tk, "Target flow"), label -> {
			label.setFont(UI.boldFont());
			UI.gridData(label, true, false);
		});

		new RefPanel(entry.sourceFlow, true).render(comp, tk);
		new RefPanel(entry.targetFlow, false).render(comp, tk);
		mform.reflow(true);
	}

	private class RefPanel {

		final FlowRef ref;
		final boolean forSource;

		Hyperlink flowLink;
		Label categoryLabel;
		Label propertyLabel;
		Label unitLabel;
		Hyperlink providerLink;

		RefPanel(FlowRef ref, boolean forSource) {
			this.ref = ref;
			this.forSource = forSource;
		}

		void render(Composite parent, FormToolkit tk) {
			Composite comp = tk.createComposite(parent);
			UI.gridLayout(comp, 2, 15, 10);

			UI.formLabel(comp, tk, M.Flow);
			flowLink = UI.formLink(comp, tk, "");
			Controls.onClick(flowLink, _e -> {
				IProvider p = forSource
						? tool.sourceSystem
						: tool.targetSystem;

				if (p == null) {
					Error.showBox("Cannot select flow",
							"No data source for flows connected");
					return;
				}

				FlowRefDialog.open(p, opt -> {
					if (!opt.isPresent())
						return;
					updateWith(opt.get());
				});
			});

			UI.formLabel(comp, tk, M.Category);
			categoryLabel = UI.formLabel(comp, tk, "");
			UI.formLabel(comp, tk, M.FlowProperty);
			propertyLabel = UI.formLabel(comp, tk, "");
			UI.formLabel(comp, tk, M.Unit);
			unitLabel = UI.formLabel(comp, tk, "");
			if (canHaveProvider()) {
				UI.formLabel(comp, tk, M.Provider);
				providerLink = UI.formLink(comp, tk, "");
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
				return;
			}
			ref.flow = newRef.flow;
			ref.flowCategory = newRef.flowCategory;
			ref.flowLocation = newRef.flowLocation;
			ref.property = newRef.property;
			ref.unit = newRef.unit;
			updateLabels();
		}

		private void updateLabels() {
			int maxLen = 40;

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

			// provider
			if (providerLink != null) {
				if (ref.provider == null) {
					providerLink.setText("- none -");
				} else {
					String t = ref.provider.name;
					if (t == null) {
						t = ref.provider.refId;
					}
					if (t == null) {
						t = "?";
					}
					if (ref.providerLocation != null) {
						t += ref.providerLocation;
					}
					providerLink.setText(Strings.cut(t, maxLen));
					providerLink.setToolTipText(t);
				}
			}
			flowLink.getParent().getParent().pack();
			mform.reflow(true);
		}

		boolean canHaveProvider() {
			// TODO: as it is also possible to create mappings in via this
			// dialog, there may is not target flow assigned yet but we still
			// should be able to select a target provider...
			return !forSource
					&& tool.targetSystem instanceof DBProvider
					&& ref.flow != null
					&& (ref.flow.flowType == FlowType.PRODUCT_FLOW
							|| ref.flow.flowType == FlowType.WASTE_FLOW);
		}
	}
}
