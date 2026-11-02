-- =====================================================================
-- V5 sys_user.phone 列放宽为可空
-- ---------------------------------------------------------------------
-- 背景：手机号业务设计为可空（前端表单非必填、CreateSysUserRequest /
--       UpdateSysUserRequest 仅做 @Pattern 格式校验），但 V2 建表时
--       phone 列为 NOT NULL，导致新增用户不填手机号时 INSERT 报
--       not-null constraint 违例，本脚本对齐三层设计。
-- 说明：
--   1. 部分唯一索引 uk_sys_user_phone (phone) WHERE deleted = 0 中
--      PostgreSQL 对 NULL 互不判等，多行空手机号可共存，索引无需调整；
--   2. DROP NOT NULL 仅放宽约束，已有数据不受影响；
--   3. 幂等性：PostgreSQL 12 对已可空的列重复执行 DROP NOT NULL 不报错。
-- =====================================================================

ALTER TABLE sys_user ALTER COLUMN phone DROP NOT NULL;
