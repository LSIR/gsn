# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table client (
  id                        bigint not null,
  name                      varchar(255),
  client_id                 varchar(255),
  secret                    varchar(255),
  redirect                  varchar(255),
  user_id                   bigint,
  linked                    boolean,
  constraint pk_client primary key (id))
;

create table data_source (
  id                        bigint not null,
  value                     varchar(255),
  is_public                 boolean,
  constraint pk_data_source primary key (id))
;

create table groups (
  id                        bigint not null,
  name                      varchar(255),
  description               varchar(255),
  constraint pk_groups primary key (id))
;

create table group_data_source_read (
  id                        bigint not null,
  group_id                 bigint,
  data_source_id            bigint,
  constraint pk_group_data_source_read primary key (id),
  constraint uq_group_data_source_read unique (group_id, data_source_id))
;

create table group_data_source_write (
  id                        bigint not null,
  group_id                 bigint,
  data_source_id            bigint,
  constraint pk_group_data_source_write primary key (id),
  constraint uq_group_data_source_write unique (group_id, data_source_id))
;

create table linked_account (
  id                        bigint not null,
  user_id                   bigint,
  provider_user_id          varchar(255),
  provider_key              varchar(255),
  constraint pk_linked_account primary key (id))
;

create table oauth_code (
  id                        bigint not null,
  user_id                   bigint,
  client_id                 bigint,
  code                      varchar(255),
  creation                  bigint,
  constraint pk_oauth_code primary key (id))
;

create table oauth_token (
  id                        bigint not null,
  user_id                   bigint,
  client_id                 bigint,
  token                     varchar(255),
  refresh                   varchar(255),
  creation                  bigint,
  duration                  bigint,
  constraint pk_oauth_token primary key (id))
;

create table security_role (
  id                        bigint not null,
  role_name                 varchar(255),
  constraint pk_security_role primary key (id))
;

create table token_action (
  id                        bigint not null,
  token                     varchar(255),
  target_user_id            bigint,
  type                      varchar(2),
  created                   timestamp,
  expires                   timestamp,
  constraint ck_token_action_type check (type in ('PR','EV')),
  constraint uq_token_action_token unique (token),
  constraint pk_token_action primary key (id))
;

create table users (
  id                        bigint not null,
  email                     varchar(255),
  name                      varchar(255),
  first_name                varchar(255),
  last_name                 varchar(255),
  last_login                timestamp,
  active                    boolean,
  email_validated           boolean,
  constraint uq_users_email unique (email),
  constraint pk_users primary key (id))
;

create table user_data_source_read (
  id                        bigint not null,
  user_id                   bigint,
  data_source_id            bigint,
  constraint pk_user_data_source_read primary key (id),
  constraint uq_user_data_source_read unique (user_id, data_source_id))
;

create table user_data_source_write (
  id                        bigint not null,
  user_id                   bigint,
  data_source_id            bigint,
  constraint pk_user_data_source_write primary key (id),
  constraint uq_user_data_source_write unique (user_id, data_source_id))
;

create table user_permission (
  id                        bigint not null,
  value                     varchar(255),
  constraint pk_user_permission primary key (id))
;

create table users_security_role (
  users_id                       bigint not null,
  security_role_id               bigint not null,
  constraint pk_users_security_role primary key (users_id, security_role_id))
;

create table users_user_permission (
  users_id                       bigint not null,
  user_permission_id             bigint not null,
  constraint pk_users_user_permission primary key (users_id, user_permission_id))
;

create table client_users (
  users_id                       bigint not null,
  client_id                      bigint not null,
  constraint pk_client_users primary key (users_id, client_id))
;

create table users_groups (
  users_id                       bigint not null,
  groups_id                      bigint not null,
  constraint pk_users_groups primary key (users_id, groups_id))
;
create sequence client_seq;

create sequence data_source_seq;

create sequence groups_seq;

create sequence group_data_source_read_seq;

create sequence group_data_source_write_seq;

create sequence linked_account_seq;

create sequence oauth_code_seq;

create sequence oauth_token_seq;

create sequence security_role_seq;

create sequence token_action_seq;

create sequence users_seq;

create sequence user_data_source_read_seq;

create sequence user_data_source_write_seq;

create sequence user_permission_seq;

