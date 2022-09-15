#!/bin/bash

##########################################
# A script to free resources after testing
##########################################

cd "$(dirname "${BASH_SOURCE[0]}")"

#
# Free Docker resources
#
docker compose --project-name usernamepassword down

#
# Free ngrok resources
#
kill -9 $(pgrep ngrok) 2>/dev/null