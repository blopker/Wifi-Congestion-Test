package edu.ucsb.cs284.wifitest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import edu.ucsb.cs284.wifitest.MessageReceiver.MessageHandler;
import edu.ucsb.cs284.wifitest.adaptercontrollers.AdapterController;

public class WifiTester {
    private static final int UDP_PORT = 2345;
    private static final int PACKET_SIZE = 1024;
    private boolean uploading;
    private byte[] data;
    private InetAddress address;
    private final WifiTestActivity testActivity;
    private final AdapterController adapterController;


    public WifiTester(WifiTestActivity testActivity, AdapterController adapterController) {
        this.testActivity = testActivity;
        this.adapterController = adapterController;
        data = new byte[PACKET_SIZE];
    }


    @MessageHandler(message = "start")
    public void startUpload() {
        System.out.println("Start upload");

        if (!isUploading()) {
            setUploading(true);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    testActivity.log("Start transmission");
                    DatagramSocket toServer;
                    try {
                        toServer = new DatagramSocket();
                        DatagramPacket packet = new DatagramPacket(data, data.length);
                        packet.setSocketAddress(new InetSocketAddress(address, UDP_PORT));
                        int packetId = 0;
                        while (isUploading()) {
                            try {
                                byte[] idBytes = (Integer.toString(packetId++) + "\n").getBytes();
                                System.arraycopy(idBytes, 0, data, 0, idBytes.length);
                                toServer.send(packet);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (SocketException e1) {
                        e1.printStackTrace();
                    }
                    testActivity.log("Stop transmission");
                }
            }).start();
        } else {
            testActivity.log("Already uploading");
        }
    }


    protected synchronized boolean isUploading() {
        return uploading;
    }


    @MessageHandler(message = "stop")
    public void stopUpload() {
        System.out.println("Stop upload");
        setUploading(false);
    }


    private synchronized void setUploading(boolean uploading) {
        this.uploading = uploading;
    }


    @MessageHandler(message = "setPower")
    public void setTxPower(String db) {
        testActivity.log("Set TX power to " + db);
        adapterController.setTxPower(Integer.parseInt(db));
    }


    public void setAddress(InetAddress address) {
        stopUpload();
        this.address = address;
    }
}
