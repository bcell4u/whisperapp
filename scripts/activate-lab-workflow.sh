#!/usr/bin/env bash
# One-time migration for an existing GitHub checkout with the old production build.yml.
# Removes ONLY the obsolete production release workflow. Never commits or pushes.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
OLD='.github/workflows/build.yml'
LAB='.github/workflows/qwen-gguf-lab-6.6-autorelease.yml'

if ! git rev-parse --show-toplevel >/dev/null 2>&1; then
  echo 'ERROR: Use a Git clone or GitHub Codespaces checkout; this is not a Git repository.' >&2
  exit 1
fi
if [[ ! -f "$LAB" ]] || [[ ! -f scripts/verify-lab-source.py ]]; then
  echo 'ERROR: 6.6.0 Lab source/workflow missing. Do not remove a production workflow from production source.' >&2
  exit 1
fi
if ! grep -Fq 'applicationId = "com.example.whisperrecorder.qwengguflab"' app/build.gradle.kts; then
  echo 'ERROR: Not the isolated Qwen GGUF Lab. Refusing to edit production workflow.' >&2
  exit 1
fi
if [[ "$(git rev-parse --show-toplevel)" != "$ROOT" ]]; then
  echo 'ERROR: Run this only from the actual Lab repository root.' >&2
  exit 1
fi
if [[ -e "$OLD" || -L "$OLD" ]]; then
  if git ls-files --error-unmatch -- "$OLD" >/dev/null 2>&1; then
    git rm -- "$OLD"
  else
    rm -- "$OLD"
  fi
  echo "REMOVED ONLY: $OLD"
else
  echo "ALREADY ABSENT: $OLD"
fi
python3 scripts/verify-lab-source.py
echo
printf '%s\n' 'Ready to commit. Review:' '  git status --short' 'Then commit and push:' '  git add -- .github/workflows/qwen-gguf-lab-6.6-autorelease.yml scripts/activate-lab-workflow.sh LAB_GITHUB_MIGRATION.md LAB_6_6_AUTORELEASE.md README.md' '  git commit -m "Activate 6.6.0 Lab-only auto releases"' '  git push origin main'
