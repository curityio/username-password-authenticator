# Username/Password Authenticator Plugin Example

[![Quality](https://img.shields.io/badge/quality-test-yellow)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

The Curity Identity Server has a built-in [HTML Form Authenticator](https://curity.io/docs/idsvr/latest/authentication-service-admin-guide/authenticators/html.html).\
This example plugin provided a similar implementation using the Curity SDK.

## Customization

The [Website Customization Resources](https://curity.io/resources/customization/) show how to implement branding customizations.\
In more advanced scenarios you may also want to take closer control over runtime behavior.\
For these cases, you can use this repo as a starting point and adapt it as required.

## Example Deployment

An [example deployment](test/README.md) is provided that uses the plugin, and which also uses a custom logo.\
Behavior is largely the same as the built in HTML Form authenticator, with some minor layour differences.\
All views render correctly on both mobile and desktop devices.

![Initial Screen](doc/images/authentication/initial.png)

## Behavior of Flows

A number of flows are available, to manage self sign up, authentication and account recovery.\
Each of these are described, with screenshots and plugin coding details, in the below documents:

- [Authentication](doc/authentication.md)
- [Create Account](doc/create-account.md)
- [Activate Account](doc/activate-account.md)
- [Activate and Set Password](activate-set-password.md)
- [Forgot Username](doc/forgot-username.md)
- [Forgot Password](doc/forgot-password.md)
- [Set Password](doc/set-password.md)

## Deploying the Plugin

Follow the below steps to run this plugin in your own instances of the Curity Identity Server.

### Update Java Libraries

When deployed to the Curity Identity Server, the plugin will use shared Java libraries.\
Identity the versions of these libraries that match your version of the Curity Identity Server.\
This can be done by viewing the [Service Provided Dependencies](https://curity.io/docs/idsvr/latest/developer-guide/plugins/index.html#server-provided-dependencies) page for your version.\
Then update the `pom.xml` file of this project with matching versions.

### Build the JAR Files

Ensure that Java 8 or later is installed, and build the plugin code with this command:

```bash
mvn package
```

Next gather the following files from the `target` folder:

```text
identityserver.plugins.authenticators.usernamepassword-*.jar
gson-*.jar
guava-*.jar
commons-lang*.jar
```

### Deploy the JAR Files

Deploy JAR files to your instances of the Curity Identity Server, in a plugins subfolder:

```text
$IDSVR_HOME/usr/share/plugins/usernamepasswordauthenticator/*.jar
```

### Use the Plugin

In the Admin UI, create an instance of the `Username Password Authenticator` to use in your applications:

![New Authenticator](doc/images/shared/new-authenticator.png)

## SDK Links

- Read the [Plugins Developer Guide](https://curity.io/docs/idsvr/latest/developer-guide/plugins/index.html) for an overview of behavior
- Search the [Identity Server Plugin SDK](https://curity.io/docs/idsvr-java-plugin-sdk/latest/) to better understand SDK objects
- See the [Plugin Code Examples](https://curity.io/resources/plugins-authenticators/) for many existing plugins to compare against

## Further Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
