import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class PacketBucket {
	InetAddress address;
	List<DatagramPacket> bucket = new ArrayList<DatagramPacket>();
	public PacketBucket(InetAddress inetAddress) {
		this.address = inetAddress;
	}

	public int hashCode() {
		return address.hashCode();
	}

	public void addPacket(DatagramPacket p) {
//		System.out.println("Received from " + p.getAddress().toString());
		bucket.add(p);
	}

	public void save(String id) {
		String header = getHeader(bucket.get(0));
		
		File dir = createFolder(Server.powerString+"-"+Server.delayString+File.separator+id);
		Writer output = null;
		System.out.println("Saving; "+dir.getPath()+File.separator+bucket.get(0).getAddress().hashCode() + ".csv");
		File file = new File(dir.getPath()+File.separator+bucket.get(0).getAddress().hashCode() + ".csv");
		
		try {
			output = new BufferedWriter(new FileWriter(file));
			output.write(header);
			for (DatagramPacket packet : bucket){
				String data = getValues(packet);
				output.write(data);
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private File createFolder(String folder){
		File dir = new File(folder);
		dir.mkdirs();
		return dir;
	}

	private String getHeader(DatagramPacket p) {
		String header = "";
		List<String[]> pairs = parsePacket(p);
		if(pairs.size() == 0){
			return "\n";
		}
		for(String[] pair : pairs){
			header += "\"" + pair[0] + "\",";
		}
		header = header.substring(0, header.length()-1);
		header += "\n";
		return header;
	}
	
	private String getValues(DatagramPacket p) {
		String data = "";
		List<String[]> pairs = parsePacket(p);
		if(pairs.size() == 0){
			return "\n";
		}
		for(String[] pair : pairs){
			data += "\"" + pair[1] + "\",";
		}
		data = data.substring(0, data.length()-1);
		data += "\n";
		return data;
	}
	
	private List<String[]> parsePacket(DatagramPacket p){
		List<String[]> data = new ArrayList<String[]>();
		BufferedReader stringReader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(p.getData())));
		
		String line;
		try { // Parse the header out to a list.
			while ((line = stringReader.readLine()) != null) {
				String[] pair = new String[2];
				if(line.contains(":")){
					int i = line.indexOf(":");
					pair[0] = line.substring(0, i);
					pair[1] = line.substring(i+1);
					data.add(pair);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
}
