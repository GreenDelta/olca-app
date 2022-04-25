package org.openlca.app.results;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.ImpactValue;
import org.openlca.core.results.SimpleResult;

public class NwResultPage extends FormPage {

	private final SimpleResult result;
	private final CalculationSetup setup;
	private Composite body;
	private FormToolkit toolkit;

	public NwResultPage(FormEditor editor, SimpleResult result, CalculationSetup setup) {
		super(editor, "NwResultPage", M.NormalizationWeighting);
		this.result = result;
		this.setup = setup;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(mform,
				Labels.name(setup.target()),
				Images.get(result));
		toolkit = mform.getToolkit();
		body = UI.formBody(form, toolkit);
		if (setup.nwSet() == null)
			return;
		var nwSet = NwSetTable.of(Database.get(), setup.nwSet());
		if (nwSet.isEmpty())
			return;
		var impacts = result.getTotalImpactResults();
		if (impacts.isEmpty())
			return;
		if (nwSet.hasNormalization()) {
			createTable(M.Normalization, nwSet.normalize(impacts), false);
		} else if (nwSet.hasWeighting()) {
			createTable(M.Weighting, nwSet.weight(impacts), true);
		}
		if (nwSet.hasNormalization() && nwSet.hasWeighting()) {
			createTable(M.SingleScore, nwSet.apply(impacts), true);
		}
	}

	private void createTable(String title, List<ImpactValue> results,
			boolean withUnit) {
		var columns = withUnit
				? new String[] { M.ImpactCategory, M.Amount, M.Unit }
				: new String[] { M.ImpactCategory, M.Amount };
		var colWidths = withUnit
				? new double[] { 0.5, 0.25, 0.25 }
				: new double[] { 0.5, 0.25 };

		var section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, toolkit, 1);
		var viewer = Tables.createViewer(comp, columns);
		viewer.setLabelProvider(new Label());
		Tables.bindColumnWidths(viewer, colWidths);
		var items = Contributions.calculate(results, ImpactValue::value);
		Contributions.sortDescending(items);
		viewer.setInput(items);
		Actions.bind(viewer, TableClipboard.onCopySelected(viewer));
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		private final ContributionImage image = new ContributionImage();

		@Override
		@SuppressWarnings("unchecked")
		public Image getColumnImage(Object o, int col) {
			if (col != 0 || !(o instanceof Contribution))
				return null;
			Contribution<ImpactValue> item = Contribution.class.cast(o);
			return image.get(item.share);
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object o, int col) {
			if (!(o instanceof Contribution))
				return null;
			Contribution<ImpactValue> item = Contribution.class.cast(o);
			switch (col) {
			case 0:
				return Labels.name(item.item.impact());
			case 1:
				return Numbers.format(item.amount);
			case 2:
				if (setup.nwSet() != null)
					return setup.nwSet().weightedScoreUnit;
			default:
				return null;
			}
		}

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}
	}
}
