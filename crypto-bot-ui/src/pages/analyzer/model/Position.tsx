export type Position = {
    entryPrice: number;
    size: number;
    closePrice: number | null;
    long: boolean;
    createTime: number;
    closeTime: number | null;
}
