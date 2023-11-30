const BYBIT_API_URL = "https://api.bybit.com/v5/market/tickers";

export const fetchSymbolCurrentPrice = async (symbol: string) => {
    try {
        const response = await fetch(`${BYBIT_API_URL}?category=linear&symbol=${symbol}`)
        if (response.ok) {
            return (await response.json()).result.list[0].markPrice as number
        }
    } catch (error) {
        console.error(error)
        throw error;
    }
    throw new Error('Failed to fetch data from bybit')
}
