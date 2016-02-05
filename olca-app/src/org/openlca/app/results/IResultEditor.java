package org.openlca.app.results;

import org.openlca.core.math.CalculationSetup;
import org.openlca.core.results.ContributionResultProvider;

public interface IResultEditor<T extends ContributionResultProvider<?>> {

	T getResult();
	
	CalculationSetup getSetup();
	
}
