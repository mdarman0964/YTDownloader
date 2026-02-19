# YT Downloader

A powerful Android video downloader app built with Kotlin and Jetpack Compose, using yt-dlp for downloading videos from YouTube and other supported sites.

## Features

- **Video & Audio Downloads**: Download videos or extract audio only
- **Quality Selection**: Choose from multiple video and audio quality options
- **Download History**: View and manage all your downloads
- **Share Intent Support**: Share YouTube links directly to the app
- **YT-DLP Integration**: Uses yt-dlp for reliable and fast downloads
- **Background Downloads**: Downloads continue even when app is in background
- **Auto-Update**: Option to automatically update yt-dlp

## Screenshots

*Coming soon*

## Download

Get the latest APK from the [Releases](https://github.com/yourusername/YTDownloader/releases) page.

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or newer
- Android SDK 34

### Build Locally

```bash
# Clone the repository
git clone https://github.com/yourusername/YTDownloader.git
cd YTDownloader

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

The APK will be located at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

### GitHub Actions Build

This project includes a GitHub Actions workflow that automatically builds the APK on every push to main/master branch. You can download the built APK from the Actions artifacts.

## Usage

1. **Paste URL**: Enter a YouTube video URL in the input field
2. **Fetch Info**: Tap "Fetch Info" to get video details
3. **Select Quality**: Choose your preferred video/audio quality
4. **Download**: Tap "Download" to start the download

### Share to Download

1. Open YouTube app
2. Tap Share on any video
3. Select "YT Downloader"
4. The app will open with the URL pre-filled

## Supported Sites

- YouTube (youtube.com, youtu.be)
- YouTube Music (music.youtube.com)

## Permissions

- `INTERNET`: Required for downloading videos
- `WRITE_EXTERNAL_STORAGE`: Required for saving downloads (Android 9 and below)
- `POST_NOTIFICATIONS`: Required for download notifications (Android 13+)
- `FOREGROUND_SERVICE`: Required for background downloads

## Architecture

- **UI Layer**: Jetpack Compose with Material Design 3
- **ViewModel**: State management with Kotlin Flow
- **Data Layer**: Room Database for download history
- **Background**: Foreground Service for downloads
- **Settings**: DataStore for user preferences

## Libraries Used

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [Room](https://developer.android.com/training/data-storage/room) - Local database
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - Preferences storage
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - Background work
- [Coil](https://coil-kt.github.io/coil/) - Image loading
- [yt-dlp](https://github.com/yt-dlp/yt-dlp) - Video downloader

## License

```
Copyright 2024 YT Downloader

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Disclaimer

This app is for educational purposes only. Please respect copyright laws and only download content you have permission to download.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Acknowledgments

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) - The awesome video downloader
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI
