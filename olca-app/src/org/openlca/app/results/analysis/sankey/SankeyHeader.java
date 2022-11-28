package org.openlca.app.results.analysis.sankey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.M;
import org.openlca.app.results.analysis.sankey.model.Diagram;
import org.openlca.app.tools.graphics.frame.Header;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.descriptors.ImpactDescriptor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.openlca.app.results.analysis.sankey.SankeyConfig.CONFIG_PROP;

public class SankeyHeader extends Header implements PropertyChangeListener {

	private Label title;
	private Label contribution;
	private Label method;
	private Label referenceType;
	private Label minContributionShare;
	private Label processMaxNumber;

	public SankeyHeader(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void initialize() {
		setLayout(new GridLayout(3, false));
		createTitle();
		createReferenceType();
		createMinContributionShare();
		createMethod();
		createReferenceLabel();
		createProcessMaxNumber();
	}

	private void createTitle() {
		title = new Label(this, SWT.NONE);
		title.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setTitle();
		title.setFont(UI.boldFont());
	}

	private void setTitle() {
		if (getModel() != null) {
			var sankey = getModel().getEditor().getSankey();
			title.setText(Labels.name(sankey.root.product));
		}
		else title.setText(M.NoData);
	}

	private void createMethod() {
		method = new Label(this, SWT.NONE);
		method.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setMethod();
	}

	private void setMethod() {
		if (getModel() != null
				&& getModel().getEditor().resultEditor.setup != null) {
			var setup = getModel().getEditor().resultEditor.setup;
			method.setText(Labels.name(setup.impactMethod()));
		}
		else method.setText(M.NoData);
	}

	private void createReferenceType() {
		referenceType = new Label(this, SWT.NONE);
		referenceType.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setReferenceType();
	}

	private void setReferenceType() {
		if (getModel() != null
				&& getModel().getConfig() != null
				&& getModel().getConfig().selection() != null) {
			var selection = getModel().getConfig().selection();
			if (selection instanceof EnviFlow)
				referenceType.setText(M.Flow + ":");
			else if (selection instanceof ImpactDescriptor)
				referenceType.setText(M.ImpactCategory + ":");
			else referenceType.setText(M.NoData);
		}
		else referenceType.setText(M.NoData);
	}

	private void createMinContributionShare() {
		minContributionShare = new Label(this, SWT.NONE);
		minContributionShare.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setMinContributionShare();
	}

	private void setMinContributionShare() {
		if (getModel() != null
				&& getModel().getConfig() != null) {
			var cutoff = Numbers.format(getModel().getConfig().cutoff() * 100);
			minContributionShare.setText("Min. contribution share: " + cutoff + "%");
		}
		else minContributionShare.setText(M.NoData);
	}

	private void createProcessMaxNumber() {
		processMaxNumber = new Label(this, SWT.NONE);
		processMaxNumber.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setProcessMaxNumber();
	}

	private void setProcessMaxNumber() {
		if (getModel() != null
				&& getModel().getConfig() != null) {
			var maxCount = getModel().getConfig().maxCount();
			processMaxNumber.setText("Max. number of processes: " + maxCount);
		}
		else processMaxNumber.setText(M.NoData);
	}

	private void createReferenceLabel() {
		contribution = new Label(this, SWT.NONE);
		contribution.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setReferenceLabel();
	}

	private void setReferenceLabel() {
		if (getModel() != null && getModel().getConfig() != null
				&& getModel().getConfig().selection() != null) {
			var selection = getModel().getConfig().selection();
			if (selection instanceof EnviFlow enviFlow)
				contribution.setText(Labels.name(enviFlow.flow()));
			else if (selection instanceof ImpactDescriptor impact)
				contribution.setText(Labels.name(impact));
			else contribution.setText(M.NoReferenceSet);
		}
		else contribution.setText(M.NoReferenceSet);
	}

	@Override
	public Diagram getModel() {
		return (Diagram) super.getModel();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (CONFIG_PROP.equals(prop)) {
			setTitle();
			setMethod();
			setReferenceLabel();
			setReferenceType();
			setProcessMaxNumber();
			setMinContributionShare();
			layout();
		}
	}

}
