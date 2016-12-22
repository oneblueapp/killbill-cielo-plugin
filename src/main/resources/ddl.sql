/*! SET storage_engine=INNODB */;
drop table if exists cielo_responses;
create table cielo_responses (
  record_id int(11) unsigned not null auto_increment
, kb_account_id char(36) not null
, kb_payment_id char(36) not null
, kb_payment_transaction_id char(36) not null
, transaction_type varchar(32) not null
, amount numeric(15,9)
, currency char(3)
, cielo_payment_id varchar(50)
, cielo_status varchar(50)
, cielo_result varchar(50)
, cielo_merchant_reference varchar(64)
, cielo_authorization_code varchar(255)
, cielo_error_code varchar(64)
, cielo_error_message varchar(255)
, payment_internal_ref varchar(64)
, fraud_avs_result char(1)
, fraud_cvv_result char(1)
, fraud_service varchar(255)
, additional_data longtext default null
, created_date datetime not null
, kb_tenant_id char(36) not null
, primary key(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
create index cielo_responses_kb_payment_id on cielo_responses(kb_payment_id);
create index cielo_responses_kb_payment_transaction_id on cielo_responses(kb_payment_transaction_id);

drop table if exists cielo_payment_methods;
create table cielo_payment_methods (
  record_id int(11) unsigned not null auto_increment
, kb_account_id char(36) not null
, kb_payment_method_id char(36) not null
, token varchar(255) default null
, cc_first_name varchar(255) default null
, cc_last_name varchar(255) default null
, cc_type varchar(255) default null
, cc_exp_month varchar(255) default null
, cc_exp_year varchar(255) default null
, cc_number varchar(255) default null
, cc_last_4 varchar(255) default null
, cc_start_month varchar(255) default null
, cc_start_year varchar(255) default null
, cc_issue_number varchar(255) default null
, cc_verification_value varchar(255) default null
, cc_track_data varchar(255) default null
, address1 varchar(255) default null
, address2 varchar(255) default null
, city varchar(255) default null
, state varchar(255) default null
, zip varchar(255) default null
, country varchar(255) default null
, is_default boolean not null default false
, is_deleted boolean not null default false
, additional_data longtext default null
, created_date datetime not null
, updated_date datetime not null
, kb_tenant_id char(36) not null
, primary key(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
create unique index cielo_payment_methods_kb_payment_id on cielo_payment_methods(kb_payment_method_id);
