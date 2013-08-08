package org.openlca.core.application.evaluation;

import org.openlca.core.math.FormulaParseException;

public interface EvaluationListener {
	
	void error(FormulaParseException exception);

	void evaluated();

}
