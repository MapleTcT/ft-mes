package com.supcon.supfusion.auth.service.config;

import com.supos.license.annotations.EnableFeatureHasp;
import com.supos.license.define.ProductNameEnum;
import com.supos.license.define.SupOSFeature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(value = "integration.supos.enabled",havingValue = "true",matchIfMissing = true)
@Component
@EnableFeatureHasp(productName = ProductNameEnum.SUPOS_CORE, featureId = {SupOSFeature.AUTH_SERVICE})
public class IndependentConfig {
}

