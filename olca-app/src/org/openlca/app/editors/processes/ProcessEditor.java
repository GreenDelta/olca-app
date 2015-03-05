package org.openlca.app.editors.processes;

import java.util.List;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.parameters.Formulas;
import org.openlca.app.editors.parameters.ModelParameterPage;
import org.openlca.app.editors.parameters.ParameterChangeSupport;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessEditor extends ModelEditor<Process> implements IEditor {

	/**
	 * An event message that indicates the removal or addition of one or more
	 * exchanges exchange.
	 */
	final String EXCHANGES_CHANGED = "EXCHANGE_CHANGED";

	public static String ID = "editors.process";
	private Logger log = LoggerFactory.getLogger(getClass());
	private ParameterChangeSupport parameterSupport;

	public ProcessEditor() {
		super(Process.class);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		Process p = getModel();
		if (p.getDocumentation() == null)
			p.setDocumentation(new ProcessDocumentation());
		evalFormulas();
		parameterSupport = new ParameterChangeSupport();
		parameterSupport.doEvaluation(this::evalFormulas);
	}

	private void evalFormulas() {
		Process p = getModel();
		List<String> errors = Formulas.eval(Database.get(), p);
		if (!errors.isEmpty()) {
			String message = errors.get(0);
			if (errors.size() > 1)
				message += " (" + (errors.size() - 1) + " more)";
			org.openlca.app.util.Error.showBox(
					Messages.FormulaEvaluationFailed, message);
		}
	}

	public ParameterChangeSupport getParameterSupport() {
		return parameterSupport;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new ProcessExchangePage(this));
			addPage(new AdminInfoPage(this));
			addPage(new ProcessModelingPage(this));
			addPage(new ModelParameterPage(this));
			addPage(new AllocationPage(this));
			addPage(new ProcessCostPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}
}
