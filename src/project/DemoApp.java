/*
	File: FileShareApp.java
	Copyright 2007 by Nadeem Abdul Hamid, Patrick Valencia

	Permission to use, copy, modify, and distribute this software and its
	documentation for any purpose and without fee is hereby granted, provided
	that the above copyright notice appear in all copies and that both the
	copyright notice and this permission notice and warranty disclaimer appear
	in supporting documentation, and that the names of the authors or their
	employers not be used in advertising or publicity pertaining to distri-
	bution of the software without specific, written prior permission.

	The authors and their employers disclaim all warranties with regard to
	this software, including all implied warranties of merchantability and
	fitness. In no event shall the authors or their employers be liable for 
	any special, indirect or consequential damages or any damages whatsoever 
	resulting from loss of use, data or profits, whether in an action of 
	contract, negligence or other tortious action, arising out of or in 
	connection with the use or performance of this software, even if 
	advised of the possibility of such damage.

	Date		Author				Changes
	Feb 07 2007	Nadeem Abdul Hamid	Add to project (from source by P. Valencia)
 */


package project;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.logging.Level;

import javax.swing.*;

import peerbase.*;
import peerbase.util.SimplePingStabilizer;


/**
 * The GUI for a simple peer-to-peer file sharing
 * application. 
 * 
 * @author Nadeem Abdul Hamid
 */
@SuppressWarnings("serial")
public class DemoApp extends JFrame
{

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

	public DemoApp(String initialhost, int initialport, int maxpeers, PeerInfo mypd)
	{
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

		(new Thread() { public void run() { peer.mainLoop(); }}).start();

		/*
		  Swing is not threadsafe, so can't update GUI component
		  from a thread other than the event thread
		 */
		/*
		(new Thread() { public void run() { 
			while (true) {

				new RefreshListener().actionPerformed(null);
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
			}
		}}).start();
		 */
		new javax.swing.Timer(3000, new RefreshListener()).start();

		peer.startStabilizer(new SimplePingStabilizer(peer), 3000);
	}

	
	private void setupFrame(JFrame frame)
	{
		/* fixes the overlapping problem by using
		   a BorderLayout on the whole frame
		   and GridLayouts on the upper/lower panels*/

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

		/* by using a CENTER BorderLayout, the 
		   overlapping problem is fixed:
		   http://forum.java.sun.com/thread.jspa?threadID=551544&messageID=2698227 */

		frame.add(upperPanel, BorderLayout.NORTH);
		frame.add(lowerPanel, BorderLayout.CENTER);

		frame.setVisible(true);

	}

	
	private JPanel initPanel(JLabel textField,
			JList list)
	{
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
		for (int i = 0; i < peer.receiver.getMessageSize(); ++i) {
			messageModel.addElement(peer.receiver.getMessageAt(i));
		}
	}


	private void updatePeerList(){
		peersModel.removeAllElements();
		for (String pid : peer.getPeerKeys()) {
			peersModel.addElement(pid);
		}
	}

	class StartListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if(startButton.getText().equalsIgnoreCase("Start")) {
				peer.sender.startSending();
				startButton.setText("Stop");
			} else if(startButton.getText().equalsIgnoreCase("Stop")) {
				peer.sender.stopSending();
				startButton.setText("Start");
			}
		}
	}

	class LowerBoundListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String lower = lowerBoundTextField.getText().trim();
			long lowerBound = 0;
			try {
				lowerBound = Long.parseLong(lower);
			} catch (NumberFormatException e1) {
				lowerBound = 0;
			}
			peer.sender.setLower(lowerBound);
		}
	}

	class MessageSizeListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
		}
	}

	class RemoveListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (peersList.getSelectedValue() != null) {
				String pid = peersList.getSelectedValue().toString();
				peer.sendToPeer(pid, MessageProcessNode.PEERQUIT, peer.getId(), true);
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
			
		}
	}
}
