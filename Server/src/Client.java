import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
	// show ip, have button for connection, show status, switch to time based (sent from server), stop command, attenuate
	private static final int NUM_PACKETS = 1000;

	public static void main(String[] args) throws UnknownHostException,
			IOException {
		Socket s = new Socket("localhost", 1234);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				s.getInputStream()));

		while (true) {
			String read = reader.readLine();
			if (read.startsWith("upload")) {
				System.out.println("Beginning upload");
				
				beginUpload();
			}
			
			if (read.startsWith("stop")) {
				System.out.println("Closing socket");
				
				s.close();
				return;
			}
		}
	}

	private static void beginUpload() throws SocketException {

		DatagramSocket toServer = new DatagramSocket();

		
		for (int i = 0; i < NUM_PACKETS; i++) {
			try {
				byte[] data = "\nssid:poopie\nsignal:not good\n430958430983450".getBytes();
				DatagramPacket packet = new DatagramPacket(data, data.length);
				packet.setSocketAddress(new InetSocketAddress("localhost", 2345));
				Thread.sleep(1);
				toServer.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
