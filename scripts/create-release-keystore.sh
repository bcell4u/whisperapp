#!/usr/bin/env bash
set -euo pipefail

OUT="${1:-whisper-recorder-release.jks}"
ALIAS="${2:-whisper-recorder}"

if [ -e "$OUT" ]; then
  echo "Refusing to overwrite existing keystore: $OUT" >&2
  exit 1
fi
command -v keytool >/dev/null 2>&1 || {
  echo "keytool was not found. Install a JDK (Java 17 is fine) and retry." >&2
  exit 1
}

read -rsp "Keystore password: " STOREPASS
echo
read -rsp "Key password: " KEYPASS
echo
if [ -z "$STOREPASS" ] || [ -z "$KEYPASS" ]; then
  echo "Passwords cannot be empty." >&2
  exit 1
fi

keytool -genkeypair \
  -keystore "$OUT" \
  -storetype JKS \
  -storepass "$STOREPASS" \
  -keypass "$KEYPASS" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Whisper Recorder, O=Whisper Recorder, C=US"

FINGERPRINT="$(keytool -list -v -keystore "$OUT" -storepass "$STOREPASS" -alias "$ALIAS" | awk -F': ' '/SHA256:/{print $2; exit}')"

cat <<MSG

Created permanent release keystore:
  $OUT
Alias:
  $ALIAS
Certificate SHA-256:
  $FINGERPRINT

KEEP THIS FILE AND BOTH PASSWORDS SAFE. Do not commit the keystore.

GitHub repository secrets to create:
  ANDROID_KEYSTORE_BASE64
  ANDROID_KEYSTORE_PASSWORD
  ANDROID_KEY_ALIAS=$ALIAS
  ANDROID_KEY_PASSWORD
  ANDROID_SIGNING_CERT_SHA256=$FINGERPRINT

Encode the keystore as one-line base64 before pasting ANDROID_KEYSTORE_BASE64.
Linux example:
  base64 -w 0 "$OUT"
macOS example:
  base64 < "$OUT" | tr -d '\n'

See SIGNING.md for the complete setup and first-install instructions.
MSG
