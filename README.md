# react-native-nsd

This module is a wrapper to use Android's Network Service Discovery from a react-native App.

## Getting started

`$ npm install react-native-nsd --save`

### Mostly automatic installation

`$ react-native link react-native-nsd`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`

- Add `import net.altermundi.rn_nsd.NsdPackage;` to the imports at the top of the file
- Add `new NsdPackage()` to the list returned by the `getPackages()` method

2. Append the following lines to `android/settings.gradle`:
   ```
   include ':react-native-nsd'
   project(':react-native-nsd').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-sd/android')
   ```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
   ```
     compile project(':react-native-nsd')
   ```

## Usage

To use the module in your App.

```javascript
import { NSD } from "react-native-nsd";
import { DeviceEventEmitter } from "react-native";

// Listen and react to discovered/resolved services
DeviceEventEmitter.addListener("serviceResolved", function(e) {
  console.log("JS: service resolved");
  console.log(e.name, e.host, e.port);
  // if you are using react-native-handshake you can receive handshake message from the discovered peer
  // You would import { Handshake } from react-native-handshake first and then you can do
  Handshake.receiveKey(e.host, e.port);
  // read react-native-handshake documentation for info on how to use it
});

// NSD methods (self explanatory)
NSD.setServiceName("myservice");
NSD.discover();
NSD.stopDiscovery();
NSD.register(port_number);
NSD.unregister();
```
