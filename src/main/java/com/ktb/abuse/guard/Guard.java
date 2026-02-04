package com.ktb.abuse.guard;

import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;

public interface Guard {

    AbuseGuardResult check(AbuseCheckContext context);

    int getOrder();

    String getName();
}
