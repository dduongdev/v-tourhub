export interface User {
    userId: string;
    email: string;
    firstName?: string;
    lastName?: string;
    phone?: string;
    bio?: string;
    roles?: string[];
    avatarUrl?: string;
    address?: string;
    currency?: string;
}
