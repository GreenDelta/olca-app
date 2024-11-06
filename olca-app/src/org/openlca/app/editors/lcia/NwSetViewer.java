package org.openlca.app.editors.lcia;

import java.util.UUID;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.app.M;
import org.openlca.app.viewers.tables.AbstractTableViewer;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.field.StringModifier;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.NwSet;

class NwSetViewer extends AbstractTableViewer<NwSet> {

	private static final String NAME = M.NormalizationAndWeightingSet;
	private static final String UNIT = M.ReferenceUnit;

	private final ImpactMethodEditor editor;

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
		return new String[]{NAME, UNIT};
	}

	@OnAdd
	protected void onCreate() {
		if (!editor.isEditable())
			return;
		NwSet set = new NwSet();
		set.name = M.EnterAName;
		set.refId = UUID.randomUUID().toString();
		var method = editor.getModel();
		method.nwSets.add(set);
		setInput(method.nwSets);
		select(set);
		editor.setDirty(true);
	}

	@OnRemove
	protected void onRemove() {
		if (!editor.isEditable())
			return;
		var method = editor.getModel();
		for (NwSet set : getAllSelected())
			method.nwSets.remove(set);
		setInput(method.nwSets);
		editor.setDirty(true);
	}

	private static class SetLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof NwSet set))
				return null;
			return switch (col) {
				case 0 -> set.name;
				case 1 -> set.weightedScoreUnit;
				default -> null;
			};
		}
	}
}
