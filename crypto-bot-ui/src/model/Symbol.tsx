export type Symbol = {
    symbol: string;
    partition: number;
    testServer: boolean;
    isOneWayMode: boolean;
    minPrice: number;
    maxPrice: number;
    tickSize: number;
    minOrderQty: number;
    maxOrderQty: number;
    qtyStep: number;
}
