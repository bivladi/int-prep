create table accounts (
                          account_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          balance NUMERIC(18, 2) not null check ( balance >= 0 )
);

insert into accounts(balance) values (100.00), (100.00);

select * from accounts;

-- pessimistic locking,
-- using plpgsql
begin;
set transaction isolation level SERIALIZABLE ;
do $$
declare
a_from BIGINT := 1;
    a_to BIGINT := 2;
    transfer NUMERIC(18, 2) := 100;
    sender_balance NUMERIC(18, 2);
begin
    -- get locks for rows
    perform 1 from accounts
    where account_id in (a_from, a_to)
    order by account_id
    for update;
-- getting balance for a_from account
select balance into sender_balance from accounts where account_id = a_from;
if sender_balance < transfer then
        raise exception 'Insufficient funds';
end if;
    -- update balances
update accounts set balance = balance - transfer where account_id = a_from;
update accounts set balance = balance + transfer where account_id = a_to;
end $$;
commit;

-- pessimistic locking
-- clean sql
begin;
set transaction isolation level READ COMMITTED ;
-- get locks
select account_id from accounts
where account_id in (1, 2)
order by account_id
    for update;

with result as (
update accounts
set balance = balance - 100.00
where account_id = 1 and balance >= 100.00
    returning 1
    )
update accounts
set balance = balance + 100.00
where account_id = 2 and exists(select 1 from result);
commit;

-- same but function
create or replace function transfer(from_id BIGINT, to_id BIGINT, amount NUMERIC(18, 2))
returns void as $$
begin
    if amount <= 0 then
        raise exception 'Amount must be greater than zero';
end if;
    if from_id = to_id then
        raise exception 'Cannot transfer to same account';
end if;
    perform 1 from accounts
    where account_id in (from_id, to_id)
    order by account_id
    for update;

update accounts
set balance = balance - amount
where account_id = from_id and balance >= amount;

if NOT FOUND then
        raise exception 'Insufficient funds';
end if;

update accounts
set balance = balance + amount
where account_id = to_id;
end;
$$ language plpgsql;

select transfer(1, 2, 100.00);

alter table accounts add column version bigint not null default 0 check ( version >= 0 );

-- optimistic locking
-- plpgsql
do $$
declare
from_id bigint := 1;
    to_id bigint := 2;
    from_version bigint;
    to_version bigint;
    amount numeric(18, 2) := 100;
    row_updated bigint;
begin
    if amount <= 0 then
        raise exception 'Amount must be greater than zero';
end if;
    if from_id = to_id then
        raise exception 'Cannot transfer to same account';
end if;
select version into from_version from accounts where account_id = from_id;
if not found then
        raise exception 'Not found %', from_id;
end if;
select version into to_version from accounts where account_id = to_id;
if not found then
        raise exception 'Not found %', to_id;
end if;
update accounts
set balance = balance - 100, version = version + 1
where
    account_id = from_id
  and balance >= amount
  and version = from_version;
get diagnostics row_updated := ROW_COUNT;
if  row_updated <> 1 then
        raise exception 'version mismatch';
end if;
update accounts
set balance = balance + 100, version = version + 1
where
    account_id = to_id
  and version = to_version;
get diagnostics row_updated := ROW_COUNT;
if  row_updated <> 1 then
        raise exception 'version mismatch';
end if;
end;
$$;


explain analyse
select * from accounts;