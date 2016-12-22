/*
 * Copyright 2014 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.plugin.cielo.client.model;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import org.killbill.billing.payment.api.TransactionType;

public enum PaymentServiceProviderResult {

    AUTHORISED(new Integer[] {2, 10, 11, 20}),
    // authorize return code when using 3D-Secure
    RECEIVED(20), // direct debit, ideal payment response
    REFUSED(3),
    PENDING(new Integer[]{1, 12}),
    ERROR(new Integer[]{0, 13});

    private static final Map<Integer, PaymentServiceProviderResult> REVERSE_LOOKUP = new HashMap<Integer, PaymentServiceProviderResult>();

    static {
        for (final PaymentServiceProviderResult providerResult : PaymentServiceProviderResult.values()) {
            for (final Integer response : providerResult.getResponses()) {
                REVERSE_LOOKUP.put(response, providerResult);
            }
        }
    }

    private final Integer[] responses;

    PaymentServiceProviderResult(final Integer response) {
        this(new Integer[]{response});
    }

    PaymentServiceProviderResult(final Integer[] responses) {
        this.responses = responses;
    }

    public static PaymentServiceProviderResult getPaymentResultForId(@Nullable final String id, final TransactionType transactionType) {
        if (id == null) {
            return ERROR;
        }

        PaymentServiceProviderResult result = REVERSE_LOOKUP.get(id);
        if (result != null) {
            if ((transactionType == TransactionType.AUTHORIZE
                 || transactionType == TransactionType.CAPTURE)
                    && id.equals("PENDING_APPROVAL")) {
                return PaymentServiceProviderResult.AUTHORISED;
            }

            return result;
        } else {
            try {
                return PaymentServiceProviderResult.valueOf(id);
            }
            catch(IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown PaymentResultType id: " + id);
            }
        }
    }

    public Integer[] getResponses() {
        return responses;
    }

//    @Override
//    public String toString() {
//        return this.responses[0].toString();
//    }
}
