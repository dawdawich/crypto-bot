package space.dawdawich.client.model

import space.dawdawich.strategy.model.Position

typealias PositionUpdateCallback = (Position?) -> Unit
typealias FillOrderCallback = (String) -> Unit
