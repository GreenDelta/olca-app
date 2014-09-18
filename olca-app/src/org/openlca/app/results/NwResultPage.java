package org.openlca.app.results;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.db.Database;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.matrix.NwSetTable;
import org.openlca.core.results.ContributionItem;
import org.openlca.core.results.ContributionSet;
import org.openlca.core.results.Contributions;
import org.openlca.core.results.ImpactResult;
import org.openlca.core.results.SimpleResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows normalisation and weighting results.
 */
public class NwResultPage extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());

	private SimpleResultProvider<?> result;
	private CalculationSetup setup;
	private NwSetTable nwSetTable;
	private Composite body;
	private FormToolkit toolkit;

	public NwResultPage(FormEditor editor, SimpleResultProvider<?> result,
			CalculationSetup setup) {
		super(editor, "NwResultPage", Messages.NormalizationWeighting);
		this.result = result;
		this.setup = setup;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.NormalizationWeighting);
		toolkit = managedForm.getToolkit();
		body = UI.formBody(form, toolkit);
		nwSetTable = loadNwSetTable();
		if (nwSetTable == null)
			return;
		if (nwSetTable.hasNormalisationFactors())
			createNormalisationSection();
		else if (nwSetTable.hasWeightingFactors())
			createWeightingSection();
		if (nwSetTable.hasNormalisationFactors()
				&& nwSetTable.hasWeightingFactors())
			createSingleScoreSection();
	}

	private void createNormalisationSection() {
		List<ImpactResult> results = nwSetTable.applyNormalisation(result
				.getTotalImpactResults());
		createTable(Messages.Normalization, results, false);
	}

	private void createWeightingSection() {
		List<ImpactResult> results = nwSetTable.applyWeighting(result
				.getTotalImpactResults());
		createTable(Messages.Weighting, results, true);
	}

	private void createSingleScoreSection() {
		List<ImpactResult> results = nwSetTable.applyBoth(result
				.getTotalImpactResults());
		createTable(Messages.SingleScore, results, true);
	}

	private void createTable(String title, List<ImpactResult> results,
			boolean withUnit) {
		String[] columns;
		double[] colWidths;
		if (withUnit) {
			columns = new String[] { Messages.ImpactCategory, Messages.Amount,
					Messages.Unit };
			colWidths = new double[] { 0.5, 0.25, 0.25 };
		} else {
			columns = new String[] { Messages.ImpactCategory, Messages.Amount };
			colWidths = new double[] { 0.5, 0.25 };
		}
		Composite composite = UI.formSection(body, toolkit, title);
		TableViewer viewer = Tables.createViewer(composite, columns);
		viewer.setLabelProvider(new Label());
		Tables.bindColumnWidths(viewer, colWidths);
		List<ContributionItem<ImpactResult>> items = makeContributions(results);
		viewer.setInput(items);
		Actions.bind(viewer, TableClipboard.onCopy(viewer));
	}

	private List<ContributionItem<ImpactResult>> makeContributions(
			List<ImpactResult> results) {
		ContributionSet<ImpactResult> set = Contributions.calculate(results,
				new Contributions.Function<ImpactResult>() {
					@Override
					public double value(ImpactResult impactResult) {
						return impactResult.getValue();
					}
				});
		List<ContributionItem<ImpactResult>> items = set.getContributions();
		Contributions.sortDescending(items);
		return items;
	}

	private NwSetTable loadNwSetTable() {
		if (setup.getNwSet() == null)
			return null;
		try {
			return NwSetTable.build(Database.get(), setup.getNwSet().getId());
		} catch (Exception e) {
			log.error("failed to load NW set factors from database", e);
			return null;
		}
	}

	private class Label extends LabelProvider implements ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		@SuppressWarnings("unchecked")
		public Image getColumnImage(Object o, int col) {
			if (col != 0 || !(o instanceof ContributionItem))
				return null;
			ContributionItem<ImpactResult> item = ContributionItem.class
					.cast(o);
			return image.getForTable(item.getShare());
		}

		@Override
		@SuppressWarnings("unchecked")
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ContributionItem))
				return null;
			ContributionItem<ImpactResult> item = ContributionItem.class
					.cast(o);
			switch (col) {
			case 0:
				return Labels
						.getDisplayName(item.getItem().getImpactCategory());
			case 1:
				return Numbers.format(item.getAmount());
			case 2:
				if (setup.getNwSet() != null)
					return setup.getNwSet().getWeightedScoreUnit();
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
