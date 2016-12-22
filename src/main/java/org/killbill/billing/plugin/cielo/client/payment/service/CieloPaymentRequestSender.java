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

package org.killbill.billing.plugin.cielo.client.payment.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cieloecommerce.sdk.ecommerce.CieloEcommerce;
import cieloecommerce.sdk.ecommerce.CreditCard;
import cieloecommerce.sdk.ecommerce.CreditCardToken;
import cieloecommerce.sdk.ecommerce.Sale;
import cieloecommerce.sdk.ecommerce.request.CieloError;
import cieloecommerce.sdk.ecommerce.request.CieloRequestException;
import org.killbill.billing.payment.api.PaymentResponse;
import org.killbill.billing.plugin.cielo.client.CieloClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import static org.killbill.billing.plugin.cielo.client.payment.service.CieloCallErrorStatus.REQUEST_NOT_SEND;
import static org.killbill.billing.plugin.cielo.client.payment.service.CieloCallErrorStatus.RESPONSE_ABOUT_INVALID_REQUEST;
import static org.killbill.billing.plugin.cielo.client.payment.service.CieloCallErrorStatus.RESPONSE_INVALID;
import static org.killbill.billing.plugin.cielo.client.payment.service.CieloCallErrorStatus.RESPONSE_NOT_RECEIVED;
import static org.killbill.billing.plugin.cielo.client.payment.service.CieloCallErrorStatus.UNKNOWN_FAILURE;

public class CieloPaymentRequestSender implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(CieloPaymentRequestSender.class);

    private final CieloClientRegistry cieloClientRegistry;

    public CieloPaymentRequestSender(final CieloClientRegistry cieloClientRegistry) {
        this.cieloClientRegistry = cieloClientRegistry;
    }

    public CieloCallResult<Sale> create(final Sale sale) {
        return callCielo(new IngenicoCall<CieloEcommerce, Sale>() {
            @Override
            public Sale apply(final CieloEcommerce client) throws CieloRequestException, IOException {
                return client.createSale(sale);
            }
        });
    }

    public CieloCallResult<Sale> capture(final  String paymentId, final Integer amount) {
        return callCielo(new IngenicoCall<CieloEcommerce, Sale>() {
            @Override
            public Sale apply(final CieloEcommerce client) throws CieloRequestException, IOException {
                return client.captureSale(paymentId, amount);
            }
        });
    }

    public CieloCallResult<Sale> get(final String paymentId) {
        return callCielo(new IngenicoCall<CieloEcommerce, Sale>() {
            @Override
            public Sale apply(final CieloEcommerce client) throws CieloRequestException, IOException {
                return client.querySale(paymentId);
            }
        });
    }

    public CieloCallResult<Sale> refund(final String paymentId, final Integer amount) {
        return callCielo(new IngenicoCall<CieloEcommerce, Sale>() {
            @Override
            public Sale apply(final CieloEcommerce client) throws CieloRequestException, IOException {
                //return client.refund(paymentId, amount);
                return null;
            }
        });
    }

    public CieloCallResult<Sale> cancel(final  String paymentId) {
        return callCielo(new IngenicoCall<CieloEcommerce, Sale>() {
            @Override
            public Sale apply(final CieloEcommerce client) throws CieloRequestException, IOException {
                return client.cancelSale(paymentId);
            }
        });
    }

    public CieloCallResult<CreditCard> createToken(final CreditCard createTokenRequest) {
        return callCielo(new IngenicoCall<CieloEcommerce, CreditCard>() {
            @Override
            public CreditCard apply(final CieloEcommerce client) throws CieloRequestException, IOException {
                return client.tokenizeCard(createTokenRequest);
            }
        });
    }

    private <T> CieloCallResult<T> callCielo(final IngenicoCall<CieloEcommerce, T> ingenicoCall) {
        final long startTime = System.currentTimeMillis();
        try {
            final CieloEcommerce client = cieloClientRegistry.getCieloClient();
            final T result = ingenicoCall.apply(client);

            final long duration = System.currentTimeMillis() - startTime;
            return new SuccessfulCieloCall<T>(result, duration);
        } catch (final Exception e) {
            final long duration = System.currentTimeMillis() - startTime;
            logger.warn("Exception during Ingenico sale", e);

            final UnSuccessfulCieloCall<T> unsuccessfulResult = mapExceptionToCallResult(e);
            unsuccessfulResult.setDuration(duration);
            return unsuccessfulResult;
        }
    }

    /**
     * Educated guess approach to transform CXF exceptions into error status codes.
     * In the future if we encounter further different cases it makes sense to change this if/else structure to a map with lookup.
     */
    private <T> UnSuccessfulCieloCall<T> mapExceptionToCallResult(final Exception e) {
        //noinspection ThrowableResultOfMethodCallIgnored
        final Throwable rootCause = Throwables.getRootCause(e);
        final String errorMessage = rootCause.getMessage();
        if (rootCause instanceof ConnectException) {
            return new UnSuccessfulCieloCall<T>(REQUEST_NOT_SEND, rootCause);
        } else if (rootCause instanceof SocketTimeoutException) {
            // read timeout
            if (errorMessage.contains("Read timed out")) {
                return new UnSuccessfulCieloCall<T>(RESPONSE_NOT_RECEIVED, rootCause);
            } else if (errorMessage.contains("Unexpected end of file from server")) {
                return new UnSuccessfulCieloCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof SocketException) {
            if (errorMessage.contains("Unexpected end of file from server")) {
                return new UnSuccessfulCieloCall<T>(RESPONSE_INVALID, rootCause);
            }
        } else if (rootCause instanceof UnknownHostException) {
            return new UnSuccessfulCieloCall<T>(REQUEST_NOT_SEND, rootCause);
        } else if (rootCause instanceof IllegalArgumentException) {
            return new UnSuccessfulCieloCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause);
        } else if (rootCause instanceof CieloRequestException) {
            CieloErrors errors = parseApiError((CieloRequestException) rootCause);
            return new UnSuccessfulCieloCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, errors.getError(), errors.getPaymentId(), errors.getStatus());
        } else if (rootCause instanceof IOException) {
            if (errorMessage.contains("Invalid Http response")) {
                // unparsable data as response
                return new UnSuccessfulCieloCall<T>(RESPONSE_INVALID, rootCause);
            } else if (errorMessage.contains("Bogus chunk size")) {
                return new UnSuccessfulCieloCall<T>(RESPONSE_INVALID, rootCause);
            }
        }

        return new UnSuccessfulCieloCall<T>(UNKNOWN_FAILURE, rootCause);
    }

    @Override
    public void close() throws IOException {
        cieloClientRegistry.close();
    }

    private interface IngenicoCall<T, R> {

        R apply(T t) throws CieloRequestException, IOException;
    }

    private static class CieloErrors {
        final private CieloError error;
        final private String status;
        final private String paymentId;

        public CieloErrors(final CieloError error) {
            this(error, null, null);
        }

        private CieloErrors(final CieloError error, final String status, final String paymentId) {
            this.error = error;
            this.status = status;
            this.paymentId = paymentId;
        }

        public CieloError getError() {
            return error;
        }

        public String getStatus() {
            return status;
        }

        public String getPaymentId() {
            return paymentId;
        }
    }

    private CieloErrors parseApiError(final CieloRequestException e) {
        CieloError error = e.getError();
        return new CieloErrors(error);
    }
}
