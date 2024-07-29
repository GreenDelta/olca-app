package org.openlca.app.collaboration.util;

import org.openlca.app.M;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.collaboration.model.WebRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebRequests {

	private static final Logger log = LoggerFactory.getLogger(WebRequests.class);

	public static boolean execute(RequestWithoutResponse request) {
		try {
			request.execute();
			return true;
		} catch (WebRequestException e) {
			if (handleException("Error during collaboration server request", e))
				return execute(request);
			return false;
		}
	}

	public static <T> T execute(RequestWithResponse<T> request) {
		return execute(request, null);
	}

	public static <T> T execute(RequestWithResponse<T> request, T defaultValue) {
		try {
			var value = request.execute();
			if (value == null)
				return defaultValue;
			return value;
		} catch (WebRequestException e) {
			if (handleException("Error during collaboration server request", e))
				return execute(request, defaultValue);
			return defaultValue;
		}
	}

	public static boolean handleException(String message, WebRequestException e) {
		if (e.isSslCertificateException()) {
			if (Question.ask(M.SslCertificateUnknown, M.SslCertificateUnknownQuestion)) {
				var cert = SslCertificates.downloadCertificate(e.getHost(), e.getPort());
				SslCertificates.importCertificate(cert, e.getHost());
				return true;
			}
		}
		log.error(message, e);
		MsgBox.error(e.getMessage());
		return false;
	}

	public interface RequestWithoutResponse {

		void execute() throws WebRequestException;

	}

	public interface RequestWithResponse<T> {

		T execute() throws WebRequestException;

	}

}
