package project;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import peerbase.PeerInfo;

// public class DemoApp extends JFrame {
public class DemoApp {
  private MessageProcessNode peer;

  public DemoApp(String initialhost, int initialport, int maxpeers, PeerInfo mypd) {
    peer = new MessageProcessNode(maxpeers, mypd);
    peer.buildPeers(initialhost, initialport, 2);

    (new Thread() {
      public void run() {
        peer.mainLoop();
      }
    }).start();


    (new Thread() {
      public void run() {
        waitingForAllPeers();
        mainLoop();
      }
    }).start();

  }

  private void waitingForAllPeers() {
    System.out.println("waiting for all the peers!");
    while (this.peer.getNumberOfPeers() < this.peer.getMaxPeers()) {
      System.out.print("");
    }
    System.out.println("all the peers come in");
  }

  private ParameterGenerator pg;

  private void mainLoop() {
    peer.sender.initPeerSender();
    final double[][] parameters =
        { {10, 800, 0.0, 21}, {200, 2000, 0.05, 22}, {500, 5000, 0.1, 23}};
    int counter = 14;
    for (int i = 0; i < 3; ++i) {
      for (int j = 1; j < 3; ++j) {
        for (int k = 1; k < 3; ++k) {
          for (int l = 0; l < 3; ++l) {
            System.out.println("round " + counter);
            pg =
                new ParameterGenerator(parameters[i][0], parameters[j][1], parameters[k][2],
                    (int) parameters[l][3]);
            PrintWriter writer = null;
            try {
              writer = new PrintWriter("./data/" + peer.getId() + "_" + i + j + k + l);
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            }
            this.peer.receiver.setWriter(writer);
            runOneLoop(pg);
            writer.close();
            ++counter;
          }
        }
      }
    }
  }

  private void runOneLoop(ParameterGenerator pg) {
    double sendInterval = pg.freq;
    final int SEND_COUNT = 1000;
    long timeToLive = (long) (sendInterval * SEND_COUNT);
    long startTime = System.currentTimeMillis();
    this.peer.receiver.reset();
    this.peer.sender.setPG(pg);
    this.peer.receiver.setConstraint(pg.constraint);
    this.peer.sender.startSending();
    while (System.currentTimeMillis() < startTime + timeToLive) {
      System.out.print("");
    }
    this.peer.sender.stopSending();
    System.out.println("waiting for miners to stop mining!");
    while (this.peer.receiver.getMiner().isMining()) {
      System.out.print("");
    }
    System.out.println("miners stopped mining!");
  }
}
