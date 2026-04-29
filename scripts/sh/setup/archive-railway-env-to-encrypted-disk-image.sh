#!/usr/bin/env bash
# Archive Railway production variables plus local secret files into one encrypted
# macOS disk image file.
#
# The script never prints secret values. It creates/uses an AES-256 encrypted APFS
# sparsebundle named `Secrets.sparsebundle`, mounts/reuses it as `Secrets`, and
# writes a single combined archive file named `single-tenant-template` inside the
# volume. The volume is intentionally left mounted after a successful archive.
set -euo pipefail

umask 077

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
DEFAULT_SECRETS_DIR="$HOME/Library/Application Support/single-tenant-template/secrets"
DEFAULT_IMAGE_PATH="$DEFAULT_SECRETS_DIR/Secrets.sparsebundle"
DEFAULT_MOUNT_POINT="$DEFAULT_SECRETS_DIR/Secrets-mounted"

IMAGE_PATH="$DEFAULT_IMAGE_PATH"
MOUNT_POINT="$DEFAULT_MOUNT_POINT"
ARCHIVE_NAME="single-tenant-template"
VOLUME_NAME="Secrets"
IMAGE_SIZE="200m"
ENV_FILE="$REPO_ROOT/.env"
SECRETS_EDN_FILE="$REPO_ROOT/config/.secrets.edn"
RAILWAY_VARS_FILE=""
RAILWAY_SERVICE="single-tenant-template"
RAILWAY_ENVIRONMENT="production"
OVERWRITE=false
PASSPHRASE_STDIN=false
PASSPHRASE_GUI=false
DEVICE_NODE=""
ATTACHED=false
PASSPHRASE=""

usage() {
  cat <<'EOF'
Archive Railway variables plus local secret files into one encrypted macOS sparsebundle.

By default this creates/uses:
  image:   ~/Library/Application Support/single-tenant-template/secrets/Secrets.sparsebundle
  volume:  Secrets
  file:    single-tenant-template

Sources included in the combined archive file:
  1. Railway production variables (`railway variable list --kv`)
  2. project .env
  3. project config/.secrets.edn

If the encrypted disk is already mounted, the script reuses the mounted volume
and does not ask for the passphrase again. After writing, the volume remains
mounted so it stays visible in Finder.

Options:
  --image PATH              Path for the encrypted sparsebundle image
  --mountpoint PATH         Temporary mount point used while writing the archive
  --archive-name NAME       Filename stored inside the encrypted image
                            default: single-tenant-template
  --volume-name NAME        APFS volume name shown by macOS
                            default: Secrets
  --size SIZE               Sparsebundle capacity (e.g. 200m, 1g)
                            default: 200m
  --env-file PATH           Local .env path
                            default: project .env
  --secrets-file PATH       Local config/.secrets.edn path
                            default: project config/.secrets.edn
  --railway-vars-file PATH  Use an existing Railway variables export instead of calling Railway CLI
  --railway-service NAME    Railway service for variable export
                            default: single-tenant-template
  --railway-environment ENV Railway environment for variable export
                            default: production
  --overwrite               Replace an existing archive file inside the encrypted image
  --passphrase-stdin        Read the image passphrase from stdin
                            New image: provide passphrase twice on separate lines
                            Existing image: provide passphrase once
  --gui-passphrase          Prompt for passphrase with a macOS GUI dialog, including Show/Hide
  -h, --help                Show this help

Examples:
  ./scripts/sh/setup/archive-railway-env-to-encrypted-disk-image.sh --gui-passphrase --overwrite

  bb archive-secrets

Security notes:
  - Secret values are written only inside the mounted encrypted image.
  - The script does not print secret values.
  - Keep the sparsebundle outside the repo and remember the passphrase safely.
  - Eject the Secrets volume manually in Finder when you are done.
EOF
}

die() {
  echo "❌ $*" >&2
  exit 1
}

cleanup() {
  unset PASSPHRASE
}

trap cleanup EXIT

