package org.openlca.core.editors.result;

import java.util.UUID;

import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.results.LCIResult;
import org.openlca.ui.Error;
import org.openlca.ui.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Saves an inventory result as process. */
class SaveAsProcessHandler {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;
	private LCIResult inventoryResult;

	public SaveAsProcessHandler(IDatabase database, LCIResult inventoryResult) {
		this.database = database;
		this.inventoryResult = inventoryResult;
	}

	public void run() {
		if (inventoryResult == null)
			return;
		boolean b = Question.ask(Messages.Results_SAVE_AS_PROCESS_TITLE,
				Messages.Results_SAVE_AS_PROCESS_QUESTION);
		if (!b)
			return;
		Process process = createProcess();
		boolean valid = process.getQuantitativeReference() != null;
		if (!valid)
			Error.showBox(Messages.Results_INVALID_PROCESS);
		else
			insert(process);
	}

	private void insert(Process process) {
		try {
			database.insert(process);
			Navigator.refresh();
			App.openEditor(process, database);
		} catch (Exception e) {
			log.error("Failed to save LCI result as process", e);
		}
	}

	private Process createProcess() {
		// other required reference objects are set
		// when the process is opened in the editor
		// see the process editor
		Process process = new Process();
		process.setCategoryId(Process.class.getCanonicalName());
		String id = UUID.randomUUID().toString();
		process.setId(id);
		process.setName(inventoryResult.getProductSystemName());
		for (Exchange exchange : inventoryResult.getInventory()) {
			Exchange newExchange = copy(exchange, id);
			process.add(newExchange);
		}
		process.setProcessType(ProcessType.LCI_Result);
		setQuanRef(process);
		return process;
	}

	private void setQuanRef(Process process) {
		String productName = inventoryResult.getProductName();
		if (productName == null)
			return;
		for (Exchange exchange : process.getExchanges()) {
			Flow flow = exchange.getFlow();
			if (productName.equals(flow.getName()) && !exchange.isInput()) {
				process.setQuantitativeReference(exchange);
				break;
			}
		}
	}

	private Exchange copy(Exchange exchange, String processId) {
		Exchange copy = new Exchange(processId);
		copy.setDistributionType(exchange.getDistributionType());
		copy.setFlow(exchange.getFlow());
		copy.setFlowPropertyFactor(exchange.getFlowPropertyFactor());
		copy.setInput(exchange.isInput());
		copy.setId(UUID.randomUUID().toString());
		copy.getResultingAmount().setFormula(
				exchange.getResultingAmount().getFormula());
		copy.getResultingAmount().setValue(
				exchange.getResultingAmount().getValue());
		copy.setUnit(exchange.getUnit());
		return copy;
	}

}
