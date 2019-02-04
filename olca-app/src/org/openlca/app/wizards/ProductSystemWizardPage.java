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
import org.openlca.app.util.Labels;
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

import com.google.common.base.Strings;

class ProductSystemWizardPage extends AbstractWizardPage<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Process refProcess;
	private Text filterText;
	private TreeViewer processTree;
	private Button autoLinkCheck;
	private Button checkLinksCheck;

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
			nameText.setText(refProcess.name);
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
			refProcess = dao.getForId(elem.getContent().id);
			if (Strings.isNullOrEmpty(nameText.getText())) {
				String name = Labels.getDisplayName(refProcess);
				nameText.setText(name != null ? name : "");
			}
			checkInput();
		} catch (Exception ex) {
			log.error("failed to load process", ex);
		}
	}

	private void createOptions(Composite comp) {
		UI.filler(comp);
		autoLinkCheck = UI.checkBox(comp, M.AutoLinkProcesses);
		autoLinkCheck.setSelection(true);
		UI.filler(comp);
		checkLinksCheck = UI.checkBox(comp,
				"Check multi-provider links (experimental)");
		linkingPanel = new LinkingConfigPanel(comp);
		Controls.onSelect(autoLinkCheck, e -> {
			boolean enabled = autoLinkCheck.getSelection();
			linkingPanel.setEnabled(enabled);
			checkLinksCheck.setEnabled(enabled);
			if (enabled && checkLinksCheck.getSelection()) {
				linkingPanel.setTypeChecksEnabled(false);
			}
		});
		Controls.onSelect(checkLinksCheck, e -> {
			boolean b = !checkLinksCheck.getSelection();
			linkingPanel.setTypeChecksEnabled(b);
		});
	}

	@Override
	public ProductSystem createModel() {
		ProductSystem system = ProductSystem.from(refProcess);
		system.name = getModelName();
		system.description = getModelDescription();
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
		Exchange qRef = refProcess.quantitativeReference;
		if (qRef == null || qRef.flow == null)
			return false;
		FlowType type = qRef.flow.flowType;
		if (type == FlowType.PRODUCT_FLOW)
			return !qRef.isInput;
		if (type == FlowType.WASTE_FLOW)
			return qRef.isInput;
		return false;
	}

	boolean addSupplyChain() {
		return autoLinkCheck.getSelection();
	}

	LinkingConfig getLinkingConfig() {
		LinkingConfig config = linkingPanel.getLinkingConfig();
		if (checkLinksCheck.getSelection()) {
			config.callback = new ProviderCallback();
		}
		return config;
	}

}
