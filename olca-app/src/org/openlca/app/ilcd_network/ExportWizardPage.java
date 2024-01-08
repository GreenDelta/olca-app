package org.openlca.app.ilcd_network;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.navigation.NavigationComparator;
import org.openlca.app.navigation.NavigationContentProvider;
import org.openlca.app.navigation.NavigationLabelProvider;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.Descriptor;

public class ExportWizardPage extends WizardPage implements ICheckStateListener {

	private CheckboxTreeViewer viewer;
	private final List<Descriptor> selectedModels = new ArrayList<>();

	public ExportWizardPage() {
		super("ilcd.network.SelectProcessPage");
		setTitle(M.ILCDNetworkExport);
		setDescription(M.ILCDNetworkExportDescription);
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = UI.composite(parent);
		setControl(container);
		container.setLayout(new GridLayout(1, false));
		createAddressControl(container);
		createProcessTree(container);
	}

	private void createAddressControl(Composite container) {
		Composite composite = UI.composite(container);
		composite.setLayout(new GridLayout(3, false));
		UI.gridData(composite, true, false);
		new ConnectionText(composite);
	}

	private void createProcessTree(Composite container) {
		Composite composite = UI.composite(container);
		composite.setLayout(new FillLayout());
		UI.gridData(composite, true, true);
		viewer = new CheckboxTreeViewer(composite, SWT.MULTI | SWT.BORDER);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new NavigationContentProvider());
		viewer.setLabelProvider(NavigationLabelProvider.withoutRepositoryState());
		viewer.setInput(Navigator.getNavigationRoot());
		viewer.addCheckStateListener(new NavigationTreeCheck(viewer));
		viewer.addCheckStateListener(this);
		viewer.addFilter(new NavigationTreeFilter());
		viewer.setComparator(new NavigationComparator());
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		selectedModels.clear();
		Object[] elements = viewer.getCheckedElements();
		for (Object element : elements) {
			if (element instanceof ModelElement modelElement) {
				var model = modelElement.getContent();
				selectedModels.add(model);
			}
		}
		setPageComplete(!selectedModels.isEmpty());
	}

	public List<Descriptor> getSelectedModels() {
		return selectedModels;
	}
}
