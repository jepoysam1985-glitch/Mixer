package com.jeiannefm.mixer;

import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

@CapacitorPlugin(name = "TcpSocket")
public class TcpSocketPlugin extends Plugin {
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    private void closeQuiet() {
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null; in = null; out = null;
    }

    @PluginMethod
    public void connect(PluginCall call) {
        try {
            closeQuiet();
            socket = new Socket();
            socket.connect(new InetSocketAddress(call.getString("host"), call.getInt("port")), 10000);
            socket.setTcpNoDelay(true);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            call.resolve();
        } catch (Exception e) { call.reject("Hindi makakonekta: " + e.getMessage()); }
    }

    @PluginMethod
    public void write(PluginCall call) {
        try {
            out.write(Base64.decode(call.getString("data"), Base64.DEFAULT));
            out.flush();
            call.resolve();
        } catch (Exception e) { call.reject(String.valueOf(e.getMessage())); }
    }

    @PluginMethod
    public void read(PluginCall call) {
        String text = "";
        try {
            socket.setSoTimeout(call.getInt("timeoutMs", 5000));
            byte[] buf = new byte[2048];
            int n = in.read(buf);
            if (n > 0) text = new String(buf, 0, n, "UTF-8");
        } catch (SocketTimeoutException ignored) {
        } catch (Exception e) { call.reject(String.valueOf(e.getMessage())); return; }
        JSObject o = new JSObject();
        o.put("text", text);
        call.resolve(o);
    }

    @PluginMethod
    public void close(PluginCall call) { closeQuiet(); call.resolve(); }
}
