package com.kcjmowright.zerodte.model;

import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;

public record IronCondorContracts(
    OptionContract longCall,
    OptionContract shortCall,
    OptionContract longPut,
    OptionContract shortPut) {
}
