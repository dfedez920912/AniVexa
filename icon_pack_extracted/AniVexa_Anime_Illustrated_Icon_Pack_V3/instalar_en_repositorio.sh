#!/usr/bin/env bash
set -euo pipefail
if [[ ! -f app/build.gradle.kts ]]; then
  echo 'ERROR: ejecuta desde la raíz del proyecto AniVexa.' >&2
  exit 1
fi
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p app/src/main/res/drawable-nodpi
cp "$HERE"/android/app/src/main/res/drawable-nodpi/*.png app/src/main/res/drawable-nodpi/
# Overlay Kotlin optional, with changes already prepared in v2
if [[ -d "$HERE/integracion_kotlin/overlay/app/src/main/java" ]]; then
  cp -R "$HERE/integracion_kotlin/overlay/app/src/main/java/." app/src/main/java/
fi
echo 'Instalado. Compila y verifica las pantallas antes de publicar.'
