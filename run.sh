#!/bin/bash

set -e -o pipefail -o errtrace -o functrace
export script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "${script_dir}"/lib.sh
trap "${script_dir}"/kill.sh EXIT

#downloadDGSource
#downloadDGTarget
#
#startDGSource
#startDGTarget
#
#createUser
#
#createSourceCache
#createRemoteStore

#insertSomeEntriesIntoSource
insertSomeEntriesIntoProtobuffSource
#doRollingUpgrade

#cleanupResources