import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

export const authConfig: AuthConfig = {
    issuer: environment.keycloak.issuer,
    redirectUri: environment.keycloak.redirectUri,
    postLogoutRedirectUri: environment.keycloak.redirectUri,
    clientId: environment.keycloak.clientId,
    responseType: environment.keycloak.responseType,
    scope: environment.keycloak.scope,
    requireHttps: environment.keycloak.requireHttps,
    showDebugInformation: environment.keycloak.showDebugInformation,
    sessionChecksEnabled: environment.keycloak.sessionChecksEnabled,

    // PKCE configuration
    useSilentRefresh: true,
    silentRefreshRedirectUri: `${environment.keycloak.redirectUri}/silent-refresh.html`,
};
