-- H2 schema (local development and fast integration tests).
create table customer (
  id            uuid          not null,
  email         varchar(255)  not null,
  full_name     varchar(200)  not null,
  version       bigint        not null,
  created_at    timestamp(6)  not null,
  updated_at    timestamp(6)  not null,
  created_by    varchar(100),
  updated_by    varchar(100),
  constraint pk_customer primary key (id),
  constraint uk_customer_email unique (email)
);

create table product (
  id              uuid          not null,
  sku             varchar(64)   not null,
  name            varchar(200)  not null,
  description     varchar(2000),
  price_amount    numeric(19,4) not null,
  price_currency  varchar(3)    not null,
  active          boolean       not null,
  version         bigint        not null,
  created_at      timestamp(6)  not null,
  updated_at      timestamp(6)  not null,
  created_by      varchar(100),
  updated_by      varchar(100),
  constraint pk_product primary key (id),
  constraint uk_product_sku unique (sku)
);

create table inventory_item (
  id                 uuid         not null,
  product_id         uuid         not null,
  quantity_on_hand   integer      not null,
  quantity_reserved  integer      not null,
  version            bigint       not null,
  created_at         timestamp(6) not null,
  updated_at         timestamp(6) not null,
  created_by         varchar(100),
  updated_by         varchar(100),
  constraint pk_inventory_item primary key (id),
  constraint uk_inventory_product unique (product_id),
  constraint fk_inventory_product foreign key (product_id) references product (id)
);

create table orders (
  id              uuid          not null,
  order_number    varchar(32)   not null,
  customer_id     uuid          not null,
  status          varchar(20)   not null,
  total_amount    numeric(19,4) not null,
  total_currency  varchar(3)    not null,
  placed_at       timestamp(6)  not null,
  cancel_reason   varchar(500),
  version         bigint        not null,
  created_at      timestamp(6)  not null,
  updated_at      timestamp(6)  not null,
  created_by      varchar(100),
  updated_by      varchar(100),
  constraint pk_orders primary key (id),
  constraint uk_orders_number unique (order_number),
  constraint ck_orders_status check (status in ('PENDING','CONFIRMED','PAID','SHIPPED','DELIVERED','CANCELLED')),
  constraint fk_orders_customer foreign key (customer_id) references customer (id)
);
create index ix_orders_customer on orders (customer_id, placed_at desc);
create index ix_orders_status on orders (status);

create table order_item (
  id                    uuid          not null,
  order_id              uuid          not null,
  line_number           integer       not null,
  product_id            uuid          not null,
  sku                   varchar(64)   not null,
  product_name          varchar(200)  not null,
  quantity              integer       not null,
  unit_price_amount     numeric(19,4) not null,
  unit_price_currency   varchar(3)    not null,
  line_total_amount     numeric(19,4) not null,
  line_total_currency   varchar(3)    not null,
  constraint pk_order_item primary key (id),
  constraint uk_order_item_line unique (order_id, line_number),
  constraint fk_order_item_order foreign key (order_id) references orders (id)
);

create table outbox_event (
  id              uuid          not null,
  aggregate_type  varchar(100)  not null,
  aggregate_id    varchar(64)   not null,
  event_type      varchar(100)  not null,
  payload         clob          not null,
  occurred_at     timestamp(6)  not null,
  published_at    timestamp(6),
  attempts        integer       not null,
  last_error      varchar(2000),
  constraint pk_outbox_event primary key (id)
);
create index ix_outbox_pending on outbox_event (published_at, occurred_at);

create table idempotency_key (
  idem_key       varchar(128)  not null,
  request_hash   varchar(64)   not null,
  status         varchar(20)   not null,
  response_body  clob,
  created_at     timestamp(6)  not null,
  expires_at     timestamp(6)  not null,
  constraint pk_idempotency_key primary key (idem_key),
  constraint ck_idempotency_status check (status in ('IN_PROGRESS','COMPLETED'))
);
create index ix_idempotency_expires on idempotency_key (expires_at);
