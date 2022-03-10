package org.openlca.app.logging;

import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.util.Popup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An appender for log-messages in the user interface as pop-ups.
 */
class PopupAppender extends AppenderBase<ILoggingEvent> {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final AtomicInteger openPopupCount = new AtomicInteger(0);
	private final AtomicInteger failureCounter = new AtomicInteger(0);

	static PopupAppender create() {
		var factory = LoggerFactory.getILoggerFactory();
		if (!(factory instanceof LoggerContext context))
			return null;
		var appender = new PopupAppender();
		appender.setContext(context);
		appender.setName("popup");
		appender.start();
		return appender;
	}

	@Override
	protected void append(ILoggingEvent event) {
		if (event == null || !event.getLevel().isGreaterOrEqual(Level.ERROR))
			return;
		int MAX_POPUPS = 5;
		if (!PlatformUI.isWorkbenchRunning() || openPopupCount.get() >= MAX_POPUPS) {
			return;
		}

		try {
			openPopupCount.incrementAndGet();
			new PopupTokenWatch().start();
			var message = event.getMessage() != null
					? event.getMessage() + "<br/>"
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
				sleep(5000);
				openPopupCount.decrementAndGet();
			} catch (Exception e) {
				log.warn("Failed to remove error pop-up token", e);
			}
		}
	}

}
