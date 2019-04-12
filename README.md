# Rivetz Samples
This repository contains miscellaneous sample applications that demonstrate some of the various ways Rivetz Toolkit API's are used to access Rivetz Technology.  Each sample is a buildable and runnable app, with a very simple UI. It is assumed that Android Studio is being used to build and run them.

All of the apps use the same basic pattern, and demonstrate things such as:
- Check for existence of the _Rivet_, demonstrate how to handle if it is not installed.
- Pair the device to the _Rivetz Network_.
- See if the device is Dual Root of Trust (DRT) capable.
- Manage asynchronous calls into the _Rivet_.
- Basic error / exception handling
- Managing the UI components, managing UI and background threads
- Using the current RivetzJ API's.

### Notes
- The current apps are written with the RivetzJ version set to 1.0.12. This is specified by setting  the _rivetzJversion_ variable in the __gradle.properties__ project file to the version you are using before building these apps.
- You cannot use an emulator in the Android Studio AVD to run these samples, as each use the Trusted Execution Environment (TEE), which is not supported in any of the device emulations.  You must attach a supported physical device to your development environment in order to run these apps.  See https://rivetz.com/compatibility to check if your device is supported.

## Sample App Descriptions
1. __RivetzBoilerPlate__: This is the simplest of the sample apps, demonstrating how to pair your app / device with the Rivetz network.  It may be copied and used as the starting point for a Rivetz enabled application, as it is for most of the samples in this repository.

2. __HashSample__: Apply the SHA-256 hashing algorithm to a text string entered by the user.

3. __EncryptDecryptSample__: Demonstrates creating an AES256 symmetric key within the TEE to be used for encryption and decryption.  The app will then encrypt the user entered data string and show the results, which can then be decrypted to the original text string.

4. __SigningSample__: Creates a NIST256 signing key in the TEE, and uses the concept of real and fake messages to demonstrate both the signing and verification of user configurable text strings.

5. __AsyncKeyManipulationSample__: Creates a NIST256 key and uses it to demonstrate various key management functions, such as fetching and displaying properties of the key, deleting a key, etc.

6. __UsageRuleTUISample__: Building on the EncryptDecryptSample described above, this app demonstrates the use of a couple of other SDK capabilities;

  * Key Usage Rules - the encryption key is created with usage rules requiring a TUI confirmation for use, as well as a requirement to use Dual Roots of Trust (DRT) if DRT is supported on this device.
  * TUI Confirm - uses the _TUIConfirm_ API to send a message to the Trusted User Interface, and interprets returned results based on the users positive or negative response to that message.


7. __SingletonRivet__:  This is another simple hashing example, however it demonstrates accessing Rivetz functions via the Singleton class pattern.
