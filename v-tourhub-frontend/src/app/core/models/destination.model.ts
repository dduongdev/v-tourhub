export interface Destination {
    id: number;
    name: string;
    description?: string;
    address?: string;
    city?: string;
    province?: string;
    latitude?: number;
    longitude?: number;
    services?: any[]; // Services list
    mediaList?: Media[];
}

export interface Media {
    id: number;
    url: string;
    type?: string;
    caption?: string;
}
