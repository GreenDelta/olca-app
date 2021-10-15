package org.openlca.app.editors.results.openepd.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.EntityCombo;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.CalculationType;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ResultModel;
import org.openlca.util.Strings;

class ResultSection {

	private final ImportDialog dialog;
	private final String epdMethod;
	private final String epdScope;
	final ResultModel result;

	private ResultSection(ImportDialog dialog, String epdMethod, String epdScope) {
		this.dialog = dialog;
		this.epdMethod = epdMethod;
		this.epdScope = epdScope;
		this.result = new ResultModel();
		this.result.name = dialog.epd.name + " - " + epdScope + " - " + epdMethod;
		this.result.setup = new CalculationSetup();
		this.result.setup.withType(CalculationType.SIMPLE_CALCULATION);

		// TODO: select best matching openLCA method
	}

	/**
	 * Initializes a result section for each method and scope pair that can
	 * be found in the EPD of the given import dialog.
	 */
	static List<ResultSection> initAllOf(ImportDialog dialog) {
		if (dialog == null)
			return Collections.emptyList();

		var sections = new ArrayList<ResultSection>();
		var epd = dialog.epd;
		for (var epdMethod : epd.impacts()) {
			var impacts = epd.getImpactSet(epdMethod).orElse(null);
			if (impacts == null || impacts.isEmpty())
				continue;
			var scopes = new HashSet<String>();
			impacts.each((_indicator, scopeSet) -> {
				for (var scope : scopeSet.scopes()) {
					scopes.add(scope);
				}
			});
			for (var scope : scopes) {
				sections.add(new ResultSection(dialog, epdMethod, scope));
			}
		}

		sections.sort((s1, s2) -> {
			var c = Strings.compare(s1.epdMethod, s2.epdMethod);
			return c != 0
				? c
				: Strings.compare(s1.epdScope, s2.epdScope);
		});

		return sections;
	}

	void render(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, "Result: " + result.name);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);

		var top = tk.createComposite(comp);
		UI.gridData(top, true, false);
		UI.gridLayout(top, 2, 10, 0);

		// name
		Controls.set(UI.formText(top, tk, M.Name), result.name, name -> {
			result.name = name;
			section.setText(result.name);
		});

		// EPD method and scope
		UI.formLabel(top, tk, "EPD LCIA Method");
		var methodLabel = UI.formLabel(top, tk, epdMethod);
		var _method = dialog.impactModel.getMethod(epdMethod);
		if (_method != null && _method.description() != null) {
			methodLabel.setToolTipText(_method.description());
		}
		UI.formLabel(top, tk, "EPD Scope");
		UI.formLabel(top, tk, epdScope);

		// mapped openLCA method
		var methodCombo = UI.formCombo(top, tk, "openLCA LCIA Method");
		EntityCombo.of(methodCombo, ImpactMethod.class, dialog.db)
			.select(result.setup.impactMethod())
			.onSelected(method -> {
				result.setup.withImpactMethod(method);
				// TODO: update the mapping table
			});


	}

}
