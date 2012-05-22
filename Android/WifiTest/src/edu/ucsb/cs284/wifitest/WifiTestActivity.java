package edu.ucsb.cs284.wifitest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import edu.ucsb.cs284.wifitest.adaptercontrollers.AdapterController;
import edu.ucsb.cs284.wifitest.adaptercontrollers.GenericController;
import edu.ucsb.cs284.wifitest.adaptercontrollers.TiWlanController;

public class WifiTestActivity extends Activity {
    private WifiTester tester;
    private MessageReceiver receiver;
    private TextView status;
    private EditText ipText;
    private Button setIp;
    private TextView log;

    // Device --> Adapter controller mapping.
    private Map<String, AdapterController> controllers = new HashMap<String, AdapterController>();
    {
        controllers.put("shadow", new TiWlanController());
    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        log = (TextView) findViewById(R.id.info);
        status = (TextView) findViewById(R.id.status);
        ipText = (EditText) findViewById(R.id.ip);
        
        // Get the Wifi adapter's IP address.
        WifiManager wifiMan = (WifiManager) this.getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ip = wifiInf.getIpAddress();
        if (ip == 0) {
            Toast.makeText(this, "WiFi is not connected.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        byte[] ipBytes = { (byte) (ip & 255), (byte) (ip >> 8 & 255), (byte) (ip >> 16 & 255),
                (byte) (ip >> 24 & 255) };
        String address = "0.0.0.0";
        try {
            System.out.println(Arrays.toString(ipBytes));
            address = InetAddress.getByAddress(ipBytes).getHostAddress();
            log("WiFi IP: " + address);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }
        ipText.setText(address.substring(0, address.lastIndexOf('.') + 1));

        // Select an adapter controller, 
        AdapterController controller = controllers.get(android.os.Build.BOARD);
        if (controller == null) {
            controller = new GenericController();
        }
        String controllerName = controller.getClass().getName();
        log("Adapter controller: " + controllerName.substring(controllerName.lastIndexOf('.') + 1));
        tester = new WifiTester(this, controller);

        receiver = new MessageReceiver(tester);

        setIp = (Button) findViewById(R.id.setIp);
        setIp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InetAddress address = InetAddress.getByName(ipText.getText().toString());
                    receiver.setAddress(address);
                    tester.setAddress(address);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }
        });

        AsyncTask<MessageReceiver, String, Void> connectionMonitor = new AsyncTask<MessageReceiver, String, Void>() {
            private boolean wasConnected = true;

            @Override
            protected Void doInBackground(MessageReceiver... params) {
                while (true) {
                    boolean isConnected = params[0].isConnected();
                    if (wasConnected != isConnected) {
                        publishProgress(isConnected ? "Connected" : "Connecting");
                        wasConnected = isConnected;
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
            }

            @Override
            protected void onProgressUpdate(String... values) {
                status.setText(values[0]);
                log(values[0]);
            }

        };

        connectionMonitor.execute(receiver);
        receiver.start();

        log("Ready");
    }


    public void log(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Spannable date = new SpannableString(DateFormat.format("hh:mm:ss", System.currentTimeMillis()));
                date.setSpan(new ForegroundColorSpan(Color.GRAY), 0, date.length(), 0);
                Spannable text = new SpannableStringBuilder().append(date).append("  ").append(info).append("\n")
                        .append(log.getText());
                log.setText(text);
            }
        });
    }
}