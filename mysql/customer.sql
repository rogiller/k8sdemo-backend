create table customer
(
    id int auto_increment,
    first_name varchar(256) null,
    last_name varchar(256) null,
    city varchar(256) null,
    state varchar(256) null,
    country varchar(256) null,
    constraint customer_pk
        primary key (id)
);

INSERT INTO customer (first_name, last_name, city, state, country)
VALUES ('Matthew', 'Rosenberger', 'Lititz', 'Pennsylvania', 'US');