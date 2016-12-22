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

package org.killbill.billing.plugin.cielo.client.payment.converter.impl;

import cieloecommerce.sdk.ecommerce.CreditCard;
import cieloecommerce.sdk.ecommerce.Payment;
import cieloecommerce.sdk.ecommerce.Sale;
import org.killbill.billing.plugin.cielo.client.model.PaymentInfo;
import org.killbill.billing.plugin.cielo.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.cielo.client.payment.converter.PaymentInfoConverter;

public class CreditCardConverter extends PaymentInfoConverter<Card> {

    @Override
    public boolean supportsPaymentInfo(final PaymentInfo paymentInfo) {
        return paymentInfo instanceof Card;
    }

    @Override
    public Sale convertPaymentInfoToPaymentRequest(final Card paymentInfo) {
        CreditCard card = new CreditCard(paymentInfo.getCvc(), paymentInfo.getBrand());
        card.setCardNumber(paymentInfo.getNumber());
        card.setHolder(paymentInfo.getHolderName());
        card.setExpirationDate(paymentInfo.getExpiryDate());

        card.setCardToken(paymentInfo.getToken());

        final Sale sale = super.convertPaymentInfoToPaymentRequest(paymentInfo);
        Payment payment = sale.getPayment() != null ? sale.getPayment() : new Payment(0);
        payment.setCreditCard(card);
        sale.setPayment(payment);
        return sale;
    }

    @Override
    public CreditCard convertPaymentInfoToCreateTokenRequest(final Card paymentInfo) {
        final CreditCard creditCard = super.convertPaymentInfoToCreateTokenRequest(paymentInfo);

        if (paymentInfo.getNumber() != null) {
            creditCard.setBrand(paymentInfo.getBrand());
            creditCard.setCardNumber(paymentInfo.getNumber());
            creditCard.setHolder(paymentInfo.getHolderName());
            creditCard.setExpirationDate(paymentInfo.getExpiryDate());
        }

        return creditCard;
    }
}
