package org.openlca.app.results.analysis.sankey;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.openlca.app.App;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;

/**
 * Opens the process editor on a double click. As 'mouseDoubleClick' not works
 * this listener reacts on 'mousePressed' with a delay function between the
 * clicks.
 */
public class ProcessMouseClick implements MouseListener {

	private boolean firstClick = true;
	private ProcessNode processNode;

	public ProcessMouseClick(ProcessNode processNode) {
		this.processNode = processNode;
	}

	@Override
	public void mouseDoubleClicked(MouseEvent evt) {
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		if (evt.button != 1)
			return;
		if (firstClick) {
			firstClick = false;
			scheduleTimer();
		} else {
			App.openEditor(processNode.getProcess());
		}
	}

	private void scheduleTimer() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				firstClick = true;
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 250);
	}

	@Override
	public void mouseReleased(MouseEvent evt) {
	}

}
