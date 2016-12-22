package org.killbill.billing.plugin.cielo.core;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;
import org.killbill.billing.plugin.cielo.client.CieloClient;
import org.killbill.billing.plugin.cielo.client.CieloClientRegistry;
import org.killbill.billing.plugin.cielo.client.CieloConfigProperties;
import org.killbill.billing.plugin.cielo.client.payment.builder.CieloRequestFactory;
import org.killbill.billing.plugin.cielo.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.cielo.client.payment.converter.impl.PaymentInfoConverterService;
import org.killbill.billing.plugin.cielo.client.payment.service.CieloPaymentRequestSender;

import java.util.Properties;

/**
 * Created by otaviosoares on 14/11/16.
 */
public class CieloConfigurationHandler extends PluginTenantConfigurableConfigurationHandler<CieloClient> {
    public CieloConfigurationHandler(String pluginName, OSGIKillbillAPI osgiKillbillAPI, OSGIKillbillLogService osgiKillbillLogService) {
        super(pluginName, osgiKillbillAPI, osgiKillbillLogService);
    }

    @Override
    protected CieloClient createConfigurable(final Properties properties) {
        final CieloConfigProperties cieloConfigProperties = new CieloConfigProperties(properties);

        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final CieloRequestFactory cieloRequestFactory = new CieloRequestFactory(paymentInfoConverterManagement, cieloConfigProperties);

        final CieloClientRegistry cieloClientRegistry = new CieloClientRegistry(cieloConfigProperties);
        final CieloPaymentRequestSender cieloPaymentRequestSender = new CieloPaymentRequestSender(cieloClientRegistry);
        return new CieloClient(cieloRequestFactory, cieloPaymentRequestSender);
    }
}
