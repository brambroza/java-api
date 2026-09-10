-- Microsoft SQL Server schema. UUID -> uniqueidentifier, boolean -> bit, Instant -> datetime2(7), decimal -> numeric.
-- Text columns are NVARCHAR (Unicode): plain VARCHAR is code-page bound on SQL Server and mangles non-Latin text.
-- The mssql profile sets hibernate.use_nationalized_character_data=true so the mapping expects these types.
create table customer (
  id            uniqueidentifier not null,
  email         nvarchar(255)     not null,
  full_name     nvarchar(200)     not null,
  version       bigint           not null,
  created_at    datetime2(7)     not null,
  updated_at    datetime2(7)     not null,
  created_by    nvarchar(100),
  updated_by    nvarchar(100),
  constraint pk_customer primary key (id),
  constraint uk_customer_email unique (email)
);

create table product (
  id              uniqueidentifier not null,
  sku             nvarchar(64)      not null,
  name            nvarchar(200)     not null,
  description     nvarchar(2000),
  price_amount    numeric(19,4)    not null,
  price_currency  nvarchar(3)       not null,
  active          bit              not null,
  version         bigint           not null,
  created_at      datetime2(7)     not null,
  updated_at      datetime2(7)     not null,
  created_by      nvarchar(100),
  updated_by      nvarchar(100),
  constraint pk_product primary key (id),
  constraint uk_product_sku unique (sku)
);

create table inventory_item (
  id                 uniqueidentifier not null,
  product_id         uniqueidentifier not null,
  quantity_on_hand   int              not null,
  quantity_reserved  int              not null,
  version            bigint           not null,
  created_at         datetime2(7)     not null,
  updated_at         datetime2(7)     not null,
  created_by         nvarchar(100),
  updated_by         nvarchar(100),
  constraint pk_inventory_item primary key (id),
  constraint uk_inventory_product unique (product_id),
  constraint fk_inventory_product foreign key (product_id) references product (id)
);

create table orders (
  id              uniqueidentifier not null,
  order_number    nvarchar(32)      not null,
  customer_id     uniqueidentifier not null,
  status          varchar(20)      not null,   -- enum codes are ASCII; mapping pins them to VARCHAR
  total_amount    numeric(19,4)    not null,
  total_currency  nvarchar(3)       not null,
  placed_at       datetime2(7)     not null,
  cancel_reason   nvarchar(500),
  version         bigint           not null,
  created_at      datetime2(7)     not null,
  updated_at      datetime2(7)     not null,
  created_by      nvarchar(100),
  updated_by      nvarchar(100),
  constraint pk_orders primary key (id),
  constraint uk_orders_number unique (order_number),
  constraint ck_orders_status check (status in ('PENDING','CONFIRMED','PAID','SHIPPED','DELIVERED','CANCELLED')),
  constraint fk_orders_customer foreign key (customer_id) references customer (id)
);
create index ix_orders_customer on orders (customer_id, placed_at desc);
create index ix_orders_status on orders (status);

create table order_item (
  id                    uniqueidentifier not null,
  order_id              uniqueidentifier not null,
  line_number           int              not null,
  product_id            uniqueidentifier not null,
  sku                   nvarchar(64)      not null,
  product_name          nvarchar(200)     not null,
  quantity              int              not null,
  unit_price_amount     numeric(19,4)    not null,
  unit_price_currency   nvarchar(3)       not null,
  line_total_amount     numeric(19,4)    not null,
  line_total_currency   nvarchar(3)       not null,
  constraint pk_order_item primary key (id),
  constraint uk_order_item_line unique (order_id, line_number),
  constraint fk_order_item_order foreign key (order_id) references orders (id)
);

create table outbox_event (
  id              uniqueidentifier not null,
  aggregate_type  nvarchar(100)     not null,
  aggregate_id    nvarchar(64)      not null,
  event_type      nvarchar(100)     not null,
  payload         nvarchar(max)     not null,
  occurred_at     datetime2(7)     not null,
  published_at    datetime2(7),
  attempts        int              not null,
  last_error      nvarchar(2000),
  constraint pk_outbox_event primary key (id)
);
-- vendor-specific tuning: filtered index for the pending scan
create index ix_outbox_pending on outbox_event (occurred_at) where published_at is null;

create table idempotency_key (
  idem_key       nvarchar(128)  not null,
  request_hash   nvarchar(64)   not null,
  status         varchar(20)   not null,   -- enum codes are ASCII; mapping pins them to VARCHAR
  response_body  nvarchar(max),
  created_at     datetime2(7)  not null,
  expires_at     datetime2(7)  not null,
  constraint pk_idempotency_key primary key (idem_key),
  constraint ck_idempotency_status check (status in ('IN_PROGRESS','COMPLETED'))
);
create index ix_idempotency_expires on idempotency_key (expires_at);
