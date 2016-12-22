/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.cielo.client;

import cieloecommerce.sdk.ecommerce.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CieloConfigProperties {

    private static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.cielo.";

    private static final String DEFAULT_ENVIRONMENT = "sandbox";
    private static final String DEFAULT_CONNECTION_TIMEOUT = "30000";
    private static final String DEFAULT_SOCKET_TIMEOUT = "60000";
    private static final String DEFAULT_MAX_CONNECTIONS = "10";

    private static final Map<String, Environment> ENV_MAP = new HashMap<String, Environment>() {
        {
            put("production", Environment.PRODUCTION);
            put("sandbox", Environment.SANDBOX);
        }
    };

    private final String connectTimeout;
    private final String socketTimeout;
    private final String maxConnections;
    private final String merchantId;
    private final String merchantKey;
    private final Environment environment;

    public CieloConfigProperties(final Properties properties) {
        this.connectTimeout = properties.getProperty(PROPERTY_PREFIX + "connectTimeout", DEFAULT_CONNECTION_TIMEOUT);
        this.socketTimeout = properties.getProperty(PROPERTY_PREFIX + "socketTimeout", DEFAULT_SOCKET_TIMEOUT);
        this.maxConnections = properties.getProperty(PROPERTY_PREFIX + "maxConnections", DEFAULT_MAX_CONNECTIONS);
        this.merchantId = properties.getProperty(PROPERTY_PREFIX + "merchantId");
        this.merchantKey = properties.getProperty(PROPERTY_PREFIX + "merchantKey");
        this.environment = ENV_MAP.get(properties.getProperty(PROPERTY_PREFIX + "environment", DEFAULT_ENVIRONMENT));
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public Environment getEnvironment() {
        return environment;
    }

}
