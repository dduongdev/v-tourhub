export const environment = {
    production: true,
    apiUrl: 'http://localhost:8000/api', // Update with production URL
    keycloak: {
        issuer: 'http://localhost:8080/realms/v-tourhub', // Update with production Keycloak
        redirectUri: 'http://localhost:4200', // Update with production frontend URL
        clientId: 'v-tourhub-public',
        scope: 'openid profile email',
        responseType: 'code',
        requireHttps: true,
        showDebugInformation: false,
        sessionChecksEnabled: true
    }
};
