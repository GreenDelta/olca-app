package org.openlca.app.devtools.csharp.server;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

public class ServerDispatcher {
	private Vector mMessageQueue = new Vector();
	private Vector<ClientInfo> mClients = new Vector<ClientInfo>();

	public synchronized void addClient(ClientInfo aClientInfo) {
		mClients.add(aClientInfo);
	}

	public synchronized void deleteClient(ClientInfo aClientInfo) {
		int clientIndex = mClients.indexOf(aClientInfo);
		if (clientIndex != -1)
			mClients.removeElementAt(clientIndex);
	}

	private synchronized void sendMessageToAllClients(String aMessage) {
		for (int i = 0; i < mClients.size(); i++) {
			ClientInfo infy = (ClientInfo) mClients.get(i);
			infy.mClientSender.sendMessage(aMessage);
		}
	}

	public void sendMessage(ClientInfo aClientInfo, String aMessage) {
		aClientInfo.mClientSender.sendMessage(aMessage);

	}

}