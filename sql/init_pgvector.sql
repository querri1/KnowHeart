-- PostgreSQL 向量库初始化
-- 重要：请先连接到 knowheart_vector 库再执行（不是 postgres 系统库）
-- 例如：psql -U postgres -d knowheart_vector -f init_pgvector.sql
--
-- 若库尚未创建，先在 postgres 库执行：
-- CREATE DATABASE knowheart_vector;

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 应用启动时会自动建表 love_knowledge；若需手动建表可取消下面注释：
-- CREATE TABLE IF NOT EXISTS love_knowledge (
--     id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
--     content text,
--     metadata json,
--     embedding vector(1536)
-- );

-- 若之前未装 pgvector 导致表结构错误，可先删表再重启应用：
-- DROP TABLE IF EXISTS love_knowledge;
