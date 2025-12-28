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
        let roles: string[] = [];

        // 1. Check Identity Claims (ID Token)
        const idClaims = this.oauthService.getIdentityClaims() as any;
        if (idClaims && idClaims.realm_access?.roles) {
            roles = roles.concat(idClaims.realm_access.roles);
        }

        // 2. Check Access Token (if different from ID Token or contains extra scopes)
        const accessToken = this.oauthService.getAccessToken();
        if (accessToken) {
            try {
                const parts = accessToken.split('.');
                if (parts.length === 3) {
                    const payload = JSON.parse(atob(parts[1]));
                    if (payload.realm_access?.roles) {
                        roles = roles.concat(payload.realm_access.roles);
                    }
                    if (payload.resource_access?.[authConfig.clientId || '']?.roles) {
                        roles = roles.concat(payload.resource_access[authConfig.clientId || ''].roles);
                    }
                }
            } catch (e) {
                console.error('Error parsing access token', e);
            }
        }

        // De-duplicate
        return Array.from(new Set(roles));
    }

    hasRole(role: string): boolean {
        const roles = this.getUserRoles();
        return roles.includes(role);
    }

    isAdmin(): boolean {
        const roles = this.getUserRoles();
        console.log('Checking isAdmin. Roles:', roles);
        if (!roles || roles.length === 0) return false;
        // Common possibilities: 'ROLE_ADMIN' (backend prefixed) or 'admin'
        if (roles.includes('ROLE_ADMIN') || roles.includes('admin')) return true;
        // Fallback: any role name containing 'admin'
        return roles.some(r => /admin/i.test(r));
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
