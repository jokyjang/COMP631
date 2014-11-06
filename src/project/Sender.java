package project;

import java.util.Random;

public class Sender extends Thread{
	private static final int DEFAULT_MESSAGE_LENGTH = 80;
	private static final int DEFAULT_LOWER_BOUND = 1000;
	private static final int DEFAULT_UPPER_BOUND = 2000;
	private boolean sendFlag = false;
	private int messageLength;
	private long lower;
	private long upper;
	
	private MessageProcessNode peer;
	
	public Sender(MessageProcessNode p) {
		this(p, DEFAULT_MESSAGE_LENGTH, DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND);
	}
	
	public Sender(MessageProcessNode p, int ml, long l, long u) {
		this.peer = p;
		this.messageLength = ml;
		this.lower = l;
		this.upper = u;
	}
	
	private byte[] generateRandomMessage() {
		byte[] message = new byte[this.messageLength];
		new Random().nextBytes(message);
		return message;
	}

	public void run() {
		while(true) {
			if(!this.sendFlag) continue;
			long waitTime = new Random().nextInt((int)(upper-lower)) + lower;
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				byte[] message = this.generateRandomMessage();
				peer.broadCast(message);
			}
		}
	}
	
	public void startSending() {
		this.sendFlag = true;
	}
	
	public void stopSending() {
		this.sendFlag = false;
	}
	
	public int getMessageLength() {
		return messageLength;
	}

	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}

	public long getLower() {
		return lower;
	}

	public void setLower(long lower) {
		this.lower = lower;
	}

	public long getUpper() {
		return upper;
	}

	public void setUpper(long upper) {
		this.upper = upper;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
