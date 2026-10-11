#!/usr/bin/env bash
# validate_eou.sh - Validate an XML file against an XSD using xmllint (Debian)
# Usage: ./validate_eou.sh [schema.xsd] [file.xml]
#
# needs libxml2-utils: sudo apt install -y libxml2-utils

set -euo pipefail

SCHEMA=${1:-./eou.xsd}
XMLFILE=${2:-./eou.xml}

if ! command -v xmllint >/dev/null 2>&1; then
  echo "ERROR: xmllint not found. Try to install it using these commends:"
  echo "  sudo apt update && sudo apt install -y libxml2-utils"
  exit 2
fi

if [ ! -f "$SCHEMA" ]; then
  echo "ERROR: XSD File'$SCHEMA' not found."
  exit 3
fi

if [ ! -f "$XMLFILE" ]; then
  echo "ERROR: XML-File '$XMLFILE' not found."
  exit 4
fi

echo "Validating '$XMLFILE' against '$SCHEMA' ..."
# xmllint exit code 0 when successful, non-zero when errors occur.; --noout suppresses output, --schema specifies the XSD schema to validate against
if xmllint --noout --schema "$SCHEMA" "$XMLFILE"; then
  echo "VALIDATION SUCCESSFUL: '$XMLFILE' is valid."
  exit 0
else
  echo "VALIDATION FAILED: Error during validation of '$XMLFILE'."
  exit 1
fi
