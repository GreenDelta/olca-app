package org.openlca.app.results.analysis.groups;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.agroups.AnalysisGroupResult;

public class AnalysisGroupsPage extends FormPage {

	private final ResultEditor editor;
	private final ProductSystem system;
	private final List<AnalysisGroup> groups;
	private volatile AnalysisGroupResult result;

	public AnalysisGroupsPage(ResultEditor editor, ProductSystem system) {
		super(editor, "AnalysisGroupsPage", "Analysis groups");
		this.editor = editor;
		this.system = system;
		this.groups = system.analysisGroups;
		this.groups.sort((g1, g2) -> Strings.compareIgnoreCase(g1.name, g2.name));
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.header(mform,
				Labels.name(editor.setup().target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mform.getToolkit();
		var body = UI.body(form, tk);

		var impacts = new ImpactTableSection(editor, system);
		impacts.render(body, tk);
		var contibutions = new ContributionSection(editor, groups);
		contibutions.render(body, tk);

		var ref = new AtomicReference<List<ImpactGroupResult>>();
		App.runWithProgress("Calculate group results...",
				() -> {
					result = AnalysisGroupResult.of(system, editor.result());
					var indicators = editor.items().impacts();
					ref.set(ImpactGroupResult.allOf(indicators, result));
				},
				() -> {
					var results = ref.get();
					if (result == null || results == null) {
						MsgBox.error("Calculation failed", "No result was calculated.");
						return;
					}
					impacts.setInput(results);
					contibutions.setResult(result);
				});
	}
}
