#!/bin/bash

##############################################
# A script to build the plugin ready to deploy
##############################################

#
# Ensure that we are in the root folder
#
cd "$(dirname "${BASH_SOURCE[0]}")"
cd ..

#
# Check we have a license file
#
if [ ! -f './test/license.json' ]; then
  echo 'Please provide a license file at ./test/license.json before deploying the system'
  exit 1
fi

#
# This is used by Curity developers to prevent checkins of license files
#
cp ./hooks/pre-commit ./.git/hooks

#
# Start ngrok if required, and use 'kill -9 $(pgrep ngrok)' to
#
export RUNTIME_BASE_URL='https://localhost:8443'
if [ "$USE_NGROK" != 'false' ]; then

  if [ "$(pgrep ngrok)" == '' ]; then
    ngrok http 8443 -log=stdout &
    sleep 5
  fi
  
  export RUNTIME_BASE_URL=$(curl -s http://localhost:4040/api/tunnels | jq -r '.tunnels[] | select(.proto == "https") | .public_url')
  if [ "$RUNTIME_BASE_URL" == "" ]; then
    echo 'Problem encountered getting an NGROK URL'
    exit 1
  fi
fi

#
# Deploy the system
#
cd test
#cp ~/Desktop/config-backup.xml ./config-backup.xml
docker compose --project-name usernamepassword down
docker compose --project-name usernamepassword up --force-recreate --detach
if [ $? -ne 0 ]; then
  echo 'Problem encountered deploying the Curity Identity Server and plugin'
  exit 1
fi

#
# Indicate the URL to paste into OAuth tools
#
echo "The OpenID Connect Metadata URL is: $RUNTIME_BASE_URL/oauth/v2/oauth-anonymous/.well-known/openid-configuration"

#
# View logs in a child window
#
open -a Terminal ./logs.sh
