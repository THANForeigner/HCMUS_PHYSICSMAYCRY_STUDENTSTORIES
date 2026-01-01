# HCMUS Physics May Cry - Student Stories

An Android application for the "Physics May Cry" event at HCMUS, designed to let users share and discover event-related stories.

---

## Server

This project uses a server running on Google Colab.

### Prerequisites

#### 1. Ngrok Account & Token

You need a free Ngrok account to expose the Colab server to the internet.

1. Sign up at [ngrok.com](https://ngrok.com).
2. Navigate to the [Your Authtoken](https://dashboard.ngrok.com/get-started/your-authtoken) page and copy your token.

#### 2. Google AI (Gemini) API Key

1. Go to the [Google AI Studio API Keys](https://aistudio.google.com/app/api-keys) page.
2. Click **Create API key**.
3. Follow the instructions to generate your key.

#### 3. Setup Firebase

**Note:** Make sure you have created/enabled the following services in your Firebase Console and you need to upgrade your project to Blaze plan to use Storage:

##### 3.1. Enable Cloud Firestore

1. **Navigate to Database:**
   * Open your project in the Firebase Console.
   * In the left-hand menu, expand the **Build** dropdown.
   * Click on **Firestore Database**.
2. **Create Database:**
   * Click the **Create database** button.
3. **Select Location:**
   * Choose a **Location** (e.g., `nam5 (us-central)`).
   * *Note: This cannot be changed later.*
   * Click **Next**.
4. **Secure Rules:**
   * Select **Production mode** (secure) or **Test mode** (open for 30 days).
   * Click **Enable**.
5. **Note for data saving:**
   * Base on our project concept if your want to create an indoor or outdoor location please follow this file structure:
   * For outdoor location:
        * /locations
            * /locations
                * /outdoor_locations
                    * /Location_A
                        * /posts
     * In outdoor location the fields are just the latitude, longitude of the location, and the bool "zone" is false
   * For indoor location:
        * /locations
            * /locations
                * /indoor_locations
                    * /Building_I
                        * /floor
                            * /1
                                * posts
        * In indoor location the fields are the latitude and longitude of the locations center, the bool "zone" is true, then the latitude and longitude of each conner of the bulding.
---

##### 3.2. Enable Firebase Storage

1. **Navigate to Storage:**
   * In the left-hand menu under **Build**, click on **Storage**.
2. **Get Started:**
   * Click **Get started**.
3. **Set Up Rules:**
   * Choose **Production mode** or **Test mode**.
   * Click **Next**.
4. **Set Cloud Location:**
   * Confirm the location (usually matches your Firestore setting).
   * Click **Done**.
---

##### 3.3. Enable Realtime Database

1. **Navigate to Realtime Database:**
   * In the left-hand menu under **Build**, click on **Realtime Database**.
2. **Create Database:**
   * Click **Create Database**.
3. **Select Location:**
   * Choose a region (e.g., `United States`, `Belgium`, or `Singapore`).
   * Click **Next**.
4. **Configure Security Rules:**
   * Select **Locked mode** or **Test mode**.
   * Click **Enable**.
### Running the Server

1. **Open the Colab Notebook**
    - Access the server code here: [Google Colab](https://colab.research.google.com/drive/1pmE5y1WtK-zsrIINoejdq15co_tSb90r?usp=sharing)

2. **Configure Secrets**
    - In the Colab notebook, locate the cells for `NGROK_TOKEN` and `GEMINI_API_KEY`.
    - Paste your Ngrok token and Gemini API key into the respective fields.

3. **Run the Notebook**
    - Execute all cells in the notebook to start the server.

    ---

## Client

This section guides you through setting up, configuring, and running the Android client application.

### Prerequisites

- Android Studio (latest version recommended)
- JDK 17 or higher
- An Android device or emulator (API level 26+)

### Configuration

Before building the app, you need to configure the Firebase connection and the backend API server URL.

#### 1. Firebase Configuration

The app uses Firebase for various backend services.

- **File to Change:** `app/google-services.json`
- **Action:** Replace the existing file with your own `google-services.json` file, which you can download from your project's settings in the [Firebase Console](https://console.firebase.google.com/).
- To download Configuration File (google-services.json)

1.  **Open Project Settings:**
    * In the Firebase Console, click the **Gear icon** next to "Project Overview" in the top-left sidebar.
    * Select **Project settings**.
2.  **Locate Your App:**
    * Scroll down to the **Your apps** card at the bottom of the **General** tab.
    * *Note: If you haven't added an Android app yet, click the **Android icon** to create one first.*
3.  **Download the File:**
    * Select your Android app from the list (if you have multiple).
    * Click the button labeled **google-services.json** to download the file.
4.  **Add to Project:**
    * Move the downloaded file into the **app** directory of your Android project (e.g., `MyProject/app/google-services.json`).

#### 2. API Server URL

The app connects to a backend server for data. You must update the server's base URL in the code.

- **File 1:** `app/src/main/java/com/example/afinal/data/network/ApiService.kt`
  - **Line to change (approx. 38):** Update the `BASE_URL` constant with your server's public URL.

        ```kotlin
        private const val BASE_URL = "YOUR_NGROK_OR_SERVER_URL"
        ```

- **File 2:** `app/src/main/java/com/example/afinal/ui/screen/AddPostScreen.kt`
  - **Line to change (approx. 145):** Update the `ngrokUrl` state variable.

        ```kotlin
        var ngrokUrl by remember { mutableStateOf("YOUR_NGROK_OR_SERVER_URL") }
        ```

### Setup and Installation

1. **Clone the Repository**

    ```bash
    git clone https://github.com/your-repository/hcmus-physicsmaycry-studentstories.git
    ```

2. **Open in Android Studio**
    - Launch Android Studio.
    - Select "Open an Existing Project" and choose the cloned directory.

3. **Sync Gradle**
    - Allow Android Studio to sync the project automatically. If it fails, manually sync by clicking the "Sync Project with Gradle Files" icon in the toolbar.

4. **Build the APK**
    - Navigate to `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.
    - Once the build is complete, click the "locate" link in the notification to find the APK file at `app/build/outputs/apk/debug/app-debug.apk`.

5. **Install the App**
    - **Enable USB Debugging** on your Android device from Developer Options.
    - Connect your device via USB.
    - Install the APK using Android Debug Bridge (`adb`):

      ```bash
      adb install app/build/outputs/apk/debug/app-debug.apk
      ```

    - Alternatively, transfer the APK file to your device and install it using a file manager.

6. **Get the API Key & Enable SDK**

    1.  Go to Google Cloud Console:
        * Navigate to the [Google Cloud Console](https://console.cloud.google.com/).
        * Select your project (or create a new one).
    2.  Enable Maps SDK for Android:
        * In the sidebar menu, go to **APIs & Services** > **Library**.
        * Search for **"Maps SDK for Android"**.
        * Click on the result and click **Enable**.
    3.  Create Credentials:
        * Go to **APIs & Services** > **Credentials**.
        * Click **+ CREATE CREDENTIALS** at the top.
        * Select **API key**.
        * **Copy the API key** generated (it starts with `AIza...`).

    4. Add Key to Android Manifest

    * Open Manifest File:
        * In Android Studio, navigate to `app/src/main/AndroidManifest.xml`.
    *  Add Meta-Data:
        * Paste the following code inside the `<application>` tag, but *outside* of any `<activity>` tags.

        ```xml
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_API_KEY_HERE" />
        ```

    3. (Optional but Recommended) Restrict Your Key
    To prevent unauthorized use of your key (and potential billing charges):
        *  Go back to the **Credentials** page in Google Cloud Console.
        *  Click the **Edit icon** (pencil) next to your API Key.
        *  Under **Application restrictions**, select **Android apps**.
        *  Click **Add an item** and enter your **Package name** and **SHA-1 certificate fingerprint**.
        *  Click **Save**.