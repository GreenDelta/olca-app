package org.openlca.app.collaboration.util;

import org.openlca.app.M;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.collaboration.model.WebRequestException;

public class WebRequests {

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
		return execute(null, request, defaultValue);
	}

	public static <T> T execute(String feature, RequestWithResponse<T> request, T defaultValue) {
		try {
			var value = request.execute();
			if (value == null)
				return defaultValue;
			return value;
		} catch (WebRequestException e) {
			if (handleException(feature, "Error during collaboration server request", e))
				return execute(request, defaultValue);
			return defaultValue;
		}
	}

	public static boolean handleException(String message, WebRequestException e) {
		return handleException(null,  message, e);
	}

	private static boolean handleException(String feature, String message, WebRequestException e) {
		if (e.isSslCertificateException()) {
			if (Question.ask(M.SslCertificateUnknown, M.SslCertificateUnknownQuestion)) {
				var cert = SslCertificates.downloadCertificate(e.getHost(), e.getPort());
				SslCertificates.importCertificate(cert, e.getHost());
				return true;
			}
		}
		if (e.getErrorCode() == 503) {
			MsgBox.warning(M.FeatureNotAvailable + ": " + feature);
			return false;
		}
		if (e.isConnectException()) {
			MsgBox.error(e.getMessage());
			return false;
		}
		ErrorReporter.on(message, e);
		return false;
	}

	public interface RequestWithoutResponse {

		void execute() throws WebRequestException;

	}

	public interface RequestWithResponse<T> {

		T execute() throws WebRequestException;

	}

}
