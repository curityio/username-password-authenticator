# Test the Plugin

The below deployment uses the ngrok tool to enable a productive test setup when working on the plugin.\
If preferred, the deployment can be studied and adapted to your own requirements.

## Prerequisites

Download a `license.json` file for the Curity Identity Server and copy it into the `test` folder.\
Also ensure that these tools are installed on your local computer:

- [Docker](https://www.docker.com/products/docker-desktop)
- [ngrok](https://ngrok.com/download)

## Deploy the Plugin

First do a docker pull to ensure that your latest tag for the Curity Identity Server is updated:

```bash
docker pull curity.azurecr.io/curity/idsvr
```

Whenever you change the plugin code, build it into deployable JAR files.\
The build script does so by producing a custom Docker image for the Curity Identity Server:

```bash
./test/build.sh
```

The example deployment enables the internet based OAuth Tools to be used as a test client.\
The deployment script uses ngrok to expose port 8443 of the local identity server to OAuth Tools:

```bash
./test/deploy.sh
```

The script outputs an external base URL that can be pasted into OAuth tools:

```text
https://4bdb-2-26-158-168.eu.ngrok.io/oauth/v2/oauth-anonymous/.well-known/openid-configuration
```

## Configure OAuth Tools

Browse to https://oauth.tools and create an environment from the metadata URL:

![OAuth Tools Configuration](images/oauthtools-configuration.png)

Add a `code flow` and configure these values:

- client ID: demo-web-client
- client secret: Password1
- scope: openid
- prompt: login

## Configure the Curity Identity Server

Login to the admin UI at this URL, with credentials `admin / Password1`:

- https://localhost:6749/admin

From the `Facilities` menu, configure the account manager options according to your preferences:

![Account Manager](../doc/images/shared/account-manager.png)

## Test Password Operations for Browser-Based Flows

Run a code flow from OAuth tools to perform end-to-end testing of password flows.\
Whenever the system is deployed, select `Create Account` and register a user:

![Create Account](images/login.png)

A mock SMTP server is provided, and you can process emails by browsing to `http://localhost:8080`:

![Email Inbox](images/email-inbox.png)

Activate the user if required, then test logins and account recovery behaviour:

![Authenticate](../doc/images/authentication/initial.png)

## Test Password Operations with Native Forms using the Hypermedia Authentication API

Clone the HAAPI code examples with these commands:

```bash
git clone https://github.com/curityio/android-haapi-ui-sdk-demo
git clone https://github.com/curityio/ios-haapi-ui-sdk-demo
```

Edit configuration files and change the base URL to the ngrok value, then run the apps:

- For Android, the configuration file is at `app/src/main/java/io/curity/haapidemo/Configuration.kt`.
- For iOS, the configuration file is at `iOS/Configuration.swift`.

## Query User Account and Credential Data

Get a shell to the postgres Docker container:

```bash
POSTGRES_CONTAINER_ID=$(docker ps | grep postgres | awk '{print $1}')
docker exec -it $POSTGRES_CONTAINER_ID bash
```

Then connect to the database:

```bash
export PGPASSWORD=Password1 && psql -p 5432 -d idsvr -U postgres
```

Then run queries to see how users and their passwords are stored and updated:

```sql
select * from accounts;
select * from credentials;
```

## Free Resources

When finished testing, run this command to free Docker and ngrok resources:

```bash
./test/teardown.sh
```
