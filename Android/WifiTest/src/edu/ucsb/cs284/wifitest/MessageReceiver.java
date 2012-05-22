package edu.ucsb.cs284.wifitest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

/**
 * Class that maintains a TCP connection to a server and calls methods
 * on a handler object when string messages are received.
 * 
 * Messages must be in the format "name" or "nameWithArgs|arg1|arg2".
 * Message handler methods must have a String parameter for each argument.
 * 
 */
public class MessageReceiver implements Runnable {
    public static final int PORT = 1234;
    private InetAddress address;
    private Object handlerObject;
    private Map<String, Method> handlerMethods;

    private Socket socket;
    private boolean running;


    public MessageReceiver(Object handler) {
        this.handlerMethods = new HashMap<String, Method>();
        this.handlerObject = handler;

        // Find the message handler methods.
        for (Method m : handler.getClass().getMethods()) {
            MessageHandler handlerAnnotation = m.getAnnotation(MessageHandler.class);
            if (handlerAnnotation != null) {
                handlerMethods.put(handlerAnnotation.message(), m);
            }
        }
    }


    public synchronized void setAddress(InetAddress address) {
        this.address = address;
        if (isConnected()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }


    public void start() {
        this.running = true;
        new Thread(this).start();
    }


    public synchronized void stop() {
        this.running = false;
    }


    @Override
    public void run() {
        while (isRunning()) {
            // Wait for a connection.
            Log.w("WifiTest", "Connecting");
            while (!isConnected() && isRunning()) {
                connect();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Don't care.
                }
            }
            Log.w("WifiTest", "Connected");

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (isRunning()) {
                    String read = br.readLine();
                    if (read == null) {
                        break; // Connection lost.
                    }
                    System.out.println(read);
                    String[] message = read.split("\\|");
                    Object[] args = Arrays.copyOfRange(message, 1, message.length);

                    Method m = handlerMethods.get(message[0]);
                    if (m != null) {
                        try {
                            m.invoke(handlerObject, args);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            Log.e("WifiTest", "Handler expected incorrect arguments.");
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e("WifiTest", "Unrecognized command: " + message[0]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.w("WifiTest", "Connection Lost");
            }
            try {
                socket.close();
            } catch (IOException e) {
                // Don't care.
            }
        }
    }


    private synchronized boolean isRunning() {
        return running;
    }


    private void connect() {
        if (address != null) {
            try {
                socket = new Socket(address, PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Annotation for message handler methods.
     * 
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MessageHandler {
        String message();
    }

}
