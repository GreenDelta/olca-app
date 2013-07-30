package org.openlca.core.application.logging;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.ui.PlatformUI;
import org.openlca.app.Error;
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
		if (openPopupCount.get() >= MAX_POPUPS) {
			return;
		}
		if (!PlatformUI.isWorkbenchRunning())
			return;
		if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
			String message = createMessage(event);
			try {
				openPopupCount.incrementAndGet();
				new PopupTokenWatch().start();
				Error.showPopup(message);
			} catch (Exception e) {
				handlePopupError(e);
			}
		}
	}

	/** If the creation of the popup creates an error itself handle it here. */
	private void handlePopupError(Exception e) {
		if (failureCounter.incrementAndGet() > 3) {
			log.warn("Showing of failed error popups "
					+ "stopped because of repetetive failures");
		} else {
			log.error("Show message failed", e);
		}
	}

	private String createMessage(LoggingEvent event) {
		String message = null;
		if (event.getMessage() instanceof String) {
			message = (String) event.getMessage();
		}
		return message;
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
