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

import cieloecommerce.sdk.ecommerce.request.CieloError;
import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public interface CieloCallResult<T> {

    Optional<T> getResult();

    long getDuration();

    String getPaymentId();

    String getStatus();

    Optional<CieloCallErrorStatus> getResponseStatus();

    Optional<String> getExceptionClass();

    Optional<String> getExceptionMessage();

    Optional<CieloError> getError();

    boolean receivedWellFormedResponse();

}

abstract class CieloCallBase<T> implements CieloCallResult<T> {
    private final String paymentId;
    private final String status;

    public CieloCallBase(String paymentId, String status) {
        this.paymentId = paymentId;
        this.status = status;
    }

    @Override
    public Optional<T> getResult() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDuration() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPaymentId() {
        return paymentId;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Optional<CieloCallErrorStatus> getResponseStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> getExceptionClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> getExceptionMessage() {
        throw new UnsupportedOperationException();
    }

    public Optional<CieloError> getError() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean receivedWellFormedResponse() {
        throw new UnsupportedOperationException();
    }
}

class SuccessfulCieloCall<T> extends CieloCallBase {

    private final T result;

    private final long duration;

    public SuccessfulCieloCall(final T result, long duration) {
        this(result, null, null, duration);
    }

    public SuccessfulCieloCall(final T result, String paymentId, String status, long duration) {
        super(paymentId, status);
        this.result = checkNotNull(result, "result");
        this.duration = duration;
    }

    @Override
    public Optional<T> getResult() {
        return Optional.of(result);
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public Optional<CieloCallErrorStatus> getResponseStatus() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getExceptionClass() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.absent();
    }

    @Override
    public Optional<CieloError> getError() {
        return null;
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SuccessfulCieloCall{");
        sb.append("result=").append(result);
        sb.append(" }");
        return sb.toString();
    }
}

class UnSuccessfulCieloCall<T> extends CieloCallBase {

    private final CieloCallErrorStatus responseStatus;
    private final String exceptionClass;
    private final String exceptionMessage;
    private final CieloError error;
    private long duration;

    UnSuccessfulCieloCall(final CieloCallErrorStatus responseStatus, final Throwable rootCause) {
        this(responseStatus, rootCause, null, null, null);
    }

    UnSuccessfulCieloCall(final CieloCallErrorStatus responseStatus, final Throwable rootCause, CieloError error) {
        this(responseStatus, rootCause, error, null, null);
    }

    UnSuccessfulCieloCall(final CieloCallErrorStatus responseStatus, final Throwable rootCause, CieloError error, String paymentId, String status) {
        super(paymentId, status);
        this.responseStatus = responseStatus;
        this.exceptionClass = rootCause.getClass().getCanonicalName();
        this.exceptionMessage = rootCause.getMessage();
        this.error = error;
    }

    @Override
    public Optional<T> getResult() {
        return Optional.absent();
    }

    @Override
    public long getDuration() {
        return duration;
    }

    public void setDuration(final long duration) {
        this.duration = duration;
    }

    @Override
    public Optional<CieloCallErrorStatus> getResponseStatus() {
        return Optional.of(responseStatus);
    }

    @Override
    public Optional<String> getExceptionClass() {
        return Optional.of(exceptionClass);
    }

    @Override
    public Optional<String> getExceptionMessage() {
        return Optional.of(exceptionMessage);
    }

    public Optional<CieloError> getError() {
        return Optional.of(error);
    }

    @Override
    public boolean receivedWellFormedResponse() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UnSuccessfulCieloCall{");
        sb.append("responseStatus=").append(responseStatus);
        sb.append(", exceptionMessage='").append(exceptionMessage).append('\'');
        sb.append(", exceptionClass='").append(exceptionClass).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}
