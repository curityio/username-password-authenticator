package io.curity.identityserver.plugin.usernamepassword.utils;

public class ViewModelReservedKeys {

    // Where a form should be posted
    public static final String ACTION = "_action";

    // Used to post data so that values in a form are maintained when there is a validation error
    public static final String FORM_POST_BACK = "_postBack";

    // Used to the registration endpoint, or null if it should not be shown
    public static final String REGISTER_ENDPOINT = "_registerUrl";

    // The URL to the activation endpoint
    public static final String ACTIVATION_ENDPOINT = "_activationUrl";

    // The URL to the set password endpoint
    public static final String SET_PASSWORD_ENDPOINT = "_setPasswordUrl";

    // The recipient who will receive emails
    public static final String RECIPIENT_OF_COMMUNICATION = "_recipientOfCommunication";

    // The current username
    public static final String USERNAME = "_username";

    // When username is email, an extra email field is not shown and the email is instead entered in the username field
    public static final String SHOW_EMAIL_FIELD = "_showEmailField";

    // When a password is set during activation, password fields are not shown in the registration form
    public static final String SHOW_PASSWORD_FIELDS = "_showPasswordFields";

    // Whether email related options can be used
    public static final String IS_EMAIL_PROVIDER_CONFIGURED = "_isEmailProviderConfigured";
}
