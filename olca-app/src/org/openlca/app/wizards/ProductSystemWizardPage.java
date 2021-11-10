package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Selections;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.linking.LinkingConfig;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;
import org.slf4j.LoggerFactory;

class ProductSystemWizardPage extends AbstractWizardPage<ProductSystem> {

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
	protected void modelWidgets(Composite comp) {
		filterText = UI.formText(comp, M.ReferenceProcess);
		UI.filler(comp);
		createProcessTree(comp);
		createOptions(comp);
		if (refProcess != null) {
			nameText.setText(refProcess.name);
			var d = Descriptor.of(refProcess);
			var elem = Navigator.find(processTree, d);
			if (elem != null) {
				processTree.setSelection(new StructuredSelection(elem));
			}
			checkInput();
		}
	}

	private void createProcessTree(Composite comp) {
		processTree = NavigationTree.forSingleSelection(comp, ModelType.PROCESS);
		processTree.addFilter(new EmptyCategoryFilter());
		processTree.addFilter(new ModelTextFilter(filterText, processTree));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		processTree.getTree().setLayoutData(gd);
		processTree.addSelectionChangedListener(this::processSelected);
	}

	private void processSelected(SelectionChangedEvent e) {
		Object obj = Selections.firstOf(e);
		if (!(obj instanceof ModelElement)) {
			refProcess = null;
			checkInput();
			return;
		}
		ModelElement elem = (ModelElement) obj;
		try {
			ProcessDao dao = new ProcessDao(Database.get());
			refProcess = dao.getForId(elem.getContent().id);
			if (Strings.nullOrEmpty(nameText.getText())) {
				String name = Labels.name(refProcess);
				nameText.setText(name != null ? name : "");
			}
			checkInput();
		} catch (Exception ex) {
			var log = LoggerFactory.getLogger(this.getClass());
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
		ProductSystem system;
		if (refProcess != null) {
			system = ProductSystem.of(refProcess);
		} else {
			// create an empty reference process
			var processName = filterText.getText().trim();
			if (Strings.nullOrEmpty(processName)) {
				processName = nameText.getText().trim();
			}
			var process = new Process();
			process.name = processName;
			process.refId = UUID.randomUUID().toString();
			system = new ProductSystem();
			system.refId = UUID.randomUUID().toString();
			system.referenceProcess = process;
		}
		system.name = getModelName();
		system.description = getModelDescription();
		return system;
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() != null) {
			setPageComplete(false);
			return;
		}
		if (refProcess == null) {
			setMessage("No reference process is selected. " +
							"This will create a product system " +
							"with an empty reference process.",
					IMessageProvider.WARNING);
			linkingPanel.setEnabled(false);
		} else if (!hasRefFlow()) {
			setMessage("The selected process does not have " +
							"a product output or waste input as " +
							"reference flow.",
					IMessageProvider.WARNING);
			linkingPanel.setEnabled(true);
		} else {
			linkingPanel.setEnabled(true);
			setMessage(null);
		}
	}

	private boolean hasRefFlow() {
		if (refProcess == null)
			return false;
		var qRef = refProcess.quantitativeReference;
		if (qRef == null || qRef.flow == null)
			return false;
		var type = qRef.flow.flowType;
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
			config.callback(new ProviderCallback());
		}
		return config;
	}

}
