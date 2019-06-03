# Android-Things

1. Follow the steps for setting up your Raspberry PI 3 B. (Keep in mind, B+ is still not supported by Google)
https://developer.android.com/things/hardware/raspberrypi

2. Open Android Studio and clone the SDK (File>New>Project from Version Control>Git)
https://github.com/BankingofThings/BoT-Android-Things-SDK.git

3. After clonning make the project by clicking, Make Project (Green Hammer).

4. Open ExampleActivity file (app/src/main/java/io/bankingofthings/iot)
Fill in your Maker ID and other device related data in the Finn constructor parameters (ExampleActivity.initFinn()).

#Important
The last parameter, newInstall, should only be set to 'true' when the Maker ID changes or a new device should be generated (for adding multiple devices on app level).
Keep in mind to set it to false after, otherwise every compilation will create a new device. 
See Finn file (app/src/main/java/io/bankingofthings/iot) for more details

5. Go to ExampleActivity.startFinnCallbackPattern() and fill in the action ID you want to trigger after the device is paired. Or add or change the code to what you want (start reading sensors, manipulate actuators, etc).
#Important
If you have multi pair on, add (replace null) the alternative ID (the same ID entered in the app).

### A. Finn supports observable and callback patterns.
- **For the callback pattern there are 3 callbacks:**
    - Finn.StartCallback: notifies when device is paired.
    - Finn.GetActionsCallback: returns a list of ActionModels.
    - Finn.TriggerActionCallback: notifies when an action is triggered.

### B. Finn generates a QR bitmap after initialization. 
- **The QR code has a json text with the following data:**
    - makerID
    - deviceID
    - publicKey
    - name
    - multipair true/false
    - aid (alternative identifier id))

See documents folder for the diagrams.
