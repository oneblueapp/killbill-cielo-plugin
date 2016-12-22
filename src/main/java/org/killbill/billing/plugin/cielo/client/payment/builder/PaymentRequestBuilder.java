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

import cieloecommerce.sdk.ecommerce.Address;
import cieloecommerce.sdk.ecommerce.Customer;
import cieloecommerce.sdk.ecommerce.Payment;
import cieloecommerce.sdk.ecommerce.Sale;
import org.killbill.billing.plugin.cielo.client.model.PaymentData;
import org.killbill.billing.plugin.cielo.client.model.SplitSettlementData;
import org.killbill.billing.plugin.cielo.client.model.UserData;
import org.killbill.billing.plugin.cielo.client.payment.converter.PaymentInfoConverterManagement;

import javax.annotation.Nullable;

public class PaymentRequestBuilder extends RequestBuilder<Sale> {

    private final PaymentData paymentData;
    private final UserData userData;
    private final SplitSettlementData splitSettlementData;
    private final Payment payment;

    public PaymentRequestBuilder(final PaymentData paymentData,
                                 final UserData userData,
                                 @Nullable final SplitSettlementData splitSettlementData,
                                 final PaymentInfoConverterManagement paymentInfoConverterManagement) {
        super(paymentInfoConverterManagement.convertPaymentInfoToPaymentRequest(paymentData.getPaymentInfo()));
        this.paymentData = paymentData;
        this.userData = userData;
        this.splitSettlementData = splitSettlementData;
        this.payment = sale.getPayment();
    }

    @Override
    public Sale build() {
        setReferences();
        setAmount();
        setShopperData();
        setShippingAddress();
        setSplitSettlementData();

        sale.setPayment(payment);
        return sale;
    }

    private void setReferences() {
        sale.setMerchantOrderId(paymentData.getPaymentTransactionExternalKey());
    }

    private void setAmount() {
        if (paymentData.getAmount() == null || paymentData.getCurrency() == null) {
            return;
        }

        payment.setAmount(toInteger(paymentData.getAmount()));
    }

    private void setShopperData() {
        Customer customer = sale.getCustomer();
        customer.setName(userData.getFirstName() + " " + userData.getLastName());
        customer.setEmail(userData.getShopperEmail());
        customer.setBirthDate(userData.getFormattedDateOfBirth("yyyyMMdd"));

        customer.setIdentity("", userData.getVatNumber());

        sale.setCustomer(customer);
    }

    private void setShippingAddress() {
//        Address shippingAddress = new Address();
//        shippingAddress.setCity("Monument Valley");
//        shippingAddress.setCountry("US");
//        shippingAddress.setNumber("1");
//        shippingAddress.setState("Utah");
//        shippingAddress.setStreet("Desertroad");
//        shippingAddress.setZipCode("84536");
//
//
//        Customer customer = sale.getCustomer();
//        customer.setDeliveryAddress(shippingAddress);
//        sale.setCustomer(customer);
    }

    private void setSplitSettlementData() {
//        if (splitSettlementData != null) {
//            final List<AnyType2AnyTypeMap.Entry> entries = new SplitSettlementParamsBuilder().createEntriesFrom(splitSettlementData);
//            sale.getAdditionalData().getEntry().addAll(entries);
//        }
    }
}
