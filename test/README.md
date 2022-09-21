# Test the Plugin

The below deployment uses the ngrok tool to enable a productive test setup when working on the plugin.\
If preferred, the deployment can be studied and adapted to your own requirements.

## Prerequisites

Download a `license.json` file for the Curity Identity Server and copy it into the `test` folder.\
Also ensure that these tools are installed on your local computer:

- [Docker](https://www.docker.com/products/docker-desktop)
- [ngrok](https://ngrok.com/download)

## Deploy the Plugin

Whenever the plugin code changes, build it into JAR files in a custom Docker image:

```bash
./test/build.sh
```

Then deploy the Curity Identity Server using the updated Docker image.\
The example deployment also exposes port 8443 of the local identity server to the internet, via ngrok.\
This enables the internet hosted OAuth Tools to be used as a test client:

```bash
./test/deploy.sh
```

The script will output an external base URL that is called from OAuth tools:

```text
https://4bdb-2-26-158-168.eu.ngrok.io/oauth/v2/oauth-anonymous/.well-known/openid-configuration
```

## Configure OAuth Tools

Browse to https://oauth.tools and create an environment from the metadata URL:

![OAuth Tools Configuration](images/oauthtools-configuration.png)

In the `Code Flow` window, configure the three values highlighted below:

![OAuth Tools Configuration](images/codeflow-settings.png)

## Configure the Curity Identity Server

Login to the admin UI at this URL, with credentials `admin / Password1`:

- https://localhost:6749/admin

From the `Facilities` menu, configure the account manager options according to your preferences:

![Account Manager](../doc/images/shared/account-manager.png)

To use email features for account activation and recovery, update the email provider settings:

![Email Provider](../doc/images/shared/email-provider.png)

## Test Password Flows

The system is deployed using an in-memory database for user accounts.\
Whenever the system is deployed, select `Create Account` and register one or more users:

![Create Account](../doc/images/create-account/initial.png)

Then activate the user if required, sign in, and test recovery fewatures such as set password:

![Authenticate](../doc/images/authentication/initial.png)

## Free Resources

When finished testing, run this command to free Docker and ngrok resources:

```bash
./test/teardown.sh
```
