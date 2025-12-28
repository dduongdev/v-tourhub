export interface User {
    userId: string;
    email: string;
    firstName?: string;
    lastName?: string;
    phoneNumber?: string;
    bio?: string;
    roles?: string[];
}
