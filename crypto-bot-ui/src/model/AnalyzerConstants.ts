export const StrategyTypes = [{
    label: 'Grid-Table strategy', value: 'GRID_TABLE_STRATEGY'
}];

export const MarketTypes = [{value: 'BYBIT', label: 'ByBit'}];

export const getStrategyOptionFromValue = (value: string | undefined) => !!value ? StrategyTypes.find(el => el.value === value) : null
export const getMarketOptionFromValue = (value: string | undefined) => !!value ? MarketTypes.find(el => el.value === value) : null
