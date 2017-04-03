package org.openlca.app.cloud.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.M;
import org.openlca.app.util.TimeEstimatingMonitor;
import org.openlca.cloud.api.FetchNotifier;

public class FetchNotifierMonitor extends TimeEstimatingMonitor implements FetchNotifier {

	private final String fetchMessage;

	public FetchNotifierMonitor(IProgressMonitor monitor, String fetchMessage) {
		super(monitor);
		this.fetchMessage = fetchMessage;
	}

	@Override
	public void beginTask(TaskType type, int total) {
		if (type == TaskType.FETCH) {
			beginTask(fetchMessage, total);
		} else {
			beginTask(M.ImportData, total);
		}
	}
}