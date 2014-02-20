package org.openlca.app.editors.processes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.Event;
import org.openlca.app.db.Database;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.Error;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.Process;
import org.openlca.core.model.Uncertainty;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.InterpreterException;
import org.openlca.expressions.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.ibm.icu.util.Calendar;

public class ProcessEditor extends ModelEditor<Process> implements IEditor {

	/**
	 * An event message that indicates the removal or addition of one or more
	 * exchanges exchange.
	 */
	final String EXCHANGES_CHANGED = "EXCHANGE_REMOVED";

	public static String ID = "editors.process";
	private Logger log = LoggerFactory.getLogger(getClass());
	private final FormulaInterpreter interpreter = new FormulaInterpreter();

	public ProcessEditor() {
		super(Process.class);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		initInterpreter();
	}

	public FormulaInterpreter getInterpreter() {
		return interpreter;
	}

	private void initInterpreter() {
		Process process = getModel();
		log.trace("init formula interpreter for process {}", process);
		IDatabase database = Database.get();
		ParameterDao dao = new ParameterDao(database);
		interpreter.clear();
		for (Parameter globalParam : dao.getGlobalParameters()) {
			interpreter.getGlobalScope().bind(globalParam.getName(),
					Double.toString(globalParam.getValue()));
		}
		Scope scope = interpreter.createScope(process.getId());
		for (Parameter param : process.getParameters()) {
			if (param.isInputParameter())
				scope.bind(param.getName(), Double.toString(param.getValue()));
			else
				scope.bind(param.getName(), param.getFormula());
		}
	}

	/**
	 * Evaluates the expression in the context of this process.
	 */
	public double eval(String expression) throws InterpreterException {
		Scope scope = interpreter.getScope(getModel().getId());
		return scope.eval(expression);
	}

	@Subscribe
	public void handleParameterChange(Event event) {
		if (!event.match(PARAMETER_CHANGE))
			return;
		log.trace("parameter changed");
		log.trace("re-init formula interpreter and eval exchanges");
		initInterpreter();
		Process process = getModel();
		try {
			for (Parameter parameter : process.getParameters())
				evalParameter(parameter);
			for (Exchange exchange : getModel().getExchanges())
				evalExchange(exchange);
			postEvent(FORMULAS_EVALUATED, this);
		} catch (Exception e) {
			Error.showBox("Formula evaluation in exchanges failed",
					e.getMessage());
		}
	}

	private void evalParameter(Parameter parameter) throws Exception {
		if (parameter.isInputParameter())
			return;
		double val = eval(parameter.getFormula());
		parameter.setValue(val);
	}

	private void evalExchange(Exchange e) throws Exception {
		if (e.getAmountFormula() != null)
			e.setAmountValue(eval(e.getAmountFormula()));
		Uncertainty u = e.getUncertainty();
		if (u == null)
			return;
		if (u.getParameter1Formula() != null)
			u.setParameter1Value(eval(u.getParameter1Formula()));
		if (u.getParameter2Formula() != null)
			u.setParameter2Value(eval(u.getParameter2Formula()));
		if (u.getParameter3Formula() != null)
			u.setParameter3Value(eval(u.getParameter3Formula()));
	}

	@Override
	protected void addPages() {
		try {
			addPage(new ProcessInfoPage(this));
			addPage(new ProcessExchangePage(this));
			addPage(new ProcessAdminInfoPage(this));
			addPage(new ProcessModelingPage(this));
			addPage(new ProcessParameterPage(this));
			addPage(new AllocationPage(this));
			addPage(new ProcessCostPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		getModel().getDocumentation().setLastChange(
				Calendar.getInstance().getTime());
		super.doSave(monitor);
	}

}
