package org.openlca.app.editors.libraries;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.library.LibraryInfo;

public class LibraryInfoPage extends FormPage {

	public static final String ID = "LibraryInfoPage";
	private final LibraryInfo info;

	LibraryInfoPage(LibraryEditor editor) {
		super(editor, ID, M.GeneralInformation);
		info = editor.info;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var title = M.GeneralInformation + ": " + info.name();
		var form = UI.header(mForm, title, Icon.LIBRARY.get());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);

		var comp = UI.formSection(body, tk, M.GeneralInformation);
		var name = UI.labeledText(comp, tk, M.Name);
		name.setEditable(false);
		if (info.name() != null) {
			name.setText(info.name());
		}

		var description = UI.multiText(comp, tk, M.Description);
		description.setEditable(false);
		if (info.description() != null) {
			description.setText(info.description());
		}

		var isRegionalized = UI.labeledCheckbox(comp, tk, "Is regionalized");
		isRegionalized.setSelection(info.isRegionalized());
		isRegionalized.setEnabled(false);

		if (!info.dependencies().isEmpty()) {
			renderDepTable(body, tk);
		}
	}

	private void renderDepTable(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, "Library dependencies", 1);
		var table = Tables.createViewer(comp, "Library");
		Tables.bindColumnWidths(table, 1.0);
		table.setInput(info.dependencies());
		table.setLabelProvider(new DepLabel());

		var onOpen = Actions.onOpen(() -> {
			if (Viewers.getFirstSelected(table) instanceof String id) {
				var libDir = Workspace.getLibraryDir();
				libDir.getLibrary(id)
						.ifPresent(LibraryEditor::open);
			}
		});
		Actions.bind(table, onOpen);
		Tables.onDoubleClick(table, $ -> onOpen.run());
	}

	private static class DepLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Icon.LIBRARY.get();
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return obj instanceof String s ? s : null;
		}
	}

}
