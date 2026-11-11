#!/bin/bash
set -e
D=android/app/src/main/java/com/jeiannefm/mixer
mkdir -p "$D"
cp android-src/*.java "$D/"
M=android/app/src/main/AndroidManifest.xml
sed -i 's#<application#<uses-permission android:name="android.permission.RECORD_AUDIO"/>\n    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>\n    <uses-permission android:name="android.permission.WAKE_LOCK"/>\n    <application#' "$M"
