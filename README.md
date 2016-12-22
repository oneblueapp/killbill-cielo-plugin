killbill-cielo-plugin
======================

**This is a WIP. Any attempt to use it will fail miserably.**

Plugin to use [Cielo](https://www.cielo.com.br) as a gateway.

Release builds are available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.kill-bill.billing.plugin.java%22%20AND%20a%3A%22cielo-plugin%22) with coordinates `org.kill-bill.billing.plugin.java:cielo-plugin`.

Kill Bill compatibility
-----------------------

| Plugin version | Kill Bill version |
| -------------: | ----------------: |
| 0.0.y          | 0.18.z            |

Requirements
------------

The plugin needs a database. The latest version of the schema can be found [here](https://github.com/Artyou/killbill-cielo-plugin/blob/master/src/main/resources/ddl.sql).

Configuration
-------------

The following properties are required:

* `org.killbill.billing.plugin.cielo.merchantId=your merchant id
* `org.killbill.billing.plugin.cielo.merchantKey=your merchant key
* `org.killbill.billing.plugin.cielo.environment=production|sandbox

These properties can be specified globally via System Properties or on a per tenant basis:

```
curl -v \
     -X POST \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
     -H 'Content-Type: text/plain' \
     -d 'org.killbill.billing.plugin.cielo.merchantId=######
         org.killbill.billing.plugin.cielo.merchantKey=#####
         org.killbill.billing.plugin.cielo.environment=sandbox' \
     http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/killbill-cielo
```

Usage
-----

Add a payment method (Credit Card):

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{
       "pluginName": "killbill-cielo",
       "pluginInfo": {
         "properties": [
          {
            "key": "ccAlias",
            "value": "My personal card"
          },
          {
            "key": "ccNumber",
            "value": "000000000000"
          },
          {
            "key": "ccType",
            "value": "VISA|MASTERCARD (..)"
          },
          {
            "key": "ccExpirationMonth",
            "value": "00"
          },
          {
            "key": "ccExpirationYear",
            "value": "0000"
          },
          {
            "key": "ccVerificationValue",
            "value": "000"
          },
          {
            "key": "ccFirstName",
            "value": "Card holder first name"
          },
          {
            "key": "ccLastName",
            "value": "Card holder last name"
          },
          {
            "key": "country",
            "value": "CA"
          }
         ]
       }
     }' \
     "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/paymentMethods?isDefault=true"
```

Notes:
* Make sure to replace *ACCOUNT_ID* with the id of the Kill Bill account

To trigger a payment:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{"transactionType":"PURCHASE","amount":"500","currency":"USD","transactionExternalKey":"INV-'$(uuidgen)'-PURCHASE"}' \
    "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/payments"
```

Notes:
* Make sure to replace *ACCOUNT_ID* with the id of the Kill Bill account

You can verify the payment via:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/payments?withPluginInfo=true"
```

Notes:
* Make sure to replace *ACCOUNT_ID* with the id of the Kill Bill account
