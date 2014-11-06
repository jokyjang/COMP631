package project;

import java.util.List;

import javax.xml.bind.DatatypeConverter;

import peerbase.HandlerInterface;
import peerbase.LoggerUtil;
import peerbase.Node;
import peerbase.PeerConnection;
import peerbase.PeerInfo;
import peerbase.PeerMessage;
import peerbase.RouterInterface;

public class MessageProcessNode extends Node {

	/* MESSAGE TYPES */
	public static final String INSERTPEER = "JOIN";
	public static final String LISTPEER = "LIST";
	public static final String PEERNAME = "NAME";
	public static final String RECVMSG = "RECV";
	public static final String FETCHBUF = "FECH";
	public static final String STOPPROC = "STOP";
	public static final String PEERQUIT = "QUIT";

	public static final String REPLY = "REPL";
	public static final String ERROR = "ERRO";
	
	public Sender sender;
	public Receiver receiver;

	public List<String> buffer;

	public MessageProcessNode(int maxPeers, PeerInfo info) {
		super(maxPeers, info);
		sender = new Sender(this);
		receiver = new Receiver(this);

		this.addRouter(new Router(this));

		this.addHandler(INSERTPEER, new JoinHandler(this));
		this.addHandler(LISTPEER, new ListHandler(this));
		this.addHandler(PEERNAME, new NameHandler(this));
		this.addHandler(RECVMSG, receiver);
		this.addHandler(FETCHBUF, new FetchHandler(this));
		this.addHandler(STOPPROC, new StopHandler(this));
		this.addHandler(PEERQUIT, new QuitHandler(this));
	}

	public void buildPeers(String host, int port, int hops) {
		LoggerUtil.getLogger().fine("build peers");

		if (this.maxPeersReached() || hops <= 0)
			return;
		PeerInfo pd = new PeerInfo(host, port);
		List<PeerMessage> resplist = this
				.connectAndSend(pd, PEERNAME, "", true);
		if (resplist == null || resplist.size() == 0)
			return;
		String peerid = resplist.get(0).getMsgData();
		LoggerUtil.getLogger().fine("contacted " + peerid);
		pd.setId(peerid);

		String resp = this.connectAndSend(pd, INSERTPEER,
				String.format("%s %s %d", this.getId(), this.getHost(),
				this.getPort()), true).get(0).getMsgType();
		if (!resp.equals(REPLY) || this.getPeerKeys().contains(peerid))
			return;

		this.addPeer(pd);

		resplist = this.connectAndSend(pd, FETCHBUF, "", true);
		if (resplist.size() > 1) {
			resplist.remove(0);
			for (PeerMessage pm : resplist) {
				buffer.add(pm.getMsgData());
			}
		}

		// do recursive depth first search to add more peers
		resplist = this.connectAndSend(pd, LISTPEER, "", true);

		if (resplist.size() > 1) {
			resplist.remove(0);
			for (PeerMessage pm : resplist) {
				String[] data = pm.getMsgData().split("\\s");
				String nextpid = data[0];
				String nexthost = data[1];
				int nextport = Integer.parseInt(data[2]);
				if (!nextpid.equals(this.getId()))
					buildPeers(nexthost, nextport, hops - 1);
			}
		}
	}

	/* msg syntax: JOIN pid host port */
	private class JoinHandler implements HandlerInterface {
		private Node peer;

		public JoinHandler(Node peer) {
			this.peer = peer;
		}

		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			if (peer.maxPeersReached()) {
				LoggerUtil.getLogger().fine(
						"maxpeers reached " + peer.getMaxPeers());
				peerconn.sendData(new PeerMessage(ERROR, "Join: "
						+ "too many peers"));
				return;
			}

			// check for correct number of arguments
			String[] data = msg.getMsgData().split("\\s");
			if (data.length != 3) {
				peerconn.sendData(new PeerMessage(ERROR, "Join: "
						+ "incorrect arguments"));
				return;
			}

			// parse arguments into PeerInfo structure
			PeerInfo info = new PeerInfo(data[0], data[1],
					Integer.parseInt(data[2]));

			if (peer.getPeer(info.getId()) != null)
				peerconn.sendData(new PeerMessage(ERROR, "Join: "
						+ "peer already inserted"));
			else if (info.getId().equals(peer.getId()))
				peerconn.sendData(new PeerMessage(ERROR, "Join: "
						+ "attempt to insert self"));
			else {
				peer.addPeer(info);
				peerconn.sendData(new PeerMessage(REPLY, "Join: "
						+ "peer added: " + info.getId()));
			}
		}
	}

	/* msg syntax: LIST */
	private class ListHandler implements HandlerInterface {
		private Node peer;

		public ListHandler(Node peer) {
			this.peer = peer;
		}

		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			peerconn.sendData(new PeerMessage(REPLY, String.format("%d",
					peer.getNumberOfPeers())));
			for (String pid : peer.getPeerKeys()) {
				peerconn.sendData(new PeerMessage(REPLY, String.format(
						"%s %s %d", pid, peer.getPeer(pid).getHost(), peer
								.getPeer(pid).getPort())));
			}
		}
	}

	/* msg syntax: NAME */
	private class NameHandler implements HandlerInterface {
		private Node peer;

		public NameHandler(Node peer) {
			this.peer = peer;
		}

		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			peerconn.sendData(new PeerMessage(REPLY, peer.getId()));
		}
	}

	private class FetchHandler implements HandlerInterface {
		@SuppressWarnings("unused")
		private Node peer;

		public FetchHandler(Node peer) {
			this.peer = peer;
		}
		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			peerconn.sendData(new PeerMessage(REPLY, buffer.size() + ""));
			for (String msgToSend : buffer) {
				peerconn.sendData(new PeerMessage(REPLY, msgToSend));
			}
		}

	}

	/* msg syntax: STOP */
	private class StopHandler implements HandlerInterface {
		@SuppressWarnings("unused")
		private Node peer;

		public StopHandler(Node peer) {
			this.peer = peer;
		}

		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			// TODO Auto-generated method stub

		}

	}

	/* msg syntax: QUIT pid */
	private class QuitHandler implements HandlerInterface {
		private Node peer;

		public QuitHandler(Node peer) {
			this.peer = peer;
		}

		public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
			String pid = msg.getMsgData().trim();
			if (peer.getPeer(pid) == null) {
				peerconn.sendData(new PeerMessage(ERROR,
						"Quit: peer not found: " + pid));
			} else {
				peer.removePeer(pid);
				peerconn.sendData(new PeerMessage(REPLY, "Quit: peer removed: "
						+ pid));
			}
		}
	}

	private class MessageProcess extends Thread {

	}

	private class Router implements RouterInterface {
		private Node peer;

		public Router(Node peer) {
			this.peer = peer;
		}

		public PeerInfo route(String peerid) {
			if (peer.getPeerKeys().contains(peerid))
				return peer.getPeer(peerid);
			else
				return null;
		}
	}

	/**
	 * This method will broadcast message to all the other peers it connects.
	 * @param message
	 */
	public void broadCast(byte[] message) {
		String strMsg = DatatypeConverter.printBase64Binary(message);
		for(String pid : this.getPeerKeys()) {
			PeerInfo info = this.getPeer(pid);
			this.connectAndSend(info, RECVMSG, strMsg, false);
		}
	}
	
	
}
