package org.openlca.app.tools.mcp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Display;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.tools.mcp.tools.DataSync;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.slf4j.LoggerFactory;

/// Synchronizes the changes of agents with the openLCA application: it evicts
/// the application caches, refreshes the navigation, and takes care of open
/// editors. Tool calls run in background threads of the server, so all UI
/// operations are dispatched to the UI thread.
class AppDataSync implements DataSync {

	/// Delay for collecting multiple changes into one navigation refresh.
	private static final int REFRESH_DELAY_MS = 500;

	private final Display display;
	private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);

	AppDataSync(Display display) {
		this.display = display;
	}

	@Override
	public String checkModifiable(RootDescriptor d) {
		if (d == null)
			return null;
		// we do not use `syncExec` here, as this could dead-lock when the UI
		// thread waits for the server, e.g. when it is stopped
		var future = new CompletableFuture<Boolean>();
		if (!asyncExec(() -> future.complete(App.hasDirtyEditor(d))))
			return null;
		try {
			boolean dirty = future.get(10, TimeUnit.SECONDS);
			return dirty
					? "The data set '" + d.name + "' is currently opened in an "
					+ "editor in openLCA with unsaved changes; ask the user to "
					+ "save or close the editor first."
					: null;
		} catch (Exception e) {
			return "Could not check if the data set '" + d.name
					+ "' is currently edited in openLCA: " + e.getMessage();
		}
	}

	@Override
	public void saved(RootDescriptor d) {
		asyncExec(() -> {
			AppContext.evict(d);
			// reload an open editor so that it shows the new state; editors with
			// unsaved changes were rejected before in `checkModifiable`
			if (App.isOpen(d) && !App.hasDirtyEditor(d)) {
				App.close(d);
				App.open(d);
			}
		});
		scheduleRefresh();
	}

	@Override
	public void deleted(RootDescriptor d) {
		asyncExec(() -> {
			App.close(d);
			AppContext.evict(d);
		});
		scheduleRefresh();
	}

	private void scheduleRefresh() {
		if (!refreshScheduled.compareAndSet(false, true))
			return;
		boolean ok = asyncExec(() -> display.timerExec(REFRESH_DELAY_MS, () -> {
			refreshScheduled.set(false);
			Navigator.refresh();
		}));
		if (!ok) {
			refreshScheduled.set(false);
		}
	}

	private boolean asyncExec(Runnable fn) {
		if (display == null || display.isDisposed())
			return false;
		display.asyncExec(() -> {
			try {
				fn.run();
			} catch (Exception e) {
				LoggerFactory.getLogger(getClass())
						.error("failed to sync MCP data change with UI", e);
			}
		});
		return true;
	}
}
