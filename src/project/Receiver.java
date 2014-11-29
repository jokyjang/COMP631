package project;

import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

import javax.xml.bind.DatatypeConverter;

import peerbase.PeerInfo;

public class Receiver {

  class Miner extends Thread {
    private boolean stop = true;
    private boolean finish = false;
    private final int DEFAULT_CONSTRAINT = 24;
    private int cons = DEFAULT_CONSTRAINT;

    /**
     * This method will check if the first few bits of `hash' are 0. As for the number of bits, it
     * depends on how difficult the mining function is.
     * 
     * @param hash
     * @return
     */
    private boolean isValid(String hashValue) {
      int i = 0;
      byte[] hash = DatatypeConverter.parseBase64Binary(hashValue);
      int constraint = cons;
      while (i < hash.length && constraint >= Byte.SIZE) {
        if (hash[i] != 0)
          return false;
        constraint -= Byte.SIZE;
        ++i;
      }
      if (i == hash.length)
        return true;
      return (hash[i] & 0xFF) >> (Byte.SIZE - constraint) == 0;
    }

    private Long mine() {
      System.out.println("Miner starts mining!");
      // stop = false;
      Random random = new Random();
      String hash = null;
      do {
        curr.setPow(random.nextLong());
        hash = generateHash(curr.serialize());
      } while (isMining() && !isValid(hash));

      System.out.println("miner stops mining! stop = " + stop);
      if (!isMining())
        return null;
      stopMining();
      System.out.println("I've mined the hash: " + hash);
      return curr.getPow();
    }

    public void stopMining() {
      stop = true;
    }

    public void finishMining() {
      finish = true;
    }

    public void startMining() {
      stop = false;
    }

    public boolean finished() {
      return finish;
    }

    public void run() {
      while (true) {
        while (!isMining() && !finished()) {
          System.out.print("");
        }
        if (finished())
          break;
        Long pow = mine();
        if (pow != null) {
          notifyAllToStopMining(pow);
        }
      }
    }

    public boolean isMining() {
      return !stop;
    }
  }

  static final int DEFAULT_START_PROCESSING_SIZE = 20;
  static final double DEFAULT_LOSS_RATE = 0;

  private MessageProcessNode peer;
  private MessageBlock buffer;
  private MessageBlock curr;
  private Stack<MessageBlock> blockChain;
  private boolean init = true;
  private int startProcessingSize;
  private Miner miner;
  private PrintWriter writer;
  private double lossRate;

  public Receiver(MessageProcessNode peer) {
    this(peer, DEFAULT_START_PROCESSING_SIZE, DEFAULT_LOSS_RATE);
  }

  public Receiver(MessageProcessNode peer, int startSize, double lossRate) {
    this.peer = peer;
    buffer = new MessageBlock();
    this.startProcessingSize = startSize;
    this.lossRate = lossRate;
    blockChain = new Stack<MessageBlock>();
    miner = new Miner();
    miner.start();
  }
  
  public Miner getMiner() {
	  return miner;
  }
  
  public void setWriter(PrintWriter pw) {
	  this.writer = pw;
  }

  public MessageBlock getLastMessageBlock() {
    return blockChain.isEmpty() ? null : blockChain.peek();
  }

  /**
   * Let the current message block to be the buffer and start process it.
   * 
   * @param prevHash
   */
  public void processMessage() {
    // MessageBlock lastMB = getLastMessageBlock();
    // String prevHash = (lastMB == null) ? "" : generateHash(lastMB.serialize());
    // curr.setPrevHash(prevHash);
    // System.out.println("process message!");
    miner.startMining();
  }

  /**
   * Using the given byte array to generate the hash.
   * 
   * @param data
   * @return
   */
  public static String generateHash(byte[] data) {
    String hash = null;
    try {
      MessageDigest mDigest = MessageDigest.getInstance("SHA1");
      hash = DatatypeConverter.printBase64Binary(mDigest.digest(data));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return hash;
  }

  /**
   * Add the message to the buffer, if the size of the message block is greater than the start
   * processing size for the first time, then it start to process the message block.
   * 
   * @param msg
   */
  public void addMessage(String msg) {
    byte[] msgByte = DatatypeConverter.parseBase64Binary(msg);
    if (new Random().nextDouble() > lossRate) {
      buffer.addMessage(msgByte);
    }
    if (init && buffer.getMessages().size() >= startProcessingSize) {
      init = false;
      curr = buffer;
      buffer = new MessageBlock();
      processMessage();
    }
  }

  public void notifyAllToStopMining(Long pow) {
    System.out.println("notify all to stop mining!");
    for (String key : peer.getPeerKeys()) {
      PeerInfo pd = peer.getPeer(key);
      peer.connectAndSend(pd, MessageType.STOPPROC, pow.toString(), false);
    }
    setPow(pow);
  }

  /**
   * Set the pow of current processing block
   * 
   * @param pow
   */
  public void setPow(Long pow) {
    if (miner.isMining())
      miner.stopMining();
    curr.setPow(pow);
    this.addMessageBlock(curr);
    if(buffer.getMessages().isEmpty()) {
    	init = true;
    	curr = null;
    	return;
    }
    curr = buffer;
    buffer = new MessageBlock();
    processMessage();
  }

  public void setBuffer(MessageBlock buffer) {
    this.buffer = buffer;
  }

  public int getBufferSize() {
    return buffer.getMessages().size();
  }

  public String getMessageAt(int i) {
    return Arrays.toString(buffer.getMessages().get(i));
  }

  public int getBlockChainSize() {
    return blockChain.size();
  }

  private void addMessageBlock(MessageBlock mb) {
    blockChain.push(mb);
    for (byte[] b : mb.getMessages()) {
      writer.println(blockChain.size() + "\t" + DatatypeConverter.printBase64Binary(b));
    }
    writer.println(blockChain.size() + "\t" + mb.getPow());
    writer.flush();
    System.out.println("New Message Block: " + Receiver.generateHash(mb.serialize()));
  }

  public MessageBlock getMessageBlockAt(int i) {
    return blockChain.get(i);
  }

  public MessageBlock getBuffer() {
    return buffer;
  }

  public double getLossRate() {
    return lossRate;
  }

  public void setLossRate(double lossRate) {
    this.lossRate = lossRate;
  }
  
  public void setConstraint(int constraint) {
    miner.cons = constraint;
  }
}
