package org.openlca.app.rcp.plugins;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.RcpActivator;
import org.openlca.app.rcp.html.HtmlFolder;
import org.openlca.app.rcp.html.WebPage;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

@Deprecated
public class PluginManager extends FormDialog implements WebPage {

	private final static Gson mapper = new GsonBuilder().serializeNulls()
			.create();
	private final static PluginService service = new PluginService();
	private final static BundleService bundleService = new BundleService();
	private static Map<String, Plugin> plugins;

	private WebEngine webkit;

	public PluginManager() {
		super(UI.shell());
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 650);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = UI
				.formHeader(managedForm, "openLCA Plugin Manager");
		Composite body = form.getBody();
		body.getParent().getParent().setLayout(createNoSpacingLayout());
		body.getParent().setLayout(createNoSpacingLayout());
		body.setLayout(new FillLayout());
		toolkit.paintBordersFor(body);
		UI.createWebView(body, this);
		form.reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button b = createButton(parent, 22, "Install local file", false);
		Controls.onSelect(b, e -> installLocalFile());
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CLOSE_LABEL, true);
	}

	private GridLayout createNoSpacingLayout() {
		GridLayout layout = new GridLayout();
		layout.marginRight = 0;
		layout.marginLeft = 0;
		layout.horizontalSpacing = 0;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		layout.verticalSpacing = 0;
		layout.numColumns = 1;
		return layout;
	}

	@Override
	public String getUrl() {
		return HtmlFolder.getUrl(RcpActivator.getDefault().getBundle(),
				"plugin_manager.html");
	}

	@Override
	public void onLoaded(WebEngine webkit) {
		this.webkit = webkit;
		JSObject window = (JSObject) webkit.executeScript("window");
		window.setMember("java", new JsHandler());
		refresh();
	}

	// after installing/uninstalling the new versions won't be recognized by the
	// plugin service/bundle service until the application is restarted
	// therefore only fetch once and store in memory, otherwise the changes
	// could not be reflected in the UI after any action was taken
	private static void loadPlugins() {
		if (plugins == null) {
			plugins = new HashMap<String, Plugin>();
			List<Plugin> available = service.getAvailablePlugins();
			for (Plugin plugin : available)
				plugins.put(plugin.getSymbolicName(), plugin);
		}
	}

	private void refresh() {
		loadPlugins();
		String data = mapper.toJson(plugins.values());
		boolean online = isOnline();
		webkit.executeScript("setData(" + data + ", " + online + ")");
	}

	private boolean isOnline() {
		try {
			URL url = new URL(PluginService.BASE_URL);
			url.openConnection().connect();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void installLocalFile() {
		File file = FileChooser.forImport("*.jar");
		if (file == null)
			return;
		Plugin plugin = getPlugin(file, plugins.values());
		if (plugin == null) {
			Info.showBox("The file you selected isn't a valid openLCA plugin");
			return;
		}
		App.run("Installing " + plugin.getFullDisplayName(), () -> {
			service.copyLocalFile(file.toPath(), plugin);
		}, this::refresh);
	}

	private Plugin getPlugin(File jar, Collection<Plugin> plugins) {
		String[] nameAndVersion = bundleService.getSymbolicNameAndVersion(jar);
		for (Plugin plugin : plugins)
			if (plugin.getSymbolicName().equals(nameAndVersion[0]))
				if (plugin.getVersion().equals(nameAndVersion[1]))
					return plugin;
		return null;
	}

	public class JsHandler {

		public void install(String name) {
			Plugin plugin = plugins.get(name);
			String title = "Installing " + plugin.getFullDisplayName();
			App.runWithProgress(title, () -> service.install(plugin));
			refresh();
		}

		public void update(String name) {
			Plugin plugin = plugins.get(name);
			String title = "Updating to " + plugin.getFullDisplayName();
			App.runWithProgress(title, () -> {
				service.update(plugin);
				refresh();
			});
		}

		public void uninstall(String name) {
			Plugin plugin = plugins.get(name);
			String title = "Uninstalling " + plugin.getFullDisplayName();
			App.runWithProgress(title, () -> {
				service.uninstall(plugin);
				refresh();
			});
		}
	}

}
