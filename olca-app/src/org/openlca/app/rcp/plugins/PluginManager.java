package org.openlca.app.rcp.plugins;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
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
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PluginManager extends FormDialog implements HtmlPage {

	private final static Logger log = LoggerFactory
			.getLogger(PluginManager.class);
	private final static ObjectMapper mapper = new ObjectMapper();
	private final static PluginService service = new PluginService();
	private final static BundleService bundleService = new BundleService();
	private static Map<String, Plugin> plugins;

	private Browser browser;

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
		body.setLayout(createNoSpacingLayout());
		toolkit.paintBordersFor(body);
		UI.gridData(body, true, true);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button localButton = createButton(parent, 22, "Install local file",
				false);
		localButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				installLocalFile();
			}
		});
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
	public void onLoaded() {
		new InstallFunction(browser);
		new UpdateFunction(browser);
		new UninstallFunction(browser);
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
		try {
			String data = mapper.writeValueAsString(plugins.values());
			boolean online = isOnline();
			browser.evaluate("setData(" + data + ", " + online + ")");
		} catch (JsonProcessingException e) {
			log.error("Error writing plugins json", e);
		}
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
		App.runWithProgress("Installing " + plugin.getFullDisplayName(),
				() -> {
					service.copyLocalFile(file.toPath(), plugin);
					refresh();
				});
	}

	private Plugin getPlugin(File jar, Collection<Plugin> plugins) {
		String[] nameAndVersion = bundleService.getSymbolicNameAndVersion(jar);
		for (Plugin plugin : plugins)
			if (plugin.getSymbolicName().equals(nameAndVersion[0]))
				if (plugin.getVersion().equals(nameAndVersion[1]))
					return plugin;
		return null;
	}

	private class InstallFunction extends BrowserFunction {

		public InstallFunction(Browser browser) {
			super(browser, "install");
		}

		@Override
		public Object function(Object[] arguments) {
			Plugin plugin = plugins.get(arguments[0]);
			App.runWithProgress("Installing " + plugin.getFullDisplayName(),
					() -> {
						service.install(plugin);
					});
			refresh();
			return null;
		}
	}

	private class UpdateFunction extends BrowserFunction {

		public UpdateFunction(Browser browser) {
			super(browser, "update");
		}

		@Override
		public Object function(Object[] arguments) {
			Plugin plugin = plugins.get(arguments[0]);
			App.runWithProgress("Updating to " + plugin.getFullDisplayName(),
					() -> {
						service.update(plugin);
						refresh();
					});
			return null;
		}

	}

	private class UninstallFunction extends BrowserFunction {

		public UninstallFunction(Browser browser) {
			super(browser, "uninstall");
		}

		@Override
		public Object function(Object[] arguments) {
			Plugin plugin = plugins.get(arguments[0]);
			App.runWithProgress("Uninstalling " + plugin.getFullDisplayName(),
					() -> {
						service.uninstall(plugin);
						refresh();
					});
			return null;
		}

	}

}
