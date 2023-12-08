db = db.getSiblingDB('crypto-bot');
db.createCollection('symbols_to_listen');
db.symbols_to_listen.insertMany([
    {
        "_id": "655a1f590e11f632c7714734",
        "isOneWayMode": "false",
        "partition": "0",
        "priceMinStep": "0.1",
        "symbol": "BTCUSDT"
    },
    {
        "_id": "655a1f590e11f632c7714735",
        "isOneWayMode": "false",
        "partition": "1",
        "priceMinStep": "0.01",
        "symbol": "ETHUSDT"
    },
    {
        "_id": "655a1f590e11f632c7714736",
        "isOneWayMode": "false",
        "partition": "2",
        "priceMinStep": "0.001",
        "symbol": "SOLUSDT"
    },
    {
        "_id": "655a1f590e11f632c7714737",
        "isOneWayMode": "true",
        "partition": "3",
        "priceMinStep": "0.001",
        "symbol": "ORDIUSDT"
    },
    {
        "_id": "655a1f590e11f632c7714738",
        "isOneWayMode": "false",
        "partition": "4",
        "priceMinStep": "0.00001",
        "symbol": "DOGEUSDT"
    },
    {
        "_id": "655a1f590e11f632c7714739",
        "isOneWayMode": "true",
        "partition": "5",
        "priceMinStep": "0.0001",
        "symbol": "TIAUSDT"
    },
    {
        "_id": "655a1f590e11f632c771473a",
        "isOneWayMode": "true",
        "partition": "6",
        "priceMinStep": "0.0001",
        "symbol": "XRPUSDT"
    },
    {
        "_id": "655a1f590e11f632c771473b",
        "isOneWayMode": "true",
        "partition": "7",
        "priceMinStep": "0.001",
        "symbol": "LINKUSDT"
    },
    {
        "_id": "655a1f590e11f632c771473c",
        "isOneWayMode": "true",
        "partition": "8",
        "priceMinStep": "0.001",
        "symbol": "ETCUSDT"
    }
]);
