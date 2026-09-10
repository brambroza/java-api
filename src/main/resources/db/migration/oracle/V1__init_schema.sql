-- Oracle schema (19c / 21c / 23ai). UUID -> raw(16), boolean -> number(1,0), Instant -> timestamp(9).
-- On Oracle 23ai you may switch 'active' to the native BOOLEAN type.
create table customer (
  id            raw(16)            not null,
  email         varchar2(255 char) not null,
  full_name     varchar2(200 char) not null,
  version       number(19,0)       not null,
  created_at    timestamp(9)       not null,
  updated_at    timestamp(9)       not null,
  created_by    varchar2(100 char),
  updated_by    varchar2(100 char),
  constraint pk_customer primary key (id),
  constraint uk_customer_email unique (email)
);

create table product (
  id              raw(16)             not null,
  sku             varchar2(64 char)   not null,
  name            varchar2(200 char)  not null,
  description     varchar2(2000 char),
  price_amount    number(19,4)        not null,
  price_currency  varchar2(3 char)    not null,
  active          number(1,0)         not null,
  version         number(19,0)        not null,
  created_at      timestamp(9)        not null,
  updated_at      timestamp(9)        not null,
  created_by      varchar2(100 char),
  updated_by      varchar2(100 char),
  constraint pk_product primary key (id),
  constraint uk_product_sku unique (sku)
);

create table inventory_item (
  id                 raw(16)      not null,
  product_id         raw(16)      not null,
  quantity_on_hand   number(10,0) not null,
  quantity_reserved  number(10,0) not null,
  version            number(19,0) not null,
  created_at         timestamp(9) not null,
  updated_at         timestamp(9) not null,
  created_by         varchar2(100 char),
  updated_by         varchar2(100 char),
  constraint pk_inventory_item primary key (id),
  constraint uk_inventory_product unique (product_id),
  constraint fk_inventory_product foreign key (product_id) references product (id)
);

create table orders (
  id              raw(16)            not null,
  order_number    varchar2(32 char)  not null,
  customer_id     raw(16)            not null,
  status          varchar2(20 char)  not null,
  total_amount    number(19,4)       not null,
  total_currency  varchar2(3 char)   not null,
  placed_at       timestamp(9)       not null,
  cancel_reason   varchar2(500 char),
  version         number(19,0)       not null,
  created_at      timestamp(9)       not null,
  updated_at      timestamp(9)       not null,
  created_by      varchar2(100 char),
  updated_by      varchar2(100 char),
  constraint pk_orders primary key (id),
  constraint uk_orders_number unique (order_number),
  constraint ck_orders_status check (status in ('PENDING','CONFIRMED','PAID','SHIPPED','DELIVERED','CANCELLED')),
  constraint fk_orders_customer foreign key (customer_id) references customer (id)
);
create index ix_orders_customer on orders (customer_id, placed_at desc);
create index ix_orders_status on orders (status);

create table order_item (
  id                    raw(16)            not null,
  order_id              raw(16)            not null,
  line_number           number(10,0)       not null,
  product_id            raw(16)            not null,
  sku                   varchar2(64 char)  not null,
  product_name          varchar2(200 char) not null,
  quantity              number(10,0)       not null,
  unit_price_amount     number(19,4)       not null,
  unit_price_currency   varchar2(3 char)   not null,
  line_total_amount     number(19,4)       not null,
  line_total_currency   varchar2(3 char)   not null,
  constraint pk_order_item primary key (id),
  constraint uk_order_item_line unique (order_id, line_number),
  constraint fk_order_item_order foreign key (order_id) references orders (id)
);

create table outbox_event (
  id              raw(16)             not null,
  aggregate_type  varchar2(100 char)  not null,
  aggregate_id    varchar2(64 char)   not null,
  event_type      varchar2(100 char)  not null,
  payload         clob                not null,
  occurred_at     timestamp(9)        not null,
  published_at    timestamp(9),
  attempts        number(10,0)        not null,
  last_error      varchar2(2000 char),
  constraint pk_outbox_event primary key (id)
);
create index ix_outbox_pending on outbox_event (published_at, occurred_at);

create table idempotency_key (
  idem_key       varchar2(128 char) not null,
  request_hash   varchar2(64 char)  not null,
  status         varchar2(20 char)  not null,
  response_body  clob,
  created_at     timestamp(9)       not null,
  expires_at     timestamp(9)       not null,
  constraint pk_idempotency_key primary key (idem_key),
  constraint ck_idempotency_status check (status in ('IN_PROGRESS','COMPLETED'))
);
create index ix_idempotency_expires on idempotency_key (expires_at);
