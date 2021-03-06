package com.tavultesoft.kmea;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import com.tavultesoft.kmea.util.FileDownloader;

import static com.tavultesoft.kmea.KMManager.KMDefault_AssetPackages;

public class KMKeyboardDownloaderActivity extends Activity {
  // Bundle Keys
  // Cloud
  public static final String ARG_PKG_ID = "KMKeyboardActivity.pkgID";
  public static final String ARG_KB_ID = "KMKeyboardActivity.kbID";
  public static final String ARG_LANG_ID = "KMKeyboardActivity.langID";
  public static final String ARG_KB_NAME = "KMKeyboardActivity.kbName";
  public static final String ARG_LANG_NAME = "KMKeyboardActivity.langName";
  public static final String ARG_IS_CUSTOM = "KMKeyboardActivity.isCustom";

  // custom keyboard
  public static final String ARG_KEYBOARD = "KMKeyboardActivity.keyboard";
  public static final String ARG_LANGUAGE = "KMKeyboardActivity.language";
  public static final String ARG_IS_DIRECT = "KMKeyboardActivity.isDirect";
  public static final String ARG_URL = "KMKeyboardActivity.url";
  public static final String ARG_FILENAME = "KMKeyboardActivity.filename";

  public static final String kKeymanApiBaseURL = "https://r.keymanweb.com/api/3.0/";
  public static final String kKeymanApiRemoteURL = "https://r.keymanweb.com/api/2.0/remote?url=";

  // Keyman public keys
  public static final String KMKey_Direct = "direct";
  public static final String KMKey_URL = "url";
  public static final String KMKey_Keyboard = "keyboard";
  public static final String KMKey_LanguageKeyboards = "keyboards";
  public static final String KMKey_Options = "options";
  public static final String KMKey_Language = "language";
  public static final String KMKey_Languages = "languages";
  public static final String KMKey_Filename = "filename";

  // Keyman internal keys
  public static final String KMKey_KeyboardBaseURI = "keyboardBaseUri";
  public static final String KMKey_FontBaseURI = "fontBaseUri";

  private static String pkgID;
  private static String kbID;
  private static String langID;
  private static String kbName;
  private static String langName;
  private static Boolean isCustom;

  private static String customKeyboard;
  private static String customLanguage;
  private static Boolean isDirect;
  private static String url;
  private static String filename;

  private static ArrayList<KeyboardEventHandler.OnKeyboardDownloadEventListener> kbDownloadEventListeners = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      pkgID = bundle.getString(ARG_PKG_ID);
      if (pkgID == null || pkgID.isEmpty()) {
        pkgID = KMManager.KMDefault_LegacyPackageID;
      }
      kbID = bundle.getString(ARG_KB_ID);
      langID = bundle.getString(ARG_LANG_ID);
      kbName = bundle.getString(ARG_KB_NAME);
      langName = bundle.getString(ARG_LANG_NAME);
      isCustom = bundle.getBoolean(ARG_IS_CUSTOM);

