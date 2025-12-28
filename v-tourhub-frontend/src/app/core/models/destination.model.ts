export interface Destination {
    id: number;
    name: string;
    description?: string;
    address?: string;
    city?: string;
    services?: any[]; // Services list
    mediaList?: Media[];
}

export interface Media {
    id: number;
    url: string;
    type?: string;
}
