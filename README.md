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

### 1. Open the Colab Notebook

* **Access the server code here:** [Google Colab](https://colab.research.google.com/drive/1pmE5y1WtK-zsrIINoejdq15co_tSb90r?authuser=1&fbclid=IwY2xjawPF-BZleHRuA2FlbQIxMQBzcnRjBmFwcF9pZAEwAAEeXI0e0xzOlE-nzDQreS2LOmIJy0cc_qGc-S0qFbDWINfDCjpp4hld3pNzow0_aem_GiKVwgWKkNFvvwJ04kkG3w#scrollTo=WYilVHpS1uMk)

---

### 2. Configure Secrets

Go to the Colab notebook and locate the second cell. You will need to add the following credentials:

#### A. Ngrok (Tunneling)

**Used for:** `ngrok_token`

1. Log in to your [Ngrok Dashboard](https://dashboard.ngrok.com/).
2. In the left sidebar, go to **Cloud Edge** > **Endpoints** (or **Your Authtoken** under Getting Started).
3. Copy the long alphanumeric string.

#### B. Google AI Studio (Gemini API)

**Used for:** `gemini_api_key`

1. Visit [Google AI Studio](https://aistudio.google.com/app/api-keys).
2. Click **Create API key** (or copy an existing one).
3. Ensure you have the Gemini API enabled for this key.

#### C. Firebase Service Account (Backend & Storage)

**Used for:** `project_id`, `pri_key_id`, `pri_key`, `c_email`, `c_id`, `client_x509_cert_url` and `storage_bucket`.

**Step A: Download the Private Key**

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Click the **Gear icon** next to "Project Overview" and select **Project settings**.
3. Navigate to the **Service accounts** tab.
4. Click **Generate new private key**, then click **Generate key** to download the `.json` file.

**Step B: Map JSON values to Secrets**
Open the downloaded `.json` file in any text editor. Map the values as follows:

| Script Variable | JSON Key Name | Description |
| :--- | :--- | :--- |
|`"project_id"` | Your unique Firebase project name. |
|`"private_key_id"` | The unique ID for your private key. |
|`"private_key"` | The full string starting with `-----BEGIN PRIVATE KEY-----`. |
|`"client_email"` | The service account robot email. |
|`"client_id"` | The numeric unique client identifier. |

The json file you download should look like this:

```
{
  "type": ,
  "project_id": ,
  "private_key_id": ,
  "private_key": ,
  "client_email": ,
  "client_id": ,
  "auth_uri": ,
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url":,
  "universe_domain":
}
```

Add `"googleapis.com"` into the `"universe_domain"`.

**Step C: Find the Storage Bucket**

1. In the Firebase Console, go to **Build** > **Storage**.
2. Copy the URL found at the top (e.g., `your-project.firebasestorage.app`).
3. **Note:** Do not include the `gs://` prefix.

### D: Replace the keys and tokens in cell two to yours keys and tokens

1. Go to "Secret key" in the collab.
2. Create keys with the name like the name in each `userdata.get('name')` or you can use your own name; however you need to replace that name in `userdata.get('name')`.
3. In the value, copy and paste in the keys and token that you gather above.

---

### 3. Run the Notebook

* Execute all cells in the notebook to start the server.

## Client

This section guides you through setting up, configuring, and running the Android client application.

### Prerequisites

* Android Studio (latest version recommended)
* JDK 17 or higher
* An Android device or emulator (API level 26+)

### Configuration

Before building the app, you need to configure the Firebase connection and the backend API server URL.

#### 1. Firebase Configuration

The app uses Firebase for various backend services.

* **File to Change:** `app/google-services.json`
* **Action:** Replace the existing file with your own `google-services.json` file, which you can download from your project's settings in the [Firebase Console](https://console.firebase.google.com/).
* To download Configuration File (google-services.json)

1. **Open Project Settings:**
    * In the Firebase Console, click the **Gear icon** next to "Project Overview" in the top-left sidebar.
    * Select **Project settings**.
2. **Locate Your App:**
    * Scroll down to the **Your apps** card at the bottom of the **General** tab.
    * *Note: If you haven't added an Android app yet, click the **Android icon** to create one first.*
3. **Download the File:**
    * Select your Android app from the list (if you have multiple).
    * Click the button labeled **google-services.json** to download the file.
4. **Add to Project:**
    * Move the downloaded file into the **app** directory of your Android project (e.g., `MyProject/app/google-services.json`).

#### 2. API Server URL

To connect the app to your backend, you must update the server's base URL in the code. After successfully running the server notebook, you will get a public `https://` address from ngrok. Use this URL for the configuration.

You need to update the URL in the following two locations:

| File                                                              | Variable to Update | Line (approx.) | Code Snippet                                                    |
| :---------------------------------------------------------------- | :----------------- | :------------- | :-------------------------------------------------------------- |
| `app/src/main/java/com/example/afinal/data/network/ApiService.kt` | `BASE_URL`         | 38             | `private const val BASE_URL = "YOUR_NGROK_URL"`                 |
| `app/src/main/java/com/example/afinal/ui/screen/AddPostScreen.kt` | `ngrokUrl`         | 145            | `var ngrokUrl by remember { mutableStateOf("YOUR_NGROK_URL") }` |


### Setup and Installation

1. **Clone the Repository**

    ```bash
    git clone https://github.com/your-repository/hcmus-physicsmaycry-studentstories.git
    ```

2. **Open in Android Studio**
    * Launch Android Studio.
    * Select "Open an Existing Project" and choose the cloned directory.

3. **Sync Gradle**
    * Allow Android Studio to sync the project automatically. If it fails, manually sync by clicking the "Sync Project with Gradle Files" icon in the toolbar.

4. **Build the APK**
    * Navigate to `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.
    * Once the build is complete, click the "locate" link in the notification to find the APK file at `app/build/outputs/apk/debug/app-debug.apk`.

5. **Install the App**
    * **Enable USB Debugging** on your Android device from Developer Options.
    * Connect your device via USB.
    * Install the APK using Android Debug Bridge (`adb`):

      ```bash
      adb install app/build/outputs/apk/debug/app-debug.apk
      ```

    * Alternatively, transfer the APK file to your device and install it using a file manager.

6. **Get the API Key & Enable SDK**

    1. Go to Google Cloud Console:
        * Navigate to the [Google Cloud Console](https://console.cloud.google.com/).
        * Select your project (or create a new one).
    2. Enable Maps SDK for Android:
        * In the sidebar menu, go to **APIs & Services** > **Library**.
        * Search for **"Maps SDK for Android"**.
        * Click on the result and click **Enable**.
    3. Create Credentials:
        * Go to **APIs & Services** > **Credentials**.
        * Click **+ CREATE CREDENTIALS** at the top.
        * Select **API key**.
        * **Copy the API key** generated (it starts with `AIza...`).

    4. Add Key to Android Manifest

    * Open Manifest File:
        * In Android Studio, navigate to `app/src/main/AndroidManifest.xml`.
    * Add Meta-Data:
        * Paste the following code inside the `<application>` tag, but *outside* of any `<activity>` tags.

        ```xml
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_API_KEY_HERE" />
        ```

    1. (Optional but Recommended) Restrict Your Key
    To prevent unauthorized use of your key (and potential billing charges):
        * Go back to the **Credentials** page in Google Cloud Console.
        * Click the **Edit icon** (pencil) next to your API Key.
        * Under **Application restrictions**, select **Android apps**.
        * Click **Add an item** and enter your **Package name** and **SHA-1 certificate fingerprint**.
        * Click **Save**.
