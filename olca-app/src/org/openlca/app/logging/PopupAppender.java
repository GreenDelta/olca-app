package org.openlca.app.logging;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.util.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An appender for log-messages in the user interface as pop-ups.
 */
public class PopupAppender extends AppenderSkeleton {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final int MAX_POPUPS = 5;
	private final int POPUP_REMOVAL_TIME = 5000;
	private AtomicInteger openPopupCount = new AtomicInteger(0);
	private AtomicInteger failureCounter = new AtomicInteger(0);

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		if (event == null || !event.getLevel().isGreaterOrEqual(Level.ERROR))
			return;
		if (!PlatformUI.isWorkbenchRunning()
				|| openPopupCount.get() >= MAX_POPUPS) {
			return;
		}

		try {
			openPopupCount.incrementAndGet();
			new PopupTokenWatch().start();
			String message = event.getMessage() != null
					? event.getMessage().toString() + "<br/>"
					: "";
			message += "<b>See the log file (under Help)"
					+ " for further information.</b>";

			Popup.error("An internal error occurred", message);
		} catch (Exception e) {
			// If the creation of the popup creates an error itself
			// handle it here.
			if (failureCounter.incrementAndGet() > 3) {
				log.warn("Showing of failed error popups "
						+ "stopped because of repetetive failures");
			} else {
				log.error("Show message failed", e);
			}
		}
	}

	private class PopupTokenWatch extends Thread {
		@Override
		public void run() {
			try {
				sleep(POPUP_REMOVAL_TIME);
				openPopupCount.decrementAndGet();
			} catch (Exception e) {
				log.warn("Failed to remove error pop-up token", e);
			}
		}
	}

}
