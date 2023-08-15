# Overview

If you do not want to lose the list of Keyri accounts after deleting the application, you can
configure Backup. This document will help you set up your account backup configuration.

You can find comparison of Key/Value Backup and Auto Backup
here: [Android Backup Overview](https://developer.android.com/guide/topics/data/backup).

## Contents

* [Android Auto Backup](#android-auto-backup)
* [Key/Value Backup (Android Backup Service)](#keyvalue-backup-android-backup-service)
* [Testing](#testing)

### **Android Auto Backup**

Auto Backup for Apps automatically backs up a user's data from apps that target and run on Android
6.0 (API level 23) or higher.

Make sure that backup is not disabled in your application. `allowBackup` should not be false. The
default value is true but to make your intentions clear, we recommend explicitly setting the
attribute in your manifest as shown below:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:allowBackup="true">
        ...
    </application>
</manifest>
```

#### **Control backup on Android 11 and lower**

Follow the steps in this section to include or exclude files that are backed up on devices running
Android 11 (API level 30) or lower.

1. In `AndroidManifest.xml`, add the `android:fullBackupContent` attribute to the `<application>`
   element. This attribute points to an XML file that contains backup rules. For example:

```xml

<application android:fullBackupContent="@xml/backup_rules">
    ...
</application>
```

2. Create an XML file called `@xml/backup_rules` in the `res/xml/` directory. Inside the file, add
   rules with the `<include>` and `<exclude>` elements. The following sample backs up all shared
   preferences except device.xml:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="." />
    <exclude domain="sharedpref" path="device.xml" />
</full-backup-content>
```

If you have more granular control over sharedPref backups, make sure you have include
`keyri_backup_prefs.xml` file.

#### **Control backup on Android 12 or higher**

If your app targets Android 12 (API level 31) or higher, follow the steps in this section to include
or exclude files that are backed up on devices that are running Android 12 or higher.

1. In `AndroidManifest.xml`, add the android:dataExtractionRules attribute to the `<application>`
   element. This attribute points to an XML file that contains backup rules. For example:

```xml

<application android:dataExtractionRules="backup_rules.xml">
    ...
</application>
```

2. Create an XML file called backup_rules.xml in the `res/xml/` directory. Inside the file, add
   rules with the `<include>` and `<exclude>` elements. The following sample backs up all shared
   preferences
   `except device.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="." />
        <exclude domain="sharedpref" path="device.xml" />
    </cloud-backup>
</data-extraction-rules>
```

If you have more granular control over sharedPref backups, make sure you have include
`keyri_backup_prefs.xml` file.

Check for more details
on [Back up user data with Auto Backup](https://developer.android.com/guide/topics/data/autobackup).

### **Key/Value Backup (Android Backup Service)**

Android Backup Service provides cloud storage backup and restore for key-value data in your Android
app. During a key-value backup operation, the app's backup data is passed to the device's backup
transport. If the device is using the default Google backup transport, then the data is passed to
Android Backup Service for archiving. Data is limited to 5MB per user of your app. There is no
charge for storing backup data.

`
Note: Key-value backup requires you to write code to define your backup content explicitly, while Auto Backup requires no code and will back up entire files. Most apps should use Auto Backup to implement backup and restore. Your app can implement Auto Backup or key-value backup, but not both.
`

#### **Implement key-value backup**

To back up your app data, you need to implement a backup agent. Your backup agent is called by the
Backup Manager both during backup and restore.

To implement a backup agent, you must:

1. Declare your backup agent in your manifest file with the android:backupAgent attribute.

2. Define a backup agent by doing one of the following:

    * Extending BackupAgent The BackupAgent class provides the central interface that your app uses
      to communicate with the Backup Manager. If you extend this class directly, you must override
      `onBackup()` and `onRestore()` to handle the backup and restore operations for your data.

    * Extending BackupAgentHelper The BackupAgentHelper class provides a convenient wrapper around
      the `BackupAgent` class, minimizing the amount of code you need to write. In your
      `BackupAgentHelper`, you must use one or more helper objects, which automatically back up and
      restore certain types of data, so that you don't need to implement `onBackup()`
      and `onRestore()`. Unless you need full control over your app's backups, we recommend using
      the `BackupAgentHelper`
      to handle your app's backups.

   Android currently provides backup helpers that will back up and restore complete files from
   SharedPreferences and internal storage.

You can implement your own backup according to the
documentation: [Back up key-value pairs with Android Backup Service](https://developer.android.com/guide/topics/data/keyvaluebackup)
. In this case just include `keyri_backup_prefs` string to your BackupAgentHelper as shown below:

```kotlin
class MyBackupAgent : BackupAgentHelper() {

    override fun onCreate() {
        val helper = SharedPreferencesBackupHelper(this, "your prefs files", "keyri_backup_prefs")

        addHelper("YOUR BACKUP KEY", helper)
    }
}
```

After it just add your `MyBackupAgent` to `AndroidManifest.xml`:

```xml

<manifest>
    ...
    <application android:backupAgent="MyBackupAgent">
        <activity>
            ...
        </activity>
    </application>
</manifest>
```

Or you can just use our [KeyriPrefsBackupAgent](KeyriPrefsBackupAgent.kt) which implements the
necessary logic under the hood as shown below:

```xml

<manifest>
    ...
    <application android:backupAgent="com.keyrico.keyrisdk.backup.KeyriPrefsBackupAgent">
        <activity>
            ...
        </activity>
    </application>
</manifest>
```

### **Testing**

See [Test backup and restore](https://developer.android.com/guide/topics/data/testingbackup) to test
backup of Keyri accounts.
