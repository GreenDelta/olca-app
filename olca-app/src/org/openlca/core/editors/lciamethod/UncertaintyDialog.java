package org.openlca.core.editors.lciamethod;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.core.application.Messages;
import org.openlca.core.model.LCIAFactor;
import org.openlca.core.model.UncertaintyDistributionType;
import org.openlca.ui.Colors;
import org.openlca.ui.DataBinding;
import org.openlca.ui.EnumLabels;
import org.openlca.ui.UI;

/** Dialog for editing uncertainty information in impact assessment factors. */
public class UncertaintyDialog extends Dialog {

	private FormToolkit toolkit;
	private UncertaintyDistributionType[] types = UncertaintyDistributionType
			.values();
	private Combo combo;
	private StackLayout stackLayout;

	private Client[] clients;
	private Client selectedClient;
	private double firstParam;
	private double secondParam;
	private double thirdParam;
	private String[] properties = { "firstParam", "secondParam", "thirdParam" };

	private LCIAFactor factor;

	public UncertaintyDialog(Shell parentShell, LCIAFactor factor) {
		super(parentShell);
		toolkit = new FormToolkit(parentShell.getDisplay());
		this.factor = factor;
		if (factor != null) {
			firstParam = factor.getUncertaintyParameter1();
			secondParam = factor.getUncertaintyParameter2();
			thirdParam = factor.getUncertaintyParameter3();
		}
	}

	@Override
	protected void okPressed() {
		int idx = combo.getSelectionIndex();
		if (factor != null && idx >= 0 && idx < types.length) {
			UncertaintyDistributionType type = types[idx];
			UncertaintySetter.setValues(factor, type, firstParam, secondParam,
					thirdParam);
		}
		super.okPressed();
	}

	public double getFirstParam() {
		return firstParam;
	}

	public void setFirstParam(double firstParam) {
		this.firstParam = firstParam;
	}

	public double getSecondParam() {
		return secondParam;
	}

	public void setSecondParam(double secondParam) {
		this.secondParam = secondParam;
	}

	public double getThirdParam() {
		return thirdParam;
	}

	public void setThirdParam(double thirdParam) {
		this.thirdParam = thirdParam;
	}

	@Override
	protected Control createDialogArea(Composite root) {
		getShell().setText(Messages.Common_Uncertainty);
		toolkit.adapt(root);
		Composite area = (Composite) super.createDialogArea(root);
		toolkit.adapt(area);
		Composite container = toolkit.createComposite(area);
		UI.gridData(container, true, true);
		UI.gridLayout(container, 1);
		createCombo(container);
		createCompositeStack(container);
		initComposite();
		getShell().pack();
		UI.center(getParentShell(), getShell());
		return area;
	}

	private void createCombo(Composite container) {
		Composite comboComposite = toolkit.createComposite(container);
		UI.gridData(comboComposite, true, false);
		UI.gridLayout(comboComposite, 2);
		combo = UI.formCombo(comboComposite, toolkit,
				Messages.Common_UncertaintyDistribution);
		String[] items = new String[types.length];
		int idx = 0;
		for (int i = 0; i < items.length; i++) {
			UncertaintyDistributionType type = types[i];
			items[i] = EnumLabels.uncertaintyType(type);
			if (factor.getUncertaintyType() == type)
				idx = i;
		}
		combo.setItems(items);
		combo.select(idx);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				initComposite();
			}
		});
	}

	private void initComposite() {
		int item = combo.getSelectionIndex();
		if (item == -1)
			return;
		if (selectedClient != null)
			selectedClient.releaseFields();
		selectedClient = clients[item];
		selectedClient.bindFields();
		stackLayout.topControl = selectedClient.composite;
		getShell().layout(true, true);
		getShell().pack();
	}

	private void createCompositeStack(Composite container) {
		Composite stack = toolkit.createComposite(container);
		UI.gridData(stack, true, true);
		stackLayout = new StackLayout();
		stack.setLayout(stackLayout);
		clients = new Client[types.length];
		for (int i = 0; i < types.length; i++) {
			Composite composite = toolkit.createComposite(stack);
			UI.gridLayout(composite, 2);
			Client client = createClient(types[i], composite);
			clients[i] = client;
		}
	}

	private Client createClient(UncertaintyDistributionType type,
			Composite composite) {
		switch (type) {
		case LOG_NORMAL:
			return new Client(composite, Messages.Common_GeometricMean,
					Messages.Common_GeometricStandardDeviation);
		case NONE:
			return new Client(composite);
		case NORMAL:
			return new Client(composite, Messages.Common_Mean,
					Messages.Common_StandardDeviation);
		case TRIANGLE:
			return new Client(composite, Messages.Common_Minimum,
					Messages.Common_Mode, Messages.Common_Maximum);
		case UNIFORM:
			return new Client(composite, Messages.Common_Minimum,
					Messages.Common_Maximum);
		default:
			return new Client(composite);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		toolkit.adapt(parent);
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		getShell().pack();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	@Override
	public boolean close() {
		if (toolkit != null)
			toolkit.dispose();
		return super.close();
	}

	private class Client {

		private Composite composite;
		private Text[] texts;

		private DataBinding binding = new DataBinding();

		Client(Composite composite, String... labels) {
			this.composite = composite;
			texts = new Text[labels.length];
			for (int i = 0; i < labels.length; i++)
				texts[i] = UI.formText(composite, toolkit, labels[i]);
			if (labels.length == 0) {
				Label label = toolkit.createLabel(composite,
						Messages.Common_NoDistribution);
				label.setForeground(Colors.getDarkGray());
			}
		}

		void bindFields() {
			for (int i = 0; i < texts.length; i++)
				binding.onDouble(UncertaintyDialog.this, properties[i],
						texts[i]);
		}

		void releaseFields() {
			for (Text text : texts)
				binding.release(text);
		}
	}

}
