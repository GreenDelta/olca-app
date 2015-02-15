package org.openlca.app.editors.processes;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.IEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ParameterPage;
import org.openlca.app.editors.ParameterPageListener;
import org.openlca.app.editors.ParameterPageSupport;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.expressions.FormulaInterpreter;
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
	private ParameterPageSupport parameterSupport;

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
		Supplier<List<Parameter>> supplier = () -> getModel().getParameters();
		parameterSupport = new ParameterPageSupport(this, supplier,
				ParameterScope.PROCESS);
		// it is important that this listener is added before the listener
		// in the exchange page, otherwise the exchange tables will be refreshed
		// with old values
		parameterSupport.addListener(new ParameterPageListener() {
			@Override
			public void parameterChanged() {
				log.trace("evaluate exchange formulas");
				for (Exchange exchange : getModel().getExchanges()) {
					parameterSupport.eval(exchange);
				}
			}
		});
	}

	public FormulaInterpreter getInterpreter() {
		return parameterSupport.getInterpreter();
	}

	public ParameterPageSupport getParameterSupport() {
		return parameterSupport;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new InfoPage(this));
			addPage(new ProcessExchangePage(this));
			addPage(new AdminInfoPage(this));
			addPage(new ProcessModelingPage(this));
			addPage(new ParameterPage(parameterSupport));
			addPage(new AllocationPage(this));
			addPage(new ProcessCostPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}
}
