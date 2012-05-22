import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;

public class Server {
	
	protected static final int RECEIVE_SIZE = 1024;
	List<Socket> sockets = Collections
			.synchronizedList(new ArrayList<Socket>());
	List<PacketBucket> bucketList = Collections
			.synchronizedList(new ArrayList<PacketBucket>());;
	boolean running = true;
	/**
     * @return the running
     */
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * @param running the running to set
     */
    public synchronized void setRunning(boolean running) {
        this.running = running;
    }

    long packetCount;
	JLabel countLabel;
	int connectedCount;
	JLabel connectedLabel;
	public static String powerString = "default";
	public static String delayString = "0";
	public static void main(String[] args) throws IOException {
	    try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        } 
		new Server();
	}

	public Server() throws IOException {
		final ServerSocket ss = new ServerSocket(1234);
		final DatagramSocket udpSocket = new DatagramSocket(2345);
		constructUI();
		Thread clientAccepter = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Socket socket = ss.accept();
						sockets.add(socket);
						System.out.println("Accepted client " + socket.getInetAddress());
						for (PacketBucket bucket : bucketList) {
							if(socket.getInetAddress().hashCode() == bucket.hashCode()){
								System.out.println("Client already seen: " + socket.getInetAddress());
								return;
							}
						}
						bucketList.add(new PacketBucket(socket.getInetAddress()));
						connectedCount = bucketList.size();
						connectedLabel.setText(Integer.toString(connectedCount));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		Thread reader = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					if(isRunning()){
						try {
							DatagramPacket p = new DatagramPacket(new byte[RECEIVE_SIZE], RECEIVE_SIZE);
							udpSocket.receive(p);
							countLabel.setText(Long.toString(packetCount++));
							for (PacketBucket bucket : bucketList) {
								if(p.getAddress().hashCode() == bucket.hashCode()){
									bucket.addPacket(p);
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		reader.start();

		clientAccepter.start();
		System.out.println("Waiting for clients to connect");
	}
	
	private void constructUI(){
		// Make UI
		JFrame frame = new JFrame("Server");
		frame.setLayout(null);
        frame.setTitle("Server");
        frame.setSize(300, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JButton go = new JButton("Go!");
		go.setBounds(50, 50, 80, 25);
		frame.add(go);
		go.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				startUpload();
			}
		});
		
		JButton stop = new JButton("Stop!");
		stop.setBounds(150, 50, 80, 25);
		frame.add(stop);
		stop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stopUpload();
			}
		});
		
		JLabel delayLabel = new JLabel("Delay (ms):");
		delayLabel.setBounds(50, 0, 180, 25);
		frame.add(delayLabel);
		
		final JFormattedTextField delay = new JFormattedTextField(NumberFormat
                .getIntegerInstance());
		delay.setBounds(130, 20, 100, 25);
		frame.add(delay);
		
		JButton setDelay = new JButton("Set");
		setDelay.setBounds(50, 20, 70, 25);
		frame.add(setDelay);
		setDelay.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(delay.getText());
				setDelay(delay.getText());
			}
		});
		
		JLabel powerLabel = new JLabel("Power (dBm):");
		powerLabel.setBounds(50, 75, 180, 25);
		frame.add(powerLabel);
		
		final JFormattedTextField power = new JFormattedTextField(NumberFormat
                .getIntegerInstance());
		power.setBounds(130, 100, 100, 25);
		frame.add(power);
		
		JButton setPower = new JButton("Set");
		setPower.setBounds(50, 100, 70, 25);
		frame.add(setPower);
		setPower.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(power.getText());
				setPower(power.getText());
			}
		});
		
		JLabel devicesLabel = new JLabel("Devices seen:");
		devicesLabel.setBounds(50, 140, 100, 25);
		frame.add(devicesLabel);
		
		connectedLabel = new JLabel("0");
		connectedLabel.setBounds(150, 140, 70, 25);
		frame.add(connectedLabel);
		
		JLabel packetLabel = new JLabel("Packet count:");
		packetLabel.setBounds(50, 160, 100, 25);
		frame.add(packetLabel);
		
		countLabel = new JLabel("0");
		countLabel.setBounds(150, 160, 70, 25);
		frame.add(countLabel);
		
		JButton save = new JButton("Write results to file!");
		save.setBounds(50, 180, 180, 25);
		frame.add(save);
		save.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Saving buckets!!");
				saveBuckets();
			}
		});
		frame.setVisible(true);
	}

	protected void setPower(String text) {
		powerString = text;
		sendCommand("setPower|"+text);		
	}
	
	protected void setDelay(String newDelay) {
		sendCommand("setDelay|"+newDelay);		
		delayString = newDelay;
	}

	protected void saveBuckets() {
		String id = Long.toString(System.currentTimeMillis());
		for (PacketBucket bucket : bucketList){
			bucket.save(id);
		}		
	}

	protected void stopUpload(){
		System.out.println("Stopping upload.");
		setRunning(false);
		sendCommand("stop");

	}

	protected void startUpload(){
		setRunning(true);
		sendCommand("start");
	}
	
	private void sendCommand(String command){
		for (Socket s : sockets) {
			PrintWriter writer;
			try {
				writer = new PrintWriter(s.getOutputStream());
				writer.println(command);
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
