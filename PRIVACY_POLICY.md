# Privacy Policy for AniVexa

*Effective Date: May 2024 / Updated for Xiaomi App Store & Google Play*

AniVexa ("the Application") is an Android application created and maintained by **dfedez920912** (`com.github.dfedez920912.anivexa`).

## 1. Data Collection & Privacy
AniVexa respects your privacy:
- **No Account Required**: You do not need to register or create an account to use AniVexa.
- **Local Data Storage**: All your anime lists, custom ratings, watchlist statuses, and settings are stored locally on your device in a Room SQLite database.
- **No Third-Party Analytics / Tracking**: AniVexa does not include tracking SDKs, advertising SDKs, or third-party telemetry.

## 2. Network Traffic & External Services
- **AniList GraphQL API**: AniVexa fetches publicly available anime schedule data, ratings, and media info directly from the official AniList API via HTTPS (`https://graphql.anilist.co`).
- **Google Drive Backup (Optional)**: If you choose to use the cloud backup feature, backups are encrypted on-device (`.avb`) using AES-GCM before being uploaded directly to your own personal Google Drive. AniVexa only accesses files created by the app in your Drive account.

## 3. Application Permissions
AniVexa requests only essential Android permissions:
- `INTERNET` / `ACCESS_NETWORK_STATE`: Required to fetch anime release schedules and images from AniList over HTTPS.
- `POST_NOTIFICATIONS`: Required on Android 13+ to deliver notifications for upcoming anime episode releases.
- `SCHEDULE_EXACT_ALARM`: Used to schedule precise alarms for episode release reminders set by the user.
- `VIBRATE`: Optional haptic feedback for notifications.

## 4. Children's Privacy
AniVexa does not knowingly collect or solicit personal information from anyone under the age of 13.

## 5. Contact
If you have any questions regarding this Privacy Policy, you can open an issue on GitHub:
https://github.com/dfedez920912/AniVexa
