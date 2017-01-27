package org.openlca.app.components;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.devtools.python.Python;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.html.HtmlFolder;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Desktop;
import org.openlca.app.util.UI;
import org.openlca.updates.Update;
import org.openlca.updates.UpdateHelper;
import org.openlca.updates.UpdateManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class UpdateManager {

	private final static Logger log = LoggerFactory.getLogger(UpdateManager.class);

	/**
	 * Opens the update manager with all "unseen" and/or required updates.
	 * 
	 * @return true if the user installed (at least the required) updates, false
	 *         if canceled
	 */
	public static boolean openNewAndRequired() {
		UpdateHelper updater = new UpdateHelper(Database.get(), App.getCalculationContext(), Python.getDir());
		Set<UpdateManifest> updates = updater.getNewAndRequired();
		if (updates.isEmpty())
			return true;
		return new Dialog(updater, updates).open() == IDialogConstants.OK_ID;
	}

	public static void openAll() {
		UpdateHelper updater = new UpdateHelper(Database.get(), App.getCalculationContext(), Python.getDir());
		Set<UpdateManifest> updates = updater.getAllUpdates();
		if (updates.isEmpty())
			return;
		new Dialog(updater, updates).open();
	}

	private UpdateManager() {

	}

	private static class Dialog extends FormDialog implements WebPage {

		private final Set<UpdateManifest> updates;
		private final UpdateHelper updater;
		private WebEngine webkit;
		private boolean executedHidden = true;
		private boolean hasExecuted = false;

		public Dialog(UpdateHelper updater, Set<UpdateManifest> updates) {
			super(UI.shell());
			this.updater = updater;
			this.updates = updates;
			for (UpdateManifest update : updates) {
				if (update.executed) {
					hasExecuted = true;
				}
			}
		}

		private List<UpdateManifest> topLevelUpdates() {
			Stack<String> toCheck = new Stack<>();
			Map<String, UpdateManifest> topLevelUpdates = new HashMap<>();
			for (UpdateManifest update : updates) {
				for (String mRefId : update.dependencies) {
					toCheck.add(mRefId);
				}
				topLevelUpdates.put(update.refId, update);
			}
			while (!toCheck.isEmpty()) {
				String mRefId = toCheck.pop();
				UpdateManifest update = topLevelUpdates.remove(mRefId);
				if (update != null) {
					toCheck.addAll(update.dependencies);
				}
			}
			return new ArrayList<UpdateManifest>(topLevelUpdates.values());
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			FormToolkit toolkit = mform.getToolkit();
			ScrolledForm form = UI.formHeader(mform, "#openLCA Update Manager");
			Composite body = form.getBody();
			UI.gridLayout(body.getParent(), 1, 0, 0);
			UI.gridLayout(body.getParent().getParent(), 1, 0, 0);
			body.setLayout(new FillLayout());
			toolkit.paintBordersFor(body);
			UI.createWebView(body, this);
			form.reflow(true);
		}

		@Override
		protected void okPressed() {
			List<String> selection = Arrays.asList(webkit.executeScript("getSelection()").toString().split(","));
			App.runWithProgress("#Applying database updates", () -> {
				for (UpdateManifest manifest : updates) {
					Update update = updater.getForRefId(manifest.refId);
					if (selection.contains(manifest.refId)) {
						execute(update);
					} else {
						updater.skip(update);
					}
				}
			});
			Navigator.refresh();
			super.okPressed();
		}

		private void execute(Update update) {
			if (update == null)
				return;
			for (String depRefId : update.manifest.dependencies) {
				Update dependency = updater.getForRefId(depRefId);
				execute(dependency);
			}
			updater.execute(update);
		}

		@Override
		public String getUrl() {
			return HtmlFolder.getUrl(RcpActivator.getDefault().getBundle(), "update_manager.html");
		}

		@Override
		public void onLoaded(WebEngine webkit) {
			this.webkit = webkit;
			JSObject window = (JSObject) webkit.executeScript("window");
			window.setMember("java", new JsHandler());
			refresh();
		}

		private void refresh() {
			List<UpdateManifest> manifests = topLevelUpdates();
			manifests.sort((m1, m2) -> -Long.compare(m1.releaseDate.getTime(), m2.releaseDate.getTime()));
			String data = new Gson().toJson(manifests);
			webkit.executeScript("setData(" + data + ")");
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			if (!hasExecuted) {
				super.createButtonsForButtonBar(parent);
				return;
			}
			Button b = createButton(parent, 9999, "Show executed updates", true);
			Controls.onSelect(b, (e) -> {
				if (executedHidden == true) {
					webkit.executeScript("showExecuted()");
					b.setText("Hide executed updates");
					executedHidden = false;
				} else {
					webkit.executeScript("hideExecuted()");
					b.setText("Show executed updates");
					executedHidden = true;
				}
			});
			super.createButtonsForButtonBar(parent);
		}

		@SuppressWarnings("unused")
		public class JsHandler {

			public String getUpdate(String refId) {
				UpdateManifest manifest = updater.getForRefId(refId).manifest;
				return new Gson().toJson(manifest);
			}

			public boolean hasAttachment(String refId) {
				Update update = updater.getForRefId(refId);
				return update != null && update.attachment != null && update.attachment.length > 0;
			}

			public void openAttachment(String refId) {
				if (!hasAttachment(refId))
					return;
				Update update = updater.getForRefId(refId);
				if (update == null)
					return;
				try {
					Path tmp = Files.createTempFile("olca", ".pdf");
					Files.copy(new ByteArrayInputStream(update.attachment), tmp, StandardCopyOption.REPLACE_EXISTING);
					Desktop.browse(tmp.toUri().toString());
					tmp.toFile().deleteOnExit();
				} catch (IOException e) {
					log.error("Error opening attachment", e);
				}
			}

		}

	}

}
