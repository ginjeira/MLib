# Screenshots

Current MLib screenshots are pending a capture pass on the released APK.

The previous `docs/screenshots/*.png` images were inherited from Spotygram and still showed the old identity, Russian labels and pre-MLib controls. They were moved to `legacy/` so they are not presented as current MLib UI.

## Capture checklist

Use the release APK (`pt.ginjeira.mlib`) on an arm64 device or an x86_64 emulator.

- Settings, light and dark
- Library / tracks
- Album detail with completion state
- Playlist detail after repair
- Acquisition search and confirmation
- Update available, downloading and installed states
- Android Auto / media controls, if available

Store new images in this directory with a version prefix, for example `0.1.13-settings-dark.png`, and update `docs/usage.md` only with images that match the current identity.
