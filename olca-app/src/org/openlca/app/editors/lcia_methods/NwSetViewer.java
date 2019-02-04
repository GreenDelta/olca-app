package org.openlca.app.editors.lcia_methods;

import java.util.UUID;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.M;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.viewers.table.AbstractTableViewer;
import org.openlca.app.viewers.table.modify.field.StringModifier;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;

class NwSetViewer extends AbstractTableViewer<NwSet> {

	private static final String NAME = M.NormalizationAndWeightingSet;
	private static final String UNIT = M.ReferenceUnit;

	private ImpactMethodEditor editor;

	public NwSetViewer(Composite parent, ImpactMethodEditor editor) {
		super(parent);
		this.editor = editor;
		getModifySupport().bind(NAME, new StringModifier<>(editor, "name"));
		getModifySupport().bind(UNIT, new StringModifier<>(editor, "weightedScoreUnit"));
		Tables.onDoubleClick(getViewer(), (event) -> {
			TableItem item = Tables.getItem(getViewer(), event);
			if (item == null)
				onCreate();
		});
	}

	public void setInput(ImpactMethod method) {
		if (method == null)
			setInput(new NwSet[0]);
		else
			setInput(method.nwSets);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new SetLabelProvider();
	}

	@Override
	protected String[] getColumnHeaders() {
		return new String[] { NAME, UNIT };
	}

	@OnAdd
	protected void onCreate() {
		NwSet set = new NwSet();
		set.name = "Enter a name";
		set.refId = UUID.randomUUID().toString();
		ImpactMethod method = editor.getModel();
		method.nwSets.add(set);
		setInput(method.nwSets);
		select(set);
		editor.setDirty(true);
	}

	@OnRemove
	protected void onRemove() {
		ImpactMethod method = editor.getModel();
		for (NwSet set : getAllSelected())
			method.nwSets.remove(set);
		setInput(method.nwSets);
		editor.setDirty(true);
	}

	private class SetLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof NwSet))
				return null;
			NwSet set = (NwSet) obj;
			switch (col) {
			case 0:
				return set.name;
			case 1:
				return set.weightedScoreUnit;
			default:
				return null;
			}
		}
	}
}
