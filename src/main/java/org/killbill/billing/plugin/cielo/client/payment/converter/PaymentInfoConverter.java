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

package org.killbill.billing.plugin.cielo.client.payment.converter;

import cieloecommerce.sdk.ecommerce.Address;
import cieloecommerce.sdk.ecommerce.CreditCard;
import cieloecommerce.sdk.ecommerce.Customer;
import cieloecommerce.sdk.ecommerce.Sale;
import org.killbill.billing.plugin.cielo.client.model.PaymentInfo;

public class PaymentInfoConverter<T extends PaymentInfo> {

    /**
     * @param paymentInfo to convert
     * @return {@code true} if this converter is capable of handling the payment info
     */
    public boolean supportsPaymentInfo(final PaymentInfo paymentInfo) {
        return true;
    }

    /**
     * Convert a PaymentInfo Object into an Ingenico CreatePaymentRequest
     */
    public Sale convertPaymentInfoToPaymentRequest(final T paymentInfo) {
        final Sale sale = new Sale(paymentInfo.getMerhantPaymentId());

        setInstallments(paymentInfo, sale);
        setOrderBillingAddress(paymentInfo, sale);
        return sale;
    }

    /**
     * Convert a PaymentInfo Object into an Ingenico CreateTokenRequest
     */
    public CreditCard convertPaymentInfoToCreateTokenRequest(final T paymentInfo) {
        final CreditCard createTokenRequest = new CreditCard("", "");

        return createTokenRequest;
    }

    private void setInstallments(final PaymentInfo paymentInfo, final Sale sale) {
//        if (paymentInfo.getInstallments() != null) {
//            final Installments installments = new Installments();
//            installments.setValue(paymentInfo.getInstallments().shortValue());
//            paymentRequest.setInstallments(installments);
//        }
    }

    private Address createBillingAddress(final PaymentInfo paymentInfo) {
        final Address address = new Address();
        address.setStreet(paymentInfo.getStreet());
        address.setNumber(paymentInfo.getHouseNumberOrName());
        address.setCity(paymentInfo.getCity());
        address.setZipCode(paymentInfo.getPostalCode());
        address.setState(paymentInfo.getStateOrProvince());

        final String adjustedCountry = paymentInfo.getCountry();
        address.setCountry(adjustedCountry);
        return address;
    }

    private void setOrderBillingAddress(PaymentInfo paymentInfo, final Sale sale) {
        sale.payment(0);
        Address address = createBillingAddress(paymentInfo);
        Customer customer = sale.getCustomer() != null ? sale.getCustomer() : new Customer("");
        customer.setAddress(address);
        sale.setCustomer(customer);
    }
}
