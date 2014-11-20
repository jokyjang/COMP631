package project;

import java.io.Serializable;
import java.nio.ByteBuffer;
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
	int length = Integer.SIZE / Byte.SIZE + prevHash.length();
	length += Integer.SIZE / Byte.SIZE;
	if(!messages.isEmpty()) {
		for(byte[] message : messages) {
			length += Integer.SIZE / Byte.SIZE;
			length += message.length;
		}
	}
	length += Long.SIZE / Byte.SIZE;
	ByteBuffer buffer = ByteBuffer.allocate(length);
	
	buffer.putInt(prevHash.length());
	buffer.put(prevHash.getBytes());
	buffer.putInt(messages.size());
	for(byte[] message : messages) {
		buffer.putInt(message.length);
		buffer.put(message);
	}
	buffer.putLong(pow);
	
    return buffer.array();
  }

  // TODO : Implements this method
  public static MessageBlock deserialize(byte[] data) {
	ByteBuffer buffer = ByteBuffer.wrap(data);
	MessageBlock mb = new MessageBlock();
	int length = buffer.getInt();
	byte[] prevHashByte = new byte[length];
	buffer.get(prevHashByte);
	mb.setPrevHash(prevHashByte.toString());
	length = buffer.getInt();
	for(int i = 0; i < length; ++i) {
		int l = buffer.getInt();
	}
    return null;
  }
}
