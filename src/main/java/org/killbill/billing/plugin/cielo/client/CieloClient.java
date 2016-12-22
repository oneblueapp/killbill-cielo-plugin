package org.killbill.billing.plugin.cielo.client;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import cieloecommerce.sdk.ecommerce.CreditCard;
import cieloecommerce.sdk.ecommerce.Payment;
import cieloecommerce.sdk.ecommerce.Sale;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.plugin.cielo.client.model.PaymentData;
import org.killbill.billing.plugin.cielo.client.model.PaymentInfo;
import org.killbill.billing.plugin.cielo.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.cielo.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.cielo.client.model.PurchaseResult;
import org.killbill.billing.plugin.cielo.client.model.SplitSettlementData;
import org.killbill.billing.plugin.cielo.client.model.UserData;
import org.killbill.billing.plugin.cielo.client.model.paymentinfo.Card;
import org.killbill.billing.plugin.cielo.client.payment.builder.CieloRequestFactory;
import org.killbill.billing.plugin.cielo.client.payment.service.BaseCieloPaymentServiceProviderPort;
import org.killbill.billing.plugin.cielo.client.payment.service.CieloCallErrorStatus;
import org.killbill.billing.plugin.cielo.client.payment.service.CieloCallResult;
import org.killbill.billing.plugin.cielo.client.payment.service.CieloPaymentRequestSender;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import static jdk.nashorn.internal.runtime.JSType.toInteger;
import static org.killbill.billing.plugin.cielo.client.model.PurchaseResult.EXCEPTION_CLASS;
import static org.killbill.billing.plugin.cielo.client.model.PurchaseResult.EXCEPTION_MESSAGE;
import static org.killbill.billing.plugin.cielo.client.model.PurchaseResult.INGENICO_CALL_ERROR_STATUS;
import static org.killbill.billing.plugin.cielo.client.model.PurchaseResult.UNKNOWN;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class CieloClient extends BaseCieloPaymentServiceProviderPort implements Closeable {

    private static final java.lang.String HMAC_ALGORITHM = "";
    private CieloRequestFactory cieloRequestFactory;
    private CieloPaymentRequestSender cieloPaymentRequestSender;

    public CieloClient(final CieloRequestFactory cieloRequestFactory, CieloPaymentRequestSender cieloPaymentRequestSender) {
        this.cieloRequestFactory = cieloRequestFactory;
        this.cieloPaymentRequestSender = cieloPaymentRequestSender;
    }

    @Override
    public void close() throws IOException {
        cieloPaymentRequestSender.close();
    }

    public PurchaseResult create(TransactionType transactionType, PaymentData<Card> paymentData, UserData userData, final SplitSettlementData splitSettlementData) {
        return authorisePurchaseOrCredit(transactionType, paymentData, userData, splitSettlementData);
    }

    private PurchaseResult authorisePurchaseOrCredit(TransactionType transactionType, final PaymentData<Card> paymentData, final UserData userData, final SplitSettlementData splitSettlementData) {
        Sale body = cieloRequestFactory.createPaymentRequest(paymentData, userData, splitSettlementData);
        final CieloCallResult<Sale> cieloCallResult = cieloPaymentRequestSender.create(body);

        if (!cieloCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtPurchase(transactionType.toString(), userData, paymentData, cieloCallResult);
        }

        final Sale result = cieloCallResult.getResult().get();
        Payment paymentResponse = result.getPayment();

        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(paymentResponse.getStatus().toString(), transactionType);

        final Map<String, String> additionalData = new HashMap<String, String>();

        return new PurchaseResult(
                paymentServiceProviderResult,
                paymentResponse.getPaymentId(),
                paymentResponse.getStatus().toString(),
                paymentResponse.getAuthorizationCode(),
                null,
                null,
                null,
                paymentData.getPaymentTransactionExternalKey(),
                additionalData);
    }

    private PurchaseResult handleTechnicalFailureAtPurchase(final String transactionType, final UserData userData, final PaymentData paymentData, final CieloCallResult<Sale> ingenicoCall) {
        logTransactionError(transactionType, userData, paymentData, ingenicoCall);
        return new PurchaseResult(paymentData.getPaymentTransactionExternalKey(), ingenicoCall);
    }

    public PaymentModificationResponse capture(final PaymentData paymentData, final String paymentId, final BigDecimal amount, final SplitSettlementData splitSettlementData) {
        final CieloCallResult<Sale> cieloCallResult = cieloPaymentRequestSender.capture(paymentId, toInteger(amount));

        if (!cieloCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtApprove(paymentId, paymentData, cieloCallResult);
        }

        Sale result = cieloCallResult.getResult().get();
        Payment paymentResponse = result.getPayment();

        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(paymentResponse.getStatus().toString(), null);

        return new PaymentModificationResponse<Sale>(paymentServiceProviderResult, paymentResponse.getStatus().toString(), paymentId);
    }

    private PaymentModificationResponse handleTechnicalFailureAtApprove(final String paymentId, final PaymentData paymentData, final CieloCallResult<Sale> ingenicoCall) {
        logTransactionError("capture", paymentId, paymentData, ingenicoCall);
        return new PaymentModificationResponse(paymentId, ingenicoCall, getModificationAdditionalErrorData(ingenicoCall));
    }

    public PaymentModificationResponse cancel(final String paymentId, final SplitSettlementData splitSettlementData) {
        final CieloCallResult<Sale> cieloCallResult = cieloPaymentRequestSender.cancel(paymentId);
        if (!cieloCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtCancel(paymentId, cieloCallResult);
        }
        return null;
    }

    private PaymentModificationResponse handleTechnicalFailureAtCancel(final String paymentId, final CieloCallResult<Sale> ingenicoCall) {
        logTransactionError("cancel", paymentId, null, ingenicoCall);
        return new PaymentModificationResponse(paymentId, ingenicoCall, getModificationAdditionalErrorData(ingenicoCall));
    }

    public PaymentModificationResponse refund(final PaymentData paymentData, final String paymentId, final BigDecimal amount, final SplitSettlementData splitSettlementData) {
        final CieloCallResult<Sale> cieloCallResult = cieloPaymentRequestSender.refund(paymentId, toInteger(amount));
        if (!cieloCallResult.receivedWellFormedResponse()) {
            return handleTechnicalFailureAtRefund(paymentId, paymentData, cieloCallResult);
        }
        return null;
    }

    private PaymentModificationResponse handleTechnicalFailureAtRefund(final String paymentId, final PaymentData paymentData, final CieloCallResult<Sale> ingenicoCall) {
        logTransactionError("refund", paymentId, null, ingenicoCall);
        return new PaymentModificationResponse(paymentId, ingenicoCall, getModificationAdditionalErrorData(ingenicoCall));
    }

    private Map<Object,Object> getModificationAdditionalErrorData(final CieloCallResult<?> ingenicoCall) {
        final Map<Object, Object> additionalDataMap = new HashMap<Object, Object>();
        final Optional<CieloCallErrorStatus> responseStatus = ingenicoCall.getResponseStatus();
        additionalDataMap.putAll(ImmutableMap.<Object, Object>of(INGENICO_CALL_ERROR_STATUS, responseStatus.isPresent() ? responseStatus.get() : "",
                                                                 EXCEPTION_CLASS, ingenicoCall.getExceptionClass().or(UNKNOWN),
                                                                 EXCEPTION_MESSAGE, ingenicoCall.getExceptionMessage().or(UNKNOWN)));

        return additionalDataMap;
    }

    public String tokenizeCreditCard(PaymentInfo paymentInfo, UserData userData) throws PaymentPluginApiException {
//        CreditCard body = cieloRequestFactory.createTokenRequest(paymentInfo, userData);
//        final CieloCallResult<CreditCard> cieloCallResult = cieloPaymentRequestSender.createToken(body);
//        if (!cieloCallResult.receivedWellFormedResponse()) {
//            if (cieloCallResult.getError().isPresent() && cieloCallResult.getError().get().size() > 0) {
//                throw new PaymentPluginApiException(cieloCallResult.getError().get().get(0).getCode(), cieloCallResult.getError().get().get(0).getMessage());
//            }
//            return null;
//        }
//
//        return cieloCallResult.getResult().get().getToken();
        return null;
    }

    public PaymentModificationResponse getPaymentInfo(final String paymentId, TransactionType transactionType) {
        final CieloCallResult<Sale> cieloCallResult = cieloPaymentRequestSender.get(paymentId);
        if (!cieloCallResult.receivedWellFormedResponse()) {
            return null;
        }

        Sale result = cieloCallResult.getResult().get();
        Payment payment = result.getPayment();

        final PaymentServiceProviderResult paymentServiceProviderResult = PaymentServiceProviderResult.getPaymentResultForId(payment.getStatus().toString(), transactionType);

        return new PaymentModificationResponse(paymentServiceProviderResult, payment.getStatus().toString(), payment.getPaymentId());
    }
}
