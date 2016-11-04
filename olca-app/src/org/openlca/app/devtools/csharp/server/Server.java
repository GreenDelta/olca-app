package org.openlca.app.devtools.csharp.server;

import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.SwingWorker;

public class Server {

	private static ServerSocket serverSocket = null; 

	public static final int LISTENING_PORT = 3800;

	public static void openSocket() {
		// Open server socket for listening
		try {
			serverSocket = new ServerSocket(LISTENING_PORT);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static void startDispatching() {
		// Start ServerDispatcher thread
		ServerDispatcher serverDispatcher = new ServerDispatcher();

		// Accept and handle client connections
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				ClientInfo clientInfo = new ClientInfo();
				clientInfo.mSocket = socket;
				ClientListener clientListener = new ClientListener(clientInfo, serverDispatcher);
				ClientSender clientSender = new ClientSender(clientInfo, serverDispatcher);
				clientInfo.mClientListener = clientListener;
				clientInfo.mClientSender = clientSender;
				clientListener.start();
				clientSender.start();
				serverDispatcher.addClient(clientInfo);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} /*finally {
				closeSocket();
			}*/
		}
	}
	
	public static void closeSocket() {
		if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ioe) {
            	ioe.printStackTrace();
            }
        }
	}
	
	public static class startInBackground extends SwingWorker<Object, Object> {
		@Override
		protected Object doInBackground() throws Exception {
			try {
				Server.openSocket();
				Server.startDispatching();
				return null;
			} finally {
				Server.closeSocket();
			}
		}
	}
}