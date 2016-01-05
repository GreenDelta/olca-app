package org.openlca.app.editors.reports.model;

import java.util.ArrayList;
import java.util.List;

public class Report {

	public String title;
	public boolean withNormalisation;
	public boolean withWeighting;
	public final List<ReportSection> sections = new ArrayList<>();
	public final List<ReportParameter> parameters = new ArrayList<>();
	public final List<ReportVariant> variants = new ArrayList<>();
	public final List<ReportResult> results = new ArrayList<>();
	public final List<ReportIndicator> indicators = new ArrayList<>();
	public final List<ReportProcess> processes = new ArrayList<>();

}
