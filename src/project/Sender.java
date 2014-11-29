package project;

import java.util.List;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

import peerbase.PeerInfo;

public class Sender extends Thread {
  public static final int DEFAULT_MESSAGE_LENGTH = 80;
  public static final long DEFAULT_LOWER_BOUND = 1000;
  public static final long DEFAULT_UPPER_BOUND = 2000;
  public static final double DEFAULT_LOSS_RATE = 0;
  private boolean sendFlag = false;
  private int messageLength;
  private long lower;
  private long upper;
  private double lossRate;
  private List<Double> lossRates;
  private List<Double> delays;
  private ParameterGenerator pg;

  private MessageProcessNode peer;

  public Sender(MessageProcessNode peer) {
    this(peer, DEFAULT_MESSAGE_LENGTH, DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, DEFAULT_LOSS_RATE);
  }
  
  public void setPG(ParameterGenerator p) {
	  pg = p;
	  lossRates = pg.lossRatesGenerator(peer.getMaxPeers());
	  delays = pg.delayGenerator(peer.getMaxPeers());
  }

  public Sender(MessageProcessNode peer, int messageLength, long lower, long upper, double lossRate) {
    this.peer = peer;
    this.messageLength = messageLength;
    this.lower = lower;
    this.upper = upper;
    this.lossRate = lossRate;
  }

  private byte[] generateRandomMessage() {
    byte[] message = new byte[this.messageLength];
    new Random().nextBytes(message);
    return message;
  }

  public void run() {
    while (true) {
      System.out.print("");
      if (!this.sendFlag)
        continue;
      //long waitTime = new Random().nextInt((int) (upper - lower)) + lower;
      long waitTime = (long)(pg.nextWaitTime()*1000);
      System.out.println("waiting time is: " + waitTime + "ms");
      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      byte[] message = this.generateRandomMessage();
      broadcast(message);
    }
  }

  public void startSending() {
    this.sendFlag = true;
  }

  public void stopSending() {
    this.sendFlag = false;
  }
  
  public boolean isSending() {
	  return this.sendFlag;
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

  public double getLossRate() {
    return lossRate;
  }

  public void setLossRate(double lossRate) {
    this.lossRate = lossRate;
  }

  /**
   * This method will broadcast message to all the other peers it connects.
   * 
   * @param message
   */
  public void broadcast(byte[] message) {
    String strMsg = DatatypeConverter.printBase64Binary(message);
    Random r = new Random();
    int i = 0;
    for (String pid : peer.getPeerKeys()) {
      PeerInfo info = peer.getPeer(pid);
      if (r.nextDouble() > lossRates.get(i)) {
    	  System.out.println("Message not loss");
    	try {
    		//long sleepTime = (long)(delays.get(i) * 1000 + Math.abs(r.nextGaussian()));
    		long sleepTime = (long)(delays.get(i) * 1000);
    		System.out.println("Message delayed for " + sleepTime + "ms");
    		Thread.sleep(sleepTime);
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
        peer.connectAndSend(info, MessageType.RECVMSG,
            String.format("%s %s", peer.getId(), strMsg), false);
      } else {
    	  System.out.println("Message lost!");
      }
      ++i;
    }
    PeerInfo info = new PeerInfo(peer.getHost(), peer.getPort());
    peer.connectAndSend(info, MessageType.RECVMSG, String.format("%s %s", getId(), strMsg), false);
  }
}
