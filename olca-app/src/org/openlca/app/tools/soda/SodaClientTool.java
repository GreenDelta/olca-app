package org.openlca.app.tools.soda;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.util.Strings;


public class SodaClientTool extends SimpleFormEditor {

	private Connection con;

	public static void open() {
		var con = LoginDialog.show().orElse(null);
		if (con == null || con.hasError())
			return;
		var id = AppContext.put(con);
		var input = new SimpleEditorInput(id, Strings.cut(con.toString(), 25));
		Editors.open(input, "SodaClientTool");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		var inp = (SimpleEditorInput) input;
		con = AppContext.remove(inp.id, Connection.class);
		setTitleImage(Icon.SODA.get());
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new SodaPage(this, con);
	}
}
