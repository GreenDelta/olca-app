package org.openlca.app.tools.mapping;

import java.util.UUID;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mapping.model.FlowMap;
import org.openlca.app.tools.mapping.model.FlowMapEntry;
import org.openlca.app.tools.mapping.model.FlowRef;
import org.openlca.app.tools.mapping.model.IMapProvider;
import org.openlca.app.util.Info;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.core.model.Category;
import org.openlca.io.CategoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingTool extends SimpleFormEditor {

	private MappingPage page;
	private FlowMap flowMap;
	private IMapProvider provider;

	public static void open() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Editors.open(new SimpleEditorInput(
				"FlowMappings", UUID.randomUUID().toString(), "#Flow mapping"),
				"MappingTool");
	}

	@Override
	public void dispose() {
		closeProvider();
		super.dispose();
	}

	@Override
	protected FormPage getPage() {
		page = new MappingPage();
		return page;
	}

	void setContent(FlowMap flowMap, IMapProvider provider) {
		if (flowMap == null || provider == null)
			return;
		this.flowMap = flowMap;
		closeProvider();
		this.provider = provider;
		this.flowMap = flowMap;
		this.page.table.setInput(flowMap.entries);
	}

	private void closeProvider() {
		if (this.provider == null)
			return;
		try {
			this.provider.close();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to close provider", e);
		}
	}

	private class MappingPage extends FormPage {

		private TableViewer table;

		public MappingPage() {
			super(MappingTool.this, "MappingPage", "#Flow mapping");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, "#Flow mapping");
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);

			table = Tables.createViewer(
					body,
					"Status",
					"Source flow",
					"Source category",
					"Source unit",
					"Target flow",
					"Target category",
					"Target unit",
					"Conversion factor",
					"Default provider");
			table.setLabelProvider(new Label());

			double w = 1.0 / 9.0;
			Tables.bindColumnWidths(table, w, w, w, w, w, w, w, w, w);

			form.reflow(true);
		}
	}

	private class Label extends LabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof FlowMapEntry))
				return null;
			FlowMapEntry e = (FlowMapEntry) obj;
			if (col == 0)
				return stateIcon(e.syncState);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof FlowMapEntry))
				return null;
			FlowMapEntry e = (FlowMapEntry) obj;

			switch (col) {
			case 0:
				return stateText(e.syncState);
			case 1:
				return flow(e.sourceFlow);
			case 2:
				return category(e.sourceFlow);
			case 3:
				return unit(e.sourceFlow);
			case 4:
				return flow(e.targetFlow);
			case 5:
				return category(e.targetFlow);
			case 6:
				return unit(e.targetFlow);
			case 7:
				return Numbers.format(e.factor);
			default:
				return null;
			}

		}

		private String stateText(FlowMapEntry.SyncState state) {
			if (state == null)
				return "?";
			switch (state) {
			case APPLIED:
				return "Applied";
			case DUPLICATE:
				return "Duplicate mapping";
			case INVALID_SOURCE:
				return "Invalid source flow";
			case INVALID_TARGET:
				return "Invalid target flow";
			case MATCHED:
				return "Matched";
			case UNFOUND_SOURCE:
				return "Unfound source flow";
			case UNFOUND_TARGET:
				return "Unfound target flow";
			default:
				return "?";
			}
		}

		private Image stateIcon(FlowMapEntry.SyncState state) {
			if (state == null)
				return null;
			switch (state) {
			case APPLIED:
				return Icon.ACCEPT.get();
			case DUPLICATE:
				return Icon.WARNING.get();
			case INVALID_SOURCE:
				return Icon.ERROR.get();
			case INVALID_TARGET:
				return Icon.ERROR.get();
			case MATCHED:
				return Icon.ACCEPT.get();
			case UNFOUND_SOURCE:
				return Icon.WARNING.get();
			case UNFOUND_TARGET:
				return Icon.WARNING.get();
			default:
				return null;
			}
		}

		private String flow(FlowRef ref) {
			if (ref == null || ref.flow == null)
				return "?";
			if (ref.flow.id != 0L)
				return Labels.getDisplayName(ref.flow);
			return ref.flow.name;
		}

		private String category(FlowRef ref) {
			if (ref == null || ref.flow == null)
				return "?";
			if (ref.flow.id != 0L) {
				if (ref.flow.category == null)
					return "";
				Category category = Cache.getEntityCache().get(
						Category.class, ref.flow.category);
				if (category == null)
					return "?";
				return CategoryPath.getFull(category);
			}
			// TODO: get it from the provider.
			return "?";
		}

		private String unit(FlowRef ref) {
			if (ref == null || ref.unit == null)
				return "?";
			String unit = ref.unit.name;
			if (ref.flowProperty != null) {
				unit += " (" + ref.flowProperty.name + ")";
			}
			return unit;
		}

	}
}
