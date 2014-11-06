package project;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import peerbase.HandlerInterface;
import peerbase.PeerConnection;
import peerbase.PeerMessage;

public class Receiver implements HandlerInterface {
	/* msg syntax: RECV msg */
	@SuppressWarnings("unused")
	private MessageProcessNode peer;
	private List<String> messages;

	public Receiver(MessageProcessNode peer) {
		this.peer = peer;
		messages = new ArrayList<String>();
	}

	public String generateHash(byte[] data) {
		String hash = null;
		try {
			MessageDigest mDigest = MessageDigest.getInstance("SHA1");
			hash = DatatypeConverter.printBase64Binary(mDigest.digest(data));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return hash;
	}

	public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
		// store the msg into buffer
		msg.getMsgType();
		String[] datas = msg.getMsgData().split(" ");
		byte[] data = DatatypeConverter.parseBase64Binary(datas[1]);
		String hash = this.generateHash(data);
		messages.add(hash + ":" + datas[0]);
	}

	public int getMessageSize() {
		return messages.size();
	}

	public String getMessageAt(int i) {
		return messages.get(i);
	}
}
