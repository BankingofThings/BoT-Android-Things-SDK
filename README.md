# Android-Things

## Supported Features
   | Sl. No        | SDK Feature                                | Status      | Remarks |
   | :-----------: |:-------------------------------------------| :-----------| :-------|
   |        1      | Pairing through Bluetooth Low Energy (BLE) | :thumbsup: | Supported for both iOS and Android Mobile Applications |
   |        2      | Pairing through QR Code                    | :thumbsup: | Supported only in Console mode for device to be paired for iOS and Android Mobile Applications|
   |        3      | Secured HTTP with BoT Service              | :thumbsup: | Supported for all interactions with backend server |
   |        4      | Logging                                    | :thumbsup: | Console Logging is implemented|
   |        5      | Offline Actions                            | :thumbsup: | Helps to persist the autonomous payments on the device when there is no internet connectivity available. The saved offline actions get completed when the next action trigger happens and internet connectivity is available. This feature is in plan for implementation.|
   
1. Follow the steps for setting up your Raspberry PI 3 B. (Keep in mind, B+ is still not supported by Google)
https://developer.android.com/things/hardware/raspberrypi

2. Open Android Studio and clone the SDK (File>New>Project from Version Control>Git)
https://github.com/BankingofThings/BoT-Android-Things-SDK.git

3. After clonning make the project by clicking, Make Project (Green Hammer).

4. Open ExampleActivity class (app/src/main/java/io/bankingofthings/iot) and go to ExampleActivity.initFinn() method.
Fill in your Maker ID and other device related data, in the Finn constructor. See Finn class (app/src/main/java/io/bankingofthings/iot) for more details.

### Important
The last parameter, newInstall, should only be set to 'true' when the Maker ID changes or a new device should be generated (for adding multiple devices on app level).
Keep in mind to set it to false afterwards, otherwise every compilation will create a new device. 

5. Go to ExampleActivity.startFinnObservablePattern() (the same function is also available in callback pattern) and fill in the action ID you want to trigger, after the device is paired. Instead of trigger an action, you can also add your custom code here. (start reading sensors, manipulate actuators, etc).

### Important
If you have multi pair on, add (replace null) the alternative ID (the same ID entered in the app) as the second paramter for triggering the action.

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

### See documents folder for the diagrams.
Class diagram - https://github.com/BankingofThings/BoT-Android-Things-SDK/blob/master/documents/classdiagram.png


# BotTalk
The BotTalk mechanisme enables two way communication between IoT and FINN backend (polling mechanism).

For example when an action with type of PayPerUse is configured. When the client activates the action in the app, the backend notifies the BotTalk mechanisme with a **message** and immediatly inactivates the action. So the client can active the action again.

- The **message** contains:
    - actionID: generated when an action is created on Portal
    - customerID: clients unique id per app profile
    
The IoT SDK polls this message every 10 seconds (can be changed) and dispatches the listener. Which can be set with Finn.setBotTalkListener(BotTalkListener). The message can be handled by triggering the action( with Finn.triggerBotTalkAction(actionID:String, customerID:String)). For example the IoT devices needs to activate some actuators or sensors if everything is done, it can notify FINN backend with the triggerAction so the client is notified.
