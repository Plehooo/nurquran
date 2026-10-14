# Termux run and GitHub Actions guide

This guide is designed for Android/Termux. It does not require a PC. It does require a working internet connection when installing packages, resolving Gradle plugins, or creating a remote GitHub repository.

## 1. Extract the source ZIP

Download `NurQuranUltimate_Source_v1.0.0.zip` to the phone's Downloads folder. In Termux:

```sh
termux-setup-storage
pkg update
pkg install git gh python openjdk-17 gradle unzip coreutils -y
cd ~
unzip -o ~/storage/downloads/NurQuranUltimate_Source_v1.0.0.zip
cd ~/NurQuranUltimate
```

If the downloaded ZIP has a different filename, change the unzip path to the actual file name. `gradlew` in this ZIP is intentionally a bootstrap shim; create the standard Gradle wrapper after Gradle is installed:

```sh
gradle wrapper --gradle-version 8.13 --gradle-distribution-sha256-sum 20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78 --distribution-type bin
chmod +x gradlew
./gradlew --version
```

The project also needs Android SDK platform/build-tools API 36. Installing the complete Android SDK within Termux can be difficult, so GitHub Actions is the recommended build route below. A locally installed Gradle by itself does **not** imply that an APK can be built.

## 2. Upload the project to a GitHub repository

First authenticate through GitHub CLI and follow its interactive browser/device instructions:

```sh
gh auth login
gh auth status
```

Then, from the project directory, either push to a new private repository (choose another name if it already exists) or add your own remote repository:

```sh
git init
git add .
git commit -m "Initial NurQuran Ultimate source"
gh repo create nurquran-ultimate --private --source . --remote origin --push
```

If `gh repo create` reports the repository already exists, use the URL of your own repository as `origin` rather than repeatedly creating a repository. Check `git status` before each push; never add a `.jks`, `.keystore`, password, `local.properties`, or decoded secret file.

## 3. Trigger the Android CI build

On GitHub, open the repository's **Actions** tab and select **Android CI**. It runs automatically on a push to `main`/`master` or a pull request to those branches, and can also be started with **Run workflow**.

The `verify` job generates a standard Gradle wrapper in the CI workspace, validates it, installs Android SDK API 36, and runs tests, lint, and a debug build. When the run completes, open its run page and the **Artifacts** section. The expected artifact is `NurQuran-debug-and-reports`. A workflow file in the repository alone does not prove that the build passed; inspect the job result and logs.

## 4. Optional signed release

Do not commit the signing key. On your phone, generate a release key only in a private working directory and keep a secure backup:

```sh
mkdir -p ~/private-signing && cd ~/private-signing
keytool -genkeypair -v -keystore nurquran-release.jks -alias nurquran -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 nurquran-release.jks > nurquran-release.b64
```

Copy the **base64 text** privately into a repository secret named `ANDROID_KEYSTORE_BASE64`. Configure the matching alias and passwords as the secrets `ANDROID_KEY_ALIAS`, `ANDROID_KEYSTORE_PASSWORD`, and `ANDROID_KEY_PASSWORD` under **Settings → Secrets and variables → Actions**. Never paste secrets into this chat, a source file, a public issue, or a commit. Remove temporary base64 text from shared storage after the secret is saved, but keep a protected keystore backup.

The optional `signed-release` job runs only for non-pull-request events and only if all four secrets are present. It verifies the generated APK signature and attaches a SHA-256 checksum to `NurQuran-release-signed-apk`. If the secrets are absent, that job skips release signing by design.

## 5. Report a failure usefully

If CI fails, open the failed step and copy only the error section with secrets, usernames, tokens, and private URLs removed. Do not declare the app built until `assembleDebug` passes; do not declare it signed until the signed artifact exists and the signature-verification step passes.