alter table client add constraint fk_client_user_1 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_client_user_1 on client (user_id);
alter table linked_account add constraint fk_linked_account_user_2 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_linked_account_user_2 on linked_account (user_id);
alter table oauth_code add constraint fk_oauth_code_user_3 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_oauth_code_user_3 on oauth_code (user_id);
alter table oauth_code add constraint fk_oauth_code_client_4 foreign key (client_id) references client (id) on delete restrict on update restrict;
create index ix_oauth_code_client_4 on oauth_code (client_id);
alter table oauth_token add constraint fk_oauth_token_user_5 foreign key (user_id) references users (id) on delete restrict on update restrict;
create index ix_oauth_token_user_5 on oauth_token (user_id);
alter table oauth_token add constraint fk_oauth_token_client_6 foreign key (client_id) references client (id) on delete restrict on update restrict;
create index ix_oauth_token_client_6 on oauth_token (client_id);
alter table token_action add constraint fk_token_action_targetUser_7 foreign key (target_user_id) references users (id) on delete restrict on update restrict;
create index ix_token_action_targetUser_7 on token_action (target_user_id);



alter table group_data_source_read add constraint fk_group_data_source_read_groups_01 foreign key (group_id) references groups (id) on delete restrict on update restrict;

alter table group_data_source_read add constraint fk_group_data_source_read_data_02 foreign key (data_source_id) references data_source (id) on delete restrict on update restrict;

alter table group_data_source_write add constraint fk_group_data_source_write_groups_01 foreign key (group_id) references groups (id) on delete restrict on update restrict;

alter table group_data_source_write add constraint fk_group_data_source_write_data_02 foreign key (data_source_id) references data_source (id) on delete restrict on update restrict;

alter table users_security_role add constraint fk_users_security_role_users_01 foreign key (users_id) references users (id) on delete restrict on update restrict;

alter table users_security_role add constraint fk_users_security_role_securi_02 foreign key (security_role_id) references security_role (id) on delete restrict on update restrict;

alter table users_user_permission add constraint fk_users_user_permission_user_01 foreign key (users_id) references users (id) on delete restrict on update restrict;

alter table users_user_permission add constraint fk_users_user_permission_user_02 foreign key (user_permission_id) references user_permission (id) on delete restrict on update restrict;

alter table user_data_source_read add constraint fk_user_data_source_read_users_01 foreign key (user_id) references users (id) on delete restrict on update restrict;

alter table user_data_source_read add constraint fk_user_data_source_read_data_02 foreign key (data_source_id) references data_source (id) on delete restrict on update restrict;

alter table user_data_source_write add constraint fk_user_data_source_write_users_01 foreign key (user_id) references users (id) on delete restrict on update restrict;

alter table user_data_source_write add constraint fk_user_data_source_write_data_02 foreign key (data_source_id) references data_source (id) on delete restrict on update restrict;

alter table client_users add constraint fk_client_users_users_01 foreign key (users_id) references users (id) on delete restrict on update restrict;

alter table client_users add constraint fk_client_users_client_02 foreign key (client_id) references client (id) on delete restrict on update restrict;

alter table users_groups add constraint fk_users_groups_users_01 foreign key (users_id) references users (id) on delete restrict on update restrict;

alter table users_groups add constraint fk_users_groups_groups_02 foreign key (groups_id) references groups (id) on delete restrict on update restrict;

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists client;

drop table if exists data_source;

drop table if exists group_data_source_read;

drop table if exists user_data_source_read;

drop table if exists group_data_source_write;

drop table if exists user_data_source_write;

drop table if exists groups;

drop table if exists users_groups;

drop table if exists linked_account;

drop table if exists oauth_code;

drop table if exists oauth_token;

drop table if exists security_role;

drop table if exists token_action;

drop table if exists users;

drop table if exists client_users;

drop table if exists users_security_role;

drop table if exists users_user_permission;

drop table if exists user_permission;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists client_seq;

drop sequence if exists data_source_seq;

drop sequence if exists groups_seq;

drop sequence if exists group_data_source_read_seq;

drop sequence if exists group_data_source_write_seq;

drop sequence if exists linked_account_seq;

drop sequence if exists oauth_code_seq;

drop sequence if exists oauth_token_seq;

drop sequence if exists security_role_seq;

drop sequence if exists token_action_seq;

drop sequence if exists users_seq;

drop sequence if exists user_data_source_read_seq;

drop sequence if exists user_data_source_write_seq;

drop sequence if exists user_permission_seq;

