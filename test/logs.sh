#!/bin/bash

##########################################################################
# Report logs in a child window to make plugin debug messages easy to view
##########################################################################

cd "$(dirname "${BASH_SOURCE[0]}")"
docker compose --project-name usernamepassword logs -f
