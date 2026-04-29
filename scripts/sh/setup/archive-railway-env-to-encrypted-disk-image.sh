#!/usr/bin/env bash
# Manage an encrypted local secrets vault on macOS.
#
# The script creates/uses an AES-256 encrypted APFS sparsebundle named
# `Secrets.sparsebundle`, mounts/reuses it as `Secrets`, ensures two git repos
# inside the mounted volume (`Projects` and `General`), and can either:
#   - write the current project's secret snapshot into `Projects/<project-name>/`
#   - commit manual changes inside `General`
#
# The volume is intentionally left mounted after a successful run.
set -euo pipefail

umask 077

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
DEFAULT_PROJECT_NAME="$(basename "$REPO_ROOT")"
DEFAULT_SECRETS_DIR="$HOME/Library/Application Support/single-tenant-template/secrets"
DEFAULT_IMAGE_PATH="$DEFAULT_SECRETS_DIR/Secrets.sparsebundle"
DEFAULT_MOUNT_POINT="$DEFAULT_SECRETS_DIR/Secrets-mounted"

IMAGE_PATH="$DEFAULT_IMAGE_PATH"
MOUNT_POINT="$DEFAULT_MOUNT_POINT"
VOLUME_NAME="Secrets"
IMAGE_SIZE="200m"
ENV_FILE="$REPO_ROOT/.env"
SECRETS_EDN_FILE="$REPO_ROOT/config/.secrets.edn"
RAILWAY_VARS_FILE=""
RAILWAY_SERVICE="$DEFAULT_PROJECT_NAME"
RAILWAY_ENVIRONMENT="production"
OVERWRITE=false
PASSPHRASE_STDIN=false
PASSPHRASE_GUI=false
INTERACTIVE=false
COMMIT_ONLY=false
PASSPHRASE=""
SCOPE=""
PROJECT_NAME=""
COMMIT_MESSAGE=""
PROJECTS_DIR=""
GENERAL_DIR=""
SELECTED_REPO_PATH=""
SELECTED_CONTENT_PATH=""
SELECTED_LABEL=""
COMMIT_RESULT=""

