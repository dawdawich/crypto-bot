export interface ManagerResponse {
    id: string;
    customName: string | null;
    status: string;
    market: string;
    analyzersCount: number
    stopLoss: number | null;
    takeProfit: number | null;
}
