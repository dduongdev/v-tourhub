import { Destination } from "./destination.model";

export interface Service {
    id: number;
    name: string;
    description: string;
    price: number;
    type: 'TOUR' | 'HOTEL' | 'ACTIVITY' | 'RESTAURANT';
    availability: boolean;
    destination?: Destination;
    mediaList?: { id: number; url: string; caption?: string }[];
    attributes?: { [key: string]: string };
    inventoryCalendar?: InventoryInfo[];
}

export interface InventoryInfo {
    date: string;
    availableStock: number;
    totalStock: number;
    isAvailable: boolean;
}
