package project;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import peerbase.PeerInfo;
import peerbase.util.SimplePingStabilizer;

@SuppressWarnings("serial")
public class DemoApp extends JFrame {

  private static final int FRAME_WIDTH = 665, FRAME_HEIGHT = 265;

  private JPanel messagePanel, peersPanel;
  private JPanel lowerFilesPanel, lowerPeersPanel;
  private DefaultListModel messageModel, peersModel;
  private JList messageList, peersList;


  private JButton startButton, lowerBoundButton, messageSizeButton;
  private JButton removePeersButton, refreshPeersButton, upperBoundButton;

  private JTextField lowerBoundTextField, messageSizeTextField;
  private JTextField upperBoundTextField;

  private MessageProcessNode peer;

  public DemoApp(String initialhost, int initialport, int maxpeers, PeerInfo mypd) {
    peer = new MessageProcessNode(maxpeers, mypd);
    peer.buildPeers(initialhost, initialport, 2);

    startButton = new JButton("Start");
    startButton.addActionListener(new StartListener());
    lowerBoundButton = new JButton("Lower Bound");
    lowerBoundButton.addActionListener(new LowerBoundListener());
    messageSizeButton = new JButton("Message Size");
    messageSizeButton.addActionListener(new MessageSizeListener());
    removePeersButton = new JButton("Remove");
    removePeersButton.addActionListener(new RemoveListener());
    refreshPeersButton = new JButton("Refresh");
    refreshPeersButton.addActionListener(new RefreshListener());
    upperBoundButton = new JButton("Upper Bound");
    upperBoundButton.addActionListener(new UpperBoundListener());

    lowerBoundTextField = new JTextField(15);
    messageSizeTextField = new JTextField(15);
    upperBoundTextField = new JTextField(15);

    setupFrame(this);

    (new Thread() {
      public void run() {
        peer.mainLoop();
      }
    }).start();

    /*
     * Swing is not threadsafe, so can't update GUI component from a thread other than the event
     * thread
     */
    /*
     * (new Thread() { public void run() { while (true) {
     * 
     * new RefreshListener().actionPerformed(null); try { Thread.sleep(1000); } catch
     * (InterruptedException e) { } } }}).start();
     */
    new javax.swing.Timer(3000, new RefreshListener()).start();

    peer.startStabilizer(new SimplePingStabilizer(peer), 3000);
  }


  private void setupFrame(JFrame frame) {
    /*
     * fixes the overlapping problem by using a BorderLayout on the whole frame and GridLayouts on
     * the upper/lower panels
     */

    frame = new JFrame("Demo App ID: <" + peer.getId() + ">");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.setLayout(new BorderLayout());

    JPanel upperPanel = new JPanel();
    JPanel lowerPanel = new JPanel();
    upperPanel.setLayout(new GridLayout(1, 2));
    // allots the upper panel 2/3 of the frame height
    upperPanel.setPreferredSize(new Dimension(FRAME_WIDTH, (FRAME_HEIGHT * 2 / 3)));
    lowerPanel.setLayout(new GridLayout(1, 2));


    frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);

    messageModel = new DefaultListModel();
    messageList = new JList(messageModel);
    peersModel = new DefaultListModel();
    peersList = new JList(peersModel);
    messagePanel = initPanel(new JLabel("Message List"), messageList);
    peersPanel = initPanel(new JLabel("Peer List"), peersList);
    lowerFilesPanel = new JPanel();
    lowerPeersPanel = new JPanel();

    messagePanel.add(startButton);
    peersPanel.add(removePeersButton);
    peersPanel.add(refreshPeersButton);

    lowerFilesPanel.add(lowerBoundTextField);
    lowerFilesPanel.add(lowerBoundButton);
    lowerFilesPanel.add(messageSizeTextField);
    lowerFilesPanel.add(messageSizeButton);

    lowerPeersPanel.add(upperBoundTextField);
    lowerPeersPanel.add(upperBoundButton);

    upperPanel.add(messagePanel);
    upperPanel.add(peersPanel);
    lowerPanel.add(lowerFilesPanel);
    lowerPanel.add(lowerPeersPanel);

    /*
     * by using a CENTER BorderLayout, the overlapping problem is fixed:
     * http://forum.java.sun.com/thread.jspa?threadID=551544&messageID=2698227
     */

    frame.add(upperPanel, BorderLayout.NORTH);
    frame.add(lowerPanel, BorderLayout.CENTER);

    frame.setVisible(true);

  }


  private JPanel initPanel(JLabel textField, JList list) {
    JPanel panel = new JPanel();
    panel.add(textField);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setPreferredSize(new Dimension(200, 105));
    panel.add(scrollPane);
    return panel;
  }

  private void updateMessageList() {
    messageModel.removeAllElements();
    for (int i = 0; i < peer.receiver.getBufferSize(); ++i) {
      messageModel.addElement(peer.receiver.getMessageAt(i));
    }
  }


  private void updatePeerList() {
    peersModel.removeAllElements();
    for (String pid : peer.getPeerKeys()) {
      peersModel.addElement(pid);
    }
  }

  class StartListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (startButton.getText().equalsIgnoreCase("Start")) {
        peer.sender.startSending();
        startButton.setText("Stop");
      } else if (startButton.getText().equalsIgnoreCase("Stop")) {
        peer.sender.stopSending();
        startButton.setText("Start");
      }
    }
  }

  class LowerBoundListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String lower = lowerBoundTextField.getText().trim();
      long lowerBound = Sender.DEFAULT_LOWER_BOUND;
      try {
        lowerBound = Long.parseLong(lower);
      } catch (NumberFormatException e1) {
      }
      peer.sender.setLower(lowerBound);
    }
  }

  class MessageSizeListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String sizeStr = messageSizeTextField.getText().trim();
      long size = Sender.DEFAULT_MESSAGE_LENGTH;
      try {
        size = Long.parseLong(sizeStr);
      } catch (NumberFormatException e1) {
      }
      peer.sender.setLower(size);
    }
  }

  class RemoveListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (peersList.getSelectedValue() != null) {
        String pid = peersList.getSelectedValue().toString();
        peer.sendToPeer(pid, MessageType.PEERQUIT, peer.getId(), true);
        peer.removePeer(pid);
      }
    }
  }

  class RefreshListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      updateMessageList();
      updatePeerList();
    }
  }

  class UpperBoundListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String upper = upperBoundTextField.getText().trim();
      long upperBound = Sender.DEFAULT_UPPER_BOUND;
      try {
        upperBound = Long.parseLong(upper);
      } catch (NumberFormatException e1) {
      }
      peer.sender.setUpper(upperBound);
    }
  }
}
