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
# Build the plugin code
#
mvn package
if [ $? -ne 0 ]; then
  echo 'Problem encountered building the plugin code'
  exit 1
fi

#
# Create a custom Docker image
#
docker build -f test/Dockerfile -t custom_curity_idsvr:latest .
if [ $? -ne 0 ]; then
  echo 'Problem encountered building the Identity Server custom docker image'
  exit 1
fi