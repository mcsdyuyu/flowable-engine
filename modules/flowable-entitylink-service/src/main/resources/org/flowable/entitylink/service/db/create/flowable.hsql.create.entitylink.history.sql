create table ACT_HI_ENTITYLINK (
  ID_ varchar(64),
  LINK_TYPE_ varchar(255),
  CREATE_TIME_ timestamp,
  SCOPE_ID_ varchar(255),
  SCOPE_TYPE_ varchar(255),
  SCOPE_DEFINITION_ID_ varchar(255),
  REF_SCOPE_ID_ varchar(255),
  REF_SCOPE_TYPE_ varchar(255),
  REF_SCOPE_DEFINITION_ID_ varchar(255),
  HIERARCHY_TYPE_ varchar(255),
  primary key (ID_)
);

create index ACT_IDX_HI_ENT_LNK_SCOPE on ACT_HI_ENTITYLINK(SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_);
create index ACT_IDX_HI_ENT_LNK_SCOPE_DEF on ACT_HI_ENTITYLINK(SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_);
