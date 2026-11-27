# RomFeatureHide

LSPosed / Vector module (`com.fix.romfeatures`) running in System Framework (`android`) to hide custom ROM features, permissions, and packages (`org.lineageos.*`, `org.omnirom.*`, `org.evolution.*`, `org.crdroid.*`, `com.libremobileos.*`, `com.android.axion.*`, `io.chaldeaprjkt.*`, etc.) from third-party apps (`uid % 100000 >= 10000`).

## What It Hooks

- `PackageManagerService.hasSystemFeature` & `IPackageManagerImpl.getSystemAvailableFeatures`: hides custom ROM system features.
- `PermissionService.getPermissionInfo`, `queryPermissionsByGroup`, `getAllPermissionGroups` & `IPackageManagerBase.getPackageInfo`: hides custom ROM permissions and permission groups.
- `IPackageManagerBase.getPackagesForUid`: filters custom ROM package names from cross-UID queries (returning `null` instead of an empty array when nothing remains), while preserving self-UID lookups (`uid == Binder.getCallingUid()`) so `system_server` (`ActivityManagerService.finishAttachApplication`) does not crash built-in ROM apps like `org.lineageos.glimpse`, `org.lineageos.audiofx`, `org.lineageos.recorder`, or `org.omnirom.logcat`.

## Build

```bash
./build.sh
```

Requires Android SDK Build-Tools `37.0.0` and `android-37.2` platform (`RomFeatureHide.apk` is signed with a local `debug.keystore` generated on first build).

## Usage

1. Install `RomFeatureHide.apk`.
2. Enable **ROM Feature Hide** in Vector / LSPosed Manager and scope **System Framework** (`android`).
3. Reboot the device for `system_server` hooks to take effect.
