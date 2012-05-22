package edu.ucsb.cs284.wifitest.adaptercontrollers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * For devices with TI OMAP WLAN.
 * 
 */
public class TiWlanController implements AdapterController {

    @Override
    public boolean setTxPower(int dbm) {
        dbm *= 10; // Expected by wlan_cu

        try {
            PrintWriter pw = new PrintWriter("/sdcard/wifiscript");
            pw.println("/ m b " + dbm);
            pw.println("/ q");
            pw.println();
            pw.close();

            try {
                Process process = Runtime.getRuntime().exec(
                        new String[] { "su", "-c", "/system/bin/wlan_cu -s /sdcard/wifiscript" });
                final InputStream in = process.getInputStream();
                final InputStream err = process.getErrorStream();
                Thread inGobbler = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        int b;
                        StringBuilder bu = new StringBuilder();
                        try {
                            while ((b = in.read()) != -1) {
                                bu.append((char) b);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Output:");
                        System.out.println(bu.toString());
                    }
                });

                Thread errGobbler = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        int b;
                        StringBuilder bu = new StringBuilder();
                        try {
                            while ((b = err.read()) != -1) {
                                bu.append((char) b);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Error:");
                        System.out.println(bu.toString());
                    }
                });
                inGobbler.start();
                errGobbler.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        return false;
    }
}
