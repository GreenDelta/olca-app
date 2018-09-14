package org.openlca.app.wizards;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.LinkingConfig;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProductSystemWizardPage extends AbstractWizardPage<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private TreeViewer processTree;
	private Process refProcess;
	private Button supplyChainCheck;

	private Text filterText;

	private LinkingConfigPanel linkingPanel;

	ProductSystemWizardPage() {
		super("ProductSystemWizardPage");
		setTitle(M.NewProductSystem);
		setMessage(M.CreatesANewProductSystem);
		setPageComplete(false);
		setWithDescription(false);
	}

	void setProcess(Process process) {
		this.refProcess = process;
	}

	@Override
	protected void createContents(Composite comp) {
		filterText = UI.formText(comp, M.ReferenceProcess);
		UI.filler(comp);
		createProcessTree(comp);
		createOptions(comp);
		if (refProcess != null) {
			nameText.setText(refProcess.getName());
			ProcessDescriptor descriptor = Descriptors.toDescriptor(refProcess);
			INavigationElement<?> elem = Navigator.findElement(descriptor);
			if (elem != null)
				processTree.setSelection(new StructuredSelection(elem));
			checkInput();
		}
	}

	private void createProcessTree(Composite composite) {
		processTree = NavigationTree.createViewer(composite);
		processTree.setInput(Navigator.findElement(ModelType.PROCESS));
		processTree.addFilter(new EmptyCategoryFilter());
		processTree.addFilter(new ModelTextFilter(filterText, processTree));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		processTree.getTree().setLayoutData(gd);
		processTree.addSelectionChangedListener(this::processSelected);
	}

	private void processSelected(SelectionChangedEvent e) {
		Object obj = Viewers.getFirst(e.getSelection());
		if (!(obj instanceof ModelElement)) {
			refProcess = null;
			checkInput();
			return;
		}
		ModelElement elem = (ModelElement) obj;
		try {
			ProcessDao dao = new ProcessDao(Database.get());
			refProcess = dao.getForId(elem.getContent().getId());
			checkInput();
		} catch (Exception ex) {
			log.error("failed to load process", ex);
		}
	}

	private void createOptions(Composite comp) {
		UI.filler(comp);
		supplyChainCheck = UI.checkBox(comp, M.AutoLinkProcesses);
		supplyChainCheck.setSelection(true);
		linkingPanel = new LinkingConfigPanel(comp);
		Controls.onSelect(supplyChainCheck, e -> {
			linkingPanel.setEnabled(supplyChainCheck.getSelection());
		});
	}

	@Override
	public ProductSystem createModel() {
		ProductSystem system = ProductSystem.from(refProcess);
		system.setName(getModelName());
		system.setDescription(getModelDescription());
		return system;
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null && !hasRefFlow()) {
			setErrorMessage(M.NoReferenceProcessSelected);
		}
		setPageComplete(getErrorMessage() == null);
	}

	private boolean hasRefFlow() {
		if (refProcess == null)
			return false;
		Exchange qRef = refProcess.getQuantitativeReference();
		if (qRef == null || qRef.flow == null)
			return false;
		FlowType type = qRef.flow.getFlowType();
		if (type == FlowType.PRODUCT_FLOW)
			return !qRef.isInput;
		if (type == FlowType.WASTE_FLOW)
			return qRef.isInput;
		return false;
	}

	boolean addSupplyChain() {
		return supplyChainCheck.getSelection();
	}

	LinkingConfig getLinkingConfig() {
		return linkingPanel.getLinkingConfig();
	}

}
