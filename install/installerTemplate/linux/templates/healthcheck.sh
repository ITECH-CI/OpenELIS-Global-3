#!/bin/bash

#get location of this script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  scriptDir="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
scriptDir="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

# FHIR health = the CapabilityStatement (lightweight, always available once the
# server is up). Cheaper than querying resources (e.g. Task?status=requested).
# Uses localhost so this check is valid inside the FHIR container itself.
if [ $(curl --fail --silent http://localhost:8080/fhir/metadata -o /dev/null -w '%{http_code}' -s) == "200" ]; then
    exit 0;
else
    exit 1;
fi
