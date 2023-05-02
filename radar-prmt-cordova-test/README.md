# RADAR pRMT Cordova test

## Installation

First install `E4link-1.0.0.pom` to your local Maven repository as described in [RADAR Empatica plugin](https://github.com/RADAR-base/radar-commons-android/blob/simplify-setup/plugins/radar-android-empatica/README.md). Then build radar-commons-android with
```
./gradlew publishToMavenLocal
```
Now you can start using the Cordova app radar-prmt-cordova-test.

Install dependencies with
```
npm install
```

Then initialize the project wiht

```
npm run init
```

To build the APK, run

```
npm run build
```

It can be installed with

```
adb install -r platforms/android/app/build/outputs/apk/debug/app-debug.apk
```
