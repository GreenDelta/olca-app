package org.openlca.core.editors.analyze;

import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.results.AnalysisResult;

public interface IProcessContributionProvider<T> {

	AnalysisResult getAnalysisResult();

	IDatabase getDatabase();

	List<ProcessContributionItem> getItems(T selection, double cutOff);

	List<ProcessContributionItem> getHotSpots(T selection, double cutOff);

}
