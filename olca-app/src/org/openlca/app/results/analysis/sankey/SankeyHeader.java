package org.openlca.app.results.analysis.sankey;

import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.analysis.sankey.model.Diagram;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.tools.graphics.frame.Header;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.core.matrix.index.EnviFlow;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.ImpactDescriptor;

import java.beans.PropertyChangeEvent;

import static org.openlca.app.results.analysis.sankey.SankeyConfig.CONFIG_PROP;

public class SankeyHeader extends Header {

	private Button button;
	private Composite info;
	private ImageHyperlink contribution;
	private ImageHyperlink method;
	private Label referenceType;
	private Label minContributionShare;
	private Label processMaxNumber;
	private ImageHyperlink title;
	private Listener listener;

	public SankeyHeader(Composite parent, int style) {
		super(parent, style | SWT.BORDER);
	}

	@Override
	public void initialize() {
		var headerLayout = new GridLayout(2, false);
		headerLayout.marginHeight = 0;
		headerLayout.marginWidth = 20;
		headerLayout.horizontalSpacing = 20;
		setLayout(headerLayout);

		button = new Button(this, SWT.NONE);
		button.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		button.setImage(Icon.PREFERENCES.get());

		info = new Composite(this, SWT.NONE);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		var infoLayout = new GridLayout(3, true);
		infoLayout.marginHeight = 0;
		infoLayout.verticalSpacing = 0;
		info.setLayout(infoLayout);

		createTitle();
		createReferenceType();
		createMinContributionShare();
		createMethod();
		createReferenceLabel();
		createProcessMaxNumber();
	}

	@Override
	public void activate() {
		super.activate();
		listener = e -> {
			if (getModel() != null && getModel().getEditor() != null) {
				var editor = getModel().getEditor();
				var registry = (ActionRegistry) editor.getAdapter(ActionRegistry.class);
				var action = registry.getAction(ActionIds.EDIT_CONFIG);
				if (action.isEnabled())
					action.run();
			}
		};
		button.addListener(SWT.Selection, listener);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		button.removeListener(SWT.Selection, listener);
	}

	private void createTitle() {
		title = new ImageHyperlink(info, SWT.NONE);
		title.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		title.setForeground(Colors.linkBlue());
		setTitle();
	}

	private void setTitle() {
		if (getModel() != null
				&& getModel().getEditor().resultEditor.setup != null) {
			var setup = getModel().getEditor().resultEditor.setup;
			Object entity = setup.hasProductSystem()
					? setup.productSystem()
					: setup.process();

			if (entity instanceof RootEntity rootEntity) {
				title.setText(Labels.name(rootEntity));
				title.setImage(Images.get(rootEntity));
				Controls.onClick(title, e -> App.open(rootEntity));
				return;
			}
		}
		title.setText(M.NoData);
	}

	private void createMethod() {
		method = new ImageHyperlink(info, SWT.NONE);
		method.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		method.setForeground(Colors.linkBlue());
		setMethod();
	}

	private void setMethod() {
		if (getModel() != null
				&& getModel().getEditor().resultEditor.setup != null) {
			var impact = getModel().getEditor().resultEditor.setup.impactMethod();
			method.setText(Labels.name(impact));
			method.setImage(Images.get(impact));
			Controls.onClick(method, e -> App.open(impact));
			return;
		}
		method.setText(M.NoData);
	}

	private void createReferenceType() {
		referenceType = new Label(info, SWT.NONE);
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
		} else referenceType.setText(M.NoData);
	}

	private void createMinContributionShare() {
		minContributionShare = new Label(info, SWT.NONE);
		minContributionShare.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setMinContributionShare();
	}

	private void setMinContributionShare() {
		if (getModel() != null
				&& getModel().getConfig() != null) {
			var cutoff = Numbers.format(getModel().getConfig().cutoff() * 100, 3);
			minContributionShare.setText("Min. contribution share: " + cutoff + "%");
		} else minContributionShare.setText(M.NoData);
	}

	private void createProcessMaxNumber() {
		processMaxNumber = new Label(info, SWT.NONE);
		processMaxNumber.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		setProcessMaxNumber();
	}

	private void setProcessMaxNumber() {
		if (getModel() != null
				&& getModel().getConfig() != null) {
			var maxCount = getModel().getConfig().maxCount();
			processMaxNumber.setText("Max. number of processes: " + maxCount);
		} else processMaxNumber.setText(M.NoData);
	}

	private void createReferenceLabel() {
		contribution = new ImageHyperlink(info, SWT.NONE);
		contribution.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, true));
		contribution.setForeground(Colors.linkBlue());
		setReferenceLabel();
	}

	private void setReferenceLabel() {
		if (getModel() != null && getModel().getConfig() != null
				&& getModel().getConfig().selection() != null) {
			var selection = getModel().getConfig().selection();

			if (selection instanceof EnviFlow enviFlow) {
				contribution.setText(Labels.name(enviFlow.flow()));
				contribution.setImage(Images.get(enviFlow.flow()));
				Controls.onClick(contribution, e -> App.open(enviFlow.flow()));
				return;
			} else if (selection instanceof ImpactDescriptor impact) {
				contribution.setText(Labels.name(impact));
				contribution.setImage(Images.get(impact));
				Controls.onClick(contribution, e -> App.open(impact));
				return;
			}
		}
		contribution.setText(M.NoReferenceSet);
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
			info.layout();
		}
	}

}
