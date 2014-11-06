package project;

import java.io.IOException;
import java.util.logging.Level;

import peerbase.LoggerUtil;
import peerbase.PeerInfo;

public class BobDemo {

	public static void main(String[] args) throws IOException
	{
		int port = 9000;
		if (args.length != 1) {
			System.out.println("Usage: java ... peerbase.sample.FileShareApp <host-port>");
		}
		else {
			port = Integer.parseInt(args[0]);
		}

		//LoggerUtil.setHandlersLevel(Level.FINE);
		new DemoApp("152.23.68.198", 9001, 5, new PeerInfo("152.23.68.198", port));

		/*	FileShareApp goo2 = new FileShareApp("localhost:8000", 
		 5, new PeerData("localhost", 8001)); */
	}

}
