package org.openlca.core.editors.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.Navigator;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.SelectObjectDialog;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a flow property factor to a flow.
 * 
 * @author Michael Srocka
 */
class FlowPropertyAddAction extends Action {

	private Flow flowInfo;
	private IDatabase database;
	private TableViewer viewer;

	public FlowPropertyAddAction(Flow flowInfo, IDatabase database) {
		this.flowInfo = flowInfo;
		this.database = database;
		initAction();
	}

	public void setViewer(TableViewer viewer) {
		this.viewer = viewer;
	}

	private void initAction() {
		setId("FlowPropertyAddAction");
		setText(Messages.Flows_AddFlowPropertyFactorText);
		setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
		setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED.getDescriptor());
	}

	@Override
	public void run() {
		IModelComponent[] selection = selectFromDialog();
		if (selection == null || selection.length == 0)
			return;
		List<FlowPropertyFactor> factors = findOrCreateFactors(selection);
		refreshViewer(factors);
	}

	private List<FlowPropertyFactor> findOrCreateFactors(
			IModelComponent[] selection) {
		List<FlowPropertyFactor> factors = new ArrayList<>();
		for (int i = 0; i < selection.length; i++) {
			IModelComponent flowPropComponent = selection[i];
			FlowPropertyFactor factor = flowInfo
					.getFlowPropertyFactor(flowPropComponent.getId());
			if (factor == null)
				factor = createFactor(flowPropComponent);
			if (factor != null)
				factors.add(factor);
		}
		return factors;
	}

	private FlowPropertyFactor createFactor(IModelComponent component) {
		try {
			FlowProperty flowProperty = database.select(FlowProperty.class,
					component.getId());
			FlowPropertyFactor factor = new FlowPropertyFactor(UUID
					.randomUUID().toString(), flowProperty, 1d);
			flowInfo.add(factor);
			return factor;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(this.getClass());
			log.error("Cannot load flow property.", e);
			return null;
		}
	}

	private IModelComponent[] selectFromDialog() {
		NavigationRoot root = Navigator.getNavigationRoot();
		SelectObjectDialog dialog = new SelectObjectDialog(UI.shell(), root,
				true, database, FlowProperty.class);
		dialog.open();
		int code = dialog.getReturnCode();
		IModelComponent[] selection = dialog.getMultiSelection();
		return code == Window.OK ? selection : null;
	}

	private void refreshViewer(List<FlowPropertyFactor> factors) {
		if (viewer != null) {
			viewer.setInput(flowInfo.getFlowPropertyFactors());
			viewer.setSelection(new StructuredSelection(factors));
		}
	}
}