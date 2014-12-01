package project;

import java.util.List;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.math3.distribution.NormalDistribution;

import peerbase.PeerInfo;

public class Sender extends Thread {
  public static final int DEFAULT_MESSAGE_LENGTH = 80;
  public static final long DEFAULT_LOWER_BOUND = 1000;
  public static final long DEFAULT_UPPER_BOUND = 2000;
  private boolean sendFlag = false;
  private int messageLength;
  private long lower;
  private long upper;
  private List<Double> lossRates;
  private List<Double> delays;
  private ParameterGenerator pg;
  private int counter; // count how many messages have been sent
  private MessageProcessNode peer;

  private long waitTime;
  private long[] delayTime;
  private boolean[] loss;

  private PeerSender[] peerSender;

  public Sender(MessageProcessNode peer) {
    this(peer, DEFAULT_MESSAGE_LENGTH, DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND);
  }

  public void setPG(ParameterGenerator p) {
    pg = p;
    lossRates = pg.lossRateGenerator2(peer.getMaxPeers());
    delays = pg.delayGenerator2(peer.getMaxPeers());
  }

  public Sender(MessageProcessNode peer, int messageLength, long lower, long upper) {
    this.peer = peer;
    this.messageLength = messageLength;
    this.lower = lower;
    this.upper = upper;
    this.counter = 0;

    waitTime = 0;
    delayTime = new long[peer.getMaxPeers()];
    loss = new boolean[peer.getMaxPeers()];
  }

  /*
   * This method must be called after all the peers joined in.
   */
  public void initPeerSender() {
    peerSender = new PeerSender[peer.getMaxPeers()];
    int i = 0;
    for (String info : peer.getPeerKeys()) {
      peerSender[i] = new PeerSender(peer.getPeer(info), i);
      peerSender[i].start();
      ++i;
    }
  }

  private byte[] generateRandomMessage() {
    byte[] message = new byte[this.messageLength];
    new Random().nextBytes(message);
    return message;
  }

  public void run() {
    while (true) {
      System.out.print(""); // make compiler happy
      if (!this.sendFlag)
        continue;
      // long waitTime = new Random().nextInt((int) (upper - lower)) + lower;
      ++counter;
      waitTime = (long) (pg.nextWaitTime());
      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      byte[] message = this.generateRandomMessage();
      broadcast(message);
      // outputAllStatus();
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

  private class PeerSender extends Thread {
    private int id;
    private PeerInfo info;
    private boolean sendMessage;
    private String msg;

    public PeerSender(PeerInfo pi, int i) {
      this.info = pi;
      id = i;
      sendMessage = false;
    }

    public void send(String strMsg) {
      msg = strMsg;
      sendMessage = true;
    }

    public void run() {
      while (true) {
        if (sendMessage) {
          sendMessage = false;
          try {
            NormalDistribution nd = new NormalDistribution(delays.get(id), delays.get(id) / 5.0);
            delayTime[id] = (long) nd.sample();
            while (delayTime[id] < 0)
              delayTime[id] = (long) nd.sample();
            Thread.sleep(delayTime[id]);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          peer.connectAndSend(info, MessageType.RECVMSG, String.format("%s %s", peer.getId(), msg),
              false);
        } else {
          System.out.print("");
        }
      }
    }
  }

  /**
   * This method will broadcast message to all the other peers it connects.
   * 
   * @param message
   */
  public void broadcast(byte[] message) {
    String strMsg =
        peer.getPort() + " " + counter + " " + DatatypeConverter.printBase64Binary(message);
    // System.out.println(strMsg);
    Random r = new Random(System.currentTimeMillis());
    for (int i = 0; i < peer.getMaxPeers(); ++i) {
      if (r.nextDouble() >= lossRates.get(i)) {
        loss[i] = false;
        peerSender[i].send(strMsg);
      } else {
        loss[i] = true;
      }
    }
    PeerInfo info = new PeerInfo(peer.getHost(), peer.getPort());
    peer.connectAndSend(info, MessageType.RECVMSG, String.format("%s %s", getId(), strMsg), false);
  }
}
