package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.progress.UIJob;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.devtools.js.JavaScriptEditor;
import org.openlca.app.devtools.python.PythonEditor;
import org.openlca.app.devtools.sql.SqlEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.core.model.CategorizedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Editors {

	private static String[] PREVENT_FROM_CLOSING = {
			SqlEditor.TYPE,
			PythonEditor.TYPE,
			JavaScriptEditor.TYPE,
			StartPage.TYPE,
			LogFileEditor.TYPE };
	private static Logger log = LoggerFactory.getLogger(Editors.class);

	private Editors() {
	}

	/**
	 * Adds a refresh function to the tool-bar of the given form (content of a
	 * editor page). When this function is executed the given editor is closed and
	 * opened again.
	 */
	public static void addRefresh(ScrolledForm form, ModelEditor<?> editor) {
		if (form == null || editor == null)
			return;
		CategorizedEntity model = editor.getModel();
		Action refresh = Actions.create(M.Reload, Icon.REFRESH.descriptor(),
				() -> {
					App.closeEditor(model);
					App.openEditor(model);
				});
		IToolBarManager toolbar = form.getToolBarManager();
		toolbar.add(refresh);
	}

	/**
	 * Closes all editors (except start page, Log editor and script editors)
	 */
	public static boolean closeAll() {
		try {
			List<IEditorReference> rest = new ArrayList<>();
			for (IEditorReference ref : getReferences()) {
				if (ref.getEditorInput() instanceof SimpleEditorInput) {
					SimpleEditorInput input = (SimpleEditorInput) ref.getEditorInput();
					List<String> preventClosing = Arrays.asList(PREVENT_FROM_CLOSING);
					if (preventClosing.contains(input.type))
						continue;
				}
				rest.add(ref);
			}
			if (rest.size() == 0)
				return true;
			IEditorReference[] restArray = rest.toArray(new IEditorReference[rest.size()]);
			return getActivePage().closeEditors(restArray, true);
		} catch (Exception e) {
			log.error("Failed to close editors", e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends IEditorPart> T getActive() {
		try {
			return (T) getActivePage().getActiveEditor();
		} catch (ClassCastException e) {
			log.error("Error getting active editor", e);
			return null;
		}
	}

	public static void open(IEditorInput input, String editorId) {
		new OpenInUIJob(input, editorId).schedule();
	}

	public static void close(IEditorReference ref) {
		try {
			getActivePage().closeEditor(ref.getEditor(false), true);
		} catch (Exception e) {
			log.error("Failed to close an editor", e);
		}
	}

	public static IEditorReference[] getReferences() {
		try {
			return getActivePage().getEditorReferences();
		} catch (Exception e) {
			log.error("Failed to get editor references", e);
			return new IEditorReference[0];
		}
	}

	public static IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private static class OpenInUIJob extends UIJob {

		private IEditorInput input;
		private String editorId;

		public OpenInUIJob(IEditorInput input, String editorId) {
			super(M.OpenEditor);
			this.input = input;
			this.editorId = editorId;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			try {
				getActivePage().openEditor(input, editorId);
				return Status.OK_STATUS;
			} catch (Exception e) {
				log.error("Open editor " + editorId + " failed.", e);
				return Status.CANCEL_STATUS;
			}
		}
	}

}
