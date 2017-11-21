//
//  Constants.swift
//  KeymanEngine
//
//  Created by Gabriel Wong on 2017-10-20.
//  Copyright © 2017 SIL International. All rights reserved.
//

public enum Key {
  public static let keyboardId = "kbId"
  public static let languageId = "langId"

  public static let fontFamily = "family"
  public static let fontSource = "source"
  public static let fontFiles = "files"
  // Font filename is deprecated
  public static let fontFilename = "filename"
  public static let keyboardInfo = "keyboardInfo"

  /// Array of user keyboards info list in UserDefaults
  static let userKeyboardsList = "UserKeyboardsList"

  /// Currently/last selected keyboard info in UserDefaults
  static let userCurrentKeyboard = "UserCurrentKeyboard"

  // Internal user defaults keys
  static let engineVersion = "KeymanEngineVersion"
  static let keyboardPickerDisplayed = "KeyboardPickerDisplayed"
  static let synchronizeSWKeyboard = "KeymanSynchronizeSWKeyboard"

  // JSON keys for language REST calls
  static let options = "options"
  static let language = "language"

  // TODO: Check if it matches with the key in Keyman Cloud API
  static let keyboardCopyright = "copyright"
  static let languages = "languages"

  // Other keys
  static let update = "update"
}

public enum Constants {
  private static let defaultFont = Font(family: "LatinWeb", source: ["DejaVuSans.ttf"], size: nil)
  public static let defaultKeyboard = InstallableKeyboard(id: "european2",
                                                          name: "EuroLatin2 Keyboard",
                                                          languageID: "eng",
                                                          languageName: "English",
                                                          version: "1.6",
                                                          isRTL: false,
                                                          font: defaultFont,
                                                          oskFont: nil,
                                                          isCustom: false)

  /// Is the process of a custom keyboard extension.
  public static let isSystemKeyboard: Bool = {
    let infoDict = Bundle.main.infoDictionary
    let extensionInfo = infoDict?["NSExtension"] as? [AnyHashable: Any]
    let extensionID = extensionInfo?["NSExtensionPointIdentifier"] as? String
    return extensionID == "com.apple.keyboard-service"
  }()

  /// The version of the Keyman SDK
  public static let sdkVersion: String = {
    let info = NSDictionary(contentsOfFile: keymanBundle.path(forResource: "KeymanEngine-Info",
                                                              ofType: "plist")!)
    return info!["CFBundleVersion"] as! String
  }()

  /// Keyman Web resources
  public static let keymanBundle: Bundle = {
    let frameworkBundle =  Bundle(identifier: "org.sil.Keyman.ios.Engine")!
    return Bundle(path: frameworkBundle.path(forResource: "Keyman", ofType: "bundle")!)!
  }()
}
