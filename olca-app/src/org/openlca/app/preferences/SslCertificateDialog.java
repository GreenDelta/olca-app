package org.openlca.app.preferences;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Encoder;
import sun.security.provider.X509Factory;

public class SslCertificateDialog extends FormDialog {

	private final static Logger log = LoggerFactory.getLogger(SslCertificateDialog.class);
	private final X509Certificate cert;
	private Font font;

	public SslCertificateDialog(X509Certificate cert) {
		super(UI.shell());
		this.cert = cert;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		UI.formHeader(mform, cert.getSubjectDN().getName());
		Composite body = UI.formBody(mform.getForm(), tk);
		UI.gridLayout(body, 1);
		UI.gridData(body, true, true);
		Text text = UI.formMultiText(body, mform.getToolkit(), null);
		UI.gridData(text, true, true);
		text.setText(getEncodedText());
		text.setEditable(false);
		font = initFont(text.getFont().getFontData());
		text.setFont(font);
		text.selectAll();
	}

	private Font initFont(FontData[] fontData) {
		for (FontData fd : fontData) {
			fd.setName("consolas");
		}
		return new Font(UI.shell().getDisplay(), fontData);
	}

	private String getEncodedText() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BASE64Encoder encoder = new BASE64Encoder();
			out.write((X509Factory.BEGIN_CERT + "\r\n").getBytes("utf-8"));
			encoder.encodeBuffer(cert.getEncoded(), out);
			out.write(X509Factory.END_CERT.getBytes("utf-8"));
			return out.toString();
		} catch (Exception e) {
			log.error("Error encoding certificate", e);
			return null;
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		font.dispose();
	}

}
