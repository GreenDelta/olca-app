package org.openlca.app.results;

import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.results.ContributionResultProvider;

public interface IResultEditor<T extends ContributionResultProvider<?>> {

	T getResult();
	
	CalculationSetup getSetup();
	
	DQResult getDqResult();
	
}