usage() {
  cat <<'EOF'
Manage the encrypted local secrets vault stored in a macOS sparsebundle.

By default this creates/uses:
  image:   ~/Library/Application Support/single-tenant-template/secrets/Secrets.sparsebundle
  volume:  Secrets
  repos:   Projects/ and General/ inside the mounted volume

What it does:
  - Projects mode: stores Railway vars + project .env + config/.secrets.edn in
    Projects/<project-name>/ and commits the Projects git repo.
  - General mode: commits manual changes already made inside the General git repo.

If the encrypted disk is already mounted, the script reuses the mounted volume
and does not ask for the passphrase again. After writing, the volume remains
mounted so it stays visible in Finder.

Options:
  --projects                Target the Projects repo (default when non-interactive)
  --general                 Target the General repo
  --project-name NAME       Project folder name under Projects/
  --commit-message MESSAGE  Git commit message to use
  --commit-only             Skip writing project files; only stage/commit existing changes
  --interactive             Prompt for scope and project name interactively
  --image PATH              Path for the encrypted sparsebundle image
  --mountpoint PATH         Mount point used while the encrypted volume is attached
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
                            default: current repo folder name
  --railway-environment ENV Railway environment for variable export
                            default: production
  --overwrite               Replace existing target files for Projects mode
  --passphrase-stdin        Read the image passphrase from stdin
                            New image: provide passphrase twice on separate lines
                            Existing image: provide passphrase once
  --gui-passphrase          Prompt for passphrase with a macOS GUI dialog, including Show/Hide
  -h, --help                Show this help

Examples:
  bb archive-secrets
  bb archive-secrets --projects --project-name single-tenant-template
  bb archive-secrets --general
  bb secrets-commit-projects
  bb secrets-commit-general "2026-04-29 12:30 UTC"

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

validate_folder_name() {
  local name="$1"

  [[ -n "$name" ]] || return 1
  [[ "$name" != "." ]] || return 1
  [[ "$name" != ".." ]] || return 1
  [[ "$name" != */* ]] || return 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --projects)
      SCOPE="projects"
      shift
      ;;
    --general)
      SCOPE="general"
      shift
      ;;
    --project-name)
      [[ $# -ge 2 ]] || die "--project-name requires a value"
      PROJECT_NAME="$2"
      shift 2
      ;;
    --commit-message)
      [[ $# -ge 2 ]] || die "--commit-message requires a value"
      COMMIT_MESSAGE="$2"
      shift 2
      ;;
    --commit-only)
      COMMIT_ONLY=true
      shift
      ;;
    --interactive)
      INTERACTIVE=true
      shift
      ;;
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

command -v git >/dev/null 2>&1 || die "git is required."
command -v hdiutil >/dev/null 2>&1 || die "hdiutil is required on macOS."
command -v plutil >/dev/null 2>&1 || die "plutil is required on macOS."

if [[ "$PASSPHRASE_GUI" == "true" ]]; then
  command -v osascript >/dev/null 2>&1 || die "osascript is required for --gui-passphrase."
fi

if [[ -n "$RAILWAY_VARS_FILE" ]]; then
  [[ -f "$RAILWAY_VARS_FILE" ]] || die "Railway variables export not found: $RAILWAY_VARS_FILE"
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
  local device_node=""

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

  device_node="$(printf '%s' "$attach_plist" | plutil -extract system-entities.0.dev-entry raw -o - - 2>/dev/null || true)"
  [[ -n "$device_node" ]] || die "Failed to determine attached device node"
}

ensure_git_repo() {
  local repo_path="$1"

  mkdir -p "$repo_path"

  if [[ ! -d "$repo_path/.git" ]]; then
    if ! git -C "$repo_path" init -b main >/dev/null 2>&1; then
      git -C "$repo_path" init >/dev/null 2>&1
      git -C "$repo_path" symbolic-ref HEAD refs/heads/main >/dev/null 2>&1 || true
    fi
  fi

  if ! git -C "$repo_path" config --local --get user.name >/dev/null 2>&1; then
    git -C "$repo_path" config --local user.name "Local Secrets"
  fi

  if ! git -C "$repo_path" config --local --get user.email >/dev/null 2>&1; then
    git -C "$repo_path" config --local user.email "local-secrets@localhost"
  fi

  if ! grep -Fqx '.DS_Store' "$repo_path/.git/info/exclude" 2>/dev/null; then
    printf '.DS_Store\n' >> "$repo_path/.git/info/exclude"
  fi
}

ensure_layout() {
  PROJECTS_DIR="$MOUNT_POINT/Projects"
  GENERAL_DIR="$MOUNT_POINT/General"

  ensure_git_repo "$PROJECTS_DIR"
  ensure_git_repo "$GENERAL_DIR"
}

is_tty() {
  [[ -t 0 && -t 1 ]]
}

should_prompt() {
  [[ "$INTERACTIVE" == "true" ]] || is_tty
}

prompt_scope() {
  local answer=""

  echo "Choose secrets target:"
  echo "  1) Projects (default)"
  echo "  2) General"
  read -r -p "Selection [1]: " answer

  case "$answer" in
    ""|1|projects|Projects)
      SCOPE="projects"
      ;;
    2|general|General)
      SCOPE="general"
      ;;
    *)
      die "Unsupported selection: $answer"
      ;;
  esac
}

collect_existing_projects() {
  local path=""
  EXISTING_PROJECTS=()

  if [[ -d "$PROJECTS_DIR" ]]; then
    while IFS= read -r path; do
      [[ -n "$path" ]] || continue
      EXISTING_PROJECTS+=("$(basename "$path")")
    done < <(find "$PROJECTS_DIR" -mindepth 1 -maxdepth 1 -type d ! -name '.*' | sort)
  fi
}

prompt_project_name() {
  local answer=""
  local default_name="$DEFAULT_PROJECT_NAME"
  local index=1
  local total=0

  collect_existing_projects

  echo "Available project folders under Projects/:"
  if [[ ${#EXISTING_PROJECTS[@]} -eq 0 ]]; then
    echo "  (none yet)"
  else
    for project in "${EXISTING_PROJECTS[@]}"; do
      echo "  $index) $project"
      index=$((index + 1))
    done
  fi
  echo "Press Enter for default: $default_name"
  read -r -p "Project name [$default_name]: " answer

  if [[ -z "$answer" ]]; then
    PROJECT_NAME="$default_name"
    return 0
  fi

  if [[ "$answer" =~ ^[0-9]+$ ]]; then
    total=${#EXISTING_PROJECTS[@]}
    if (( answer >= 1 && answer <= total )); then
      PROJECT_NAME="${EXISTING_PROJECTS[$((answer - 1))]}"
      return 0
    fi
    die "Project selection out of range: $answer"
  fi

  PROJECT_NAME="$answer"
}

resolve_scope_and_project() {
  if [[ -z "$SCOPE" ]]; then
    if should_prompt; then
      prompt_scope
    else
      SCOPE="projects"
    fi
  fi

  case "$SCOPE" in
    projects)
      if [[ -z "$PROJECT_NAME" ]]; then
        if should_prompt; then
          prompt_project_name
        else
          PROJECT_NAME="$DEFAULT_PROJECT_NAME"
        fi
      fi

      validate_folder_name "$PROJECT_NAME" || die "Invalid project folder name: $PROJECT_NAME"
      SELECTED_REPO_PATH="$PROJECTS_DIR"
      SELECTED_CONTENT_PATH="$PROJECTS_DIR/$PROJECT_NAME"
      SELECTED_LABEL="Projects/$PROJECT_NAME"
      ;;
    general)
      [[ -z "$PROJECT_NAME" ]] || die "--project-name can only be used with --projects"
      SELECTED_REPO_PATH="$GENERAL_DIR"
      SELECTED_CONTENT_PATH="$GENERAL_DIR"
      SELECTED_LABEL="General"
      ;;
    *)
      die "Unknown scope: $SCOPE"
      ;;
  esac
}

default_commit_message() {
  local label="$1"
  printf '%s %s' "$label" "$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
}

ensure_commit_message() {
  if [[ -z "$COMMIT_MESSAGE" ]]; then
    COMMIT_MESSAGE="$(default_commit_message "$SELECTED_LABEL")"
  fi
}

write_text_file() {
  local target_file="$1"
  local temp_file="$target_file.tmp.$$"

  cat > "$temp_file"
  chmod 600 "$temp_file"
  mv "$temp_file" "$target_file"
}

copy_source_file_if_present() {
  local source_file="$1"
  local target_file="$2"
  local label="$3"

  if [[ -f "$source_file" ]]; then
    cat "$source_file" | write_text_file "$target_file"
    echo "   • Saved $label"
  else
    echo "   • Source not found for $label; left any existing stored copy untouched"
  fi
}

write_railway_export_file() {
  local target_file="$1"
  local temp_file="$target_file.tmp.$$"

  if [[ -n "$RAILWAY_VARS_FILE" ]]; then
    cat "$RAILWAY_VARS_FILE" > "$temp_file"
  else
    command -v railway >/dev/null 2>&1 || die "Railway CLI is required unless --railway-vars-file is provided."
    if ! RAILWAY_CLI_NO_UPDATE_CHECK=1 railway variable list \
      --service "$RAILWAY_SERVICE" \
      --environment "$RAILWAY_ENVIRONMENT" \
      --kv > "$temp_file" 2>/dev/null; then
      rm -f "$temp_file"
      die "Failed to export Railway variables. Check Railway login/link status."
    fi
  fi

  chmod 600 "$temp_file"
  mv "$temp_file" "$target_file"
}

ensure_target_writable() {
  local target_dir="$1"
  local existing_path=""

  if [[ "$OVERWRITE" == "true" ]]; then
    return 0
  fi

  for existing_path in "$target_dir/railway-$RAILWAY_ENVIRONMENT.env" "$target_dir/.env" "$target_dir/.secrets.edn"; do
    [[ ! -e "$existing_path" ]] || die "Target already exists: $existing_path (use --overwrite to replace it)"
  done
}

write_project_snapshot() {
  local target_dir="$SELECTED_CONTENT_PATH"

  mkdir -p "$target_dir"
  ensure_target_writable "$target_dir"

  echo "📦 Writing project snapshot into: $SELECTED_LABEL"
  write_railway_export_file "$target_dir/railway-$RAILWAY_ENVIRONMENT.env"
  copy_source_file_if_present "$ENV_FILE" "$target_dir/.env" "project .env"
  copy_source_file_if_present "$SECRETS_EDN_FILE" "$target_dir/.secrets.edn" "config/.secrets.edn"
}

commit_repo_changes() {
  local repo_path="$1"
  local commit_hash=""

  ensure_commit_message
  git -C "$repo_path" add -A

  if git -C "$repo_path" diff --cached --quiet --exit-code; then
    COMMIT_RESULT="no-changes"
    echo "ℹ️ No changes to commit in $SELECTED_LABEL"
    return 0
  fi

  git -C "$repo_path" commit -m "$COMMIT_MESSAGE" >/dev/null
  commit_hash="$(git -C "$repo_path" rev-parse --short HEAD 2>/dev/null || true)"
  COMMIT_RESULT="committed"
  echo "✅ Committed $SELECTED_LABEL changes${commit_hash:+ ($commit_hash)}"
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

ensure_layout
resolve_scope_and_project

if [[ "$SCOPE" == "projects" && "$COMMIT_ONLY" != "true" ]]; then
  write_project_snapshot
elif [[ "$SCOPE" == "general" ]]; then
  echo "📁 General mode selected: committing manual changes inside General"
else
  echo "📁 Commit-only mode selected for $SELECTED_LABEL"
fi

commit_repo_changes "$SELECTED_REPO_PATH"

echo "✅ Secrets vault run finished"
echo "   Image: $IMAGE_PATH"
echo "   Volume name: $VOLUME_NAME"
echo "   Mounted at: $MOUNT_POINT"
echo "   Target: $SELECTED_LABEL"
echo "   Commit status: $COMMIT_RESULT"
echo "💡 The Secrets volume is still mounted. Eject it in Finder when you are done."
