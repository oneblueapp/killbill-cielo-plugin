package org.killbill.billing.plugin.cielo.dao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.jooq.UpdateSetMoreStep;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.api.PluginProperties;
import org.killbill.billing.plugin.dao.payment.PluginPaymentDao;
import org.killbill.billing.plugin.cielo.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.cielo.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.cielo.client.model.PurchaseResult;
import org.killbill.billing.plugin.cielo.dao.gen.tables.records.CieloPaymentMethodsRecord;
import org.killbill.billing.plugin.cielo.dao.gen.tables.records.CieloResponsesRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import static org.killbill.billing.plugin.cielo.dao.gen.tables.CieloPaymentMethods.CIELO_PAYMENT_METHODS;
import static org.killbill.billing.plugin.cielo.dao.gen.tables.CieloResponses.CIELO_RESPONSES;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class CieloDao extends PluginPaymentDao<CieloResponsesRecord, org.killbill.billing.plugin.cielo.dao.gen.tables.CieloResponses, CieloPaymentMethodsRecord, org.killbill.billing.plugin.cielo.dao.gen.tables.CieloPaymentMethods> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Joiner JOINER = Joiner.on(",");

    public CieloDao(final DataSource dataSource) throws SQLException {
        super(CIELO_RESPONSES, CIELO_PAYMENT_METHODS, dataSource);
    }

    // Payment methods

    public void setPaymentMethodToken(final String kbPaymentMethodId, final String token, final String kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                new WithConnectionCallback<CieloResponsesRecord>() {
                    @Override
                    public CieloResponsesRecord withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .update(CIELO_PAYMENT_METHODS)
                                .set(CIELO_PAYMENT_METHODS.TOKEN, token)
                                .where(CIELO_PAYMENT_METHODS.KB_PAYMENT_METHOD_ID.equal(kbPaymentMethodId))
                                .and(CIELO_PAYMENT_METHODS.KB_TENANT_ID.equal(kbTenantId))
                                .and(CIELO_PAYMENT_METHODS.IS_DELETED.equal(FALSE))
                                .execute();
                        return null;
                    }
                });
    }

    public CieloResponsesRecord getResponse(final UUID kbPaymentId, final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<CieloResponsesRecord>() {
                           @Override
                           public CieloResponsesRecord withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(CIELO_RESPONSES)
                                         .where(DSL.field(CIELO_RESPONSES.getName() + "." + KB_PAYMENT_ID).equal(kbPaymentId.toString()))
                                         .and(DSL.field(CIELO_RESPONSES.getName() + "." + KB_TENANT_ID).equal(kbTenantId.toString()))
                                         .orderBy(DSL.field(CIELO_RESPONSES.getName() + "." + RECORD_ID).desc())
                                         .limit(1)
                                         .fetchOne();
                           }
                       });
    }

    public void addResponse(final UUID kbAccountId,
                            final UUID kbPaymentId,
                            final UUID kbPaymentTransactionId,
                            final TransactionType transactionType,
                            final BigDecimal amount,
                            final Currency currency,
                            final PurchaseResult result,
                            final DateTime utcNow,
                            final UUID kbTenantId) throws SQLException {
        final String additionalData = getAdditionalData(result);
        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                                .insertInto(CIELO_RESPONSES,
                                        CIELO_RESPONSES.KB_ACCOUNT_ID,
                                        CIELO_RESPONSES.KB_PAYMENT_ID,
                                        CIELO_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                        CIELO_RESPONSES.TRANSACTION_TYPE,
                                        CIELO_RESPONSES.AMOUNT,
                                        CIELO_RESPONSES.CURRENCY,
                                        CIELO_RESPONSES.CIELO_PAYMENT_ID,
                                        CIELO_RESPONSES.CIELO_STATUS,
                                        CIELO_RESPONSES.CIELO_RESULT,
                                        CIELO_RESPONSES.CIELO_AUTHORIZATION_CODE,
                                        CIELO_RESPONSES.CIELO_ERROR_CODE,
                                        CIELO_RESPONSES.CIELO_ERROR_MESSAGE,
                                        CIELO_RESPONSES.FRAUD_AVS_RESULT,
                                        CIELO_RESPONSES.FRAUD_CVV_RESULT,
                                        CIELO_RESPONSES.FRAUD_SERVICE,
                                        CIELO_RESPONSES.PAYMENT_INTERNAL_REF,
                                        CIELO_RESPONSES.ADDITIONAL_DATA,
                                        CIELO_RESPONSES.CREATED_DATE,
                                        CIELO_RESPONSES.KB_TENANT_ID)
                                .values(kbAccountId.toString(),
                                        kbPaymentId.toString(),
                                        kbPaymentTransactionId.toString(),
                                        transactionType.toString(),
                                        amount,
                                        currency.toString(),
                                        result.getPaymentId(),
                                        result.getStatus(),
                                        result.getResult().isPresent() ? result.getResult().get().toString() : null,
                                        result.getAuthorizationCode(),
                                        result.getErrorCode(),
                                        result.getErrorMessage(),
                                        result.getFraudAvsResult(),
                                        result.getFraudCvvResult(),
                                        result.getFraudResult(),
                                        result.getPaymentTransactionExternalKey(),
                                        additionalData,
                                        toTimestamp(utcNow),
                                        kbTenantId.toString())
                                .execute();
                        return null;
                    }
                });
    }

    public void addResponse(final UUID kbAccountId,
                            final UUID kbPaymentId,
                            final UUID kbPaymentTransactionId,
                            final TransactionType transactionType,
                            @Nullable final BigDecimal amount,
                            @Nullable final Currency currency,
                            final PaymentModificationResponse result,
                            final DateTime utcNow,
                            final UUID kbTenantId) throws SQLException {
        final String additionalData = getAdditionalData(result);

        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                           .insertInto(CIELO_RESPONSES,
                                       CIELO_RESPONSES.KB_ACCOUNT_ID,
                                       CIELO_RESPONSES.KB_PAYMENT_ID,
                                       CIELO_RESPONSES.KB_PAYMENT_TRANSACTION_ID,
                                       CIELO_RESPONSES.TRANSACTION_TYPE,
                                       CIELO_RESPONSES.AMOUNT,
                                       CIELO_RESPONSES.CURRENCY,
                                       CIELO_RESPONSES.CIELO_PAYMENT_ID,
                                       CIELO_RESPONSES.CIELO_STATUS,
                                       CIELO_RESPONSES.CIELO_RESULT,
                                       CIELO_RESPONSES.CIELO_AUTHORIZATION_CODE,
                                       CIELO_RESPONSES.CIELO_ERROR_CODE,
                                       CIELO_RESPONSES.CIELO_ERROR_MESSAGE,
                                       CIELO_RESPONSES.FRAUD_AVS_RESULT,
                                       CIELO_RESPONSES.FRAUD_CVV_RESULT,
                                       CIELO_RESPONSES.FRAUD_SERVICE,
                                       CIELO_RESPONSES.PAYMENT_INTERNAL_REF,
                                       CIELO_RESPONSES.ADDITIONAL_DATA,
                                       CIELO_RESPONSES.CREATED_DATE,
                                       CIELO_RESPONSES.KB_TENANT_ID)
                           .values(kbAccountId.toString(),
                                   kbPaymentId.toString(),
                                   kbPaymentTransactionId.toString(),
                                   transactionType.toString(),
                                   amount,
                                   currency.toString(),
                                   result.getPaymentId(),
                                   result.getStatus(),
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   null,
                                   additionalData,
                                   toTimestamp(utcNow),
                                   kbTenantId.toString())
                           .execute();
                        return null;
                    }
                });
    }

    public CieloResponsesRecord updateResponse(final UUID kbPaymentTransactionId, final Iterable<PluginProperty> additionalPluginProperties, final UUID kbTenantId) throws SQLException {
        return updateResponse(kbPaymentTransactionId, null, null, additionalPluginProperties, kbTenantId);
    }

    /**
     * Update the PSP reference and additional data of the latest response row for a payment transaction
     *
     * @param kbPaymentTransactionId       Kill Bill payment transaction id
     * @param status
     *@param paymentServiceProviderResult New PSP result (null if unchanged)
     * @param additionalPluginProperties   Latest properties
     * @param kbTenantId                   Kill Bill tenant id    @return the latest version of the response row, null if one couldn't be found
     * @throws SQLException For any unexpected SQL error
     */
    public CieloResponsesRecord updateResponse(final UUID kbPaymentTransactionId, final String status, @Nullable final PaymentServiceProviderResult paymentServiceProviderResult, final Iterable<PluginProperty> additionalPluginProperties, final UUID kbTenantId) throws SQLException {
        final Map<String, Object> additionalProperties = PluginProperties.toMap(additionalPluginProperties);

        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<CieloResponsesRecord>() {
                           @Override
                           public CieloResponsesRecord withConnection(final Connection conn) throws SQLException {
                               final CieloResponsesRecord response = DSL.using(conn, dialect, settings)
                                                                        .selectFrom(CIELO_RESPONSES)
                                                                        .where(CIELO_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                                                        .and(CIELO_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                                                        .orderBy(CIELO_RESPONSES.RECORD_ID.desc())
                                                                        .limit(1)
                                                                        .fetchOne();

                               if (response == null) {
                                   return null;
                               }

                               final Map originalData = new HashMap(fromAdditionalData(response.getAdditionalData()));
                               originalData.putAll(additionalProperties);
//                               final String cieloPaymentId = response.getCieloPaymentId();
//                               if (cieloPaymentId != null) {
//                                   originalData.remove(CIELO_CALL_ERROR_STATUS);
//                                   originalData.remove(EXCEPTION_CLASS);
//                                   originalData.remove(EXCEPTION_MESSAGE);
//                               }
                               final String mergedAdditionalData = asString(originalData);

                               UpdateSetMoreStep<CieloResponsesRecord> step = DSL.using(conn, dialect, settings)
                                                                                 .update(CIELO_RESPONSES)
                                                                                 .set(CIELO_RESPONSES.ADDITIONAL_DATA, mergedAdditionalData);
                               if (status != null) {
                                   step = step.set(CIELO_RESPONSES.CIELO_STATUS, status);
                               }
                               if (paymentServiceProviderResult != null) {
                                   step = step.set(CIELO_RESPONSES.CIELO_RESULT, paymentServiceProviderResult.toString());
                               }
                               step.where(CIELO_RESPONSES.RECORD_ID.equal(response.getRecordId()))
                                   .execute();

                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(CIELO_RESPONSES)
                                         .where(CIELO_RESPONSES.KB_PAYMENT_TRANSACTION_ID.equal(kbPaymentTransactionId.toString()))
                                         .and(CIELO_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                         .orderBy(CIELO_RESPONSES.RECORD_ID.desc())
                                         .limit(1)
                                         .fetchOne();
                           }
                       });
    }

    private String getAdditionalData(final PurchaseResult result) throws SQLException {
        final Map<String, String> additionalDataMap = new HashMap<String, String>();
        if (result.getAdditionalData() != null && !result.getAdditionalData().isEmpty()) {
            additionalDataMap.putAll(result.getAdditionalData());
        }
        if (additionalDataMap.isEmpty()) {
            return null;
        } else {
            return asString(additionalDataMap);
        }
    }

    private String getAdditionalData(final PaymentModificationResponse response) throws SQLException {
        return asString(response.getAdditionalData());
    }

    public static Map fromAdditionalData(@Nullable final String additionalData) {
        if (additionalData == null) {
            return ImmutableMap.of();
        }

        try {
            return objectMapper.readValue(additionalData, Map.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