      // URL parameters for custom keyboard (if they exist)
      customKeyboard = bundle.getString(ARG_KEYBOARD);
      customLanguage = bundle.getString(ARG_LANGUAGE);
      isDirect = bundle.getBoolean(ARG_IS_DIRECT);
      url = bundle.getString(ARG_URL);
      filename = bundle.getString(ARG_FILENAME);
      if (filename == null || filename.isEmpty()) {
        filename = "unknown";
      }
    } else {
      return;
    }

    Bundle args = new Bundle();
    String title = "";
    if (url != null) {
      title = "Custom Keyboard: " + filename;
    } else if (customKeyboard != null && customLanguage != null &&
        !customKeyboard.trim().isEmpty() && !customLanguage.trim().isEmpty()) {
      int kbIndex = KMManager.getKeyboardIndex(getApplicationContext(), customKeyboard, customLanguage);

      if (kbIndex >= 0) {
        KMManager.setKeyboard(getApplicationContext(), kbIndex);
        // No interaction needed
        return; // false
      }

      title = customLanguage + "_" + customKeyboard;
  } else {
    // Download keyboard from cloud server
    title = langName + ": " + kbName;
  }

  DialogFragment dialog = new ConfirmDialogFragment();
    args.putString(ConfirmDialogFragment.ARG_TITLE, title);
    dialog.setArguments(args);
    dialog.show(getFragmentManager(), "dialog");
}

  /**
   * Async task to download a Keyman keyboard from either Keyman cloud server or custom url
   * @param context
   * @param showProgressDialog
   */
    public static void download(final Context context, final boolean showProgressDialog) {

    new AsyncTask<Void, Integer, Integer>() {
      private ProgressDialog progressDialog;
      //private String languageName = "";
      //private String keyboardName = "";
      private String kbVersion = "1.0";
      private String kbIsCustom = isCustom ? "Y" : "N";
      private String font = "";
      private String oskFont = "";

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        if (showProgressDialog) {
          progressDialog = new ProgressDialog(context);
          progressDialog.setMessage("Downloading keyboard...");
          progressDialog.setCancelable(false);
          if (!((Activity) context).isFinishing()) {
            progressDialog.show();
          } else {
            cancel(true);
            progressDialog = null;
          }
        }
      }

      @Override
      protected Integer doInBackground(Void... voids) {
        int ret = -1;
        ArrayList<String> urls = new ArrayList<String>();

        if (isCancelled())
          return ret;

        try {
          String exceptionStr = "Invalid keyboard";
          if (pkgID == null || pkgID.trim().isEmpty() ||
            (!isCustom && (langID == null || langID.trim().isEmpty() || kbID == null || kbID.trim().isEmpty()))) {
            throw new Exception(exceptionStr);
          }

          String deviceType = context.getResources().getString(R.string.device_type);
          if (deviceType.equals("AndroidTablet")) {
            deviceType = "androidtablet";
          } else {
            deviceType = "androidphone";
          }

          // Placeholder here to attempt to process .kmp keyboard first:

          ////

          JSONParser jsonParser = new JSONParser();
          JSONObject kbData = null;
          String remoteUrl = "";
          if (isCustom) {
            if (isDirect) {
              remoteUrl = url;
            } else {
              String encodedUrl = URLEncoder.encode(filename, "utf-8");
              remoteUrl = String.format("%s%s&device=%s", kKeymanApiRemoteURL, encodedUrl, deviceType);
            }
          } else {
            // Keyman cloud
            remoteUrl = String.format("%slanguages/%s/%s?device=%s", kKeymanApiBaseURL, langID, kbID, deviceType);
          }

          kbData = jsonParser.getJSONObjectFromUrl(remoteUrl);

          exceptionStr = "Could not reach server";
          if (kbData == null) {
            throw new Exception(exceptionStr);
          }

          exceptionStr = "The keyboard could not be installed";
          JSONObject options = kbData.optJSONObject(KMKey_Options);
          if (options == null) {
            throw new Exception(exceptionStr);
          }
          String kbBaseUri = options.optString(KMKey_KeyboardBaseURI, "");
          if (kbBaseUri.isEmpty()) {
            throw new Exception(exceptionStr);
          }

          String fontBaseUri = options.optString(KMKey_FontBaseURI, "");
          JSONObject language, keyboard;
          if (!isCustom) {
            // JSON from Keyman cloud server
            language = kbData.optJSONObject(KMKey_Language);
            if (language == null) {
              throw new Exception(exceptionStr);
            }

            langName = language.optString(KMManager.KMKey_Name, "");

            JSONArray keyboards = language.getJSONArray(KMKey_LanguageKeyboards);
            if (keyboards == null ) {
              throw new Exception(exceptionStr);
            }

            keyboard = keyboards.getJSONObject(0);
            if (keyboard == null) {
              throw new Exception(exceptionStr);
            }


          } else {
            // JSON from custom URL
            keyboard = kbData.optJSONObject(KMKey_Keyboard);
            if (keyboard == null) {
              throw new Exception(exceptionStr);
            }

            JSONArray languages = keyboard.optJSONArray(KMKey_Languages);
            if (languages == null) {
              throw new Exception(exceptionStr);
            }

            // Concatenate langID and langName
            langID = "";
            langName = "";
            int langCount = languages.length();
            for (int i=0; i < langCount; i++) {
              langID += languages.getJSONObject(i).getString(KMManager.KMKey_ID);
              langName += languages.getJSONObject(i).getString(KMManager.KMKey_Name);
              if (i < langCount -1) {
                langID += ";";
                langName += ";";
              }
            }
            pkgID = keyboard.optString(KMManager.KMKey_PackageID, KMManager.KMDefault_LegacyPackageID);
            kbID = keyboard.optString(KMManager.KMKey_ID, "");

          }

          kbName = keyboard.optString(KMManager.KMKey_Name, "");
          kbVersion = keyboard.optString(KMManager.KMKey_KeyboardVersion, "1.0");
          font = keyboard.optString(KMManager.KMKey_Font, "").replace(
            "\"" + KMKeyboardDownloaderActivity.KMKey_Filename + "\"",
            "\"" + KMManager.KMKey_FontSource + "\"");
          oskFont = keyboard.optString(KMManager.KMKey_OskFont, "").replace(
            "\"" + KMKeyboardDownloaderActivity.KMKey_Filename + "\"",
            "\"" + KMManager.KMKey_FontSource + "\"");
          String kbFilename = keyboard.optString(KMKey_Filename, "");

          if (kbName.isEmpty() || langName.isEmpty() || kbFilename.isEmpty())
            throw new Exception(exceptionStr);

          String kbUrl = kbBaseUri + kbFilename;
          urls.add(kbUrl);
          JSONObject jsonFont = keyboard.optJSONObject(KMManager.KMKey_Font);
          JSONObject jsonOskFont = keyboard.optJSONObject(KMManager.KMKey_OskFont);
          ArrayList<String> fontUrls = fontUrls(jsonFont, fontBaseUri, true);
          ArrayList<String> oskFontUrls = fontUrls(jsonOskFont, fontBaseUri, true);
          if (fontUrls != null)
            urls.addAll(fontUrls);
          if (oskFontUrls != null) {
            for (String url : oskFontUrls) {
              if (!urls.contains(url))
                urls.add(url);
            }
          }

          notifyListeners(KeyboardEventHandler.EventType.KEYBOARD_DOWNLOAD_STARTED, 0);

          ret = 1;
          int result = 0;
          for (String url : urls) {
            String directory = KMDefault_AssetPackages + File.separator + pkgID;
            File dir = new File(directory);
            String filename = "";
            if (url.endsWith(".js")) {

              int start = kbFilename.lastIndexOf("/");
              if (start < 0) {
                start = 0;
              } else {
                start++;
              }
              if (!kbFilename.contains("-")) {
                filename = kbFilename.substring(start, kbFilename.length() - 3) + "-" + kbVersion + ".js";
              } else {
                filename = kbFilename.substring(start);
              }
            }

            result = FileDownloader.download(context, url, directory, filename);
            if (result < 0) {
              ret = -1;
              break;
            }
          }
        } catch (Exception e) {
          ret = -1;
          Log.e("Keyboard download", "Error: " + e);
        }

        return ret;
      }

      @Override
      protected void onProgressUpdate(Integer... progress) {
        // Do nothing
      }

      @Override
      protected void onPostExecute(Integer result) {
        if (showProgressDialog) {
          if (progressDialog != null && progressDialog.isShowing()) {
            try {
              progressDialog.dismiss();
              progressDialog = null;
            } catch (Exception e) {
              progressDialog = null;
            }
          }
        }

        notifyListeners(KeyboardEventHandler.EventType.KEYBOARD_DOWNLOAD_FINISHED, result);
        super.onPostExecute(result);
      }

      /**
       * Notify listeners when an event happens
       * @param eventType
       * @param result
       */
      protected void notifyListeners(KeyboardEventHandler.EventType eventType, int result) {
        if (kbDownloadEventListeners != null) {
          HashMap<String, String> keyboardInfo = new HashMap<String, String>();
          keyboardInfo.put(KMManager.KMKey_PackageID, pkgID);
          keyboardInfo.put(KMManager.KMKey_KeyboardID, kbID);
          keyboardInfo.put(KMManager.KMKey_LanguageID, langID);
          keyboardInfo.put(KMManager.KMKey_KeyboardName, kbName);
          keyboardInfo.put(KMManager.KMKey_LanguageName, langName);
          keyboardInfo.put(KMManager.KMKey_KeyboardVersion, kbVersion);
          keyboardInfo.put(KMManager.KMKey_CustomKeyboard, kbIsCustom);
          keyboardInfo.put(KMManager.KMKey_Font, font);
          if (oskFont != null)
            keyboardInfo.put(KMManager.KMKey_OskFont, oskFont);
          KeyboardEventHandler.notifyListeners(kbDownloadEventListeners, eventType, keyboardInfo, result);
        }
      }
    }.execute();
  }

  public static boolean isCustom(String u) {
    boolean ret = false;
    if (u != null && !u.contains(KMKeyboardDownloaderActivity.kKeymanApiBaseURL) &&
      !u.contains(KMKeyboardDownloaderActivity.kKeymanApiRemoteURL)) {
      ret = true;
    }
    return ret;
  }

  private static ArrayList<String> fontUrls(JSONObject jsonFont, String baseUri, boolean isOskFont) {
    if (jsonFont == null)
      return null;

    ArrayList<String> urls = new ArrayList<String>();
    JSONArray fontSource = jsonFont.optJSONArray(KMManager.KMKey_FontSource);
    if (fontSource != null) {
      int fcCount = fontSource.length();
      for (int i = 0; i < fcCount; i++) {
        String fontSourceString;
        try {
          fontSourceString = fontSource.getString(i);
          if (fontSourceString.endsWith(".ttf") || fontSourceString.endsWith(".otf")) {
            urls.add(baseUri + fontSourceString);
          } else if (isOskFont && (fontSourceString.endsWith(".svg") || fontSourceString.endsWith(".woff"))) {
            urls.add(baseUri + fontSourceString);
          } else if (isOskFont && fontSourceString.contains(".svg#")) {
            String fontFilename = fontSourceString.substring(0, fontSourceString.indexOf(".svg#") + 5);
            urls.add(baseUri + fontFilename);
          }
        } catch (JSONException e) {
          return null;
        }
      }
    } else {
      String fontSourceString;
      try {
        fontSourceString = jsonFont.getString(KMManager.KMKey_FontSource);
        if (fontSourceString.endsWith(".ttf") || fontSourceString.endsWith(".otf")) {
          urls.add(baseUri + fontSourceString);
        } else if (isOskFont && (fontSourceString.endsWith(".svg") || fontSourceString.endsWith(".woff"))) {
          urls.add(baseUri + fontSourceString);
        } else if (isOskFont && fontSourceString.contains(".svg#")) {
          String fontFilename = fontSourceString.substring(0, fontSourceString.indexOf(".svg#") + 5);
          urls.add(baseUri + fontFilename);
        }
      } catch (JSONException e) {
        return null;
      }
    }

    return urls;
  }

  public static void addKeyboardDownloadEventListener(KeyboardEventHandler.OnKeyboardDownloadEventListener listener) {
    if (kbDownloadEventListeners == null) {
      kbDownloadEventListeners = new ArrayList<KeyboardEventHandler.OnKeyboardDownloadEventListener>();
    }

    if (listener != null && !kbDownloadEventListeners.contains(listener)) {
      kbDownloadEventListeners.add(listener);
    }
  }

  public static void removeKeyboardDownloadEventListener(KeyboardEventHandler.OnKeyboardDownloadEventListener listener) {
    if (kbDownloadEventListeners != null) {
      kbDownloadEventListeners.remove(listener);
    }
  }
}
