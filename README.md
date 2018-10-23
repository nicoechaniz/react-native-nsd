
# react-native-nsd

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
```javascript
import Nsd from 'react-native-nsd';

// TODO: What to do with the module?
Nsd;
```
  