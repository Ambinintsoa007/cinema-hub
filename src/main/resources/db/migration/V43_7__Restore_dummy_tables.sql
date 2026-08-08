create table if not exists dummy
(
    id varchar
        constraint dummy_pk primary key
);

insert into dummy (id)
values ('dummy-table-id-1')
on conflict do nothing;

create table if not exists dummy_uuid
(
    id varchar
        constraint dummy_uuid_pk primary key
);

insert into dummy_uuid (id)
values ('dummy-uuid-id-1')
on conflict do nothing;