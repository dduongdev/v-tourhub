import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth-config';
import { Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    constructor(
        private oauthService: OAuthService,
        private router: Router
    ) { }

    async initAuth(): Promise<void> {
        // Configure OAuth service
        this.oauthService.configure(authConfig);
        this.oauthService.setupAutomaticSilentRefresh();

        // Try to discover and load user profile
        try {
            await this.oauthService.loadDiscoveryDocumentAndTryLogin();

            if (this.hasValidToken()) {
                // Load user profile
                await this.oauthService.loadUserProfile();
            }
        } catch (error) {
            console.error('Error during authentication init', error);
        }
    }

    login(targetUrl?: string): void {
        if (targetUrl) {
            sessionStorage.setItem('auth_redirect_url', targetUrl);
        }
        this.oauthService.initCodeFlow();
    }

    logout(): void {
        this.oauthService.logOut();
        sessionStorage.clear();
        this.router.navigate(['/']);
    }

    isAuthenticated(): boolean {
        return this.hasValidToken();
    }

    private hasValidToken(): boolean {
        return this.oauthService.hasValidAccessToken() && this.oauthService.hasValidIdToken();
    }

    getAccessToken(): string | null {
        return this.oauthService.getAccessToken();
    }

    getUserRoles(): string[] {
        const claims = this.oauthService.getIdentityClaims() as any;
        if (!claims) return [];

        // Try realm_access.roles first (Keycloak standard)
        if (claims.realm_access?.roles) {
            return claims.realm_access.roles;
        }

        // Fallback to resource_access for client roles
        const clientId = authConfig.clientId;
        if (claims.resource_access?.[clientId || '']?.roles) {
            return claims.resource_access[clientId || ''].roles;
        }

        return [];
    }

    hasRole(role: string): boolean {
        const roles = this.getUserRoles();
        return roles.includes(role);
    }

    getCurrentUser(): any {
        return this.oauthService.getIdentityClaims();
    }

    getUserName(): string {
        const claims = this.oauthService.getIdentityClaims() as any;
        if (!claims) return '';

        return claims.given_name || claims.name || claims.preferred_username || claims.email || '';
    }

    getRedirectUrlAfterLogin(): string {
        const redirectUrl = sessionStorage.getItem('auth_redirect_url');
        sessionStorage.removeItem('auth_redirect_url');
        return redirectUrl || '/';
    }
}
