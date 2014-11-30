package project;

import java.util.List;

import peerbase.HandlerInterface;
import peerbase.LoggerUtil;
import peerbase.Node;
import peerbase.PeerConnection;
import peerbase.PeerInfo;
import peerbase.PeerMessage;
import peerbase.RouterInterface;

public class MessageProcessNode extends Node {

  public Sender sender;
  public Receiver receiver;

  public MessageProcessNode(int maxPeers, PeerInfo info) {
    super(maxPeers, info);
    sender = new Sender(this);
    receiver = new Receiver(this);
    sender.start();
    this.addRouter(new Router(this));

    this.addHandler(MessageType.INSERTPEER, new JoinHandler(this));
    this.addHandler(MessageType.LISTPEER, new ListHandler(this));
    this.addHandler(MessageType.PEERNAME, new NameHandler(this));
    this.addHandler(MessageType.RECVMSG, new ReceiveHandler(this));
    // Update: use receive handler instand of receiver
    // this.addHandler(MessageType.RECVMSG, receiver);
    this.addHandler(MessageType.FETCHBUF, new FetchHandler(this));
    this.addHandler(MessageType.STOPPROC, new StopHandler(this));
    this.addHandler(MessageType.PEERQUIT, new QuitHandler(this));
  }

  public void buildPeers(String host, int port, int hops) {
    LoggerUtil.getLogger().fine("build peers");

    if (this.maxPeersReached() || hops <= 0)
      return;
    PeerInfo pd = new PeerInfo(host, port);
    List<PeerMessage> resplist = connectAndSend(pd, MessageType.PEERNAME, "", true);
    if (resplist == null || resplist.size() == 0)
      return;
    String peerid = resplist.get(0).getMsgData();
    LoggerUtil.getLogger().fine("contacted " + peerid);
    pd.setId(peerid);

    String resp =
        connectAndSend(pd, MessageType.INSERTPEER,
            String.format("%s %s %d", getId(), getHost(), getPort()), true).get(0).getMsgType();
    if (!resp.equals(MessageType.REPLY) || this.getPeerKeys().contains(peerid))
      return;

    this.addPeer(pd);

    // TODO: Fix the inconsistency of fetch buffer from other peer.
    /*
     * resplist = this.connectAndSend(pd, MessageType.FETCHBUF, "", true);
     * 
     * if (resplist.size() == 1) { PeerMessage pm = resplist.get(0);
     * receiver.setBuffer(MessageBlock.deserialize(pm.getMsgDataBytes())); }
     */

    // do recursive depth first search to add more peers
    resplist = this.connectAndSend(pd, MessageType.LISTPEER, "", true);

    if (resplist.size() > 1) {
      resplist.remove(0);
      for (PeerMessage pm : resplist) {
        String[] data = pm.getMsgData().split("\\s");
        String nextpid = data[0];
        String nexthost = data[1];
        int nextport = Integer.parseInt(data[2]);
        if (!nextpid.equals(this.getId()))
          buildPeers(nexthost, nextport, hops - 1);
      }
    }
  }

  /* msg syntax: JOIN pid host port */
  private class JoinHandler implements HandlerInterface {
    private Node peer;

    public JoinHandler(Node peer) {
      this.peer = peer;
    }

    public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
      if (peer.maxPeersReached()) {
        LoggerUtil.getLogger().fine("maxpeers reached " + peer.getMaxPeers());
        peerconn.sendData(new PeerMessage(MessageType.ERROR, "Join: " + "too many peers"));
        return;
      }

      // check for correct number of arguments
      String[] data = msg.getMsgData().split("\\s");
      if (data.length != 3) {
        peerconn.sendData(new PeerMessage(MessageType.ERROR, "Join: " + "incorrect arguments"));
        return;
      }

      // parse arguments into PeerInfo structure
      PeerInfo info = new PeerInfo(data[0], data[1], Integer.parseInt(data[2]));

      if (peer.getPeer(info.getId()) != null)
        peerconn.sendData(new PeerMessage(MessageType.ERROR, "Join: " + "peer already inserted"));
      else if (info.getId().equals(peer.getId()))
        peerconn.sendData(new PeerMessage(MessageType.ERROR, "Join: " + "attempt to insert self"));
      else {
        peer.addPeer(info);
        peerconn.sendData(new PeerMessage(MessageType.REPLY, "Join: " + "peer added: "
            + info.getId()));
      }
    }
  }

  /* msg syntax: LIST */
  private class ListHandler implements HandlerInterface {
    private Node peer;

    public ListHandler(Node peer) {
      this.peer = peer;
    }

    public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
      peerconn.sendData(new PeerMessage(MessageType.REPLY, String.format("%d",
          peer.getNumberOfPeers())));
      for (String pid : peer.getPeerKeys()) {
        peerconn.sendData(new PeerMessage(MessageType.REPLY, String.format("%s %s %d", pid, peer
            .getPeer(pid).getHost(), peer.getPeer(pid).getPort())));
      }
    }
  }

  /* msg syntax: NAME */
  private class NameHandler implements HandlerInterface {
    private Node peer;

    public NameHandler(Node peer) {
      this.peer = peer;
    }

    public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
      peerconn.sendData(new PeerMessage(MessageType.REPLY, peer.getId()));
    }
  }

  /* msg syntax: RECV msg */
  private class ReceiveHandler implements HandlerInterface {
    @SuppressWarnings("unused")
    private Node peer;

    public ReceiveHandler(Node peer) {
      this.peer = peer;
    }

    public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
      String[] datas = msg.getMsgData().split(" ");
      receiver.addMessage(datas);
    }

  }

  /* msg syntax: FECH */
  private class FetchHandler implements HandlerInterface {
    @SuppressWarnings("unused")
    private Node peer;

    public FetchHandler(Node peer) {
      this.peer = peer;
    }

    public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
      peerconn.sendData(new PeerMessage(MessageType.REPLY, receiver.getBuffer().serialize()));
      /*
       * for (byte[] msgToSend : buffer) { peerconn.sendData(new PeerMessage(MessageType.REPLY,
       * Arrays.toString(msgToSend))); }
       */
    }
  }

  /* msg syntax: STOP */
  private class StopHandler implements HandlerInterface {
    @SuppressWarnings("unused")
    private Node peer;

    public StopHandler(Node peer) {
      this.peer = peer;
    }

    public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
      receiver.setPow(Long.parseLong(msg.getMsgData()));
    }

  }

  /* msg syntax: QUIT pid */
  private class QuitHandler implements HandlerInterface {
    private Node peer;

    public QuitHandler(Node peer) {
      this.peer = peer;
    }

    public void handleMessage(PeerConnection peerconn, PeerMessage msg) {
      String pid = msg.getMsgData().trim();
      if (peer.getPeer(pid) == null) {
        peerconn.sendData(new PeerMessage(MessageType.ERROR, "Quit: peer not found: " + pid));
      } else {
        peer.removePeer(pid);
        peerconn.sendData(new PeerMessage(MessageType.REPLY, "Quit: peer removed: " + pid));
      }
    }
  }

  private class Router implements RouterInterface {
    private Node peer;

    public Router(Node peer) {
      this.peer = peer;
    }

    public PeerInfo route(String peerid) {
      if (peer.getPeerKeys().contains(peerid))
        return peer.getPeer(peerid);
      else
        return null;
    }
  }
}
