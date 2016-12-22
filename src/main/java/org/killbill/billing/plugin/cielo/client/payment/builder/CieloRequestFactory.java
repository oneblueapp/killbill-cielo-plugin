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

package org.killbill.billing.plugin.cielo.client.payment.builder;

import javax.annotation.Nullable;

import cieloecommerce.sdk.ecommerce.CreditCard;
import cieloecommerce.sdk.ecommerce.Sale;
import org.killbill.billing.plugin.cielo.client.CieloConfigProperties;
import org.killbill.billing.plugin.cielo.client.model.PaymentData;
import org.killbill.billing.plugin.cielo.client.model.PaymentInfo;
import org.killbill.billing.plugin.cielo.client.model.SplitSettlementData;
import org.killbill.billing.plugin.cielo.client.model.UserData;
import org.killbill.billing.plugin.cielo.client.payment.converter.PaymentInfoConverterManagement;

public class CieloRequestFactory {

    private final PaymentInfoConverterManagement paymentInfoConverterManagement;
    private final CieloConfigProperties cieloConfigProperties;

    public CieloRequestFactory(final PaymentInfoConverterManagement paymentInfoConverterManagement,
                               final CieloConfigProperties cieloConfigProperties) {
        this.paymentInfoConverterManagement = paymentInfoConverterManagement;
        this.cieloConfigProperties = cieloConfigProperties;
    }

    public Sale createPaymentRequest(final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData) {
        final PaymentRequestBuilder paymentRequestBuilder = new PaymentRequestBuilder(paymentData, userData, splitSettlementData, paymentInfoConverterManagement);
        return paymentRequestBuilder.build();
    }

    public CreditCard createTokenRequest(final PaymentInfo paymentInfo, final UserData userData) {
        final CreateTokenRequestBuilder createTokenRequestBuilder = new CreateTokenRequestBuilder(paymentInfo, userData, null, paymentInfoConverterManagement);
        return createTokenRequestBuilder.build();
    }
    //
//    public Map<String, String> createHppRequest(final String merchantAccount, final PaymentData paymentData, final UserData userData, @Nullable final SplitSettlementData splitSettlementData) throws SignatureGenerationException {
//        final HPPRequestBuilder builder = new HPPRequestBuilder(merchantAccount,
//                                                                paymentData,
//                                                                userData,
//                                                                splitSettlementData,
//                                                                adyenConfigProperties,
//                                                                signer);
//        return builder.build();
//    }
}
