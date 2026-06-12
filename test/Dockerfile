FROM curity.azurecr.io/curity/idsvr:latest

# Copy in the plugin and the dependencies not available in the SDK
RUN mkdir -p /opt/idsvr/usr/share/plugins/usernamepassword
COPY target/identityserver.plugins.authenticators.usernamepassword-4.2.1.jar /opt/idsvr/usr/share/plugins/usernamepassword/
COPY target/gson-2.13.2.jar                                                  /opt/idsvr/usr/share/plugins/usernamepassword/
COPY target/guava-33.5.0-jre.jar                                             /opt/idsvr/usr/share/plugins/usernamepassword/
COPY target/commons-lang3-3.20.0.jar                                         /opt/idsvr/usr/share/plugins/usernamepassword/