while [[ $# -gt 0 ]]; do
  case "$1" in
    --image)
      [[ $# -ge 2 ]] || die "--image requires a path"
      IMAGE_PATH="$2"
      shift 2
      ;;
    --mountpoint)
      [[ $# -ge 2 ]] || die "--mountpoint requires a path"
      MOUNT_POINT="$2"
      shift 2
      ;;
    --archive-name)
      [[ $# -ge 2 ]] || die "--archive-name requires a name"
      ARCHIVE_NAME="$2"
      shift 2
      ;;
    --volume-name)
      [[ $# -ge 2 ]] || die "--volume-name requires a name"
      VOLUME_NAME="$2"
      shift 2
      ;;
    --size)
      [[ $# -ge 2 ]] || die "--size requires a value"
      IMAGE_SIZE="$2"
      shift 2
      ;;
    --env-file)
      [[ $# -ge 2 ]] || die "--env-file requires a path"
      ENV_FILE="$2"
      shift 2
      ;;
    --secrets-file)
      [[ $# -ge 2 ]] || die "--secrets-file requires a path"
      SECRETS_EDN_FILE="$2"
      shift 2
      ;;
    --railway-vars-file)
      [[ $# -ge 2 ]] || die "--railway-vars-file requires a path"
      RAILWAY_VARS_FILE="$2"
      shift 2
      ;;
    --railway-service)
      [[ $# -ge 2 ]] || die "--railway-service requires a name"
      RAILWAY_SERVICE="$2"
      shift 2
      ;;
    --railway-environment)
      [[ $# -ge 2 ]] || die "--railway-environment requires a name"
      RAILWAY_ENVIRONMENT="$2"
      shift 2
      ;;
    --overwrite)
      OVERWRITE=true
      shift
      ;;
    --passphrase-stdin)
      PASSPHRASE_STDIN=true
      shift
      ;;
    --gui-passphrase)
      PASSPHRASE_GUI=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

command -v hdiutil >/dev/null 2>&1 || die "hdiutil is required on macOS."
command -v plutil >/dev/null 2>&1 || die "plutil is required on macOS."

if [[ "$PASSPHRASE_GUI" == "true" ]]; then
  command -v osascript >/dev/null 2>&1 || die "osascript is required for --gui-passphrase."
fi

if [[ -n "$RAILWAY_VARS_FILE" ]]; then
  [[ -f "$RAILWAY_VARS_FILE" ]] || die "Railway variables export not found: $RAILWAY_VARS_FILE"
else
  command -v railway >/dev/null 2>&1 || die "Railway CLI is required unless --railway-vars-file is provided."
fi

mkdir -p "$(dirname "$IMAGE_PATH")"
chmod 700 "$(dirname "$IMAGE_PATH")" >/dev/null 2>&1 || true
mkdir -p "$MOUNT_POINT"

is_mount_point() {
  local path="$1"

  mount | grep -F " on $path (" >/dev/null 2>&1
}

detect_existing_mount() {
  local finder_mount="/Volumes/$VOLUME_NAME"

  if is_mount_point "$MOUNT_POINT"; then
    echo "📂 Reusing already mounted volume: $MOUNT_POINT"
    return 0
  fi

  if is_mount_point "$finder_mount"; then
    MOUNT_POINT="$finder_mount"
    echo "📂 Reusing already mounted volume: $MOUNT_POINT"
    return 0
  fi

  return 1
}

prompt_passphrase() {
  local image_exists="$1"
  local first=""
  local confirm=""

  prompt_gui_secret() {
    local prompt_text="$1"
    local response=""

    if ! response="$(OSASCRIPT_PROMPT="$prompt_text" osascript <<'APPLESCRIPT'
set promptText to system attribute "OSASCRIPT_PROMPT"
set answerText to ""
set hiddenMode to true

try
  repeat
    if hiddenMode then
      set toggleLabel to "Show"
      set dialogResult to display dialog promptText default answer answerText buttons {"Cancel", toggleLabel, "OK"} default button "OK" cancel button "Cancel" with title "Encrypted Secrets Archive" with hidden answer
    else
      set toggleLabel to "Hide"
      set dialogResult to display dialog promptText default answer answerText buttons {"Cancel", toggleLabel, "OK"} default button "OK" cancel button "Cancel" with title "Encrypted Secrets Archive"
    end if

    set answerText to text returned of dialogResult

    if button returned of dialogResult is "OK" then
      exit repeat
    end if

    set hiddenMode to not hiddenMode
  end repeat
on error number -128
  error "User cancelled passphrase prompt" number -128
end try

return answerText
APPLESCRIPT
    )"; then
      die "Passphrase prompt was cancelled or failed"
    fi

    printf '%s' "$response"
  }

  if [[ "$PASSPHRASE_STDIN" == "true" ]]; then
    if ! IFS= read -r first; then
      [[ -n "$first" ]] || die "Failed to read passphrase from stdin"
    fi
    if [[ "$image_exists" != "true" ]]; then
      if ! IFS= read -r confirm; then
        [[ -n "$confirm" ]] || die "Failed to read passphrase confirmation from stdin"
      fi
    fi
  elif [[ "$PASSPHRASE_GUI" == "true" ]]; then
    first="$(prompt_gui_secret "Enter the encrypted disk image passphrase:")"
    if [[ "$image_exists" != "true" ]]; then
      confirm="$(prompt_gui_secret "Confirm the encrypted disk image passphrase:")"
    fi
  else
    read -r -s -p "Enter encrypted disk image passphrase: " first
    echo
    if [[ "$image_exists" != "true" ]]; then
      read -r -s -p "Confirm passphrase: " confirm
      echo
    fi
  fi

  [[ -n "$first" ]] || die "Passphrase cannot be empty"

  if [[ "$image_exists" != "true" ]]; then
    [[ "$first" == "$confirm" ]] || die "Passphrase confirmation does not match"
  fi

  PASSPHRASE="$first"
}

create_image() {
  echo "💿 Creating encrypted sparsebundle at: $IMAGE_PATH"
  printf '%s' "$PASSPHRASE" | hdiutil create \
    -size "$IMAGE_SIZE" \
    -type SPARSEBUNDLE \
    -fs APFS \
    -volname "$VOLUME_NAME" \
    -encryption AES-256 \
    -stdinpass \
    "$IMAGE_PATH" \
    >/dev/null

  chmod 700 "$IMAGE_PATH" >/dev/null 2>&1 || true
}

verify_existing_image() {
  hdiutil isencrypted "$IMAGE_PATH" >/dev/null 2>&1 || die "Existing image is not encrypted: $IMAGE_PATH"
}

attach_image() {
  local attach_plist

  if detect_existing_mount; then
    return 0
  fi

  attach_plist="$(printf '%s' "$PASSPHRASE" | hdiutil attach \
    "$IMAGE_PATH" \
    -stdinpass \
    -mountpoint "$MOUNT_POINT" \
    -noverify \
    -noautofsck \
    -noautoopen \
    -plist)"

  DEVICE_NODE="$(printf '%s' "$attach_plist" | plutil -extract system-entities.0.dev-entry raw -o - - 2>/dev/null || true)"
  [[ -n "$DEVICE_NODE" ]] || die "Failed to determine attached device node"
  ATTACHED=true
}

append_header() {
  local target_file="$1"
  local title="$2"

  {
    printf '\n'
    printf '================================================================================\n'
    printf '%s\n' "$title"
    printf '================================================================================\n'
  } >> "$target_file"
}

append_file_section() {
  local target_file="$1"
  local title="$2"
  local source_file="$3"

  append_header "$target_file" "$title"
  if [[ -f "$source_file" ]]; then
    cat "$source_file" >> "$target_file"
    printf '\n' >> "$target_file"
  else
    printf 'NOT FOUND: %s\n' "$source_file" >> "$target_file"
  fi
}

append_railway_section() {
  local target_file="$1"

  append_header "$target_file" "Railway variables: service=$RAILWAY_SERVICE environment=$RAILWAY_ENVIRONMENT"
  if [[ -n "$RAILWAY_VARS_FILE" ]]; then
    cat "$RAILWAY_VARS_FILE" >> "$target_file"
    printf '\n' >> "$target_file"
  else
    if ! RAILWAY_CLI_NO_UPDATE_CHECK=1 railway variable list \
      --service "$RAILWAY_SERVICE" \
      --environment "$RAILWAY_ENVIRONMENT" \
      --kv >> "$target_file" 2>/dev/null; then
      die "Failed to export Railway variables. Check Railway login/link status."
    fi
    printf '\n' >> "$target_file"
  fi
}

write_archive() {
  local target_file="$MOUNT_POINT/$ARCHIVE_NAME"
  local temp_file="$target_file.tmp.$$"

  if [[ -e "$target_file" && "$OVERWRITE" != "true" ]]; then
    die "Archive file already exists inside the encrypted image: $target_file (use --overwrite to replace it)"
  fi

  echo "📦 Writing combined secrets archive as: $ARCHIVE_NAME"

  {
    printf '# single-tenant-template secrets archive\n'
    printf '# Created: %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    printf '# This file is intentionally stored inside an encrypted local disk image.\n'
  } > "$temp_file"

  append_railway_section "$temp_file"
  append_file_section "$temp_file" "Project .env: $ENV_FILE" "$ENV_FILE"
  append_file_section "$temp_file" "Project config/.secrets.edn: $SECRETS_EDN_FILE" "$SECRETS_EDN_FILE"

  chmod 600 "$temp_file"
  mv "$temp_file" "$target_file"
  test -s "$target_file" || die "Archive file was not written correctly"
}

if [[ -e "$IMAGE_PATH" ]]; then
  echo "🔐 Using existing encrypted image: $IMAGE_PATH"
  if detect_existing_mount; then
    :
  else
    verify_existing_image
    prompt_passphrase true
    attach_image
  fi
else
  prompt_passphrase false
  create_image
  attach_image
fi

write_archive

echo "✅ Wrote combined secrets archive inside encrypted image: $IMAGE_PATH"
echo "   Volume name: $VOLUME_NAME"
echo "   Archive file: $ARCHIVE_NAME"
echo "   Mounted at: $MOUNT_POINT"
echo "💡 The Secrets volume is still mounted. Eject it in Finder when you are done."
