package project;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

  /*
   * 
   */
  public static MessageBlock deserialize(byte[] data) {
	ByteBuffer buffer = ByteBuffer.wrap(data);
	MessageBlock mb = new MessageBlock();
	int length = buffer.getInt();
	byte[] prevHashByte = new byte[length];
	buffer.get(prevHashByte);
	mb.setPrevHash(new String(prevHashByte));
	length = buffer.getInt();
	for(int i = 0; i < length; ++i) {
		int l = buffer.getInt();
		byte[] message = new byte[l];
		buffer.get(message);
		mb.addMessage(message);
	}
	mb.setPow(buffer.getLong());
    return mb;
  }
  
  /**
   * Just for test
   * @return
   */
  public static MessageBlock generateRandom() {
	MessageBlock mb = new MessageBlock();
	mb.setPrevHash("ImPrevHash");
	int length = new Random().nextInt(20);
	for(int i = 0; i < length; ++i) {
		byte[] message = new byte[10];
		new Random().nextBytes(message);
		mb.addMessage(message);
	}
	mb.setPow(new Random().nextLong());
	return mb;
  }
  
  public boolean equals(MessageBlock mb) {
	  if(mb == null) return false;
	  if(!this.prevHash.equals(mb.getPrevHash())) return false;
	  if(this.messages.size() != mb.getMessages().size()) return false;
	  for(int i = 0; i < messages.size(); ++i) {
		  if(!Arrays.equals(messages.get(i), mb.getMessages().get(i))) return false;
	  }
	  return (long)pow == (long)mb.getPow();
  }
  
  public static void main(String[] args) {
	
	MessageBlock mb = MessageBlock.generateRandom();
	byte[] bytes = mb.serialize();
	MessageBlock newMB = MessageBlock.deserialize(bytes);
	System.out.println(mb.equals(newMB));
	
	mb = new MessageBlock();
	mb.setPrevHash("shabe");
	for(int i = 0 ; i < 5; ++i) {
		mb.addMessage("nishishabe".getBytes());
	}
	mb.setPow(0x432943L);
	bytes = mb.serialize();
	
	MessageBlock mb2 = MessageBlock.deserialize(bytes);
	System.out.println(mb.getPrevHash());
	System.out.println(mb2.getPrevHash());
	System.out.println(mb.equals(mb2));
  }
}
