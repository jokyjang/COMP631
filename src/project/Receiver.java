package project;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

public class Receiver {

  static final int DEFAULT_START_PROCESSING_SIZE = 20;

  @SuppressWarnings("unused")
  private MessageProcessNode peer;
  private MessageBlock buffer;
  private MessageBlock curr;
  private List<MessageBlock> blockChain;
  private boolean init = true;
  private int startProcessingSize;

  public Receiver(MessageProcessNode peer) {
    this(peer, DEFAULT_START_PROCESSING_SIZE);
  }

  public Receiver(MessageProcessNode peer, int startSize) {
    this.peer = peer;
    buffer = new MessageBlock();
    this.startProcessingSize = startSize;
    blockChain = new ArrayList<MessageBlock>();
  }

  /**
   * Using the given byte array to generate the hash.
   * 
   * @param data
   * @return
   */
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

  /**
   * Let the current message block to be the buffer and start process it.
   * 
   * @param prevHash
   */
  public void processMessage(String prevHash) {
    curr = buffer;
    curr.setPrevHash(prevHash);
    buffer = new MessageBlock();
    // TODO: implement the process message part

  }

  /**
   * Add the message to the buffer, if the size of the message block is greater than the start
   * processing size for the first time, then it start to process the message block.
   * 
   * @param msg
   */
  public void addMessage(String msg) {
    byte[] msgByte = DatatypeConverter.parseBase64Binary(msg);
    buffer.addMessage(msgByte);
    if (init && buffer.getMessages().size() > startProcessingSize) {
      processMessage(null);
      curr = buffer;
      buffer = new MessageBlock();
      init = false;
    }
  }

  /**
   * Set the pow of current processing block
   * 
   * @param pow
   */
  public void setPow(Long pow) {
    curr.setPow(pow);
    processMessage(generateHash(curr.serialize()));
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

  public MessageBlock getMessageBlockAt(int i) {
    return blockChain.get(i);
  }
  
  public MessageBlock getBuffer() {
    return buffer;
  }
}
