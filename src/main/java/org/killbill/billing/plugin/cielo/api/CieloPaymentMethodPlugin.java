package org.killbill.billing.plugin.cielo.api;

import java.util.UUID;

import org.killbill.billing.plugin.api.payment.PluginPaymentMethodPlugin;
import org.killbill.billing.plugin.cielo.dao.CieloDao;
import org.killbill.billing.plugin.cielo.dao.gen.tables.records.CieloPaymentMethodsRecord;

/**
 * Created by otaviosoares on 15/11/16.
 */
public class CieloPaymentMethodPlugin extends PluginPaymentMethodPlugin {
    public CieloPaymentMethodPlugin(CieloPaymentMethodsRecord record) {
        super(record.getKbPaymentMethodId() == null ? null : UUID.fromString(record.getKbPaymentMethodId()),
                record.getToken(),
                (record.getIsDefault() != null) && CieloDao.TRUE == record.getIsDefault(),
                CieloModelPluginBase.buildPluginProperties(record.getAdditionalData()));
    }
}
