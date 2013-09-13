package org.openlca.app.analysis;

import java.util.List;

import org.openlca.core.results.AnalysisResult;

public interface IProcessContributionProvider<T> {

	AnalysisResult getAnalysisResult();

	List<ProcessContributionItem> getItems(T selection, double cutOff);

	List<ProcessContributionItem> getHotSpots(T selection, double cutOff);

	T[] getElements();

}
