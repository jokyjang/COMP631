package project;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import peerbase.HandlerInterface;
import peerbase.Node;
import peerbase.PeerConnection;
import peerbase.PeerMessage;

public class Receiver implements HandlerInterface {
	/* msg syntax: RECV msg */
	private MessageProcessNode peer;
	private Hashtable<String, String> messages;

	public Receiver(MessageProcessNode peer) {
		this.peer = peer;
		messages = new Hashtable<String, String>();
	}
	
	private String generateHash(byte[] data) {
		String hash = null;
		try {
			MessageDigest mDigest = MessageDigest
					.getInstance("SHA1");
			hash = DatatypeConverter.printBase64Binary(mDigest.digest(data));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hash;
	}

	public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
		// store the msg into buffer
		msg.getMsgType();
		byte[] data = DatatypeConverter.parseBase64Binary(msg.getMsgData());
		String hash = this.generateHash(data);
		String pid = peerconn.getPeerInfo().getId();
		messages.put(hash, pid);
	}
	
	public Set<String> getMessageHashes() {
		return messages.keySet();
	}
	
	public String getMessagePid(String hash) {
		return messages.get(hash);
	}
}
