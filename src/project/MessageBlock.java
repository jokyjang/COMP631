package project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MessageBlock implements Serializable {
  private String prevHash;
  private List<byte[]> messages;
  private Long pow;

  public MessageBlock() {
    prevHash = null;
    messages = new ArrayList<byte[]>();
    pow = 0L;
  }

  public void addMessage(byte[] msg) {
    messages.add(msg);
  }

  public List<byte[]> getMessages() {
    return messages;
  }

  public void setPow(Long pow) {
    this.pow = pow;
  }

  public Long getPow() {
    return pow;
  }

  public void setPrevHash(String prevHash) {
    this.prevHash = prevHash;
  }

  public String getPrevHash() {
    return prevHash;
  }

  // TODO : Implements this method
  public byte[] serialize() {
    return null;
  }

  // TODO : Implements this method
  public static MessageBlock deserialize(byte[] data) {
    return null;
  }
}
