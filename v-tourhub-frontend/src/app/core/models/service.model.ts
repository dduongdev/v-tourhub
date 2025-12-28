export interface Service {
    id: number;
    name: string;
    description?: string;
    price: number;
    type: 'TOUR' | 'HOTEL' | 'RESTAURANT';
    availableQuantity: number;
    destinationId?: number;
}
