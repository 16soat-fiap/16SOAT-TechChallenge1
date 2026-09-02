#!/usr/bin/env bash

set -e

VERSION="2.336.0"
DIR="$HOME/actions-runner"
FILE="actions-runner-linux-x64-${VERSION}.tar.gz"

echo "==> Criando diretório..."
mkdir -p "$DIR"
cd "$DIR"

echo "==> Baixando GitHub Actions Runner ${VERSION}..."
curl -L -o "$FILE" \
  "https://github.com/actions/runner/releases/download/v${VERSION}/${FILE}"

echo "==> Extraindo..."
tar xzf "$FILE"

rm -f "$FILE"

echo
echo "========================================"
echo " GitHub Actions Runner instalado"
echo "========================================"
echo
echo "Diretório: $DIR"
echo
echo "Próximo passo:"
echo
echo "cd $DIR"
echo "./config.sh --url https://github.com/16soat-fiap/16SOAT-TechChallenge1 --token SEU_TOKEN"
echo
