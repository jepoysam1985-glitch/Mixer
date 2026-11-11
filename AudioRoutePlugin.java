/*
 * AudioRoutePlugin.java  (Capacitor 3+ / Android)
 * Para sa 🎧 Bluetooth at ⚙ Bluetooth settings buttons ng JEIANNE FM Mixer.
 *
 * PAANO ILAGAY SA GITHUB REPO MO (3 hakbang):
 *
 * 1) Ilagay ang file na ito sa tabi ng MainActivity.java mo:
 *      android/app/src/main/java/<package path>/AudioRoutePlugin.java
 *    Palitan ang "package ..." sa ibaba ng EKSAKTONG package ng MainActivity mo
 *    (nasa unang linya ng MainActivity.java).
 *
 * 2) Sa MainActivity.java, idagdag ang registerPlugin sa tabi ng TcpSocket mo
 *    (dapat bago ang super.onCreate):
 *      registerPlugin(AudioRoutePlugin.class);
 *
 * 3) Sa android/app/src/main/AndroidManifest.xml, idagdag (sa loob ng <manifest>, labas ng <application>):
 *      <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
 *      <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
 *      <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
 *
 * Tapos i-push at hayaang mag-build ang GitHub Actions ng bagong APK.
 *
 * TANDAAN: kapag Bluetooth ang output habang bukas ang mic, ang mic ng Bluetooth
 * headset din ang magagamit ng Android (phone-call quality). Ito ay limitasyon ng Android.
 */
package com.example.app; // <-- PALITAN ito ng package ng MainActivity mo

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "AudioRoute",
    permissions = { @Permission(alias = "bt", strings = { Manifest.permission.BLUETOOTH_CONNECT }) }
)
public class AudioRoutePlugin extends Plugin {

    // numeric para hindi masira ang build kahit mas mababa ang compileSdk
    private static final int T_SCO = 7, T_A2DP = 8, T_BLE_HEADSET = 26, T_BLE_SPEAKER = 27;

    private AudioManager am() {
        return (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    private boolean needsPermission() {
        return Build.VERSION.SDK_INT >= 31 && getPermissionState("bt") != PermissionState.GRANTED;
    }

    private boolean isBluetooth(int t) {
        return t == T_SCO || t == T_A2DP || t == T_BLE_HEADSET || t == T_BLE_SPEAKER;
    }

    /** Buksan ang Bluetooth settings ng phone para mag-pair/connect. */
    @PluginMethod
    public void openSettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("Hindi mabuksan ang Bluetooth settings: " + e.getMessage());
        }
    }

    /** Listahan ng output devices; may "bluetooth": true ang mga Bluetooth. */
    @PluginMethod
    public void listDevices(PluginCall call) {
        if (needsPermission()) { requestPermissionForAlias("bt", call, "permResult"); return; }
        doList(call);
    }

    /** Ilipat ang tunog (at mic) sa naka-connect na Bluetooth headset. */
    @PluginMethod
    public void routeBluetooth(PluginCall call) {
        if (needsPermission()) { requestPermissionForAlias("bt", call, "permResult"); return; }
        doRoute(call);
    }

    /** Ibalik sa normal na routing ng phone. */
    @PluginMethod
    @SuppressWarnings("deprecation")
    public void routeDefault(PluginCall call) {
        try {
            AudioManager a = am();
            if (Build.VERSION.SDK_INT >= 31) {
                a.clearCommunicationDevice();
            } else {
                a.stopBluetoothSco();
                a.setBluetoothScoOn(false);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("routeDefault: " + e.getMessage());
        }
    }

    @PermissionCallback
    private void permResult(PluginCall call) {
        if (needsPermission()) {
            call.reject("Kailangan ang Bluetooth permission. I-allow ito sa Settings > Apps > (app na ito) > Permissions.");
            return;
        }
        String m = call.getMethodName();
        if ("listDevices".equals(m)) doList(call);
        else if ("routeBluetooth".equals(m)) doRoute(call);
        else call.resolve();
    }

    private void doList(PluginCall call) {
        try {
            JSArray arr = new JSArray();
            for (AudioDeviceInfo d : am().getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int t = d.getType();
                JSObject o = new JSObject();
                o.put("id", d.getId());
                o.put("name", String.valueOf(d.getProductName()));
                o.put("type", t);
                o.put("bluetooth", isBluetooth(t));
                arr.put(o);
            }
            JSObject res = new JSObject();
            res.put("devices", arr);
            call.resolve(res);
        } catch (Exception e) {
            call.reject("listDevices: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void doRoute(PluginCall call) {
        try {
            AudioManager a = am();
            if (Build.VERSION.SDK_INT >= 31) {
                AudioDeviceInfo target = null;
                for (AudioDeviceInfo d : a.getAvailableCommunicationDevices()) {
                    int t = d.getType();
                    if (t == T_SCO || t == T_BLE_HEADSET) { target = d; break; }
                }
                if (target == null) {
                    call.reject("Walang Bluetooth headset na pwedeng gamitin. I-connect muna sa Bluetooth settings.");
                    return;
                }
                if (!a.setCommunicationDevice(target)) {
                    call.reject("Hindi mailipat ang tunog sa Bluetooth.");
                    return;
                }
            } else {
                a.startBluetoothSco();
                a.setBluetoothScoOn(true);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("routeBluetooth: " + e.getMessage());
        }
    }
}
