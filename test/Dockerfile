FROM curity.azurecr.io/curity/idsvr:7.3.1

# Copy in the plugin and the dependencies not available in the SDK
RUN mkdir -p /opt/idsvr/usr/share/plugins/usernamepassword
COPY target/identityserver.plugins.authenticators.usernamepassword-3.0.0.jar /opt/idsvr/usr/share/plugins/usernamepassword/
COPY target/gson-2.9.0.jar                                                   /opt/idsvr/usr/share/plugins/usernamepassword/
COPY target/guava-31.0.1-jre.jar                                             /opt/idsvr/usr/share/plugins/usernamepassword/
COPY target/commons-lang3-3.9.jar                                            /opt/idsvr/usr/share/plugins/usernamepassword/
