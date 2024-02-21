package io.curity.identityserver.plugin.usernamepassword.shared;

import se.curity.identityserver.sdk.service.credential.CredentialOperationDetails;
import se.curity.identityserver.sdk.service.credential.results.PasswordRejectedByDataSource;
import se.curity.identityserver.sdk.service.credential.results.SubjectCredentialsNotFound;
import se.curity.identityserver.sdk.web.Response;

import java.util.List;

public final class CredentialOperations {

    public static void onCredentialUpdateRejected(Response response, List<? extends CredentialOperationDetails.Rejected> details) {

        var filteredDetails = details.stream().filter(detail ->
                !(detail instanceof SubjectCredentialsNotFound || detail instanceof PasswordRejectedByDataSource)
        ).toList();
        response.putViewData("_rejection_details", filteredDetails, Response.ResponseModelScope.FAILURE);
    }
}
