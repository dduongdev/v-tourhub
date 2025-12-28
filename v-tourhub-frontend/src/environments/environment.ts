export const environment = {
    production: false,
    apiUrl: '/api',
    keycloak: {
        issuer: 'http://localhost:8080/realms/v-tourhub',
        redirectUri: 'http://localhost:4200',
        clientId: 'v-tourhub-public',
        scope: 'openid profile email',
        responseType: 'code',
        requireHttps: false,
        showDebugInformation: true,
        sessionChecksEnabled: true
    }
};
