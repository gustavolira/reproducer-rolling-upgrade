#!/bin/bash

export DG83_VERSION="redhat-datagrid-8.3.0.CD20210915-server.zip"
export script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DG_821_URL=https://download.eng.bos.redhat.com/devel/candidates/JDG/RHDG-8.2.1/redhat-datagrid-8.2.1-server.zip
DG_83_URL=http://download.eng.bos.redhat.com/rcm-guest/staging/jboss-dg/JDG-8.3.x-CD/JDG-8.3.0.CD20210915/redhat-datagrid-8.3.0.CD20210915-server.zip
#DG_83_URL=http://download.eng.bos.redhat.com/rcm-guest/staging/jboss-dg/JDG-8.3.x-CD/JDG-8.3.0.CD20210923/redhat-datagrid-8.3.0.CD20210923-server.zip

function downloadDGSource() {
  if [[ ! -e "redhat-datagrid-8.2.1-server.zip" ]]; then
      wget ${DG_821_URL}
  fi
  rm -rf redhat-datagrid-8.2.1-server
  unzip redhat-datagrid-8.2.1-server.zip
}

function downloadDGTarget() {
  if [[ ! -e ${DG83_VERSION} ]]; then
      wget ${DG_83_URL}
  fi
  rm -rf redhat-datagrid-8.3.0-server
  unzip ${DG83_VERSION}
}

function is-ready() {
   local offset=$1
   $(curl --digest -u user:passwd-123 --silent --output /dev/null http://localhost:$(( 11222 + offset ))/rest/v2/cache-managers/default/health)
}

function startDGSource() {
  mkdir -p logs
  nohup redhat-datagrid-8.2.1-server/bin/server.sh -Djgroups.mcast_addr=234.99.54.14 -Djgroups.mcast_port=55555 > logs/server-source.log &
  while ! is-ready 0 >/dev/null
  do
   echo "waiting for server to start"
   sleep 5;
  done
  echo "SOURCE SERVER STARTED"
}

function startDGTarget() {
  nohup redhat-datagrid-8.3.0-server/bin/server.sh -o 100 -Djgroups.mcast_addr=234.99.54.15 > logs/server-target.log &
  while ! is-ready 100 >/dev/null
  do
   echo "waiting for server to start"
   sleep 5;
  done
  echo "TARGET SERVER STARTED"
}

function createUser() {
  local USER="user"
  local PASS="passwd-123"
  redhat-datagrid-8.2.1-server/bin/cli.sh user create $USER -p $PASS -g admin &>/dev/null
  redhat-datagrid-8.3.0-server/bin/cli.sh user create $USER -p $PASS -g admin &>/dev/null
}

function createRemoteStore() {
  curl --digest -u user:passwd-123 -X POST --output - http://127.0.0.1:11322/rest/v2/caches/protobuff -H "Content-Type: application/json" --data-binary "@${script_dir}/assets/protobuff-remote-security.json"
}

function createSourceCache() {
  curl --digest -u user:passwd-123 -X POST --output - http://127.0.0.1:11222/rest/v2/caches/protobuff -H "Content-Type: application/json" --data-binary "@${script_dir}/assets/protobuff.json"
}

function insertSomeEntriesIntoSource() {
  curl --digest -u user:passwd-123 -X POST http://127.0.0.1:11222/rest/v2/caches/default/k1 --data "v1"
  curl --digest -u user:passwd-123 -X POST http://127.0.0.1:11222/rest/v2/caches/default/k2 --data "v2"
  curl --digest -u user:passwd-123 -X POST http://127.0.0.1:11222/rest/v2/caches/default/k3 --data "v3"
}

function insertSomeEntriesIntoProtobuffSource() {
  "${script_dir}"/jbang --fresh --verbose "${script_dir}"/ProtobuffLoad.java --entries 500 --write-batch 1000 --phrase-size 100 --hotrodversion 2.8 --version 8.3 --cache-name protobuff
}

function doRollingUpgrade() {
  curl --digest -u user:passwd-123 -X POST http://127.0.0.1:11322/rest/v2/caches/default?action=sync-data
}

function cleanupResources() {
  curl -X DELETE --digest -u user:passwd-123 http://127.0.0.1:11322/rest/v2/caches/default
  curl -X DELETE --digest -u user:passwd-123 http://127.0.0.1:11222/rest/v2/caches/default
}
