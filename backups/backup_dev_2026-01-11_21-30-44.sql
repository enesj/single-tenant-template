--
-- PostgreSQL database dump
--

-- Dumped from database version 13.23 (Debian 13.23-1.pgdg13+1)
-- Dumped by pg_dump version 14.18 (Homebrew)

-- Started on 2026-01-11 21:30:44 CET

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP DATABASE IF EXISTS single_tenant_pos;
--
-- TOC entry 3429 (class 1262 OID 16384)
-- Name: single_tenant_pos; Type: DATABASE; Schema: -; Owner: app_user
--

CREATE DATABASE single_tenant_pos WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE = 'en_US.utf8';


ALTER DATABASE single_tenant_pos OWNER TO app_user;

\connect single_tenant_pos

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 16511)
-- Name: hstore; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;


--
-- TOC entry 3430 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION hstore; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';


--
-- TOC entry 720 (class 1247 OID 16448)
-- Name: admin_role; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.admin_role AS ENUM (
    'admin',
    'support',
    'owner'
);


ALTER TYPE public.admin_role OWNER TO app_user;

--
-- TOC entry 717 (class 1247 OID 16443)
-- Name: admin_status; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.admin_status AS ENUM (
    'active',
    'suspended'
);


ALTER TYPE public.admin_status OWNER TO app_user;

--
-- TOC entry 727 (class 1247 OID 16473)
-- Name: audit_actor_type; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.audit_actor_type AS ENUM (
    'user',
    'admin'
);


ALTER TYPE public.audit_actor_type OWNER TO app_user;

--
-- TOC entry 753 (class 1247 OID 16678)
-- Name: currency; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.currency AS ENUM (
    'BAM',
    'EUR',
    'USD'
);


ALTER TYPE public.currency OWNER TO app_user;

--
-- TOC entry 734 (class 1247 OID 16490)
-- Name: login_principal_type; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.login_principal_type AS ENUM (
    'user',
    'admin'
);


ALTER TYPE public.login_principal_type OWNER TO app_user;

--
-- TOC entry 760 (class 1247 OID 16698)
-- Name: payer_type; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.payer_type AS ENUM (
    'cash',
    'card',
    'account',
    'person'
);


ALTER TYPE public.payer_type OWNER TO app_user;

--
-- TOC entry 750 (class 1247 OID 16658)
-- Name: receipt_status; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.receipt_status AS ENUM (
    'uploaded',
    'parsing',
    'parsed',
    'extracting',
    'extracted',
    'review_required',
    'approved',
    'posted',
    'failed'
);


ALTER TYPE public.receipt_status OWNER TO app_user;

--
-- TOC entry 707 (class 1247 OID 16404)
-- Name: user_role; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.user_role AS ENUM (
    'admin',
    'member',
    'viewer',
    'unassigned'
);


ALTER TYPE public.user_role OWNER TO app_user;

--
-- TOC entry 704 (class 1247 OID 16397)
-- Name: user_status; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.user_status AS ENUM (
    'active',
    'inactive',
    'suspended'
);


ALTER TYPE public.user_status OWNER TO app_user;

--
-- TOC entry 288 (class 1255 OID 16638)
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: app_user
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
       BEGIN
         NEW.updated_at = CURRENT_TIMESTAMP;
         RETURN NEW;
       END;
       $$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO app_user;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 217 (class 1259 OID 16839)
-- Name: admin_sessions; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.admin_sessions (
    id uuid NOT NULL,
    admin_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT '2025-12-10 10:03:03.576388+00'::timestamp with time zone NOT NULL,
    last_activity timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    ip_address text,
    user_agent text
);


ALTER TABLE public.admin_sessions OWNER TO app_user;

--
-- TOC entry 205 (class 1259 OID 16455)
-- Name: admins; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.admins (
    role public.admin_role DEFAULT 'admin'::public.admin_role NOT NULL,
    updated_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.431688+00'::timestamp with time zone,
    email character varying(255) NOT NULL,
    password_hash text NOT NULL,
    last_login_at timestamp with time zone,
    status public.admin_status DEFAULT 'active'::public.admin_status NOT NULL,
    id uuid NOT NULL,
    full_name character varying(255),
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.431688+00'::timestamp with time zone,
    CONSTRAINT admins_email_check CHECK (((email)::text ~* '^[^\s@]+@[^\s@]+\.[^\s@]+$'::text))
);


ALTER TABLE public.admins OWNER TO app_user;

--
-- TOC entry 216 (class 1259 OID 16810)
-- Name: article_aliases; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.article_aliases (
    id uuid NOT NULL,
    supplier_id uuid,
    raw_label_normalized character varying(255) NOT NULL,
    article_id uuid NOT NULL,
    confidence integer DEFAULT 100,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone
);


ALTER TABLE public.article_aliases OWNER TO app_user;

--
-- TOC entry 213 (class 1259 OID 16752)
-- Name: articles; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.articles (
    id uuid NOT NULL,
    canonical_name character varying(255) NOT NULL,
    normalized_key character varying(255) NOT NULL,
    barcode character varying(50),
    category character varying(100),
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone,
    updated_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone
);


ALTER TABLE public.articles OWNER TO app_user;

--
-- TOC entry 206 (class 1259 OID 16477)
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.audit_logs (
    ip text,
    user_agent text,
    actor_type public.audit_actor_type NOT NULL,
    actor_id uuid NOT NULL,
    target_id uuid,
    target_type text,
    id uuid NOT NULL,
    action text NOT NULL,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.431688+00'::timestamp with time zone,
    metadata jsonb
);


ALTER TABLE public.audit_logs OWNER TO app_user;

--
-- TOC entry 202 (class 1259 OID 16387)
-- Name: automigrate_migrations; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.automigrate_migrations (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.automigrate_migrations OWNER TO app_user;

--
-- TOC entry 201 (class 1259 OID 16385)
-- Name: automigrate_migrations_id_seq; Type: SEQUENCE; Schema: public; Owner: app_user
--

CREATE SEQUENCE public.automigrate_migrations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.automigrate_migrations_id_seq OWNER TO app_user;

--
-- TOC entry 3431 (class 0 OID 0)
-- Dependencies: 201
-- Name: automigrate_migrations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: app_user
--

ALTER SEQUENCE public.automigrate_migrations_id_seq OWNED BY public.automigrate_migrations.id;


--
-- TOC entry 204 (class 1259 OID 16427)
-- Name: email_verification_tokens; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.email_verification_tokens (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    attempts integer DEFAULT 0,
    last_attempted_at timestamp with time zone,
    used_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.431688+00'::timestamp with time zone
);


ALTER TABLE public.email_verification_tokens OWNER TO app_user;

--
-- TOC entry 214 (class 1259 OID 16762)
-- Name: expense_items; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.expense_items (
    id uuid NOT NULL,
    expense_id uuid NOT NULL,
    raw_label text NOT NULL,
    article_id uuid,
    qty numeric(10,3),
    unit_price numeric(12,2),
    line_total numeric(12,2) NOT NULL,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone,
    deleted_at timestamp with time zone
);


ALTER TABLE public.expense_items OWNER TO app_user;

--
-- TOC entry 212 (class 1259 OID 16725)
-- Name: expenses; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.expenses (
    purchased_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone,
    is_posted boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    currency public.currency DEFAULT 'BAM'::public.currency NOT NULL,
    supplier_id uuid NOT NULL,
    payer_id uuid NOT NULL,
    id uuid NOT NULL,
    notes text,
    total_amount numeric(12,2) NOT NULL,
    receipt_id uuid,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone,
    user_id uuid
);


ALTER TABLE public.expenses OWNER TO app_user;

--
-- TOC entry 207 (class 1259 OID 16495)
-- Name: login_events; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.login_events (
    id uuid NOT NULL,
    principal_type public.login_principal_type NOT NULL,
    principal_id uuid NOT NULL,
    success boolean NOT NULL,
    reason text,
    ip text,
    user_agent text,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.431688+00'::timestamp with time zone
);


ALTER TABLE public.login_events OWNER TO app_user;

--
-- TOC entry 208 (class 1259 OID 16646)
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.password_reset_tokens (
    id uuid NOT NULL,
    principal_type public.login_principal_type NOT NULL,
    principal_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.65715+00'::timestamp with time zone
);


ALTER TABLE public.password_reset_tokens OWNER TO app_user;

--
-- TOC entry 211 (class 1259 OID 16717)
-- Name: payers; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.payers (
    id uuid NOT NULL,
    type public.payer_type NOT NULL,
    label character varying(255) NOT NULL,
    last4 character varying(4),
    is_default boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone,
    updated_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone
);


ALTER TABLE public.payers OWNER TO app_user;

--
-- TOC entry 215 (class 1259 OID 16781)
-- Name: price_observations; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.price_observations (
    observed_at timestamp with time zone NOT NULL,
    expense_item_id uuid,
    line_total numeric(12,2) NOT NULL,
    article_id uuid NOT NULL,
    unit_price numeric(12,2),
    currency public.currency DEFAULT 'BAM'::public.currency,
    supplier_id uuid NOT NULL,
    id uuid NOT NULL,
    qty numeric(10,3),
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone
);


ALTER TABLE public.price_observations OWNER TO app_user;

--
-- TOC entry 209 (class 1259 OID 16685)
-- Name: receipts; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.receipts (
    original_filename character varying(255),
    raw_extract_json jsonb,
    updated_at timestamp with time zone DEFAULT now(),
    total_amount_guess numeric(12,2),
    purchased_at_guess timestamp with time zone,
    file_size integer,
    currency_guess public.currency,
    expense_id uuid,
    status public.receipt_status DEFAULT 'uploaded'::public.receipt_status NOT NULL,
    file_hash character varying(64) NOT NULL,
    id uuid NOT NULL,
    content_type character varying(100),
    error_details jsonb,
    storage_key text NOT NULL,
    error_message text,
    raw_parse_json jsonb,
    supplier_guess character varying(255),
    retry_count integer DEFAULT 0,
    created_at timestamp with time zone DEFAULT now(),
    parsed_markdown text,
    user_id uuid
);


ALTER TABLE public.receipts OWNER TO app_user;

--
-- TOC entry 210 (class 1259 OID 16707)
-- Name: suppliers; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.suppliers (
    id uuid NOT NULL,
    display_name character varying(255) NOT NULL,
    normalized_key character varying(255) NOT NULL,
    address text,
    tax_id character varying(50),
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone,
    updated_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.683291+00'::timestamp with time zone
);


ALTER TABLE public.suppliers OWNER TO app_user;

--
-- TOC entry 203 (class 1259 OID 16411)
-- Name: users; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.users (
    role public.user_role DEFAULT 'unassigned'::public.user_role NOT NULL,
    updated_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.431688+00'::timestamp with time zone,
    email character varying(255) NOT NULL,
    avatar_url text,
    password_hash text NOT NULL,
    provider_user_id text,
    last_login_at timestamp with time zone,
    status public.user_status DEFAULT 'active'::public.user_status NOT NULL,
    id uuid NOT NULL,
    full_name character varying(255),
    auth_provider text DEFAULT 'password'::text,
    created_at timestamp with time zone DEFAULT '2025-12-06 21:07:53.431688+00'::timestamp with time zone,
    email_verified boolean DEFAULT false NOT NULL,
    CONSTRAINT users_email_check CHECK (((email)::text ~* '^[^\s@]+@[^\s@]+\.[^\s@]+$'::text))
);


ALTER TABLE public.users OWNER TO app_user;

--
-- TOC entry 3137 (class 2604 OID 16390)
-- Name: automigrate_migrations id; Type: DEFAULT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.automigrate_migrations ALTER COLUMN id SET DEFAULT nextval('public.automigrate_migrations_id_seq'::regclass);


--
-- TOC entry 3423 (class 0 OID 16839)
-- Dependencies: 217
-- Data for Name: admin_sessions; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.admin_sessions (id, admin_id, token, created_at, last_activity, expires_at, ip_address, user_agent) FROM stdin;
831d320e-8573-4d0c-8b3a-03d5f737497c	69eccad4-451d-4c62-921a-b9e54d61e3d0	0856b3b3-c1c5-4004-928e-6941eb9f782a	2025-12-10 10:05:49.745553+00	2025-12-10 10:05:49.745553+00	2025-12-10 18:05:49.745553+00	0:0:0:0:0:0:0:1	curl/8.7.1
ed9ac862-0423-4773-87bc-fdcf17ebe3b8	69eccad4-451d-4c62-921a-b9e54d61e3d0	39ba2d08-af88-488f-8516-34ed75fd26d3	2025-12-10 10:07:39.08767+00	2025-12-10 10:08:35.705567+00	2025-12-10 18:07:39.08767+00	0:0:0:0:0:0:0:1	curl/8.7.1
e1af9a61-adc2-4936-8211-1af0502712f1	69eccad4-451d-4c62-921a-b9e54d61e3d0	7025c3d8-d786-4105-b9fd-d3b1aeee4a39	2025-12-11 22:22:49.013486+00	2025-12-11 23:12:39.477459+00	2025-12-12 06:22:49.013486+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
3aa58ea4-bf5c-4e2a-a21c-edd1fabf7251	69eccad4-451d-4c62-921a-b9e54d61e3d0	48ed76c6-be98-4b60-ad0e-c2b9f33cc44d	2025-12-11 08:20:16.268872+00	2025-12-11 15:41:06.359652+00	2025-12-11 16:20:16.268872+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
3d19a6b1-81d5-4d30-84d6-a3a1cfb96674	69eccad4-451d-4c62-921a-b9e54d61e3d0	0afc80f6-5310-4191-90aa-565d45e35e67	2025-12-12 20:03:02.198968+00	2025-12-12 20:03:34.653936+00	2025-12-13 04:03:02.198968+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
8a157119-a34b-461d-be1e-f5e72b4ef166	69eccad4-451d-4c62-921a-b9e54d61e3d0	52bab184-2514-488c-bcfa-47a607bc1e08	2025-12-11 18:45:38.816845+00	2025-12-11 21:57:29.163938+00	2025-12-12 02:45:38.816845+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
d8682d43-931d-4c76-a3fa-5eafd4f52fba	69eccad4-451d-4c62-921a-b9e54d61e3d0	60dea47d-7446-4b6c-ac81-bf1b2e5458c8	2025-12-11 21:57:42.3685+00	2025-12-11 22:12:35.767659+00	2025-12-12 05:57:42.3685+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
58000e00-344f-41f1-9fdf-2eeff500b4fa	69eccad4-451d-4c62-921a-b9e54d61e3d0	ddf9253f-33a4-431b-81da-d680461b09c9	2026-01-11 19:13:33.83791+00	2026-01-11 19:14:36.909235+00	2026-01-12 03:13:33.83791+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
4fdc7d2a-b522-40ea-a144-493cec119c8d	69eccad4-451d-4c62-921a-b9e54d61e3d0	2b05ac70-9766-409e-b863-af7d22ead305	2025-12-12 10:45:48.831605+00	2025-12-12 11:59:17.235143+00	2025-12-12 18:45:48.831605+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
e29bd3e9-8143-4c07-8de4-d26461a90574	69eccad4-451d-4c62-921a-b9e54d61e3d0	38855da4-9bc1-40b6-923c-f1352b9e2d29	2025-12-10 10:10:06.696992+00	2025-12-10 10:10:18.552985+00	2025-12-10 18:10:06.696992+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
a115bd28-03ad-4d1a-8801-43b86467d1e0	69eccad4-451d-4c62-921a-b9e54d61e3d0	3ea3d7a8-f0d9-49ed-93ca-9ef9699907f1	2025-12-10 10:10:41.383135+00	2025-12-10 14:44:35.97172+00	2025-12-10 18:10:41.383135+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
c0f90cd4-dbf7-4417-9644-477021c571a7	69eccad4-451d-4c62-921a-b9e54d61e3d0	20da4006-b811-482b-a6b9-23ea3b7e9dd4	2025-12-10 22:22:58.886145+00	2025-12-10 23:19:17.990829+00	2025-12-11 06:22:58.886145+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
5a746160-9db4-4468-bdfe-40f56d2839f4	69eccad4-451d-4c62-921a-b9e54d61e3d0	974db332-eed1-453f-9a15-5ff000eb50fc	2026-01-11 19:15:34.075585+00	2026-01-11 19:34:00.893603+00	2026-01-12 03:15:34.075585+00	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0
757b174c-4cb3-4dc6-9014-8ca51e5bf4f3	69eccad4-451d-4c62-921a-b9e54d61e3d0	b469b395-2657-4b6e-95bb-a8a17a63d213	2025-12-12 20:03:37.912055+00	2025-12-12 23:39:29.133548+00	2025-12-13 04:03:37.912055+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
8f81c71e-3871-45ac-a215-816f07fa3978	69eccad4-451d-4c62-921a-b9e54d61e3d0	a1383af4-ef11-4f4a-a755-5d5288957846	2025-12-13 08:40:55.81298+00	2025-12-13 15:16:13.72039+00	2025-12-13 16:40:55.81298+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
bc58e506-f6f7-4669-b210-93895243e0b4	69eccad4-451d-4c62-921a-b9e54d61e3d0	18a91712-2518-4a43-8218-a02cc94e034c	2025-12-13 15:18:31.039172+00	2025-12-13 16:58:23.519408+00	2025-12-13 23:18:31.039172+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
372b1296-ea78-499c-94bb-7e6a45a97d68	69eccad4-451d-4c62-921a-b9e54d61e3d0	fec597b9-e662-4277-a43f-1816208dd682	2025-12-14 04:51:50.031781+00	2025-12-14 05:41:14.347975+00	2025-12-14 12:51:50.031781+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
cabb8de1-8338-4f52-bf94-51037da37a39	69eccad4-451d-4c62-921a-b9e54d61e3d0	c743286e-f518-4cbb-ac5f-c8d10cb9099b	2025-12-13 16:58:44.584712+00	2025-12-13 22:49:09.825208+00	2025-12-14 00:58:44.584712+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
eb3a059d-cd42-4d8e-826c-bbfb30edb358	69eccad4-451d-4c62-921a-b9e54d61e3d0	eecc8532-8a63-4dc0-8dc4-25f507bf060d	2025-12-15 09:09:16.506667+00	2025-12-15 09:09:54.270115+00	2025-12-15 17:09:16.506667+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
67153c67-bc25-4bd6-a6e1-cb51b4f8cc04	69eccad4-451d-4c62-921a-b9e54d61e3d0	2abfbb81-bb2b-44e5-92f2-56e826b20efd	2025-12-15 09:11:07.812838+00	2025-12-15 09:11:08.159756+00	2025-12-15 17:11:07.812838+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
476ec08f-ee60-42c0-83de-bf393eea4b54	69eccad4-451d-4c62-921a-b9e54d61e3d0	cca83ef9-4a04-4b98-8303-9968bad65549	2025-12-14 19:46:42.030491+00	2025-12-15 00:00:44.795823+00	2025-12-15 03:46:42.030491+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
a256c925-a1f4-4ac1-b479-7d4cb9eeb425	69eccad4-451d-4c62-921a-b9e54d61e3d0	0660db6d-3dec-4601-bd19-5289a67574e9	2025-12-15 09:11:16.574776+00	2025-12-15 16:13:08.144788+00	2025-12-15 17:11:16.574776+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
35dc7cc6-f433-48e0-939d-b4afd961c6e6	69eccad4-451d-4c62-921a-b9e54d61e3d0	38496a7c-984d-4f70-a35d-ac6bf4f6697a	2025-12-14 05:41:16.393046+00	2025-12-14 06:19:59.637067+00	2025-12-14 13:41:16.393046+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
d854f602-6c53-4b38-b4c6-908db45a8b67	69eccad4-451d-4c62-921a-b9e54d61e3d0	c883aa6e-fdb1-42a0-b1c9-c92758b089fc	2025-12-17 09:30:53.614163+00	2025-12-17 17:28:49.336283+00	2025-12-17 17:30:53.614163+00	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0
2c0c70cd-a685-40a5-8686-00cc2c0763c4	69eccad4-451d-4c62-921a-b9e54d61e3d0	067365b5-b72a-468c-8cbb-3cecab294e99	2025-12-18 06:56:07.93161+00	2025-12-18 07:34:55.846605+00	2025-12-18 14:56:07.93161+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
9c05c1e3-13b0-4386-8e4a-d80caa3dd3f1	69eccad4-451d-4c62-921a-b9e54d61e3d0	c28b0b60-617e-40a6-bff1-a2bbc46c1adc	2025-12-17 17:29:52.982154+00	2025-12-17 18:36:51.492415+00	2025-12-18 01:29:52.982154+00	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0
854be245-1e91-4df2-ac83-5da2f7854f16	69eccad4-451d-4c62-921a-b9e54d61e3d0	92d14a7d-4133-4ca0-ab80-ac908f098abe	2025-12-16 18:18:27.86297+00	2025-12-16 22:18:27.363723+00	2025-12-17 02:18:27.86297+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
222335ef-9e41-4aaa-a9ae-437f48d48d95	69eccad4-451d-4c62-921a-b9e54d61e3d0	a48e399c-56d5-4b71-a126-7fec22e48e0e	2025-12-15 20:25:01.122004+00	2025-12-15 20:55:35.835269+00	2025-12-16 04:25:01.122004+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
195a68fd-1ea4-4555-a0d5-ff2f58ac5bca	69eccad4-451d-4c62-921a-b9e54d61e3d0	64d15430-a780-47cd-8ab3-c1ede5a64ef0	2025-12-16 10:06:52.379605+00	2025-12-16 18:04:25.187121+00	2025-12-16 18:06:52.379605+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
58d205a2-1eea-4cab-898e-98ab6c0ade5d	69eccad4-451d-4c62-921a-b9e54d61e3d0	fccaf7c7-34ee-448e-ad0c-a2e3d3ada9fb	2025-12-17 16:57:01.410564+00	2025-12-17 18:21:45.631348+00	2025-12-18 00:57:01.410564+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
e182aa41-06fe-4efa-ac1a-a0215ccd980f	69eccad4-451d-4c62-921a-b9e54d61e3d0	32617972-bae4-4c60-b999-0a3eb4cf55f6	2025-12-18 07:59:38.118392+00	2025-12-18 08:00:13.56479+00	2025-12-18 15:59:38.118392+00	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0
acb2b90f-3b4e-4b4a-a8f9-d3ab6815e451	69eccad4-451d-4c62-921a-b9e54d61e3d0	f3f160c4-d543-4d31-bd3c-fb29120f6187	2025-12-17 07:41:59.399929+00	2025-12-17 15:41:32.650671+00	2025-12-17 15:41:59.399929+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
86a07613-c3ec-4eba-b4a7-d4fbebbc8546	69eccad4-451d-4c62-921a-b9e54d61e3d0	c213b8b8-e744-4a4d-92d2-8204832a8d34	2025-12-18 07:35:23.878799+00	2025-12-18 07:38:22.338483+00	2025-12-18 15:35:23.878799+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
795855b4-db33-4474-be82-40c886938169	69eccad4-451d-4c62-921a-b9e54d61e3d0	a900892c-3c88-4677-a1fd-99d806ed6a1c	2025-12-18 07:38:24.789097+00	2025-12-18 07:39:37.979592+00	2025-12-18 15:38:24.789097+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
9ce24ab4-71e7-415c-8de0-9fea1558a917	69eccad4-451d-4c62-921a-b9e54d61e3d0	232fbdc7-dfba-4060-8cfa-6a2a8980df84	2025-12-25 07:19:35.442019+00	2025-12-25 15:16:41.789309+00	2025-12-25 15:19:35.442019+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
1ec90a36-e190-4214-9edc-c01bb68f281f	69eccad4-451d-4c62-921a-b9e54d61e3d0	54c1d12d-1f06-45c5-8053-f346b9bc4369	2025-12-19 07:29:23.051251+00	2025-12-19 13:16:06.070629+00	2025-12-19 15:29:23.051251+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
bd23dd39-de05-4b0c-a64b-b24db2d1bc1f	69eccad4-451d-4c62-921a-b9e54d61e3d0	883479e6-dd40-4360-a031-7a58bba7bcc7	2025-12-24 13:59:15.11176+00	2025-12-24 14:49:31.198032+00	2025-12-24 21:59:15.11176+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
71023d1e-dd5d-4606-8c91-bc813d1fbd1f	69eccad4-451d-4c62-921a-b9e54d61e3d0	191b7e0f-c4b2-481f-b048-e196a2c8570e	2025-12-24 08:33:54.808814+00	2025-12-24 13:59:13.23546+00	2025-12-24 16:33:54.808814+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
0650f8db-eb5b-4b38-8ed8-30a1863b2fa0	69eccad4-451d-4c62-921a-b9e54d61e3d0	15405ce4-e83c-419d-af17-798c29fa3eb1	2025-12-23 11:29:15.207088+00	2025-12-23 11:29:32.052974+00	2025-12-23 19:29:15.207088+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
61e1d58c-05c3-4b99-aeb7-b44945c2cc50	69eccad4-451d-4c62-921a-b9e54d61e3d0	89a72da7-8af9-48fa-ae37-cd32a0407c90	2025-12-18 08:07:11.187323+00	2025-12-18 15:43:46.124248+00	2025-12-18 16:07:11.187323+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
a5b644de-3ea3-472d-bd3f-6b6d1207cf82	69eccad4-451d-4c62-921a-b9e54d61e3d0	d1ac58d8-bb1b-40cc-84d0-e7c6d490d37f	2025-12-26 07:55:38.362088+00	2025-12-26 12:35:51.123417+00	2025-12-26 15:55:38.362088+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
eee44730-0ece-4767-b597-b14753c49208	69eccad4-451d-4c62-921a-b9e54d61e3d0	c8c81acc-ab27-44f3-ab3a-20e965a15635	2025-12-26 16:14:54.783997+00	2025-12-26 18:02:51.054565+00	2025-12-27 00:14:54.783997+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
67412c58-1fec-48f4-ae7d-4d132839b845	69eccad4-451d-4c62-921a-b9e54d61e3d0	a38e4cb7-3cda-4882-9058-633c84d4cc96	2025-12-18 16:10:01.107289+00	2025-12-18 16:52:59.974841+00	2025-12-19 00:10:01.107289+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
373afcd5-2985-4821-bd36-fb4549c680c4	69eccad4-451d-4c62-921a-b9e54d61e3d0	9e1e4ab8-cdb9-44b2-8c48-f9777c3ff4f6	2026-01-06 08:12:34.898445+00	2026-01-06 15:55:27.045438+00	2026-01-06 16:12:34.898445+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
31fff3ac-ae8b-4555-93a9-c59f08d49bf8	69eccad4-451d-4c62-921a-b9e54d61e3d0	afc915ec-8616-4ffb-88c5-84974a3e2cfc	2025-12-26 18:02:52.844665+00	2025-12-26 18:03:49.747651+00	2025-12-27 02:02:52.844665+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
294cc2c0-a0c4-4553-a87e-a6e32f3ad1bc	69eccad4-451d-4c62-921a-b9e54d61e3d0	d46e4bea-fbc2-4a27-b919-cbe5d318d96a	2025-12-25 15:39:42.56155+00	2025-12-25 16:07:20.477469+00	2025-12-25 23:39:42.56155+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
01617ac6-17c8-4050-bf37-af010b0ea262	69eccad4-451d-4c62-921a-b9e54d61e3d0	14e905c9-a8ae-4b4f-93f9-3593de4b4b36	2026-01-07 20:52:10.953851+00	2026-01-07 20:54:45.790138+00	2026-01-08 04:52:10.953851+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
27fb1a6e-1ca4-4567-a265-53973537a12b	69eccad4-451d-4c62-921a-b9e54d61e3d0	26ac046b-7a1e-40f2-bec0-fafc8a20dccc	2026-01-07 10:47:17.937213+00	2026-01-07 17:42:27.656779+00	2026-01-07 18:47:17.937213+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
12ae1197-1254-4ac2-9c84-e6c783e3ed54	69eccad4-451d-4c62-921a-b9e54d61e3d0	10aa6ccb-d899-4610-8939-f92430d28feb	2026-01-08 22:53:40.475154+00	2026-01-08 23:27:11.214836+00	2026-01-09 06:53:40.475154+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
e4389fe7-06b9-4a93-8e62-22697eae2ebd	69eccad4-451d-4c62-921a-b9e54d61e3d0	2fa82bed-85ed-49ef-9df3-b5f228dc1559	2026-01-08 08:56:55.260277+00	2026-01-08 16:10:11.4193+00	2026-01-08 16:56:55.260277+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
9d3aab04-dd14-445d-bf23-2a13ff41d5a6	69eccad4-451d-4c62-921a-b9e54d61e3d0	aaf1dfc1-10fe-47c7-b73b-676f6e3262f7	2026-01-09 09:59:03.171296+00	2026-01-09 14:25:36.073227+00	2026-01-09 17:59:03.171296+00	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0
4cff426f-4577-4959-92a6-a16ed007b386	69eccad4-451d-4c62-921a-b9e54d61e3d0	00b7eda7-a5bb-4522-a8fb-7177c905ac1f	2026-01-08 17:01:29.415548+00	2026-01-08 22:53:38.540424+00	2026-01-09 01:01:29.415548+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
68688d66-a657-41ff-8756-9ec8188f1d3e	69eccad4-451d-4c62-921a-b9e54d61e3d0	054580be-99da-4541-92fa-d5ddc67cb754	2026-01-10 09:50:11.731573+00	2026-01-10 10:18:16.892296+00	2026-01-10 17:50:11.731573+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
724fb7ba-f7b7-4fe4-9854-c3776eca27df	69eccad4-451d-4c62-921a-b9e54d61e3d0	8b0d473f-a0ae-4dff-857b-459695635932	2026-01-11 18:48:24.098691+00	2026-01-11 19:02:13.963315+00	2026-01-12 02:48:24.098691+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
465ccb16-0c46-4511-9815-3b0347250cb3	69eccad4-451d-4c62-921a-b9e54d61e3d0	9c5b665c-496c-4a41-b754-3dc8c80e51ce	2026-01-10 21:50:42.803556+00	2026-01-10 21:50:46.152287+00	2026-01-11 05:50:42.803556+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
7543cfd9-fae9-4737-aa28-5e6128e28550	69eccad4-451d-4c62-921a-b9e54d61e3d0	602a989e-6a6f-4af8-b9ea-a8c3832dcb29	2026-01-09 09:58:18.356209+00	2026-01-09 15:17:05.634069+00	2026-01-09 17:58:18.356209+00	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36
\.


--
-- TOC entry 3411 (class 0 OID 16455)
-- Dependencies: 205
-- Data for Name: admins; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.admins (role, updated_at, email, password_hash, last_login_at, status, id, full_name, created_at) FROM stdin;
owner	2025-12-18 08:49:08.571759+00	admin@example.com	bcrypt+sha512$8140c2c664fa1701012930c6e9b56b81$12$562666aad522ecd46974b429fe4f35e87b9841fe527a5a16	\N	active	69eccad4-451d-4c62-921a-b9e54d61e3d0	System Administratorn h	2025-12-08 07:12:59.591944+00
\.


--
-- TOC entry 3422 (class 0 OID 16810)
-- Dependencies: 216
-- Data for Name: article_aliases; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.article_aliases (id, supplier_id, raw_label_normalized, article_id, confidence, created_at) FROM stdin;
ad28bbdc-cabe-4614-b230-10cb67a8c1ae	6057fb90-df80-416c-b1a0-5a9a9dfcc264	secer-10g-sa-aromon-cimeta-dr	8876cc99-4acf-4810-918a-b00e284433a8	100	2025-12-06 21:07:53.683291+00
26b14ead-ce9e-4303-a8f2-f197967df421	6057fb90-df80-416c-b1a0-5a9a9dfcc264	secer-10g-sa-aromon-limuna-dr	8876cc99-4acf-4810-918a-b00e284433a8	100	2025-12-06 21:07:53.683291+00
5ca9ab42-5d73-4fe7-8c47-ece7a576ece0	6057fb90-df80-416c-b1a0-5a9a9dfcc264	bombone-gumene-100g-haribo-be	028b5b67-587c-442c-8328-dcf00e273f19	100	2025-12-06 21:07:53.683291+00
82fbed8a-a967-414f-8be8-d5371aadad31	cf83233f-316c-40f8-a022-b8b3c3c5df45	snala-za-kosu-bh231226	ea97eef6-47b2-425e-af6f-2db6504816ff	100	2025-12-06 21:07:53.683291+00
0950c661-e01f-4eb6-9b82-018be3b3e1f4	92a781b7-fb7d-4bde-b868-7868d4f918b0	svijeca-silver-amp-gold-s-poklopcem-135pc	f48819b3-6744-44d4-a5a1-17858f16ba5c	100	2025-12-06 21:07:53.683291+00
f754338d-2574-4dd3-b5bb-ad46e796025e	92a781b7-fb7d-4bde-b868-7868d4f918b0	oznake-za-poklone-sljokice-20kom-xmaso-npc	c93b9f4d-6780-4460-8330-7512d830d4d3	100	2025-12-06 21:07:53.683291+00
\.


--
-- TOC entry 3419 (class 0 OID 16752)
-- Dependencies: 213
-- Data for Name: articles; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.articles (id, canonical_name, normalized_key, barcode, category, created_at, updated_at) FROM stdin;
8876cc99-4acf-4810-918a-b00e284433a8	SECER 10G SA AROMON CIMETA DR	secer-10g-sa-aromon-cimeta-dr	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
028b5b67-587c-442c-8328-dcf00e273f19	BOMBONE GUMENE 100G HARIBO BE	bombone-gumene-100g-haribo-be	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
ea97eef6-47b2-425e-af6f-2db6504816ff	Snala za kosu BH231226	snala-za-kosu-bh231226	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
f48819b3-6744-44d4-a5a1-17858f16ba5c	Svijeca "silver &amp; gold" s poklopcem 13.5/pc	svijeca-silver-amp-gold-s-poklopcem-135pc	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
c93b9f4d-6780-4460-8330-7512d830d4d3	Oznake za poklone sljokice 20kom Xmas_O N/pc	oznake-za-poklone-sljokice-20kom-xmaso-npc	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
\.


--
-- TOC entry 3412 (class 0 OID 16477)
-- Dependencies: 206
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.audit_logs (ip, user_agent, actor_type, actor_id, target_id, target_type, id, action, created_at, metadata) FROM stdin;
0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	69eccad4-451d-4c62-921a-b9e54d61e3d0	admin	7ffef976-00d4-43e3-adbf-9caa2cc04818	update_admin	2025-12-18 08:47:52.930484+00	{"full_name": "System Administrator y", "initiator": {"type": "admin", "admin-id": "69eccad4-451d-4c62-921a-b9e54d61e3d0"}}
0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	69eccad4-451d-4c62-921a-b9e54d61e3d0	admin	ca2de4b8-2d3e-4404-aa10-ff37bdf64aef	update_admin	2025-12-18 08:48:09.29777+00	{"full_name": "System Administratorn", "initiator": {"type": "admin", "admin-id": "69eccad4-451d-4c62-921a-b9e54d61e3d0"}}
0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	69eccad4-451d-4c62-921a-b9e54d61e3d0	admin	8ffae7d0-8113-43d4-9f7a-b788784072ba	update_admin	2025-12-18 08:48:54.799362+00	{"full_name": "System Administratorn h", "initiator": {"type": "admin", "admin-id": "69eccad4-451d-4c62-921a-b9e54d61e3d0"}}
0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	69eccad4-451d-4c62-921a-b9e54d61e3d0	admin	72344fc1-c4ae-4746-abbd-d7619fd6e818	update_admin	2025-12-18 08:49:08.576455+00	{"full_name": "System Administratorn h", "initiator": {"type": "admin", "admin-id": "69eccad4-451d-4c62-921a-b9e54d61e3d0"}}
\.


--
-- TOC entry 3408 (class 0 OID 16387)
-- Dependencies: 202
-- Data for Name: automigrate_migrations; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.automigrate_migrations (id, name, created_at) FROM stdin;
1	0001_schema	2025-12-06 22:07:53.520035
2	0002_enable_hstore_extension	2025-12-06 22:07:53.548687
3	0003_function_update-updated-at-column	2025-12-06 22:07:53.56576
4	0004_trigger_users-updated-at-trigger	2025-12-06 22:07:53.579925
5	0005_trigger_admins-updated-at-trigger	2025-12-06 22:07:53.592474
6	0006_schema	2025-12-06 22:07:53.612736
7	0007_schema	2025-12-06 22:07:53.630485
8	0008_schema	2025-12-06 22:07:53.64888
9	0009_password_reset_tokens	2025-12-06 22:07:53.674572
10	0010_schema	2025-12-06 22:07:53.773601
11	0011_schema	2025-12-10 11:03:03.656239
12	0012_schema	2025-12-10 15:13:19.873403
13	0013_schema	2025-12-19 13:09:12.617252
14	0014_schema	2025-12-19 13:10:05.121433
15	0015_schema	2026-01-08 13:23:49.255555
16	0016_remove_payment_hints	2026-01-08 23:23:44.998191
\.


--
-- TOC entry 3410 (class 0 OID 16427)
-- Dependencies: 204
-- Data for Name: email_verification_tokens; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.email_verification_tokens (id, user_id, token, expires_at, attempts, last_attempted_at, used_at, created_at) FROM stdin;
dd286180-2c06-419e-a47d-83031ab8cc10	561c45b8-6e1f-4f97-bbf2-a086f61f3902	sbGWKZdepmf-LX3PrPZtKV_CuJCQt0Qm39V-SqPBS00	2025-12-26 13:23:11.641601+00	0	\N	\N	2025-12-06 21:07:53.431688+00
\.


--
-- TOC entry 3420 (class 0 OID 16762)
-- Dependencies: 214
-- Data for Name: expense_items; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.expense_items (id, expense_id, raw_label, article_id, qty, unit_price, line_total, created_at, deleted_at) FROM stdin;
392fcaa4-741d-46cf-bbb4-a18050c35eda	afac9f18-6043-4d89-82c5-749677c66895	VRECICA TREGERUNA	\N	2.000	0.10	0.20	2025-12-06 21:07:53.683291+00	\N
d6524407-2455-4012-a9b7-e5eee71c9f3b	afac9f18-6043-4d89-82c5-749677c66895	KAFA ESPRESSO 112G NESCAFE DO	\N	1.000	9.95	9.95	2025-12-06 21:07:53.683291+00	\N
b668eb0c-4232-4470-b9af-6a651349da7c	afac9f18-6043-4d89-82c5-749677c66895	SOK 1 25L COCA COLA	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	\N
aebbd30b-3890-4282-abe6-240aed13da71	afac9f18-6043-4d89-82c5-749677c66895	FLIPS 140G GOLD CORN FLIPS	\N	1.000	1.25	1.25	2025-12-06 21:07:53.683291+00	\N
297ae474-1d44-4923-9d84-e6686c44a20d	afac9f18-6043-4d89-82c5-749677c66895	COKOLADNE BANANICE 400G STARK	\N	1.000	5.80	5.80	2025-12-06 21:07:53.683291+00	\N
c72c244d-4238-4dc6-a078-7a8c838ad236	afac9f18-6043-4d89-82c5-749677c66895	KEKS 260G MILKA CHOCO CREME	\N	1.000	2.45	2.45	2025-12-06 21:07:53.683291+00	\N
e08d1e2a-06bf-4f94-99e5-d1b7be15945a	afac9f18-6043-4d89-82c5-749677c66895	PASTRMKA PECENIA	\N	0.272	17.95	4.88	2025-12-06 21:07:53.683291+00	\N
55a2050a-f34d-4af5-a297-3eac7db1c092	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	folija za hranu 30 m box_ONE_Multicolor /pc	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
5f011f57-6627-489c-a3c5-4a2093d4b403	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Ukrasni papir_ONE_Multicolor/pc	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
5bf78cb0-829b-4319-b513-2de4e7d61ba9	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Papirnati tanjuri 12-PAK Xmas_ONE_Patter /pc	\N	1.000	1.02	1.02	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
c415eb4c-c966-4392-b6fd-f141e2c91a59	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Spuzva Christmas oblik_ONE_Multicolor/pc	\N	1.000	0.71	0.71	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
ca01de61-a791-4824-94a4-e0e440a30b50	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Papirnate case 12-PAK Xmas_ONE_Pattern /pc	\N	1.000	1.79	1.79	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
24bbb1fb-e9f2-4df0-ad87-090afbf84d37	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Oznake za poklone sljokice 20kom Xmas_O N/pc	\N	1.000	1.48	1.48	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
d514211c-31ca-435e-ab92-81c0cee3e2ee	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Kuhinjski organizer_ONE_Dark beige/pc	\N	1.000	12.00	12.00	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
6d97cbf5-f6c6-478b-9316-858291b3b8fa	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Svijeca "silver &amp; gold" s poklopcem 13.5/pc	\N	1.000	10.00	10.00	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
5989a1e4-e0ff-4749-841a-bc39058817e5	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Svijeca "silver &amp; gold" s poklopcem 13.5/pc	\N	1.000	10.00	10.00	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
cb4101b7-eeaa-4b8d-8958-1ffb3936d1ec	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Torba papirna velika 32 x 16 x 45 - bez /pc	\N	1.000	0.70	0.70	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.711859+00
55d20b4f-f6aa-4f58-8069-c0cb3f028778	69cbc88b-cfae-4e13-8c5b-703d963ad8ae	Mirisna svijeca u staklu Premium Collec	\N	1.000	5.00	5.00	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.731884+00
fa185bd2-7c4d-4dc7-940a-7306b0d83f04	69cbc88b-cfae-4e13-8c5b-703d963ad8ae	Mirisna svijeca u staklu Premium Collec	\N	1.000	5.00	5.00	2025-12-06 21:07:53.683291+00	2026-01-08 12:25:38.731884+00
22097737-5215-450c-becc-8485faaba4ee	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	folija za hranu 30 m box_ONE_Multicolor /pc	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
a6e5b56a-9610-425c-a322-6c33d2b96aa9	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Ukrasni papir_ONE_Multicolor/pc	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
3864fc4e-cad4-45aa-87b0-42023bff61d8	2e988baa-668b-4754-bd9b-14ea87817717	Mivolis flasteri za djecu	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:59.867616+00
3c0bea7a-8897-4970-a5be-e5c9c17e9644	2e988baa-668b-4754-bd9b-14ea87817717	Maybelline Stay Matt tečni	\N	1.000	21.65	21.65	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:59.867616+00
8caa317a-f1f6-4b9a-9eb3-dff17c448a55	2e988baa-668b-4754-bd9b-14ea87817717	Syoss oleo 8-05	\N	1.000	11.45	11.45	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:59.867616+00
ddb4eccc-2db0-424b-9cb9-f7ba371cbb11	2e988baa-668b-4754-bd9b-14ea87817717	Profissimo salv. 33x33cm Do	\N	1.000	2.25	2.25	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:59.867616+00
6f225cd6-2fd9-4b7a-b1cf-0c013969ec02	3a8f2b7d-ba03-4409-8792-da05d010c8cd	HLJEB 400G SA SJEMELKA MA	\N	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
f908c6a7-5d1e-4cb5-b16b-3c01af5692de	3a8f2b7d-ba03-4409-8792-da05d010c8cd	PASTETA 114G KOKOSTJA ARGETA	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
66cbd4cb-7aa4-49e6-8c55-b3df2a7f9cfc	3a8f2b7d-ba03-4409-8792-da05d010c8cd	KEKS MLJEVENI ZKØGOK LIMENKA	\N	1.000	9.80	9.80	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
c264b27f-6085-433a-9e8c-f85ee998f338	3a8f2b7d-ba03-4409-8792-da05d010c8cd	ZACIN 20G KORTGA NATANOZE KOT	\N	1.000	1.60	1.60	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
16d3248f-153d-4112-bba1-81a1aae97c54	3a8f2b7d-ba03-4409-8792-da05d010c8cd	ZACIN 27G MJELAUNA ZA MEDENJ	\N	1.000	1.15	1.15	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
4852777e-8cac-42b7-83a6-3c89b53ba9cc	3a8f2b7d-ba03-4409-8792-da05d010c8cd	BOMBONE 200G ZELE JELLY ROSHE	\N	1.000	1.95	1.95	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
37f40965-3426-409d-b658-4cf0636241d6	3a8f2b7d-ba03-4409-8792-da05d010c8cd	ZELE 200G LJETNI MIX ROSHEN	\N	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
6a12d701-0f62-4967-949d-1f3a0da85786	3a8f2b7d-ba03-4409-8792-da05d010c8cd	POURAT:	\N	1.000	0.00	0.00	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
44f3892d-5a56-4a7f-ae35-359acc30444d	4b4cd5e5-6105-41c0-b1d7-b85b7cd6e77c	PREMIUM 95 BAS EN 228/L	\N	42.200	2.29	96.64	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:09.047674+00
d2830c38-68f6-47a6-a7b6-f21ac2cd28a6	4b4cd5e5-6105-41c0-b1d7-b85b7cd6e77c	TAKSA NAF.DER.ČL.25S.GPDV/L	\N	42.200	0.01	0.42	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:09.047674+00
486c1109-f4c9-4aed-b878-5efa4c0dbe72	4b4cd5e5-6105-41c0-b1d7-b85b7cd6e77c	CIGARETE DUNHILL DISTINCT	\N	2.000	6.70	13.40	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:09.047674+00
6543e8cf-4ea1-482c-b0e7-f7da03d9a3f5	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	MUGGLE MLIJEKO 3 2%MM 1L 12	\N	3.000	2.10	6.30	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:11.766197+00
10e3c8c6-3178-4722-b200-30e2beb65a9c	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	PODENI KAJMOK MLADI 300G ZD	\N	1.000	9.80	9.80	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:11.766197+00
aab81abc-df3a-4243-9da0-6f9e45034649	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	BEĆJE ABC SIR 100G CLASSIK	\N	1.000	2.35	2.35	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:11.766197+00
fa830080-c691-4ecd-876a-9bb43ddb34e9	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	SEĆI CAJ MENTA 30GR 24/1	\N	1.000	1.50	1.50	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:11.766197+00
fff52149-8ec6-41f3-b0fe-6939303c6790	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	KOKOŠIJA JUHA S TJES 62G PO	\N	3.000	1.30	3.90	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:11.766197+00
5369f276-255c-4018-b757-d078e1531093	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	CIGARE DUNHILL DISTINCT BLE	\N	1.000	6.70	6.70	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:11.766197+00
933ed7a9-ff48-44ff-a9a9-da10c5471993	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	CIGARE DUNHILL DISTINCT BLE	\N	1.000	6.70	6.70	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:11.766197+00
b968af33-43bf-42c1-8bb9-e26d6018e32f	5bdcd961-22ff-432b-9be2-d62542aa7f66	SECE R BRAZILAS 1KG	\N	2.000	1.50	3.00	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:15.110889+00
ad9cb0f6-afc0-4fd2-b863-40635617b4a6	5bdcd961-22ff-432b-9be2-d62542aa7f66	SECE SMEDI 800G	\N	1.000	3.25	3.25	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:15.110889+00
daf82a9b-e560-4f63-b17a-c0bd4f931faf	646313cf-a855-429b-9ecf-92b1ab8a4b39	CIG DUNHIL DIST BL	\N	2.000	6.70	13.40	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:57.744077+00
3c8566be-4da5-4bce-9d22-10256ea4b2ae	9d1ad9df-394b-4a2c-951a-fed3ea60dad7	MLIJEKO MEGGLE 3,2% 657	\N	3.000	2.25	6.75	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:57.744117+00
ad45a928-e733-4214-8685-637062ab8b4a	c23f98a3-99da-4e9c-bf51-0f82daba7d59	Mirisna svijeca u staklu Premium Collec	\N	1.000	5.00	5.00	2025-12-06 21:07:53.683291+00	2026-01-09 10:51:02.657988+00
4f99e23f-8f73-405f-af36-476823e32b7c	c02f9945-ca3c-453a-b22b-f482e705ac86	VOLTAREN RETARD TABLETE 100 MG A 2 0 SA P 172e	\N	1.000	5.85	5.85	2025-12-06 21:07:53.683291+00	2026-01-09 10:51:02.657831+00
89bdeaaf-ff89-4cd7-80a4-5270254fae86	c02f9945-ca3c-453a-b22b-f482e705ac86	PARACETAMOL TABLETE 500 MG A 10 BO SNALIJ 577f	\N	1.000	2.45	2.45	2025-12-06 21:07:53.683291+00	2026-01-09 10:51:02.657831+00
e54e7d89-b4a6-486a-8d37-1391b3d1f312	c02f9945-ca3c-453a-b22b-f482e705ac86	ANDOL TABLETE 300 MG A 20 5673	\N	1.000	5.70	5.70	2025-12-06 21:07:53.683291+00	2026-01-09 10:51:02.657831+00
972363dd-6e05-43f0-b4ef-7fe524c3f335	f49ab2aa-5188-4cfe-9b10-2570e8bbe66b	CASA_ZA_URIN_KLIK_125_ML_ROMED_48d	\N	2.000	0.55	1.10	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.287084+00
06075c96-fdd5-4508-b1c8-078c5d7546e9	f49ab2aa-5188-4cfe-9b10-2570e8bbe66b	TOPLOMJER_DIGITALNI_UEBE_TH1_COLOR _CVRST_17e0	\N	1.000	7.15	7.15	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.287084+00
03932333-064f-483f-88f1-4ba3f9289803	f5f7141e-8a01-4735-b1a6-a8ce608f9c9c	TUBORG 0,33 NEPOVRATNI/KO	\N	24.000	1.55	37.20	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.287023+00
f17c8d91-34ba-4a0c-87c1-5c144aeec071	f5f7141e-8a01-4735-b1a6-a8ce608f9c9c	SCHWEPPES TONIC 1L/KO	\N	3.000	2.00	6.00	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.287023+00
d54bd010-f022-45bf-9001-40f3bee9ef1e	f5f7141e-8a01-4735-b1a6-a8ce608f9c9c	BULLDOG GIN SA CASOM 0,7/KO	\N	1.000	42.00	42.00	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.287023+00
f608229a-6f55-4264-9383-daaeac551ece	c6427a31-da10-4e9f-b6f9-30275dfcc392	Snala za kosu BH231226	ea97eef6-47b2-425e-af6f-2db6504816ff	1.000	1.95	1.95	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.287023+00
9ff3baa4-51ee-48a7-8969-f17c68958ad1	c6427a31-da10-4e9f-b6f9-30275dfcc392	Snala za kosu BH231226	ea97eef6-47b2-425e-af6f-2db6504816ff	1.000	1.95	1.95	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.287023+00
c8a5dee2-6245-400a-a06a-b70e438cccbf	afac9f18-6043-4d89-82c5-749677c66895	PASTRMKA PECENIA	\N	0.266	17.95	4.77	2025-12-06 21:07:53.683291+00	\N
3c3ede05-b6aa-4801-9b0a-3933eb016382	afac9f18-6043-4d89-82c5-749677c66895	SUNKA PURECA DELUX VINDON	\N	0.156	29.95	4.67	2025-12-06 21:07:53.683291+00	\N
a325271a-14bc-4ee0-b37c-17003e885e17	aa176990-095b-4d63-b2d7-34205e51f71e	STRANGE LUVE GIN 40% 0,7L/KO	\N	1.000	19.95	19.95	2025-12-06 21:07:53.683291+00	2026-01-08 18:19:45.243258+00
8a548649-f1f1-4372-aeac-73628ee2447f	d5200e3c-f16c-4bda-b82e-09975b314c67	STRANGE LUVE GIN 40% 0,7L/KO	\N	1.000	19.95	19.95	2025-12-06 21:07:53.683291+00	2026-01-08 18:21:10.332699+00
7eed7776-96f4-40c5-8538-851c8714e0f8	e742b959-557e-46cb-82f1-a8fee4ce5385	STRANGE LUVE GIN 40% 0,7L/KO	\N	1.000	19.95	19.95	2025-12-06 21:07:53.683291+00	2026-01-08 18:22:10.124466+00
fde29a40-0ea6-4520-a0d8-356072308990	d634afd4-7665-4a8a-a251-5f743067bf76	STRANGE LUVE GIN 40% 0,7L/KO	\N	1.000	19.95	19.95	2025-12-06 21:07:53.683291+00	2026-01-08 18:22:32.87765+00
697976d4-2613-4510-86f5-79b457659d61	d7510606-d56a-4071-b701-8126df2b3420	STRANGE LUVE GIN 40% 0,7L/KO	\N	1.000	19.95	19.95	2025-12-06 21:07:53.683291+00	2026-01-08 18:23:36.900013+00
6a4cc16e-ae45-4687-bcc4-418b426a31e0	afac9f18-6043-4d89-82c5-749677c66895	PECIVO 9X33 33G BROTLINIES SU	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	\N
12d86634-5475-441a-9b64-b459e5469ce0	afac9f18-6043-4d89-82c5-749677c66895	SVJEZI LIMUN	\N	0.854	2.60	2.22	2025-12-06 21:07:53.683291+00	\N
6f343d30-ca05-4309-a866-46d6ecfd5b6d	b59747f2-d587-4d83-bdc8-b17316fbd20e	CASA_ZA_URIN_KLIK_125_ML_ROMED_48d	\N	2.000	0.55	1.10	2025-12-06 21:07:53.683291+00	\N
e6301341-0429-44e0-8445-f433c341cf28	b59747f2-d587-4d83-bdc8-b17316fbd20e	TOPLOMJER_DIGITALNI_UEBE_TH1_COLOR _CVRST_17e0	\N	1.000	7.15	7.15	2025-12-06 21:07:53.683291+00	\N
de9a2ffe-1264-4867-baa0-21429dfc3660	79891ebc-a33d-47f8-8309-1492a8845c05	STRANGE LUVE GIN 40% 0,7L/KO	\N	1.000	19.95	19.95	2025-12-06 21:07:53.683291+00	2026-01-08 18:46:16.647051+00
354703a6-bae9-4822-ae0c-0afdfd788b08	1e0e0e3f-7a4f-443b-9c7e-ef1f6b37a766	SE7033 POKLON VRECICA 40X30CM DJ ASS	\N	1.000	2.50	2.50	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:32.448857+00
586c3795-76d2-415a-b07a-12148785286e	1e0e0e3f-7a4f-443b-9c7e-ef1f6b37a766	BOMBONE GUMENE 100G HARIBO BE	028b5b67-587c-442c-8328-dcf00e273f19	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:32.448857+00
cd95b5ba-e66f-4648-915d-553aea8f032b	1e0e0e3f-7a4f-443b-9c7e-ef1f6b37a766	BOMBONE GUMENE 100G HARIBO BE	028b5b67-587c-442c-8328-dcf00e273f19	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:32.448857+00
517ba63a-816b-4c99-99b1-06ea785fdbae	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Papirnati tanjuri 12-PAK Xmas_ONE_Patter /pc	\N	1.000	1.02	1.02	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
2f26e21a-4e6b-427d-9e9c-8dfdc7f1bbdf	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Spuzva Christmas oblik_ONE_Multicolor/pc	\N	1.000	0.71	0.71	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
320a3062-7d50-498e-842a-968826033d0d	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Papirnate case 12-PAK Xmas_ONE_Pattern /pc	\N	1.000	1.79	1.79	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
23e529c6-9760-4671-abb9-3b0ff646b8f6	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Kuhinjski organizer_ONE_Dark beige/pc	\N	1.000	12.00	12.00	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
6f7421ce-8373-4723-bbd0-cfa41734fe94	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Torba papirna velika 32 x 16 x 45 - bez /pc	\N	1.000	0.70	0.70	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
f6a7e913-3660-44a0-9da3-c8bace89e3f8	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Svijeca "silver &amp; gold" s poklopcem 13.5/pc	f48819b3-6744-44d4-a5a1-17858f16ba5c	1.000	10.00	10.00	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
aad4cea8-b650-4cd5-b1c4-058b3c767876	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Oznake za poklone sljokice 20kom Xmas_O N/pc	c93b9f4d-6780-4460-8330-7512d830d4d3	1.000	1.48	1.48	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
bccbcc41-068a-48c3-920a-e727b0e7bfa3	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Svijeca "silver &amp; gold" s poklopcem 13.5/pc	f48819b3-6744-44d4-a5a1-17858f16ba5c	1.000	10.00	10.00	2025-12-06 21:07:53.683291+00	2026-01-08 22:55:36.525138+00
86328b54-cb19-42a8-a723-bf9883d026f5	3a8f2b7d-ba03-4409-8792-da05d010c8cd	SECER 10G SA AROMON CIMETA DR	8876cc99-4acf-4810-918a-b00e284433a8	1.000	0.65	0.65	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
3448d9b4-993d-4e06-a117-9cd49ef48e6e	3a8f2b7d-ba03-4409-8792-da05d010c8cd	SECER 10G SA AROMON LIMUNA DR	8876cc99-4acf-4810-918a-b00e284433a8	1.000	0.65	0.65	2025-12-06 21:07:53.683291+00	2026-01-08 22:56:04.143073+00
b282f119-db7f-4bec-8882-21edb7e2c220	89310a1f-e8f7-4ade-8f46-28ca5f4cfbe2	STRANGE LUVE GIN 40% 0,7L/KO	\N	1.000	19.95	19.95	2025-12-06 21:07:53.683291+00	2026-01-09 09:59:19.053856+00
f95898bc-fda8-4b7e-9430-1e65cdc3686f	ef590425-6f90-49bb-8c21-b79e544ded7e	Mivolis flasteri za djecu	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	2026-01-09 09:59:19.053856+00
d14ffcdd-f88d-4c33-81be-3da526b5826f	ef590425-6f90-49bb-8c21-b79e544ded7e	Maybelline Stay Matt tečni	\N	1.000	21.65	21.65	2025-12-06 21:07:53.683291+00	2026-01-09 09:59:19.053856+00
ffc4aff4-09a8-4e60-93b6-18f4413161a2	ef590425-6f90-49bb-8c21-b79e544ded7e	Syoss oleo 8-05	\N	1.000	11.45	11.45	2025-12-06 21:07:53.683291+00	2026-01-09 09:59:19.053856+00
1ec90b22-001b-40b6-b505-51d9dcdcf12f	ef590425-6f90-49bb-8c21-b79e544ded7e	Profissimo salv. 33x33cm Do	\N	1.000	2.25	2.25	2025-12-06 21:07:53.683291+00	2026-01-09 09:59:19.053856+00
1163af93-7b4c-4acc-9303-e6be9c451409	bcb1459b-f678-483a-9db1-47f0c2eb32bf	VRECICA TREGERUNA	\N	2.000	0.10	0.20	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
70e4e4fd-0ce5-4dac-bdef-4a80a090fb08	180dcbfd-068c-4a14-971e-659efbf6d130	BOMBONJERA 230G RAFFAELLO FER	\N	1.000	9.90	9.90	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
6e05bf3b-c644-4b77-8981-b7b940490cca	180dcbfd-068c-4a14-971e-659efbf6d130	SA9192 ZDJELA SA POKLOPCEM 0 65L FR	\N	1.000	1.90	1.90	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
ebed7592-f202-42b5-b486-8a109095dcf9	180dcbfd-068c-4a14-971e-659efbf6d130	KOLAC 230G ECLAIRS MARLENKA	\N	1.000	10.75	10.75	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
66a008cf-1289-491d-a543-9799868e7756	180dcbfd-068c-4a14-971e-659efbf6d130	SUNKA PURECA DELUX VINDON	\N	0.098	29.95	2.94	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
fba29b3c-89b3-4c08-9ed4-60e84ac27d87	c23f98a3-99da-4e9c-bf51-0f82daba7d59	Mirisna svijeca u staklu Premium Collec	\N	1.000	5.00	5.00	2025-12-06 21:07:53.683291+00	2026-01-09 10:51:02.657988+00
fa8d5a53-21bf-4125-ac57-7b6c1b0a2e51	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	HLJEB 400G SA SJEMELKA MA	\N	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
24a134fa-fd7d-424c-a8cc-4bf6d80fdfa5	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	PASTETA 114G KOKOSTJA ARGETA	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
c1422d9c-dc30-42c8-9839-b5840dd029a5	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	KEKS MLJEVENI ZKØGOK LIMENKA	\N	1.000	9.80	9.80	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
6b3dfdaf-dcb9-48e9-919c-82f19c5b1f12	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	ZACIN 20G KORTGA NATANOZE KOT	\N	1.000	1.60	1.60	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
1da10649-3627-4f7d-b9dc-b9bb09603cba	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	SECER 10G SA AROMON CIMETA DR	8876cc99-4acf-4810-918a-b00e284433a8	1.000	0.65	0.65	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
74e2c919-98e3-4274-bb5c-70500c022b30	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	SECER 10G SA AROMON LIMUNA DR	8876cc99-4acf-4810-918a-b00e284433a8	1.000	0.65	0.65	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
f5d075ef-2667-4563-93c0-5638ab07d50e	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	ZACIN 27G MJELAUNA ZA MEDENJ	\N	1.000	1.15	1.15	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
f9fba5ce-c60e-4f7e-85ff-6e39d0efcee5	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	BOMBONE 200G ZELE JELLY ROSHE	\N	1.000	1.95	1.95	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
4db02613-e512-4a98-92d4-86704ccf2adc	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	ZELE 200G LJETNI MIX ROSHEN	\N	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
717ca79a-86e4-4ed6-89ab-0e80d8db0775	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	POURAT:	\N	1.000	0.00	0.00	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:00.3012+00
effdd54d-065b-4040-8553-b150f8fe5a79	bcb1459b-f678-483a-9db1-47f0c2eb32bf	KAFA ESPRESSO 112G NESCAFE DO	\N	1.000	9.95	9.95	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
edf8e21d-38a0-4ec1-9000-5348725b4de0	bcb1459b-f678-483a-9db1-47f0c2eb32bf	SOK 1 25L COCA COLA	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
3918af6d-91ad-47bf-be32-739e6b7a8ca5	bcb1459b-f678-483a-9db1-47f0c2eb32bf	FLIPS 140G GOLD CORN FLIPS	\N	1.000	1.25	1.25	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
124d820d-6b28-4928-b095-6b1402b064d3	bcb1459b-f678-483a-9db1-47f0c2eb32bf	COKOLADNE BANANICE 400G STARK	\N	1.000	5.80	5.80	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
b25c2a2b-773c-46f8-8a1f-5754b8512db1	bcb1459b-f678-483a-9db1-47f0c2eb32bf	KEKS 260G MILKA CHOCO CREME	\N	1.000	2.45	2.45	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
3c4741c8-028d-4dd8-a70c-e6aebdb48a3b	bcb1459b-f678-483a-9db1-47f0c2eb32bf	PASTRMKA PECENIA	\N	0.272	17.95	4.88	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
f4ba859e-5544-40b9-ad70-96021b57dfad	bcb1459b-f678-483a-9db1-47f0c2eb32bf	PASTRMKA PECENIA	\N	0.266	17.95	4.77	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
4ee897a6-757f-4150-80c3-235e0138f256	bcb1459b-f678-483a-9db1-47f0c2eb32bf	SUNKA PURECA DELUX VINDON	\N	0.156	29.95	4.67	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
279f5f42-9547-40a7-86ca-e4cfe2951e6f	bcb1459b-f678-483a-9db1-47f0c2eb32bf	PECIVO 9X33 33G BROTLINIES SU	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
7b26eecc-2be4-4628-93ef-0c664eed2ee5	bcb1459b-f678-483a-9db1-47f0c2eb32bf	SVJEZI LIMUN	\N	0.854	2.60	2.22	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.128979+00
f9307a6b-8fd8-4bdc-bb97-a638b166a092	f65a59cb-16ac-4d7a-a252-7090916d4d1a	BOMBONE GUMENE 100G HARIBO BE	028b5b67-587c-442c-8328-dcf00e273f19	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	\N
8bee2c73-7ed9-4fc3-9998-2ea0c9d2f110	f65a59cb-16ac-4d7a-a252-7090916d4d1a	BOMBONE GUMENE 100G HARIBO BE	028b5b67-587c-442c-8328-dcf00e273f19	1.000	2.10	2.10	2025-12-06 21:07:53.683291+00	\N
ff04f2a3-c25f-4455-a632-f90c0f301445	f65a59cb-16ac-4d7a-a252-7090916d4d1a	SE7033 POKLON VRECICA 40X30CM DJ ASS	\N	1.000	2.50	2.50	2025-12-06 21:07:53.683291+00	\N
96d8a878-9d8d-46a6-b9c5-2646cfbc7e9c	a56e84de-9e68-431e-9ab5-1b3012f959bb	folija za hranu 30 m box_ONE_Multicolor /pc	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	\N
d80d81ff-2843-4e54-aa05-c9cbcedbe648	a56e84de-9e68-431e-9ab5-1b3012f959bb	Ukrasni papir_ONE_Multicolor/pc	\N	1.000	2.00	2.00	2025-12-06 21:07:53.683291+00	\N
952c2865-8ce4-48e1-abec-30ebcac616f5	a56e84de-9e68-431e-9ab5-1b3012f959bb	Papirnati tanjuri 12-PAK Xmas_ONE_Patter /pc	\N	1.000	1.02	1.02	2025-12-06 21:07:53.683291+00	\N
c9d98b31-9870-433b-926c-e671a303b2c5	a56e84de-9e68-431e-9ab5-1b3012f959bb	Spuzva Christmas oblik_ONE_Multicolor/pc	\N	1.000	0.71	0.71	2025-12-06 21:07:53.683291+00	\N
33aa3037-94f7-43b1-b3d8-0b6cc619465f	a56e84de-9e68-431e-9ab5-1b3012f959bb	Papirnate case 12-PAK Xmas_ONE_Pattern /pc	\N	1.000	1.79	1.79	2025-12-06 21:07:53.683291+00	\N
b22b6173-1460-45d1-a567-1ea682d55dd4	a56e84de-9e68-431e-9ab5-1b3012f959bb	Oznake za poklone sljokice 20kom Xmas_O N/pc	c93b9f4d-6780-4460-8330-7512d830d4d3	1.000	1.48	1.48	2025-12-06 21:07:53.683291+00	\N
7dcb0492-dfe9-43b4-b16c-9395f6bb7826	a56e84de-9e68-431e-9ab5-1b3012f959bb	Kuhinjski organizer_ONE_Dark beige/pc	\N	1.000	12.00	12.00	2025-12-06 21:07:53.683291+00	\N
f5d3776d-c23b-4653-8c96-2d8e02932a13	a56e84de-9e68-431e-9ab5-1b3012f959bb	Svijeca "silver &amp; gold" s poklopcem 13.5/pc	f48819b3-6744-44d4-a5a1-17858f16ba5c	1.000	10.00	10.00	2025-12-06 21:07:53.683291+00	\N
22fec3c6-83f0-4394-bc04-f73834439d3f	a56e84de-9e68-431e-9ab5-1b3012f959bb	Svijeca "silver &amp; gold" s poklopcem 13.5/pc	f48819b3-6744-44d4-a5a1-17858f16ba5c	1.000	10.00	10.00	2025-12-06 21:07:53.683291+00	\N
d673498f-f100-4c03-946d-3de2a2fcb89c	a56e84de-9e68-431e-9ab5-1b3012f959bb	Torba papirna velika 32 x 16 x 45 - bez /pc	\N	1.000	0.70	0.70	2025-12-06 21:07:53.683291+00	\N
2965be8d-9403-45dc-886e-e69f28191531	4d6f9473-c71b-4780-b131-2bed4664a1d0	Mivolis flasteri za djecu	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:38.693595+00
ff2f5057-3978-4694-bd9d-28cd9f1bac4c	4d6f9473-c71b-4780-b131-2bed4664a1d0	Maybelline Stay Matt tečni	\N	1.000	21.65	21.65	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:38.693595+00
996bad89-3c34-4b29-87e3-0f28c02f872a	4d6f9473-c71b-4780-b131-2bed4664a1d0	Syoss oleo 8-05	\N	1.000	11.45	11.45	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:38.693595+00
9e07a4a5-cd86-46a0-b354-179c34e44cea	4d6f9473-c71b-4780-b131-2bed4664a1d0	Profissimo salv. 33x33cm Do	\N	1.000	2.25	2.25	2025-12-06 21:07:53.683291+00	2026-01-09 11:28:38.693595+00
dfc79b9f-3e56-41f9-88b2-2a193319f15d	180d5ef2-6858-48b6-bc47-ae918980f616	Mivolis flasteri za djecu	\N	1.000	1.85	1.85	2025-12-06 21:07:53.683291+00	2026-01-09 13:32:46.638349+00
aa62df1f-0256-460e-bcf9-477c898905a1	180d5ef2-6858-48b6-bc47-ae918980f616	Maybelline Stay Matt tečni	\N	1.000	21.65	21.65	2025-12-06 21:07:53.683291+00	2026-01-09 13:32:46.638349+00
a74c8f07-305f-4d39-a79e-5c40bcecbb45	180d5ef2-6858-48b6-bc47-ae918980f616	Syoss oleo 8-05	\N	1.000	11.45	11.45	2025-12-06 21:07:53.683291+00	2026-01-09 13:32:46.638349+00
a1fca492-2ae7-4869-abb9-a7002d93368f	180d5ef2-6858-48b6-bc47-ae918980f616	Profissimo salv. 33x33cm Do	\N	1.000	2.25	2.25	2025-12-06 21:07:53.683291+00	2026-01-09 13:32:46.638349+00
eac105c9-198b-4572-b46c-8470e005c514	14484146-1dff-49f1-a1ec-3ae275798ca3	Snala za kosu BH231226	ea97eef6-47b2-425e-af6f-2db6504816ff	1.000	1.95	1.95	2025-12-06 21:07:53.683291+00	2026-01-09 13:32:46.676327+00
cf3d2ef7-4d3f-4622-83fe-963703263e72	14484146-1dff-49f1-a1ec-3ae275798ca3	Snala za kosu BH231226	ea97eef6-47b2-425e-af6f-2db6504816ff	1.000	1.95	1.95	2025-12-06 21:07:53.683291+00	2026-01-09 13:32:46.676327+00
143b0fbb-ab23-430d-b08c-9505f1e75bc6	180dcbfd-068c-4a14-971e-659efbf6d130	SALATA AMERICKA	\N	0.314	7.95	2.50	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
f7357273-2b87-46aa-b5c9-04f1a0cd2565	180dcbfd-068c-4a14-971e-659efbf6d130	VODA PRIRODNA 2 5L AQUA VIVA	\N	1.000	1.55	1.55	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
6a729747-84d5-4a3f-9616-ca66e9447bf9	180dcbfd-068c-4a14-971e-659efbf6d130	KAJMAK MLADI PADJENI RINFUZA	\N	0.198	29.40	5.82	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
75f9a8c9-200a-4688-82d8-786a3d97e0b1	180dcbfd-068c-4a14-971e-659efbf6d130	KAFA MLJEVENA 200G ZLATNA DZE	\N	1.000	6.85	6.85	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
b6b40180-4e21-4d0b-9e37-b8383185ece5	180dcbfd-068c-4a14-971e-659efbf6d130	SIR SITNI 500G PADJENI	\N	1.000	3.50	3.50	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
6a6c6957-ceef-491b-83cd-2785baf22e1b	180dcbfd-068c-4a14-971e-659efbf6d130	MILERAM 30 MEGGLE RINFUZA	\N	0.352	11.95	4.21	2025-12-06 21:07:53.683291+00	2026-01-09 10:33:28.129002+00
\.


--
-- TOC entry 3418 (class 0 OID 16725)
-- Dependencies: 212
-- Data for Name: expenses; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.expenses (purchased_at, updated_at, is_posted, deleted_at, currency, supplier_id, payer_id, id, notes, total_amount, receipt_id, created_at, user_id) FROM stdin;
2025-10-21 15:20:55.849076+00	2025-12-06 21:07:53.683291+00	t	2025-12-12 11:23:21.639874+00	BAM	5b7e78de-b195-4848-832c-d84f0e0631df	9ce86e1a-d4de-4368-8008-decdfc9bca20	51152062-e36f-4238-9e82-a2207c7b61e9	Appliance repair	210.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-06 15:14:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-07 20:54:08.896522+00	BAM	fad06371-df63-4788-9f1f-6d24998e8e2e	23636b13-c7af-47cb-9046-2b1a155f8517	3fb08e66-6244-4c73-b09a-c1a1b68f80a2	Extracted from receipt: IMG_3612.jpg	41.70	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-06 15:40:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-07 20:54:08.896576+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	08d4ff77-426f-4fd7-a4a4-0573dbc5f823	Extracted from receipt: IMG_3617.jpg	85.20	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-12-23 18:25:00+00	2025-12-06 21:07:53.683291+00	t	2025-12-26 12:35:30.707107+00	BAM	36296fee-8aa6-4a34-87f6-f5672cabdd83	9ce86e1a-d4de-4368-8008-decdfc9bca20	5e265faf-7c59-4bb7-8bf8-790560e8fc3d	Extracted from receipt: samoni-1.png	19.95	\N	2025-12-06 21:07:53.683291+00	\N
2025-12-23 18:25:00+00	2025-12-06 21:07:53.683291+00	t	2025-12-26 18:01:25.322168+00	BAM	36296fee-8aa6-4a34-87f6-f5672cabdd83	9ce86e1a-d4de-4368-8008-decdfc9bca20	b8e0ce26-ac5a-4c92-91ab-3948eebfbeaa	Extracted from receipt: samoni-1.png	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-12-07 15:21:00+00	2025-12-17 10:42:50.85531+00	t	2026-01-06 13:58:28.27787+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	9ce86e1a-d4de-4368-8008-decdfc9bca20	f1ae5eb1-2ba1-442c-a06b-88bf1f6631e0	Groceries week 1r	6.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-12-17 09:54:00+00	2025-12-18 08:11:41.450573+00	t	2026-01-06 13:58:28.279456+00	EUR	6057fb90-df80-416c-b1a0-5a9a9dfcc264	9ce86e1a-d4de-4368-8008-decdfc9bca20	cbcb31ce-1ccd-45be-a7d7-bc59fb287a72	Updated with items	76.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-11-20 15:21:06.430686+00	2025-12-06 21:07:53.683291+00	t	2026-01-06 13:58:28.278139+00	BAM	32d65bb6-c7db-465a-a5fc-16ccccb853c3	9ce86e1a-d4de-4368-8008-decdfc9bca20	b5feb79d-5934-4c62-8211-965918431a19	Household supplies	75.25	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-12-12 23:29:00+00	2025-12-18 15:43:45.967112+00	t	2026-01-06 13:58:28.28194+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	9ce86e1a-d4de-4368-8008-decdfc9bca20	c8429d3f-5692-43b6-bc1c-c0a8330db681	dgzfdsdf g	12.28	\N	2025-12-06 21:07:53.683291+00	\N
2025-10-21 15:21:13.10359+00	2025-12-06 21:07:53.683291+00	t	2026-01-06 13:58:28.285783+00	BAM	5b7e78de-b195-4848-832c-d84f0e0631df	9ce86e1a-d4de-4368-8008-decdfc9bca20	acf5feb4-676f-4d7e-9480-2e113aee2890	Appliance repair	210.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-11-20 15:20:55.849076+00	2025-12-06 21:07:53.683291+00	t	2026-01-06 13:58:28.292873+00	BAM	32d65bb6-c7db-465a-a5fc-16ccccb853c3	9ce86e1a-d4de-4368-8008-decdfc9bca20	445b5712-9cb4-4b85-bbb1-db393ba37031	Household supplies	75.25	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-12-12 22:20:00+00	2025-12-18 16:10:17.352708+00	t	2026-01-06 13:58:28.294339+00	EUR	5b7e78de-b195-4848-832c-d84f0e0631df	9ce86e1a-d4de-4368-8008-decdfc9bca20	940d2800-29f9-4284-9486-eff38b9d27da		9.42	\N	2025-12-06 21:07:53.683291+00	\N
2025-12-12 23:26:00+00	2025-12-18 16:52:59.814344+00	t	2026-01-06 13:58:28.295851+00	USD	6057fb90-df80-416c-b1a0-5a9a9dfcc264	9ce86e1a-d4de-4368-8008-decdfc9bca20	478fde16-6c02-4edd-9aa2-cc33e6903f97		9.19	\N	2025-12-06 21:07:53.683291+00	\N
2025-12-07 15:20:00+00	2025-12-17 12:10:26.437295+00	t	2026-01-06 13:58:28.297853+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	9ce86e1a-d4de-4368-8008-decdfc9bca20	9b7fa018-bedb-4578-96ed-e57d90c2a778	Groceries week 1	9.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2025-12-12 22:19:00+00	2025-12-24 08:35:32.4973+00	t	2026-01-06 13:58:28.300439+00	BAM	32d65bb6-c7db-465a-a5fc-16ccccb853c3	9ce86e1a-d4de-4368-8008-decdfc9bca20	6daeea74-8001-4452-aa84-85215356766d		9.00	\N	2025-12-06 21:07:53.683291+00	\N
2026-01-06 15:32:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-06 15:40:44.578116+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	a75f9be7-62af-44a9-9e72-985fdb950557	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:46:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 22:55:59.867616+00	BAM	3798e5f7-776b-462a-b182-55f4906272ce	23636b13-c7af-47cb-9046-2b1a155f8517	2e988baa-668b-4754-bd9b-14ea87817717	Extracted from receipt: IMG_3613.jpg	37.20	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:47:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 11:28:00.287023+00	BAM	cf83233f-316c-40f8-a022-b8b3c3c5df45	23636b13-c7af-47cb-9046-2b1a155f8517	c6427a31-da10-4e9f-b6f9-30275dfcc392	Extracted from receipt: IMG_3614.jpg	3.90	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-07 12:09:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-07 20:54:08.896098+00	BAM	5b7e78de-b195-4848-832c-d84f0e0631df	23636b13-c7af-47cb-9046-2b1a155f8517	3d894a2d-02a6-4dd2-af79-08a9fd9ee764	Extracted from receipt: IMG_3811.jpg	37.25	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2023-12-25 18:46:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-07 20:54:08.903689+00	BAM	fad06371-df63-4788-9f1f-6d24998e8e2e	23636b13-c7af-47cb-9046-2b1a155f8517	f9cbf3a8-41f1-40ef-9c8b-475392249b36	Extracted from receipt: IMG_3611.jpg	10.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-06 20:03:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-07 20:54:08.896111+00	BAM	5b7e78de-b195-4848-832c-d84f0e0631df	23636b13-c7af-47cb-9046-2b1a155f8517	330fda8c-f6a8-4edb-a134-31418a5fd4d8	Extracted from receipt: IMG_3811.jpg	42.66	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:51:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 11:28:00.287084+00	BAM	7192d67a-8306-4bc2-a8d0-501782f3015b	23636b13-c7af-47cb-9046-2b1a155f8517	f49ab2aa-5188-4cfe-9b10-2570e8bbe66b	Extracted from receipt: IMG_3814.jpg	8.25	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:47:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 10:51:02.657831+00	BAM	7192d67a-8306-4bc2-a8d0-501782f3015b	23636b13-c7af-47cb-9046-2b1a155f8517	c02f9945-ca3c-453a-b22b-f482e705ac86	Extracted from receipt: IMG_3615.jpg	14.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:50:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 22:56:15.110889+00	BAM	ab2e9d22-1de2-400d-9e53-cdb87106568d	23636b13-c7af-47cb-9046-2b1a155f8517	5bdcd961-22ff-432b-9be2-d62542aa7f66	Extracted from receipt: IMG_3812.jpg	6.25	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:53:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 18:19:45.243258+00	BAM	36296fee-8aa6-4a34-87f6-f5672cabdd83	23636b13-c7af-47cb-9046-2b1a155f8517	aa176990-095b-4d63-b2d7-34205e51f71e	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 18:19:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 18:21:10.332699+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	d5200e3c-f16c-4bda-b82e-09975b314c67	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 18:21:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 18:22:10.124466+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	e742b959-557e-46cb-82f1-a8fee4ce5385	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 18:22:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 18:22:32.87765+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	d634afd4-7665-4a8a-a251-5f743067bf76	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 18:23:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 18:23:36.900013+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	d7510606-d56a-4071-b701-8126df2b3420	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:49:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 10:33:57.744077+00	BAM	ab2e9d22-1de2-400d-9e53-cdb87106568d	23636b13-c7af-47cb-9046-2b1a155f8517	646313cf-a855-429b-9ecf-92b1ab8a4b39	Extracted from receipt: IMG_3619.jpg	13.40	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 11:21:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 12:25:38.731884+00	BAM	fad06371-df63-4788-9f1f-6d24998e8e2e	23636b13-c7af-47cb-9046-2b1a155f8517	69cbc88b-cfae-4e13-8c5b-703d963ad8ae	Extracted from receipt: IMG_3611.jpg	10.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:52:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 22:55:32.448857+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	23636b13-c7af-47cb-9046-2b1a155f8517	1e0e0e3f-7a4f-443b-9c7e-ef1f6b37a766	Extracted from receipt: IMG_3610.jpg	6.70	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 11:21:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 11:22:22.980916+00	BAM	fad06371-df63-4788-9f1f-6d24998e8e2e	23636b13-c7af-47cb-9046-2b1a155f8517	78e0c523-56b1-4fc9-ac3c-03b3e6a03a15	Extracted from receipt: IMG_3612.jpg	41.70	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 12:09:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 12:25:38.711859+00	BAM	fad06371-df63-4788-9f1f-6d24998e8e2e	23636b13-c7af-47cb-9046-2b1a155f8517	95f122b7-ea13-48ce-b0a0-c2b5cd275cf3	Extracted from receipt: IMG_3612.jpg	41.70	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:52:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 10:51:02.657988+00	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	23636b13-c7af-47cb-9046-2b1a155f8517	c23f98a3-99da-4e9c-bf51-0f82daba7d59	Extracted from receipt: IMG_3611.jpg	10.00	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-09 11:28:00+00	2026-01-09 13:16:47.812495+00	t	2026-01-09 13:32:46.676327+00	EUR	cf83233f-316c-40f8-a022-b8b3c3c5df45	23636b13-c7af-47cb-9046-2b1a155f8517	14484146-1dff-49f1-a1ec-3ae275798ca3	Extracted from receipt: IMG_3614.jpg	3.90	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 18:43:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 10:33:28.129002+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	23636b13-c7af-47cb-9046-2b1a155f8517	180dcbfd-068c-4a14-971e-659efbf6d130	Extracted from receipt: IMG_3616.jpg	49.92	\N	2025-12-06 21:07:53.683291+00	\N
2026-01-08 18:31:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 18:46:16.647051+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	79891ebc-a33d-47f8-8309-1492a8845c05	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 18:46:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 09:59:19.053856+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	89310a1f-e8f7-4ade-8f46-28ca5f4cfbe2	Extracted from receipt: IMG_3609.jpg	19.95	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:48:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 11:28:00.287023+00	BAM	3e8cbf97-854d-4536-b366-ac2296910e85	23636b13-c7af-47cb-9046-2b1a155f8517	f5f7141e-8a01-4735-b1a6-a8ce608f9c9c	Extracted from receipt: IMG_3617.jpg	85.20	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:50:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 22:56:11.766197+00	BAM	5b7e78de-b195-4848-832c-d84f0e0631df	23636b13-c7af-47cb-9046-2b1a155f8517	4d6fb7f6-28d6-49ea-b871-6691bb50e9a8	Extracted from receipt: IMG_3811.jpg	37.25	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-09 13:56:00+00	2025-12-06 21:07:53.683291+00	t	\N	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	23636b13-c7af-47cb-9046-2b1a155f8517	afac9f18-6043-4d89-82c5-749677c66895	Extracted from receipt: IMG_3815.jpg	40.04	a15ccedc-0769-4d71-b949-79d125c06554	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-09 13:57:00+00	2025-12-06 21:07:53.683291+00	t	\N	BAM	7192d67a-8306-4bc2-a8d0-501782f3015b	23636b13-c7af-47cb-9046-2b1a155f8517	b59747f2-d587-4d83-bdc8-b17316fbd20e	Extracted from receipt: IMG_3814.jpg	8.25	a7d47b14-3ed0-4761-b90e-646f41b210ac	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-09 14:02:00+00	2025-12-06 21:07:53.683291+00	t	\N	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	23636b13-c7af-47cb-9046-2b1a155f8517	f65a59cb-16ac-4d7a-a252-7090916d4d1a	Extracted from receipt: IMG_3610.jpg	6.70	f22c3592-941a-46a8-9df9-63cb79c29e82	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-09 14:04:00+00	2025-12-06 21:07:53.683291+00	t	\N	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	23636b13-c7af-47cb-9046-2b1a155f8517	a56e84de-9e68-431e-9ab5-1b3012f959bb	Extracted from receipt: IMG_3612.jpg	41.70	5028c4cf-1b07-479d-855d-ca717448bf28	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:50:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 10:33:57.744117+00	BAM	ab2e9d22-1de2-400d-9e53-cdb87106568d	23636b13-c7af-47cb-9046-2b1a155f8517	9d1ad9df-394b-4a2c-951a-fed3ea60dad7	Extracted from receipt: IMG_3620.jpg	6.75	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2023-12-25 05:21:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 22:56:04.143073+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	23636b13-c7af-47cb-9046-2b1a155f8517	3a8f2b7d-ba03-4409-8792-da05d010c8cd	Extracted from receipt: IMG_3618.jpg	21.85	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:52:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 22:55:36.525138+00	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	23636b13-c7af-47cb-9046-2b1a155f8517	1f2ad8ba-3433-4a09-9f9a-8c80ceb19acf	Extracted from receipt: IMG_3612.jpg	41.70	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 15:51:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-08 22:56:09.047674+00	BAM	bdfe1baa-e999-4910-a71a-0a8594f2020a	23636b13-c7af-47cb-9046-2b1a155f8517	4b4cd5e5-6105-41c0-b1d7-b85b7cd6e77c	Extracted from receipt: IMG_3813.jpg	110.46	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2023-12-25 05:21:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 11:28:00.3012+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	23636b13-c7af-47cb-9046-2b1a155f8517	d56376fb-0cd0-4c2c-a8ca-bd544228fc28	Extracted from receipt: IMG_3618.jpg	21.85	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 18:39:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 10:33:28.128979+00	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	23636b13-c7af-47cb-9046-2b1a155f8517	bcb1459b-f678-483a-9db1-47f0c2eb32bf	Extracted from receipt: IMG_3815.jpg	40.04	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-08 22:56:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 09:59:19.053856+00	BAM	3798e5f7-776b-462a-b182-55f4906272ce	23636b13-c7af-47cb-9046-2b1a155f8517	ef590425-6f90-49bb-8c21-b79e544ded7e	Extracted from receipt: IMG_3613.jpg	37.20	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-09 11:28:00+00	2025-12-06 21:07:53.683291+00	t	2026-01-09 11:28:38.693595+00	BAM	3798e5f7-776b-462a-b182-55f4906272ce	23636b13-c7af-47cb-9046-2b1a155f8517	4d6f9473-c71b-4780-b131-2bed4664a1d0	Extracted from receipt: IMG_3613.jpg	37.20	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
2026-01-09 11:28:00+00	2026-01-09 13:16:47.824819+00	t	2026-01-09 13:32:46.638349+00	EUR	3798e5f7-776b-462a-b182-55f4906272ce	23636b13-c7af-47cb-9046-2b1a155f8517	180d5ef2-6858-48b6-bc47-ae918980f616	Extracted from receipt: IMG_3613.jpg	37.20	\N	2025-12-06 21:07:53.683291+00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
\.


--
-- TOC entry 3413 (class 0 OID 16495)
-- Dependencies: 207
-- Data for Name: login_events; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.login_events (id, principal_type, principal_id, success, reason, ip, user_agent, created_at) FROM stdin;
9c13bbd1-05c7-4e7d-8321-891725cb048e	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 07:13:39.279853+00
9f3ccd73-f062-4867-b8f5-517c97edd55e	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 08:41:00.704507+00
6aa7e410-dd90-47cf-b885-747d53cae031	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 10:24:42.227201+00
ccb19b03-9317-4df7-b2a6-b7dc6228a2e7	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 10:28:43.077228+00
e7303f22-0b9a-457c-8ec5-d3f13194ecbd	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 10:28:57.266324+00
40d19e33-ab71-4b85-a69b-c32650fe782b	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 10:29:12.437804+00
beef6fa0-256e-4cbe-b4ef-27bd0f6da1ee	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 10:29:18.661391+00
bb3d8687-0118-43e4-ac30-5bf256872e5f	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 10:29:35.412144+00
2bd353fb-d3fd-4079-bad8-3e92fe9dc32b	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 11:16:30.121014+00
b3c5bf5d-9642-4c62-8d2f-380e1fe80803	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 11:34:34.31137+00
907019c4-3fde-4fbe-97da-58567ff68236	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 11:38:30.223447+00
69f801ea-5b67-47e9-91ea-d294f299c224	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 11:39:18.088188+00
d508864f-2ac7-419a-8733-8f9162e0816d	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 12:06:13.034241+00
e9fc9139-4955-47b8-a2f0-4ef0e72b1923	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 12:40:16.240189+00
cccac2a3-e148-453b-9a9e-1d6be1138aaf	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	127.0.0.1	Apache-HttpClient/4.5.14 (Java/21.0.8)	2025-12-08 12:48:52.465797+00
2c7f343d-f611-49fd-97c4-0f23c7a5a373	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 13:43:38.425774+00
5062113a-ad9a-4283-bf15-c282daa2e639	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 20:26:59.102094+00
4b50f003-cb2c-4ae1-b618-58932aab9e3a	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 20:46:27.822701+00
2716639c-e7f7-40ec-b525-0af2127360f2	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 21:58:51.286351+00
8c46ee7a-4d4a-462c-adfd-674bbaa2c250	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 22:50:25.96141+00
9e32b591-da2a-4670-a7c1-933048531b25	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 22:50:55.101603+00
8e1a263e-5148-44ec-9a12-4536bab7fecc	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-08 22:52:48.617962+00
8db7ed2b-0ad1-4443-a98e-73e4258f485f	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-09 06:13:43.905317+00
cc5de82b-d0bb-46eb-a8eb-229f06e1bb17	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-09 14:43:56.390528+00
ce3c97ad-c7f4-4849-b7fe-1e0e72bfc3c7	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 08:24:03.910376+00
e691246f-1a19-4642-8ade-4f92faecd1cf	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 08:33:31.453791+00
365d355e-1dd6-47cb-aa8e-a07923202e89	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 08:34:17.510336+00
e9cede1b-81e6-4876-9db6-1b43a872b37a	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 08:34:42.01912+00
c4cfc093-f123-436d-b1d7-7871b11d0674	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 08:35:31.483972+00
9d5905b8-ee93-4178-a8f6-82b736e40311	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 08:36:40.752943+00
a9e32df1-ef63-4e00-ab78-f186516e8d72	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 08:37:00.696505+00
0d221906-ef76-420c-abac-ade102494602	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 09:07:20.31994+00
895b311b-aa35-4fd0-8106-2578c98ce9bf	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 09:22:02.592096+00
50f36a21-edd4-46e7-9bb8-eb37279eb241	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 09:22:31.042735+00
c59859be-cf42-42d8-9b1b-851b0432be06	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 09:22:49.831805+00
d8e32163-30ec-4402-ad00-654ccbf67a8a	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 09:24:52.830702+00
e2a2b20a-a3b5-4ecd-87f9-cd8d474292c8	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-10 09:42:33.290112+00
bf3f5aaf-4ce2-4a3f-8bb8-b647d232d5e3	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 09:46:41.144339+00
d05a40c9-0277-4b88-8b61-9673c8332a7e	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 09:47:02.154663+00
2c276c1a-29ab-48c8-9dcd-f4c873bde42e	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	curl/8.7.1	2025-12-10 10:05:49.756804+00
67e7d0c3-ab9b-48ae-b624-91348ff0c364	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	curl/8.7.1	2025-12-10 10:07:39.096157+00
9703ac1d-b21f-4335-b7f7-f79272e891f4	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 10:10:06.712592+00
84e50fa9-e71a-4c88-821c-b3d455e308d7	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 10:10:41.391811+00
092a04c8-2155-4f26-8338-ca10a57eace1	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 14:23:36.650465+00
320d8f2b-5f7f-4fd5-9639-8948cdb7cff2	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 14:24:03.476818+00
3a737aa3-5b84-4d69-a2e2-0ce49d5d7b26	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 14:30:51.478421+00
e4ab73ff-0bb4-40c5-b522-dbf565774201	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 14:43:53.739741+00
7ec638fb-a764-4174-9db4-3c438559ad96	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 15:40:45.126676+00
bb51029a-47f3-4543-ada7-02c13956fefe	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 17:07:33.630481+00
bb93ad0a-add4-4dec-86c1-0770f21da4d1	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 22:22:58.897587+00
4345612e-6d24-4096-916e-888a7fdf415a	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-10 22:46:26.179205+00
96829d55-48ae-4831-aedf-32c3e9b1a5e1	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 08:19:11.319601+00
92be923e-54f9-4fce-8412-113b42938d33	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 08:20:16.273686+00
786c56f3-65a6-4fd9-9e29-8bcdc03d6e57	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 08:20:44.136388+00
994ae79e-88c5-4362-8d91-1b57d6474160	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 15:23:09.833753+00
b8418db2-7bf0-40a6-96d6-82e445082872	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 18:45:38.829228+00
8356db19-2b46-4d97-9468-81dbab2fc41c	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 21:57:42.382545+00
aa9db1d9-201e-4069-a736-e9fa9aadac90	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 22:12:22.071945+00
80c1224e-6b2e-4b03-ac39-7d006a73767d	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 22:17:44.873714+00
3cd1ce1f-26d2-48cb-9c68-8e647db8c3b0	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 22:22:49.01913+00
fa9f73c1-af87-443f-9d81-ef33b66db425	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 22:24:13.284633+00
3f2a9e95-56bd-4635-886c-b2ebf93a0672	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-11 22:43:19.730399+00
d8eb2f02-8fd8-4e1a-9221-7805d5916ba1	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-12 10:45:48.845126+00
5a241882-b8d7-4a49-81b6-bc7fe374e047	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-12 11:15:13.032658+00
05373185-9a35-4483-b418-c3ec120d51a2	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-12 12:16:33.556391+00
a07c8489-dd0f-4b9a-bb32-0ff526c15ba5	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-12 20:03:02.20969+00
4674736c-8e6c-41ea-a37b-d91dcc27fe84	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-12 20:03:37.917059+00
089695fd-00b6-4b15-abae-521723d8e46c	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-12 20:25:48.410582+00
2fbb2887-bb9a-4782-a73c-3f6d8616c9ea	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-12 22:10:44.908818+00
28ddb13a-50cd-4f8c-a574-d4cab2738934	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 08:40:20.115002+00
4784ffcd-cd20-45b7-b6a5-88ed5df1c51a	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 08:40:55.820199+00
a1300738-2de8-402c-9a23-c11810f6de65	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 10:06:50.422273+00
2fe50aaa-49b7-495f-bf15-be5f2899f8ea	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 10:38:46.073682+00
c59e4c63-75fb-46b1-8d67-9c83ff8bee2a	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 10:56:59.816199+00
42fea64b-bca0-4a21-ae93-c66d56516372	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 11:16:33.544384+00
e556dacf-cd4d-4ffb-8a3a-2b34e88f60af	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 12:57:02.437217+00
5284731f-94e1-48e6-84b7-15f6c137e388	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 15:18:31.052123+00
e5d67610-344a-4cfc-98eb-1ba3d3600fbd	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 15:22:51.768551+00
4b632166-1a21-49b5-9856-1d6a8634ba4a	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 16:58:44.601935+00
ff892bc3-4356-4a21-8c69-3bf3e107011f	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 18:04:50.921112+00
b50db79f-1253-45e7-b9c8-452a20a2176f	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-13 19:14:42.294958+00
f88fcadf-f2ac-4abe-9d2b-63a71ac5fa8a	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-14 04:51:50.046764+00
9a94e4f2-d022-4257-9915-e3ecfef391cd	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-14 05:38:32.831551+00
673722e3-4f86-4af6-b4d8-e925d9d5fb13	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-14 05:41:16.404688+00
ddf8a3da-7d50-4d9b-98b4-e3f15ddd737e	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-14 05:44:16.973561+00
49c74f03-aa36-46c7-995c-22f3958a5745	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-14 19:46:42.04452+00
f44a1d30-887e-42de-90e3-d122bef38048	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-15 09:09:16.518737+00
edab81ac-e04a-492b-b875-2fbb58fdbdc2	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-15 09:09:40.122647+00
e8dd3c02-1aba-4891-b712-5a2e5be1096f	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-15 09:11:07.822716+00
3ec9560c-e7cf-4714-b7f4-9dca3bd0e023	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-15 09:11:16.580415+00
a0709b24-0fa9-49cb-8728-889a1df2723a	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-15 20:25:01.132882+00
b2fb1be9-de49-4a71-806d-d00eee8fc788	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-16 10:06:52.390761+00
21c6588e-1b65-4e56-bd87-2eeb7e2e46b7	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-17 08:18:12.473126+00
0367e301-4f59-4333-8c17-1dac1784fb85	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-17 09:20:10.495713+00
0d6e6ca8-67db-4b1c-986f-60341d013a8e	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-17 09:21:22.016315+00
aa8f3fed-1c1a-4be3-a1d1-47ccc701145e	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-17 09:30:53.621902+00
636804c1-e48a-467f-bb27-97b7271dafd6	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-17 09:33:12.857017+00
5b82f96f-6dfb-42e7-996d-496e58efd759	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-17 10:21:40.643035+00
92beda03-91d3-4e8b-a1f6-e02da389e33a	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-17 10:39:05.689208+00
787bc2ad-288c-4dbc-926f-444437a4fdfe	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-17 11:22:23.742927+00
b33e63f4-9f28-4016-b9d0-d2bd7e49f6a3	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-17 12:12:58.435926+00
55290171-9953-428b-80cb-e9b25dccc0a4	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-17 16:43:12.912792+00
9dcc6c36-b2c1-48af-8090-01e086e03933	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-17 16:43:40.347443+00
eeadb5f9-15bf-49ac-8522-0011cdfa1c84	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-17 16:57:01.417816+00
be7c766d-0577-4de5-9c3f-2aacf60f238c	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-17 16:59:07.970253+00
93c8f10e-3c13-4428-8d93-8862d8d3b9b0	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-17 17:29:52.988527+00
1edafc7d-43a2-4acb-a8ba-b8fb86d809ca	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 06:56:07.942647+00
4f4dcb68-cd17-49cf-b9a4-34f372c7e76b	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 06:56:48.52188+00
2ba91200-6edb-4d9f-a0ca-e871cb7ddcbf	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:35:15.067917+00
42dda1c8-1352-477d-9b04-22ff83efe11b	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:35:23.883646+00
82972b71-1dca-4b80-a0a2-aa47c162fcf4	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:37:52.861588+00
d38c3d1b-df2b-4c20-b0f6-d2ad7d9a9311	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:38:24.794815+00
f74d32ac-9ad1-4bc2-a30e-3504a18204a0	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:38:52.905845+00
a0922f8e-7c93-456c-ab50-ad41cc794ea1	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:39:04.827017+00
2551fc44-6a40-4633-8b0a-2355ce7a8bd3	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:39:42.048338+00
ac89396d-4eb1-44ab-8820-5480985328b1	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:44:09.79338+00
1ec5958b-cada-4fd7-b6cc-e87c750994ae	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-18 07:55:41.901839+00
f2b36e55-1f4b-4c81-9c0e-01e4bfde2567	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-18 07:56:16.290998+00
ad2eb6ff-c2f4-4616-a723-9e5b3d51d9a3	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 07:58:23.685397+00
132df154-40fc-47d2-9a92-ceba0267b74d	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-18 07:59:38.130979+00
e080e63d-cc27-47fa-b1f0-b55e737d9017	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-18 08:00:55.085769+00
c9c97515-b1f1-486b-a2e0-159c8b66628f	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 08:06:56.552268+00
e2d80702-35c9-44e7-b6a5-073a2946a704	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 08:07:11.193268+00
b471ea33-4f05-497c-b89a-5b0621beba58	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 08:07:38.549054+00
fcfd66f0-0dcb-4b9a-8e3a-c282c1199159	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 08:49:36.201851+00
1bcc9214-f8aa-41fb-958e-e946ba424c52	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-18 16:10:01.114365+00
a9b6207a-4ba7-40d1-b654-833128fb9013	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-19 07:28:56.130781+00
a318c07f-c463-43e0-b821-85332b0b10e6	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-19 07:29:23.056385+00
95801368-7978-44c9-b29a-c4d9e5f342e0	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-23 11:28:40.322209+00
3dcc75f7-b15c-44cd-828b-f0004480cd72	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-23 11:29:15.213112+00
44128f97-bcbc-42c4-97a5-3a5d5e822749	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-23 11:29:29.338795+00
5776a2b9-49b6-4113-abd8-07f7cebdf4f5	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-23 11:30:12.806487+00
384fee4f-fc08-4f33-b4bb-d601da009ab1	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-23 11:41:40.321867+00
417b98a5-972c-47f0-8425-8a3709f31ae3	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-23 15:11:58.965282+00
cd448d48-327f-4e60-be01-9944c2264864	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-24 07:57:34.276213+00
209d64e0-97e8-4575-b250-6a55fea5c679	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-24 08:33:54.818666+00
25206e6f-1c67-437b-b0db-aadb37545d01	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-24 13:02:21.110376+00
7e1c83ad-4f27-416a-82ce-16b7cf1e4ca9	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-24 13:59:15.12081+00
c47d6381-4a2a-477c-8c22-11ca7b5b3281	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 07:19:35.450591+00
aa5f1c33-b85a-44dd-874b-a74f61d68310	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 08:31:17.046763+00
92ba4f49-9c57-4887-9c53-20dd621c52a2	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 08:54:29.46124+00
35f509f9-6068-426f-b1e4-f069bb0bc856	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 09:56:31.131878+00
b89e22b8-2c74-4d40-9439-f754c5bb57b0	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 11:49:24.340461+00
e6627260-02b2-428d-9e3e-8fb8131af75b	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 13:16:30.887779+00
6cd35760-e59a-4c53-b53a-a19f6b075194	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-25 13:48:48.998257+00
5fbd5296-ba9a-4a07-a51f-4df34a1ebfe8	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2025-12-25 14:54:35.769624+00
d5724ff6-dc5c-4b52-bc0c-42d3dfac02b4	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 15:21:27.352038+00
64b67a58-fd3d-4ed4-8e8d-4d57c68abe3e	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 15:39:17.310671+00
4871f91e-e465-401b-9cb4-928888ed5281	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 15:39:42.569542+00
6acfc652-472c-4c70-ab60-bccc033ec460	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 15:40:00.508496+00
a0e72cdc-cc09-4f08-9149-edf6ab11fd5c	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-25 16:44:52.704082+00
5a41892c-ce2b-49de-bba9-dc874b56a4ca	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 07:46:23.890533+00
11694cf6-8a38-4dc7-9ad8-84812d08d3f3	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 07:55:38.37278+00
21807aec-b9c0-4602-918f-b8d37e917b74	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 07:56:40.200197+00
887451c6-7339-47ef-b31c-4a58bd9713e4	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 09:03:39.616318+00
215f0bee-0367-4d84-b782-dcc96ba59cb1	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 09:33:56.407804+00
11736b1b-068d-44c6-a7e6-c00fecaea555	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 10:07:04.312301+00
4c6193ea-4465-4e22-af00-fb99bf8035ba	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 10:13:01.157037+00
aed6c257-0e45-4971-a546-820ee0f90294	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 11:22:48.540529+00
0d23a40b-1763-4df5-a07a-798eb7e376c9	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 12:24:52.699403+00
dc9103e9-f724-4c28-ad9f-b22855e23238	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 13:43:00.018682+00
682cf2b1-7986-41bb-87a3-a8c90c602deb	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 15:58:39.606716+00
5bc4da99-7e51-41d7-b2f5-8813546ba18b	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 16:14:54.791929+00
f93fcc4a-8108-4024-86bb-485ec81950f9	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 16:19:31.487441+00
15749b32-9c74-4368-9be3-fdf78b7d93ea	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 17:27:46.729836+00
5f9d7433-d75b-4e23-a86a-3b69418cf688	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 18:02:52.851129+00
34351ea9-b774-49eb-bc8d-426ffd6dca55	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-26 18:04:05.200396+00
78022e97-18f6-497e-9303-bfa053fb0bbd	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-29 09:36:19.75826+00
6ed3352f-fc64-4908-831f-698c9347351c	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-30 08:04:10.057514+00
32074b62-595f-4be4-b887-06cf35d8a805	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-30 09:13:55.939896+00
b8a7ef13-53b9-4a3b-9a26-a629c1ee454b	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-30 10:14:12.646906+00
dfd3b3dd-bbe9-477e-8310-a19f6b63dacc	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-30 11:18:52.010291+00
46953b06-6eb0-4e78-b2ff-ed7f094513d1	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2025-12-30 12:19:12.637611+00
3d3b9ce1-7674-4c27-b5e2-cbb50bca0b09	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 06:52:35.083184+00
4a97dfe8-b332-4003-a169-26cfbe29e78b	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 07:58:54.772391+00
4d5719b7-57c5-4253-a219-20dfba0395ec	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	f	invalid_credentials	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 08:11:49.715594+00
90b223c3-7294-41e4-a7a9-a7527772ae71	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 08:12:34.907886+00
5b182557-5552-4869-ae03-159499dd5b09	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 08:17:23.639592+00
8a8bed9a-f893-43a3-bd92-74884be0a0b3	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 09:17:53.301255+00
dfc4078b-9a44-4fc8-8420-6229c5c0e3bc	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-06 09:45:46.857998+00
d48da884-d480-433b-a04a-bef66efd68e2	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 10:03:02.614693+00
a8b6e56d-d5b0-4a17-acde-dde68c716ca9	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 10:05:52.558165+00
f284a345-3546-4e53-802b-30a8c9d85e06	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 10:10:15.040108+00
d9e7b0f9-d736-45c8-8bb1-bd48b516da51	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 11:10:34.362136+00
41b71ea7-b2f2-4b59-b8bf-8023cca09c0b	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 12:46:37.369015+00
40ff8eab-aedd-4bf0-8fde-70d6fe22e816	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-06 13:28:04.977835+00
db69edbf-0cd0-4680-9812-b66e94503a30	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 13:48:30.354572+00
f4637e95-38a7-4640-b301-73e3c6be450b	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-06 14:30:37.539082+00
0c3c4894-5a57-4111-8b31-fbb32da52a33	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 15:13:10.317009+00
93dfe0cb-4be7-4144-a8b1-d8e75e808c31	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-06 15:33:11.409964+00
303458dd-06a7-4b3a-b930-88ec48724408	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-06 19:37:38.726676+00
cc880123-0c9a-4205-bd35-49eff4c5188a	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 09:36:54.256686+00
13d98373-9e1b-4eec-9826-4840a647b9d3	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 10:45:37.165393+00
e114e4b7-73ed-4b3b-839d-7ceef356e98e	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 10:47:17.949557+00
5a3ca3c0-1240-4f01-ae85-32632510f99e	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 10:48:23.191922+00
09eeb8bf-3be1-4dfc-88bb-dce525734097	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-07 11:27:12.301848+00
d89ed87d-218a-4c9f-ab0f-e1464c46eb95	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 12:08:16.174305+00
f06b6b91-3d5b-413a-b233-308829f63a7c	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 15:19:48.232394+00
ff091f31-047b-4b0d-a503-67f9f27d435a	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 16:56:16.824005+00
8da8a5aa-4f6c-41f5-a77d-708fbe67aeae	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-07 20:52:10.960246+00
7151b590-b6a2-4661-94fd-ffbfd33a5c54	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 08:56:55.313341+00
3205aa59-b948-4da8-ad3a-6c0a5b1d920a	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 09:01:34.397944+00
e323a131-16f9-4e22-bb9b-bcd1c9c991c1	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-08 09:06:31.404198+00
611f6073-7e29-45b5-8e57-0031fa1397da	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 10:06:41.952563+00
cc77c74c-01c3-45a3-a06c-89098f7010cb	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-08 10:45:55.536667+00
d236f832-e6cd-4d83-8815-afb3286d61ac	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 11:28:40.805082+00
9fe3a733-7ca3-44ef-98bb-8812ee5b34b6	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-08 15:45:14.741176+00
935d3077-7f9c-456c-b7f7-904a1d2024f6	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 16:43:24.964955+00
2477c7e9-8522-43e9-b634-78adaa6d6c30	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 17:01:29.423971+00
9a79fd42-1a04-4405-a457-1126d0d1c3ee	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 17:43:16.425818+00
c6d7f0dd-36e5-4722-8656-3278b1b1efc0	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-08 17:54:30.116376+00
84115faa-7fb6-495e-91c2-2a2640d0dfdf	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 18:45:53.521954+00
c33cdbb9-332d-4361-8aa8-a8937d5a541f	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 21:40:04.661135+00
37154af3-fb3e-4f20-90b9-1fd98edf2a66	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 22:52:41.97044+00
5bf16579-33b6-42c6-bfba-80d0cb83763e	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 22:53:40.481609+00
0acdb752-60e2-48e8-a996-a33963a577b6	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-08 22:54:09.104316+00
bb2d99aa-6671-4bca-ad66-afae30cf9fc6	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-09 09:56:12.55998+00
569c39f0-c226-4746-be40-97454bf18a74	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-09 09:58:18.361744+00
60b5b8bf-5abb-4c91-b18c-c9267b6ce624	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-09 09:58:47.989094+00
ea4aac94-06e7-42db-aabc-6a6fff4b7f71	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-09 09:59:03.177291+00
d172170c-f0c3-493f-a49c-d8a024c75f46	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-09 09:59:43.701418+00
68929900-e7b6-449f-a957-d01aac1b1f1a	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-09 10:29:56.231988+00
2a94f23c-48db-4880-b793-7fe6053e96c7	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-09 10:50:51.294953+00
5da56841-6c23-43b7-93d9-a073dd16a335	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-09 11:31:15.417218+00
0df7a9ac-6182-4f44-9422-fab779b40b71	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-09 11:51:36.306382+00
75017a01-d105-4511-b0b9-e9fc7bbef635	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-09 12:53:44.632084+00
f5668d4a-b688-4551-910f-3b672fb4b2d9	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-09 13:16:26.837833+00
465b8eca-7d33-4858-8146-4147a3413864	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-09 13:55:25.926095+00
f4303069-4f04-44bd-bff2-1a744897c1ee	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-09 14:24:46.443908+00
cccf96ee-736b-402d-9b62-5fafccbc0d1c	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-09 15:11:39.353073+00
5f71e191-9651-45cc-ad4a-e90a1440d408	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-10 09:49:52.324896+00
7025e7a0-eeae-4aa8-a4c8-87125eb99789	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-10 09:50:11.74739+00
c4a448e9-b52f-4ac2-b9c7-2557c8c7f37b	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-10 09:50:35.083813+00
8bfe085d-42da-4620-af7d-ae165e6134fc	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-10 21:50:08.70701+00
5863bf52-a860-4125-9dd6-d2e1d375f67f	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-10 21:50:42.80814+00
b4bd2f1b-03d3-44c3-a551-884399e18bd9	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-11 18:48:24.11434+00
2cf97901-c1d4-48b7-b333-fe0c2fac65fc	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-11 18:49:54.366747+00
95d9b29d-d62c-4d3a-bb39-1f29c8166c6f	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-11 19:13:33.847976+00
1716d563-bf0e-4e8b-9198-b35a48412a33	admin	69eccad4-451d-4c62-921a-b9e54d61e3d0	t	\N	127.0.0.1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:146.0) Gecko/20100101 Firefox/146.0	2026-01-11 19:15:34.085421+00
4fbd77f3-8c24-4624-abeb-3a74fd00ab72	user	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	2026-01-11 19:18:36.886342+00
\.


--
-- TOC entry 3414 (class 0 OID 16646)
-- Dependencies: 208
-- Data for Name: password_reset_tokens; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.password_reset_tokens (id, principal_type, principal_id, token, expires_at, used_at, created_at) FROM stdin;
\.


--
-- TOC entry 3417 (class 0 OID 16717)
-- Dependencies: 211
-- Data for Name: payers; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.payers (id, type, label, last4, is_default, created_at, updated_at) FROM stdin;
9ce86e1a-d4de-4368-8008-decdfc9bca20	person	Enes	\N	f	2025-12-06 21:07:53.683291+00	2025-12-26 17:16:12.777091+00
23636b13-c7af-47cb-9046-2b1a155f8517	card	Test Payer	\N	t	2025-12-06 21:07:53.683291+00	2025-12-26 17:16:18.016476+00
\.


--
-- TOC entry 3421 (class 0 OID 16781)
-- Dependencies: 215
-- Data for Name: price_observations; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.price_observations (observed_at, expense_item_id, line_total, article_id, unit_price, currency, supplier_id, id, qty, created_at) FROM stdin;
2023-12-25 05:21:00+00	86328b54-cb19-42a8-a723-bf9883d026f5	0.65	8876cc99-4acf-4810-918a-b00e284433a8	0.65	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	45b26534-69c4-4f1b-b405-530891b50ab6	1.000	2025-12-06 21:07:53.683291+00
2026-01-08 15:52:00+00	586c3795-76d2-415a-b07a-12148785286e	2.10	028b5b67-587c-442c-8328-dcf00e273f19	2.10	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	38e91a1a-442e-4212-8b2c-19c64858e8bd	1.000	2025-12-06 21:07:53.683291+00
2026-01-08 15:47:00+00	f608229a-6f55-4264-9383-daaeac551ece	1.95	ea97eef6-47b2-425e-af6f-2db6504816ff	1.95	BAM	cf83233f-316c-40f8-a022-b8b3c3c5df45	2fd61cd5-c57a-435c-a9c7-6985c98352a9	1.000	2025-12-06 21:07:53.683291+00
2026-01-08 15:52:00+00	f6a7e913-3660-44a0-9da3-c8bace89e3f8	10.00	f48819b3-6744-44d4-a5a1-17858f16ba5c	10.00	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	652dd394-789f-4bd0-9105-34c6f2aa4c8a	1.000	2025-12-06 21:07:53.683291+00
2026-01-08 15:52:00+00	bccbcc41-068a-48c3-920a-e727b0e7bfa3	10.00	f48819b3-6744-44d4-a5a1-17858f16ba5c	10.00	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	459f4c20-d7f6-4e34-aff3-3fdaff5c9dd8	1.000	2025-12-06 21:07:53.683291+00
2026-01-08 15:52:00+00	aad4cea8-b650-4cd5-b1c4-058b3c767876	1.48	c93b9f4d-6780-4460-8330-7512d830d4d3	1.48	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	b14f7fef-5e60-4261-9e85-d0d5c04f2187	1.000	2025-12-06 21:07:53.683291+00
2023-12-25 05:21:00+00	1da10649-3627-4f7d-b9dc-b9bb09603cba	0.65	8876cc99-4acf-4810-918a-b00e284433a8	0.65	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	d6739c15-0588-4538-b59e-4e736f480daa	1.000	2025-12-06 21:07:53.683291+00
2023-12-25 05:21:00+00	74e2c919-98e3-4274-bb5c-70500c022b30	0.65	8876cc99-4acf-4810-918a-b00e284433a8	0.65	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	087b3b48-0e9f-4882-b796-f7217d75569f	1.000	2025-12-06 21:07:53.683291+00
2026-01-09 11:28:00+00	eac105c9-198b-4572-b46c-8470e005c514	1.95	ea97eef6-47b2-425e-af6f-2db6504816ff	1.95	BAM	cf83233f-316c-40f8-a022-b8b3c3c5df45	729c0396-601b-42ea-afa6-138dbe3183cf	1.000	2025-12-06 21:07:53.683291+00
2026-01-09 11:28:00+00	cf3d2ef7-4d3f-4622-83fe-963703263e72	1.95	ea97eef6-47b2-425e-af6f-2db6504816ff	1.95	BAM	cf83233f-316c-40f8-a022-b8b3c3c5df45	32bfd2d8-6d98-4b19-8111-6eb0c8aef16c	1.000	2025-12-06 21:07:53.683291+00
2026-01-09 14:02:00+00	f9307a6b-8fd8-4bdc-bb97-a638b166a092	2.10	028b5b67-587c-442c-8328-dcf00e273f19	2.10	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	90903f93-fe15-4d1f-b569-ade00ebd53c8	1.000	2025-12-06 21:07:53.683291+00
2026-01-09 14:02:00+00	8bee2c73-7ed9-4fc3-9998-2ea0c9d2f110	2.10	028b5b67-587c-442c-8328-dcf00e273f19	2.10	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	210b4676-8458-4af8-89e4-9e7b0711d4c5	1.000	2025-12-06 21:07:53.683291+00
2026-01-09 14:04:00+00	b22b6173-1460-45d1-a567-1ea682d55dd4	1.48	c93b9f4d-6780-4460-8330-7512d830d4d3	1.48	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	1df93131-1ab2-4d7e-b9ba-8c61a09e9717	1.000	2025-12-06 21:07:53.683291+00
2026-01-09 14:04:00+00	f5d3776d-c23b-4653-8c96-2d8e02932a13	10.00	f48819b3-6744-44d4-a5a1-17858f16ba5c	10.00	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	bfa9d988-9c01-4f81-b49b-18d4a5bc9719	1.000	2025-12-06 21:07:53.683291+00
2026-01-09 14:04:00+00	22fec3c6-83f0-4394-bc04-f73834439d3f	10.00	f48819b3-6744-44d4-a5a1-17858f16ba5c	10.00	BAM	92a781b7-fb7d-4bde-b868-7868d4f918b0	00df944c-e696-4ecc-8255-b750a7495340	1.000	2025-12-06 21:07:53.683291+00
2023-12-25 05:21:00+00	3448d9b4-993d-4e06-a117-9cd49ef48e6e	0.65	8876cc99-4acf-4810-918a-b00e284433a8	0.65	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	f048a401-9d0b-49c7-b2c0-c05b8f7518e5	1.000	2025-12-06 21:07:53.683291+00
2026-01-08 15:52:00+00	cd95b5ba-e66f-4648-915d-553aea8f032b	2.10	028b5b67-587c-442c-8328-dcf00e273f19	2.10	BAM	6057fb90-df80-416c-b1a0-5a9a9dfcc264	7a16a27c-3bfe-4a21-b155-7da31ed4adf4	1.000	2025-12-06 21:07:53.683291+00
2026-01-08 15:47:00+00	9ff3baa4-51ee-48a7-8969-f17c68958ad1	1.95	ea97eef6-47b2-425e-af6f-2db6504816ff	1.95	BAM	cf83233f-316c-40f8-a022-b8b3c3c5df45	ee0af836-7e37-44d7-8fbc-da7cbff3fd92	1.000	2025-12-06 21:07:53.683291+00
\.


--
-- TOC entry 3415 (class 0 OID 16685)
-- Dependencies: 209
-- Data for Name: receipts; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.receipts (original_filename, raw_extract_json, updated_at, total_amount_guess, purchased_at_guess, file_size, currency_guess, expense_id, status, file_hash, id, content_type, error_details, storage_key, error_message, raw_parse_json, supplier_guess, retry_count, created_at, parsed_markdown, user_id) FROM stdin;
IMG_3619.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "footer": null, "header": null, "images": [], "tables": [], "markdown": "\\"KONZUM\\" d.o.o. Sarajevo\\nPodružnica br. 66\\nProdavnica br. 90 Sarajevo\\nBraće Beqić 3\\n71101 SARAJEVO CENTAR\\n\\nJIB: 4200918605499\\nPIB: 200918600004\\n\\nIBFM: BT005790\\n\\nFISKALNI RACUN\\nBF: 390178\\n25.12.2025. 15:43\\n\\n|  CIG DUNHIL DIST BL | 2,000x | 6,70 | 13,40E  |\\n| --- | --- | --- | --- |\\n|  VE: 17,00% |  |  |   |\\n|  OSN. E: |  | 11,45 |   |\\n|  PDV E: |  | 1,95 |   |\\n|  PDV: |  | 1,95 |   |\\n\\nTOTAL: 13,40\\nUPLACENO:\\nKARTICA: 13,40\\nUkupno: 13,40\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}, "hyperlinks": []}], "usage_info": {"doc_size_bytes": 2613890, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 2.0, "raw_label": "CIG DUNHIL DIST BL", "line_total": 13.4, "unit_price": 6.7}], "totals": {"total": 13.4}, "merchant": {"name": "KONZUM", "address": "Prodavnica br. 90 Sarajevo, Braće Beqić 3, 71101 SARAJEVO CENTAR", "store_name": "Podružnica br. 66"}}, "received_at": "2026-01-09T15:18:47.593013Z", "valid_shape?": true}	2026-01-09 15:18:47.61289+00	13.40	\N	2613890	BAM	\N	extracted	0cfb9594310bdc09a2015d456c80000730466f7a8cba39ce3faa74b3815ec6da	610b6042-dc80-4660-9408-5e0fa40bd4ba	image/jpeg	\N	0ea3f83f-01be-42ef-90bb-d9d255819f08.jpg	\N	\N	KONZUM	4	2026-01-09 15:11:53.78628+00	"KONZUM" d.o.o. Sarajevo\nPodružnica br. 66\nProdavnica br. 90 Sarajevo\nBraće Beqić 3\n71101 SARAJEVO CENTAR\n\nJIB: 4200918605499\nPIB: 200918600004\n\nIBFM: BT005790\n\nFISKALNI RACUN\nBF: 390178\n25.12.2025. 15:43\n\n|  CIG DUNHIL DIST BL | 2,000x | 6,70 | 13,40E  |\n| --- | --- | --- | --- |\n|  VE: 17,00% |  |  |   |\n|  OSN. E: |  | 11,45 |   |\n|  PDV E: |  | 1,95 |   |\n|  PDV: |  | 1,95 |   |\n\nTOTAL: 13,40\nUPLACENO:\nKARTICA: 13,40\nUkupno: 13,40\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3614.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "\\"CM-COSMETIC MARKET\\" d. Vitez,\\nPoslávna Jedinica CM-71, Sarajevo\\nN.Sarajevo, Z.od Bosne 2, Import.C\\n71000, Sarajevo\\n\\nJIB: 4236280581192\\nPIB: 236280580005\\n\\nIBFM: AE011151\\n\\nFIS NENI RACUN\\nBF: 301.37\\n17.12.2025 13:28\\n\\nA10150772 Snala za kosu BH231226\\n1,95E\\nA10150772 Snala za kosu BH231226\\n1,95E\\n\\nVE: 17,00%\\nOSN. E: 3,33\\nPDV E: 0,57\\nPDV: 0,57\\n\\nTOTAL: 3,90\\nUPLACENO:\\nGotovina: 3,90\\nUkupno: 3,90\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3122848, "pages_processed": 1}, "document_annotation": "{\\n  \\"merchant\\": {\\n    \\"name\\": \\"CM-Cosmetic Market\\",\\n    \\"address\\": \\"Z. od Bosne i Hercegovine 71000, Sarajevo\\",\\n    \\"tax_id\\": \\"2362605800005\\"\\n  },\\n  \\"purchased_at\\": \\"2023-12-17T00:00:00\\",\\n  \\"currency\\": \\"BAM\\",\\n  \\"totals\\": {\\n    \\"subtotal\\": 3.39,\\n    \\"tax\\": 0.57,\\n    \\"total\\": 3.96\\n  }\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "Snala za kosu BH231226", "line_total": 1.95, "unit_price": 1.95}, {"qty": 1.0, "raw_label": "Snala za kosu BH231226", "line_total": 1.95, "unit_price": 1.95}], "totals": {"total": 3.9}, "currency": "BAM", "merchant": {"name": "CM-Cosmetic Market", "tax_id": "2362605800005", "address": "Z. od Bosne i Hercegovine 71000, Sarajevo"}, "purchased_at": "2023-12-17T00:00:00"}, "received_at": "2026-01-09T15:21:22.259725Z", "valid_shape?": true}	2026-01-09 15:21:22.298815+00	3.90	2023-12-16 23:00:00+00	3122848	BAM	\N	extracted	3999228a5236d8f95b0f7242ba88a165972a8945032b5118417c7cf6081c552f	a0e7068a-e763-43e9-a158-418870946b52	image/jpeg	\N	7a0d67bc-bd89-48f0-9faa-5e459afc9550.jpg	\N	\N	CM-Cosmetic Market	1	2026-01-09 15:20:58.054059+00	"CM-COSMETIC MARKET" d. Vitez,\nPoslávna Jedinica CM-71, Sarajevo\nN.Sarajevo, Z.od Bosne 2, Import.C\n71000, Sarajevo\n\nJIB: 4236280581192\nPIB: 236280580005\n\nIBFM: AE011151\n\nFIS NENI RACUN\nBF: 301.37\n17.12.2025 13:28\n\nA10150772 Snala za kosu BH231226\n1,95E\nA10150772 Snala za kosu BH231226\n1,95E\n\nVE: 17,00%\nOSN. E: 3,33\nPDV E: 0,57\nPDV: 0,57\n\nTOTAL: 3,90\nUPLACENO:\nGotovina: 3,90\nUkupno: 3,90\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3618.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "BINGO doo export import Tuzla\\nPJ broj 213 HIPELMAKKI MERKUR Sarajevo\\nGratičarka 1\\n71,00 00,00 390\\n\\nJIB: 4209253454262\\nPIB: 209253450003\\nTBFM: BP003650\\n\\nFISKALNI RACUN\\nBF: 444603\\n25.12.2025. 06:21\\n\\n020327 HLJEB 400G SA SJEMELKA MA\\n1.000x 2,10 2.10E\\nB31508 PASTETA 114G KOKOSTJA ARGETA\\n1.000x 1,85 1.85E\\nE15348 KEKS MLJEVENI ZKØGOK LIMENKA\\n1.000x 9,80 9.80E\\nK02698 ZACIN 20G KORTGA NATANOZE KOT\\n1.000x 1,60 1.60E\\nD16394 SECER 10G SA AROMON CIMETA DR\\n1.000x 0,65 0.65E\\nD16392 SECER 10G SA AROMON LIMUNA DR\\n1.000x 0,65 0.65E\\nK00719 ZACIN 27G MJELAUINA ZA MEDENJ\\n1.000x 1,15 1.15E\\nE54096 BOMBONE 200G ZELE JELLY ROSHE\\n1.000x 1,95 1.95E\\nE53986 ZELE 200G LJETNI MIX ROSHEN\\n1.000x 2,10 2.10E\\n\\nUE: 17,00%\\nOSN. E: 18,68\\nPDU E: 3,17\\nPDU: 3,17\\n\\nTOTAL: 21,85\\nUPLACENO:\\nGOTOVINA: 21,85\\nUkupno: 21,85\\nPOURAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3232065, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "HLJEB 400G SA SJEMELKA MA", "line_total": 2.1, "unit_price": 2.1}, {"qty": 1.0, "raw_label": "PASTETA 114G KOKOSTJA ARGETA", "line_total": 1.85, "unit_price": 1.85}, {"qty": 1.0, "raw_label": "KEKS MLJEVENI ZKØGOK LIMENKA", "line_total": 9.8, "unit_price": 9.8}, {"qty": 1.0, "raw_label": "ZACIN 20G KORTGA NATANOZE KOT", "line_total": 1.6, "unit_price": 1.6}, {"qty": 1.0, "raw_label": "SECER 10G SA AROMON CIMETA DR", "line_total": 0.65, "unit_price": 0.65}, {"qty": 1.0, "raw_label": "SECER 10G SA AROMON LIMUNA DR", "line_total": 0.65, "unit_price": 0.65}, {"qty": 1.0, "raw_label": "ZACIN 27G MJELAUINA ZA MEDENJ", "line_total": 1.15, "unit_price": 1.15}, {"qty": 1.0, "raw_label": "BOMBONE 200G ZELE JELLY ROSHE", "line_total": 1.95, "unit_price": 1.95}, {"qty": 1.0, "raw_label": "ZELE 200G LJETNI MIX ROSHEN", "line_total": 2.1, "unit_price": 2.1}, {"qty": 1.0, "raw_label": "POURAT:", "line_total": 0.0, "unit_price": 0.0}], "totals": {"total": 21.85}, "merchant": {"name": "BINGO doo export import Tuzla", "address": "Gratičarka 1, 71,00 00,00 390", "store_name": "PJ broj 213 HIPELMAKKI MERKUR Sarajevo"}}, "received_at": "2026-01-09T15:21:22.259875Z", "valid_shape?": true}	2026-01-09 15:21:22.319817+00	21.85	\N	3232065	BAM	\N	extracted	1a70098642975c46e4d10af678dab1a1d88b9930d27e015e8b98b2efde906d9e	6480b507-6680-4c4c-90a5-28f53bfeceb9	image/jpeg	\N	c41def6f-cd1f-45ab-9ee9-915fc2b92058.jpg	\N	\N	BINGO doo export import Tuzla	1	2026-01-09 15:20:58.353678+00	BINGO doo export import Tuzla\nPJ broj 213 HIPELMAKKI MERKUR Sarajevo\nGratičarka 1\n71,00 00,00 390\n\nJIB: 4209253454262\nPIB: 209253450003\nTBFM: BP003650\n\nFISKALNI RACUN\nBF: 444603\n25.12.2025. 06:21\n\n020327 HLJEB 400G SA SJEMELKA MA\n1.000x 2,10 2.10E\nB31508 PASTETA 114G KOKOSTJA ARGETA\n1.000x 1,85 1.85E\nE15348 KEKS MLJEVENI ZKØGOK LIMENKA\n1.000x 9,80 9.80E\nK02698 ZACIN 20G KORTGA NATANOZE KOT\n1.000x 1,60 1.60E\nD16394 SECER 10G SA AROMON CIMETA DR\n1.000x 0,65 0.65E\nD16392 SECER 10G SA AROMON LIMUNA DR\n1.000x 0,65 0.65E\nK00719 ZACIN 27G MJELAUINA ZA MEDENJ\n1.000x 1,15 1.15E\nE54096 BOMBONE 200G ZELE JELLY ROSHE\n1.000x 1,95 1.95E\nE53986 ZELE 200G LJETNI MIX ROSHEN\n1.000x 2,10 2.10E\n\nUE: 17,00%\nOSN. E: 18,68\nPDU E: 3,17\nPDU: 3,17\n\nTOTAL: 21,85\nUPLACENO:\nGOTOVINA: 21,85\nUkupno: 21,85\nPOURAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3811.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "\\"HOSÉ-KOMERC\\" d.o.o. Sarajevo\\nP.J: br. 14 Market Košeusko Brdo\\nUl. Brače Begića broj 1\\n71000 Sarajevo\\n\\nJOB: 4700442500227\\nPIB: 700442500006\\n\\nINFM: 8N007320\\n\\nFISKALNI RAČUN\\nBr: 1233514\\n01.01.2026, 14:48\\n\\nMUGGLE MLIJEKO 3 2%MM 1L 12\\n3.000x 2,10 6,30E\\nPODENI KAJMOK MLADI 300G ZD\\n1.000x 9,80 9,80E\\nBEĆJE ABC SIR 100G CLASSIK\\n1.000x 2,35 2,35E\\nSEĆI CAJ MENTA 30GR 24/1\\n1.000x 1,50 1,50E\\nKOKOŠIJA JUHA S TJES 62G PO\\n3.000x 1,30 3,90E\\nCIGARE DUNHILL DISTINCT BLE\\n1.000x 6,70 6,70E\\nCIGARE DUNHILL DISTINCT BLE\\n1.000x 6,70 6,70E\\n\\nVJ: 17,00%\\nOSN. E 31,84\\nPOV E: 5,41\\nPOV: 5,41\\n\\nTOTAL: 37,25\\nUMLAČENO:\\nKORTICA: 37,25\\nUKUPNO: 37,25\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 2556386, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 3.0, "raw_label": "MUGGLE MLIJEKO 3 2%MM 1L 12", "line_total": 6.3, "unit_price": 2.1}, {"qty": 1.0, "raw_label": "PODENI KAJMOK MLADI 300G ZD", "line_total": 9.8, "unit_price": 9.8}, {"qty": 1.0, "raw_label": "BEĆJE ABC SIR 100G CLASSIK", "line_total": 2.35, "unit_price": 2.35}, {"qty": 1.0, "raw_label": "SEĆI CAJ MENTA 30GR 24/1", "line_total": 1.5, "unit_price": 1.5}, {"qty": 3.0, "raw_label": "KOKOŠIJA JUHA S TJES 62G PO", "line_total": 3.9, "unit_price": 1.3}, {"qty": 1.0, "raw_label": "CIGARE DUNHILL DISTINCT BLE", "line_total": 6.7, "unit_price": 6.7}, {"qty": 1.0, "raw_label": "CIGARE DUNHILL DISTINCT BLE", "line_total": 6.7, "unit_price": 6.7}], "totals": {"total": 37.25}, "merchant": {"name": "HOSÉ-KOMERC", "address": "Ul. Brače Begića broj 1, 71000 Sarajevo", "store_name": "P.J: br. 14 Market Košeusko Brdo"}}, "received_at": "2026-01-09T15:21:22.259931Z", "valid_shape?": true}	2026-01-09 15:21:22.324972+00	37.25	\N	2556386	BAM	\N	extracted	936fb24ac70e877e933f6cee5182d926eb148c4dc1a349699c1c1e5a8c2d42f3	fe467493-8759-445c-9cbb-92505a375098	image/jpeg	\N	fc93abeb-514b-4362-a366-5b304d202b3b.jpg	\N	\N	HOSÉ-KOMERC	1	2026-01-09 15:20:58.544742+00	"HOSÉ-KOMERC" d.o.o. Sarajevo\nP.J: br. 14 Market Košeusko Brdo\nUl. Brače Begića broj 1\n71000 Sarajevo\n\nJOB: 4700442500227\nPIB: 700442500006\n\nINFM: 8N007320\n\nFISKALNI RAČUN\nBr: 1233514\n01.01.2026, 14:48\n\nMUGGLE MLIJEKO 3 2%MM 1L 12\n3.000x 2,10 6,30E\nPODENI KAJMOK MLADI 300G ZD\n1.000x 9,80 9,80E\nBEĆJE ABC SIR 100G CLASSIK\n1.000x 2,35 2,35E\nSEĆI CAJ MENTA 30GR 24/1\n1.000x 1,50 1,50E\nKOKOŠIJA JUHA S TJES 62G PO\n3.000x 1,30 3,90E\nCIGARE DUNHILL DISTINCT BLE\n1.000x 6,70 6,70E\nCIGARE DUNHILL DISTINCT BLE\n1.000x 6,70 6,70E\n\nVJ: 17,00%\nOSN. E 31,84\nPOV E: 5,41\nPOV: 5,41\n\nTOTAL: 37,25\nUMLAČENO:\nKORTICA: 37,25\nUKUPNO: 37,25\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3616.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "footer": null, "header": null, "images": [], "tables": [], "markdown": "\\"BINGO\\" d.o.o. EXPORT-IMPORT TUZLA\\nPJ 219 \\"Supermarket Alta\\" Sarajevo\\nBulevar Franca Lehara br. 2. Alta Shopping Centa\\n71000 SARAJEVO\\n\\nJIB: 4209253454360\\nPIB: 209253450003\\n\\nIBFM: BT003671\\n\\nFISKALNI RACUN\\nBF: 292684\\n22.12.2025. 14:00\\n\\nE09438 BOMBONJERA 230G RAFFAELLO FER\\n1,000x 9,90 9,90E\\nSA9192 ZDJELA SA POKLOPCEM 0 65L FR\\n1,000x 1,90 1,90E\\nD20720 KOLAC 230G ECLAIRS MARLENKA\\n1,000x 10,75 10,75E\\nB21558 SUNKA PURECA DELUX VINDON\\n0,098x 29,95 2,94E\\nJ12220 SALATA AMERICKA\\n0,314x 7,95 2,50E\\nF20510 VODA PRIRODNA 2 5L AQUA VIVA\\n1,000x 1,55 1,55E\\nC05171 KAJMAK MLADI PADJENI RINFUZA\\n0,198x 29,40 5,82E\\nG00233 KAFA MLJEVENA 200G ZLATNA DZE\\n1,000x 6,85 6,85E\\nC03331 SIR SITNI 500G PADJENI\\n1,000x 3,50 3,50E\\nC01351 MILERAM 30 MEGGLE RINFUZA\\n0,352x 11,95 4,21E\\n\\nVE: 17,00%\\nOSN. E: 42,67\\nPDU E: 7,25\\nPDU: 7,25\\n\\nTOTAL: 49,92\\nUPLACENO:\\nKARTICA: 49,92\\nUkupno: 49,92\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}, "hyperlinks": []}], "usage_info": {"doc_size_bytes": 3264881, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "BOMBONJERA 230G RAFFAELLO FER", "line_total": 9.9, "unit_price": 9.9}, {"qty": 1.0, "raw_label": "SA9192 ZDJELA SA POKLOPCEM 0 65L FR", "line_total": 1.9, "unit_price": 1.9}, {"qty": 1.0, "raw_label": "KOLAC 230G ECLAIRS MARLENKA", "line_total": 10.75, "unit_price": 10.75}, {"qty": 0.098, "raw_label": "SUNKA PURECA DELUX VINDON", "line_total": 2.94, "unit_price": 29.95}, {"qty": 0.314, "raw_label": "SALATA AMERICKA", "line_total": 2.5, "unit_price": 7.95}, {"qty": 1.0, "raw_label": "VODA PRIRODNA 2 5L AQUA VIVA", "line_total": 1.55, "unit_price": 1.55}, {"qty": 0.198, "raw_label": "KAJMAK MLADI PADJENI RINFUZA", "line_total": 5.82, "unit_price": 29.4}, {"qty": 1.0, "raw_label": "KAFA MLJEVENA 200G ZLATNA DZE", "line_total": 6.85, "unit_price": 6.85}, {"qty": 1.0, "raw_label": "SIR SITNI 500G PADJENI", "line_total": 3.5, "unit_price": 3.5}, {"qty": 0.352, "raw_label": "MILERAM 30 MEGGLE RINFUZA", "line_total": 4.21, "unit_price": 11.95}], "totals": {"total": 49.92}, "merchant": {"name": "BINGO", "address": "Bulevar Franca Lehara br. 2. Alta Shopping Centa, 71000 SARAJEVO", "store_name": "PJ 219 \\"Supermarket Alta\\" Sarajevo"}}, "received_at": "2026-01-09T15:18:06.624716Z", "valid_shape?": true}	2026-01-09 15:18:06.662233+00	49.92	\N	3264881	BAM	\N	extracted	80525576dbe7888c9687a81e88f9dc28590a2254d5245e2c5e1547e7debc0d61	13afc31b-c3ac-42f8-856a-9f80208d6987	image/jpeg	\N	c55adf7f-5d0c-438b-baa7-6bafe726d222.jpg	\N	\N	BINGO	3	2026-01-09 15:15:55.080065+00	"BINGO" d.o.o. EXPORT-IMPORT TUZLA\nPJ 219 "Supermarket Alta" Sarajevo\nBulevar Franca Lehara br. 2. Alta Shopping Centa\n71000 SARAJEVO\n\nJIB: 4209253454360\nPIB: 209253450003\n\nIBFM: BT003671\n\nFISKALNI RACUN\nBF: 292684\n22.12.2025. 14:00\n\nE09438 BOMBONJERA 230G RAFFAELLO FER\n1,000x 9,90 9,90E\nSA9192 ZDJELA SA POKLOPCEM 0 65L FR\n1,000x 1,90 1,90E\nD20720 KOLAC 230G ECLAIRS MARLENKA\n1,000x 10,75 10,75E\nB21558 SUNKA PURECA DELUX VINDON\n0,098x 29,95 2,94E\nJ12220 SALATA AMERICKA\n0,314x 7,95 2,50E\nF20510 VODA PRIRODNA 2 5L AQUA VIVA\n1,000x 1,55 1,55E\nC05171 KAJMAK MLADI PADJENI RINFUZA\n0,198x 29,40 5,82E\nG00233 KAFA MLJEVENA 200G ZLATNA DZE\n1,000x 6,85 6,85E\nC03331 SIR SITNI 500G PADJENI\n1,000x 3,50 3,50E\nC01351 MILERAM 30 MEGGLE RINFUZA\n0,352x 11,95 4,21E\n\nVE: 17,00%\nOSN. E: 42,67\nPDU E: 7,25\nPDU: 7,25\n\nTOTAL: 49,92\nUPLACENO:\nKARTICA: 49,92\nUkupno: 49,92\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3611.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "footer": null, "header": null, "images": [], "tables": [], "markdown": "\\"Pepco B-H\\" d.o.o.\\nPodružnica Sarajevo 2\\nul. Kolodvorska br.12\\n71000 Sarajevo\\n\\nJIB: 4203144510090\\nPIB: 203144510006\\n\\nIBFM: AM025770\\n\\nFISKALNI RACUN\\n\\nBF: 115248\\n25.12.2025. 19:46\\n\\n62778401 Mirisna svijeca u staklu Premium Collec\\nt/pc 10,00E\\n-50,00%: 5,00\\n\\n62778401 Mirisna svijeca u staklu Premium Collec\\nt/pc 10,00E\\n-50,00%: 5,00\\n\\nVE: 17,00%\\nOSN. E: 8,55\\nPDV E: 1,45\\nPDV: 1,45\\n\\nTOTAL: 10,00\\nUPLACENO:\\nKartica: 10,00\\nUkupno: 10,00\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}, "hyperlinks": []}], "usage_info": {"doc_size_bytes": 3083546, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"merchant\\": {\\n    \\"name\\": \\"Patrijarha Sarajlija 2\\",\\n    \\"address\\": \\"Ul. Kolodvorska br.12, 71000 Sarajevo\\",\\n    \\"tax_id\\": \\"2031445100006\\"\\n  },\\n  \\"purchased_at\\": \\"2023-12-25T19:46:00\\",\\n  \\"currency\\": \\"BAM\\",\\n  \\"totals\\": {\\n    \\"subtotal\\": 10.00,\\n    \\"tax\\": 0.00,\\n    \\"total\\": 10.00\\n  }\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "Mirisna svijeca u staklu Premium Collec", "line_total": 5.0, "unit_price": 5.0}, {"qty": 1.0, "raw_label": "Mirisna svijeca u staklu Premium Collec", "line_total": 5.0, "unit_price": 5.0}], "title": "ReceiptMetaExtractionV1", "totals": {"tax": 0.0, "total": 10.0, "subtotal": 10.0}, "currency": "BAM", "merchant": {"name": "Patrijarha Sarajlija 2", "tax_id": "2031445100006", "address": "Ul. Kolodvorska br.12, 71000 Sarajevo"}, "purchased_at": "2023-12-25T19:46:00"}, "received_at": "2026-01-09T15:18:27.776168Z", "valid_shape?": true}	2026-01-09 15:18:27.798044+00	10.00	2023-12-25 18:46:00+00	3083546	BAM	\N	extracted	41397bb801147c7a524c9801079279f1e13618e2d3193644ddd6b700b85c615a	3abdc623-b30e-40ef-af7a-c7b6c30533ee	image/jpeg	\N	84f35cf6-8b58-4a1e-832a-40490a8b0434.jpg	\N	\N	Patrijarha Sarajlija 2	2	2026-01-09 14:01:58.473354+00	"Pepco B-H" d.o.o.\nPodružnica Sarajevo 2\nul. Kolodvorska br.12\n71000 Sarajevo\n\nJIB: 4203144510090\nPIB: 203144510006\n\nIBFM: AM025770\n\nFISKALNI RACUN\n\nBF: 115248\n25.12.2025. 19:46\n\n62778401 Mirisna svijeca u staklu Premium Collec\nt/pc 10,00E\n-50,00%: 5,00\n\n62778401 Mirisna svijeca u staklu Premium Collec\nt/pc 10,00E\n-50,00%: 5,00\n\nVE: 17,00%\nOSN. E: 8,55\nPDV E: 1,45\nPDV: 1,45\n\nTOTAL: 10,00\nUPLACENO:\nKartica: 10,00\nUkupno: 10,00\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3813.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "footer": null, "header": null, "images": [], "tables": [], "markdown": "\\"PETROL BH OIL COMPANY\\" D.O.O\\nSARAJEVO - PODRUŽNICA KONJIC\\nBS KONJIC ŽELJUŠA\\nPotoci\\nŽeljuša bb\\n88000 MOSTAR\\n\\nJIB: 4200505350204\\nPIB: 200505350000\\nIBFM: BJ008820\\n\\nFISKALNI RAČUN\\nBF: 553492\\n04.01.2026. 13:49\\n\\nPREMIUM 95 BAS EN 228/L\\n42,200x 2,29 96,64E\\nTAKSA NAF.DER.ČL.25S.GPDV/L\\n42,200x 0,01 0,42K\\nCIGARETE DUNHILL DISTINCT\\n2,000x 6,70 13,40E\\n\\nVE: 17,00%\\nVK: 0,00%\\nOSN. E: 94,05\\nOSN. K: 0,42\\nPDV E: 15,99\\nPDV K: 0,00\\nPDV: 15,99\\n\\nTOTAL: 110,46\\nUPLAČENO:\\nKARTICA: 110,46\\nUKUPNO: 110,46\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}, "hyperlinks": []}], "usage_info": {"doc_size_bytes": 2457129, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 42.2, "raw_label": "PREMIUM 95 BAS EN 228/L", "line_total": 96.64, "unit_price": 2.29}, {"qty": 42.2, "raw_label": "TAKSA NAF.DER.ČL.25S.GPDV/L", "line_total": 0.42, "unit_price": 0.01}, {"qty": 2.0, "raw_label": "CIGARETE DUNHILL DISTINCT", "line_total": 13.4, "unit_price": 6.7}], "totals": {"total": 110.46}, "merchant": {"name": "PETROL BH OIL COMPANY", "address": "BS KONJIC ŽELJUŠA, Potoci, Željuša bb, 88000 MOSTAR", "store_name": "SARAJEVO - PODRUŽNICA KONJIC"}}, "received_at": "2026-01-09T15:19:14.293431Z", "valid_shape?": true}	2026-01-09 15:19:14.303921+00	110.46	\N	2457129	BAM	\N	extracted	273949541c01b6e3bc24b690cd90652b970a1b78ed21856842619cc469d20005	ccb05d9e-1ede-44b9-a861-ffff9b61409c	image/jpeg	\N	da1ee4d1-5d23-461a-957a-a569583bf923.jpg	\N	\N	PETROL BH OIL COMPANY	2	2026-01-09 13:55:49.061801+00	"PETROL BH OIL COMPANY" D.O.O\nSARAJEVO - PODRUŽNICA KONJIC\nBS KONJIC ŽELJUŠA\nPotoci\nŽeljuša bb\n88000 MOSTAR\n\nJIB: 4200505350204\nPIB: 200505350000\nIBFM: BJ008820\n\nFISKALNI RAČUN\nBF: 553492\n04.01.2026. 13:49\n\nPREMIUM 95 BAS EN 228/L\n42,200x 2,29 96,64E\nTAKSA NAF.DER.ČL.25S.GPDV/L\n42,200x 0,01 0,42K\nCIGARETE DUNHILL DISTINCT\n2,000x 6,70 13,40E\n\nVE: 17,00%\nVK: 0,00%\nOSN. E: 94,05\nOSN. K: 0,42\nPDV E: 15,99\nPDV K: 0,00\nPDV: 15,99\n\nTOTAL: 110,46\nUPLAČENO:\nKARTICA: 110,46\nUKUPNO: 110,46\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3815.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "BINGO doo EXPORT-IMPORT TUZLA\\nPJ 57, \\"HIPERMARKET\\" Otoka\\nul. Dzemala Bilediča br. 123\\n79220 SARAJEVO NOVI GRAD\\n\\nJIB: 4209253451751\\nPIB: 209253450003\\n\\nIBFM: BP000190\\n\\nFISKALNI RACUN\\nBF: 248304\\n12.12.2025. 12:19\\n\\n000003 VRECICA TREGERUNA\\n2,000x 0,10 0,20E\\nG00833 KAFA ESPRESSO 112G NESCAFE DO\\n1,000x 9,95 9,95E\\nF11587 SOK 1 25L COCA COLA\\n1,000x 2,00 2,00E\\nE24002 FLIPS 140G GOLD CORN FLIPS\\n1,000x 1,25 1,25E\\nE51923 COKOLADNE BANANICE 400G STARK\\n1,000x 5,80 5,80E\\nE14286 KEKS 260G MILKA CHOCO CREME\\n1,000x 2,45 2,45E\\nB10448 PASTRMKA PECENIA\\n0,272x 17,95 4,88E\\nB10448 PASTRMKA PECENIA\\n0,266x 17,95 4,77E\\nB21558 SUNKA PURECA DELUX VINDON\\n0,156x 29,95 4,67E\\nD13147 PECIVO 9X33 33G BROTLINIES SU\\n1,000x 1,85 1,85E\\nJ11276 SVJEZI LIMUN\\n0,854x 2,60 2,22E\\n\\nVE: 17,00%\\nOSN E: 34,22\\nPDV E: 5,82\\nPDV: 5,82\\n\\nTOTAL: 40,04\\nUPLACENO:\\nCEK: 40,04\\nUkupno: 40,04\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 2564005, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 2.0, "raw_label": "VRECICA TREGERUNA", "line_total": 0.2, "unit_price": 0.1}, {"qty": 1.0, "raw_label": "KAFA ESPRESSO 112G NESCAFE DO", "line_total": 9.95, "unit_price": 9.95}, {"qty": 1.0, "raw_label": "SOK 1 25L COCA COLA", "line_total": 2.0, "unit_price": 2.0}, {"qty": 1.0, "raw_label": "FLIPS 140G GOLD CORN FLIPS", "line_total": 1.25, "unit_price": 1.25}, {"qty": 1.0, "raw_label": "COKOLADNE BANANICE 400G STARK", "line_total": 5.8, "unit_price": 5.8}, {"qty": 1.0, "raw_label": "KEKS 260G MILKA CHOCO CREME", "line_total": 2.45, "unit_price": 2.45}, {"qty": 0.272, "raw_label": "PASTRMKA PECENIA", "line_total": 4.88, "unit_price": 17.95}, {"qty": 0.266, "raw_label": "PASTRMKA PECENIA", "line_total": 4.77, "unit_price": 17.95}, {"qty": 0.156, "raw_label": "SUNKA PURECA DELUX VINDON", "line_total": 4.67, "unit_price": 29.95}, {"qty": 1.0, "raw_label": "PECIVO 9X33 33G BROTLINIES SU", "line_total": 1.85, "unit_price": 1.85}, {"qty": 0.854, "raw_label": "SVJEZI LIMUN", "line_total": 2.22, "unit_price": 2.6}], "totals": {"total": 40.04}, "merchant": {"name": "BINGO doo EXPORT-IMPORT TUZLA"}}, "received_at": "2026-01-09T13:56:03.343020Z", "valid_shape?": true}	2026-01-09 13:56:59.72575+00	40.04	\N	2564005	BAM	afac9f18-6043-4d89-82c5-749677c66895	posted	97cd0a855fc0ae948bd476ae1becc14cae104a623c127c1643963ceb46fe2eec	a15ccedc-0769-4d71-b949-79d125c06554	image/jpeg	\N	c3986632-f325-45a3-9214-f0c15d033d8c.jpg	\N	\N	BINGO doo EXPORT-IMPORT TUZLA	1	2026-01-09 13:55:48.854448+00	BINGO doo EXPORT-IMPORT TUZLA\nPJ 57, "HIPERMARKET" Otoka\nul. Dzemala Bilediča br. 123\n79220 SARAJEVO NOVI GRAD\n\nJIB: 4209253451751\nPIB: 209253450003\n\nIBFM: BP000190\n\nFISKALNI RACUN\nBF: 248304\n12.12.2025. 12:19\n\n000003 VRECICA TREGERUNA\n2,000x 0,10 0,20E\nG00833 KAFA ESPRESSO 112G NESCAFE DO\n1,000x 9,95 9,95E\nF11587 SOK 1 25L COCA COLA\n1,000x 2,00 2,00E\nE24002 FLIPS 140G GOLD CORN FLIPS\n1,000x 1,25 1,25E\nE51923 COKOLADNE BANANICE 400G STARK\n1,000x 5,80 5,80E\nE14286 KEKS 260G MILKA CHOCO CREME\n1,000x 2,45 2,45E\nB10448 PASTRMKA PECENIA\n0,272x 17,95 4,88E\nB10448 PASTRMKA PECENIA\n0,266x 17,95 4,77E\nB21558 SUNKA PURECA DELUX VINDON\n0,156x 29,95 4,67E\nD13147 PECIVO 9X33 33G BROTLINIES SU\n1,000x 1,85 1,85E\nJ11276 SVJEZI LIMUN\n0,854x 2,60 2,22E\n\nVE: 17,00%\nOSN E: 34,22\nPDV E: 5,82\nPDV: 5,82\n\nTOTAL: 40,04\nUPLACENO:\nCEK: 40,04\nUkupno: 40,04\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3814.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "JU \\"APOTEKE SARAJEVO\\" SARAJEVO\\nAPOTEKA \\"KOSEVSKO BRDO\\"\\nBRAĆE BEGIĆ br.4\\n71000 Sarajevo\\n\\nJIB: 4200280090321\\nPIB: 200280090003\\n\\nIBFM: AH033550\\n\\nFISKALNI RAČUN\\nBF: 234080\\n06.01.2026. 15:48\\n\\nCASA_ZA_URIN_KLIK_125_ML_ROMED_48d\\n7\\n2,000x  0,55  1,10E\\nTOPLOMJER_DIGITALNI_UEBE_TH1_COLOR\\n_CVRST_17e0  7,15E\\n\\nVE: 17,00%\\nOSN. E:  7,05\\nPDV E:  1,20\\nPDV:  1,20\\n\\nTOTAL:  8,25\\nUPLAĆENO:\\nGotovina:  8,25\\nUkupno:  8,25\\nPOVRAT:  0,00\\n\\n6201a7f0132a12273a540243d5ddac2d", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 2534495, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 2.0, "raw_label": "CASA_ZA_URIN_KLIK_125_ML_ROMED_48d", "line_total": 1.1, "unit_price": 0.55}, {"qty": 1.0, "raw_label": "TOPLOMJER_DIGITALNI_UEBE_TH1_COLOR _CVRST_17e0", "line_total": 7.15, "unit_price": 7.15}], "totals": {"total": 8.25}, "merchant": {"name": "JU \\"APOTEKE SARAJEVO\\" SARAJEVO"}}, "received_at": "2026-01-09T13:56:03.343372Z", "valid_shape?": true}	2026-01-09 13:57:16.455529+00	8.25	\N	2534495	BAM	b59747f2-d587-4d83-bdc8-b17316fbd20e	posted	ed21520880b5b05ce8a4561bcf2d721d31f7b158f35855df250164df5a4066aa	a7d47b14-3ed0-4761-b90e-646f41b210ac	image/jpeg	\N	4f1cbbb6-8057-47b0-b218-e26d9e12cec3.jpg	\N	\N	JU "APOTEKE SARAJEVO" SARAJEVO	1	2026-01-09 13:55:48.956388+00	JU "APOTEKE SARAJEVO" SARAJEVO\nAPOTEKA "KOSEVSKO BRDO"\nBRAĆE BEGIĆ br.4\n71000 Sarajevo\n\nJIB: 4200280090321\nPIB: 200280090003\n\nIBFM: AH033550\n\nFISKALNI RAČUN\nBF: 234080\n06.01.2026. 15:48\n\nCASA_ZA_URIN_KLIK_125_ML_ROMED_48d\n7\n2,000x  0,55  1,10E\nTOPLOMJER_DIGITALNI_UEBE_TH1_COLOR\n_CVRST_17e0  7,15E\n\nVE: 17,00%\nOSN. E:  7,05\nPDV E:  1,20\nPDV:  1,20\n\nTOTAL:  8,25\nUPLAĆENO:\nGotovina:  8,25\nUkupno:  8,25\nPOVRAT:  0,00\n\n6201a7f0132a12273a540243d5ddac2d	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3612.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "\\"Pepco B-H\\" d.o.o.\\nPodružnica Sarajevo 2\\nul. Kolodvorska br.12\\n71000 Sarajevo\\n\\nJIB: 4203144510090\\nPIB: 203144510006\\n\\nIBFM: AM025770\\n\\n# FISKALNI RACUN\\n\\nBF: 115247\\n25.12.2025, 19:45\\n\\n60972201 folija za hranu 30 m box_ONE_Multicolor /pc 2,00E\\n62690101 Ukrasni papir_ONE_Multicolor/pc 2,00E\\n62689101 Papirnati tanjuri 12-PAK Xmas_ONE_Patter /pc 1,75E\\n-41,71%: 1,02\\n62753802 Spuzva Christmas oblik_ONE_Multicolor/pc 1,00E\\n-29,00%: 0,71\\n62689001 Papirnate case 12-PAK Xmas_ONE_Pattern /pc 2,50E\\n-28,40%: 1,79\\n62693901 Oznake za poklone sljokice 20kom Xmas_O N/pc 2,50E\\n-40,80%: 1,48\\n62347901 Kuhinjski organizer_ONE_Dark beige/pc 12,00E\\n31679634 Svijeca \\"silver &amp; gold\\" s poklopcem 13.5/pc 10,00E\\n31679634 Svijeca \\"silver &amp; gold\\" s poklopcem 13.5/pc 10,00E\\n60963601 Torba papirna velika 32 x 16 x 45 - bez /pc 0,70E\\n\\nVE: 17,00%\\nOSN. E: 35,64\\nPDV E: 6,06\\nPDV: 6,06\\n\\nTOTAL: 41,70\\nUPLACENO:\\nKartica: 41,70\\nUkupno: 41,70\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3284699, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "folija za hranu 30 m box_ONE_Multicolor /pc", "line_total": 2.0, "unit_price": 2.0}, {"qty": 1.0, "raw_label": "Ukrasni papir_ONE_Multicolor/pc", "line_total": 2.0, "unit_price": 2.0}, {"qty": 1.0, "raw_label": "Papirnati tanjuri 12-PAK Xmas_ONE_Patter /pc", "line_total": 1.02, "unit_price": 1.02}, {"qty": 1.0, "raw_label": "Spuzva Christmas oblik_ONE_Multicolor/pc", "line_total": 0.71, "unit_price": 0.71}, {"qty": 1.0, "raw_label": "Papirnate case 12-PAK Xmas_ONE_Pattern /pc", "line_total": 1.79, "unit_price": 1.79}, {"qty": 1.0, "raw_label": "Oznake za poklone sljokice 20kom Xmas_O N/pc", "line_total": 1.48, "unit_price": 1.48}, {"qty": 1.0, "raw_label": "Kuhinjski organizer_ONE_Dark beige/pc", "line_total": 12.0, "unit_price": 12.0}, {"qty": 1.0, "raw_label": "Svijeca \\"silver &amp; gold\\" s poklopcem 13.5/pc", "line_total": 10.0, "unit_price": 10.0}, {"qty": 1.0, "raw_label": "Svijeca \\"silver &amp; gold\\" s poklopcem 13.5/pc", "line_total": 10.0, "unit_price": 10.0}, {"qty": 1.0, "raw_label": "Torba papirna velika 32 x 16 x 45 - bez /pc", "line_total": 0.7, "unit_price": 0.7}], "totals": {"total": 41.7}, "merchant": {"name": "\\"Pepco B-H\\" d.o.o."}}, "received_at": "2026-01-09T14:02:17.461913Z", "valid_shape?": true}	2026-01-09 14:05:03.600719+00	41.70	\N	3284699	BAM	a56e84de-9e68-431e-9ab5-1b3012f959bb	posted	56437b5caab63f520a5aad46e14f875f96bc0b6560e852cb9e5b507bc4dfb06f	5028c4cf-1b07-479d-855d-ca717448bf28	image/jpeg	\N	690cc35e-e725-4e63-8a00-a6f4cebff06d.jpg	\N	\N	"Pepco B-H" d.o.o.	1	2026-01-09 14:01:58.548548+00	"Pepco B-H" d.o.o.\nPodružnica Sarajevo 2\nul. Kolodvorska br.12\n71000 Sarajevo\n\nJIB: 4203144510090\nPIB: 203144510006\n\nIBFM: AM025770\n\n# FISKALNI RACUN\n\nBF: 115247\n25.12.2025, 19:45\n\n60972201 folija za hranu 30 m box_ONE_Multicolor /pc 2,00E\n62690101 Ukrasni papir_ONE_Multicolor/pc 2,00E\n62689101 Papirnati tanjuri 12-PAK Xmas_ONE_Patter /pc 1,75E\n-41,71%: 1,02\n62753802 Spuzva Christmas oblik_ONE_Multicolor/pc 1,00E\n-29,00%: 0,71\n62689001 Papirnate case 12-PAK Xmas_ONE_Pattern /pc 2,50E\n-28,40%: 1,79\n62693901 Oznake za poklone sljokice 20kom Xmas_O N/pc 2,50E\n-40,80%: 1,48\n62347901 Kuhinjski organizer_ONE_Dark beige/pc 12,00E\n31679634 Svijeca "silver &amp; gold" s poklopcem 13.5/pc 10,00E\n31679634 Svijeca "silver &amp; gold" s poklopcem 13.5/pc 10,00E\n60963601 Torba papirna velika 32 x 16 x 45 - bez /pc 0,70E\n\nVE: 17,00%\nOSN. E: 35,64\nPDV E: 6,06\nPDV: 6,06\n\nTOTAL: 41,70\nUPLACENO:\nKartica: 41,70\nUkupno: 41,70\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3620.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "footer": null, "header": null, "images": [], "tables": [], "markdown": "\\"KONZUM\\" d.o.o. Sarajevo\\nPodružnica br. 66\\nProdavnica br. 90 Sarajevo\\nBrace Begić 3\\n71101 SARAJEVO CENTAR\\n\\nJIB: 4200918605499\\nPIB: 200918600004\\n\\nIBFM: BT005790\\n\\nFISKALNI RACUN\\nBF: 390713\\n26.12.2025. 15:08\\n\\n|  MLIJEKO MEGGLE 3,2% 657 | 3,000x | 2,25 | 6,75E  |\\n| --- | --- | --- | --- |\\n\\nVE: 17,00%\\nOSN. E: 5,77\\nPDV E: 0,98\\nPDV: 0,98\\n\\nTOTAL: 6,75\\nUPLACENO:\\nKARTICA: 6,75\\nUkupno: 6,75\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}, "hyperlinks": []}], "usage_info": {"doc_size_bytes": 3219100, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 3.0, "raw_label": "MLIJEKO MEGGLE 3,2% 657", "line_total": 6.75, "unit_price": 2.25}], "totals": {"total": 6.75}, "merchant": {"name": "KONZUM", "address": "Prodavnica br. 90 Sarajevo, Brace Begić 3, 71101 SARAJEVO CENTAR", "store_name": "Podružnica br. 66"}}, "received_at": "2026-01-09T15:19:52.622558Z", "valid_shape?": true}	2026-01-09 15:19:52.63745+00	6.75	\N	3219100	BAM	\N	extracted	b25b60dd6a5a88f495cb8ab2bac334c752715a968411fe30a182729b63dc6647	a869916b-a656-46a9-9639-35de90f8f9b1	image/jpeg	\N	6e0a723b-4153-42da-80d7-b690096e9bc4.jpg	\N	\N	KONZUM	1	2026-01-09 15:19:38.310165+00	"KONZUM" d.o.o. Sarajevo\nPodružnica br. 66\nProdavnica br. 90 Sarajevo\nBrace Begić 3\n71101 SARAJEVO CENTAR\n\nJIB: 4200918605499\nPIB: 200918600004\n\nIBFM: BT005790\n\nFISKALNI RACUN\nBF: 390713\n26.12.2025. 15:08\n\n|  MLIJEKO MEGGLE 3,2% 657 | 3,000x | 2,25 | 6,75E  |\n| --- | --- | --- | --- |\n\nVE: 17,00%\nOSN. E: 5,77\nPDV E: 0,98\nPDV: 0,98\n\nTOTAL: 6,75\nUPLACENO:\nKARTICA: 6,75\nUkupno: 6,75\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3812.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "footer": null, "header": null, "images": [], "tables": [], "markdown": "\\"KONZUM\\" d.o.o. Sarajevo\\nPodružnica br. 66\\nProdavnica br. 90 Sarajevo\\nBraće Besić 3\\n71101 SARAJEVO CENTAR\\n\\nJIB: 4200918605499\\nPIB: 200918600004\\n\\nIBFM: BT005790\\n\\n# FISKALNI RACUN\\n\\nBF: 394987\\n06.01.2026. 15:53\\n\\n|  SECE R BRAZILAS 1KG | 2,000x | 1,50 | 3,00E  |\\n| --- | --- | --- | --- |\\n|  SECE SMEDI 800G | 1,000x | 3,25 | 3,25E  |\\n\\nVE: 17,00%\\nOSN. E: 5,34\\nPDV E: 0,91\\nPDV: 0,91\\n\\nTOTAL: 6,25\\nUPLACENO:\\nKARTICA: 6,25\\nUkupno: 6,25\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}, "hyperlinks": []}], "usage_info": {"doc_size_bytes": 2349926, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 2.0, "raw_label": "SECE R BRAZILAS 1KG", "line_total": 3.0, "unit_price": 1.5}, {"qty": 1.0, "raw_label": "SECE SMEDI 800G", "line_total": 3.25, "unit_price": 3.25}], "totals": {"total": 6.25}, "merchant": {"name": "KONZUM", "address": "Prodavnica br. 90 Sarajevo, Braće Besić 3, 71101 SARAJEVO CENTAR", "store_name": "Podružnica br. 66"}}, "received_at": "2026-01-09T15:20:36.855464Z", "valid_shape?": true}	2026-01-09 15:20:36.863707+00	6.25	\N	2349926	BAM	\N	extracted	596ba84029a399e892e0ce15788e7bdbeec09e9325a39bdfb383d56151430c68	12917550-6c5a-471d-9b80-60300b086976	image/jpeg	\N	7b734d93-5072-40ae-a869-b0d2be35cb9a.jpg	\N	\N	KONZUM	1	2026-01-09 15:20:28.69957+00	"KONZUM" d.o.o. Sarajevo\nPodružnica br. 66\nProdavnica br. 90 Sarajevo\nBraće Besić 3\n71101 SARAJEVO CENTAR\n\nJIB: 4200918605499\nPIB: 200918600004\n\nIBFM: BT005790\n\n# FISKALNI RACUN\n\nBF: 394987\n06.01.2026. 15:53\n\n|  SECE R BRAZILAS 1KG | 2,000x | 1,50 | 3,00E  |\n| --- | --- | --- | --- |\n|  SECE SMEDI 800G | 1,000x | 3,25 | 3,25E  |\n\nVE: 17,00%\nOSN. E: 5,34\nPDV E: 0,91\nPDV: 0,91\n\nTOTAL: 6,25\nUPLACENO:\nKARTICA: 6,25\nUkupno: 6,25\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3610.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "\\"BINGO\\" d.o.o. EXPORT-IMPORT TUZLA\\nPJ br.213 \\"HIPERMARKET MERKUR\\" Sarajevo\\nGradacacka broj 1\\n71120 SARAJEVO NOVI GRAD\\n\\nJIB: 4209253454262\\nPIB: 209253450003\\n\\nIBFM: BT011870\\n\\nFISKALNI RACUN\\nBF: 420332\\n25.12.2025. 17:36\\n\\n|  E51904 BOMBONE GUMENE 100G HARIBO BE | 1,000x | 2,10 | 2,10E  |\\n| --- | --- | --- | --- |\\n|  E51904 BOMBONE GUMENE 100G HARIBO BE | 1,000x | 2,10 | 2,10E  |\\n|  SE7033 POKLON VRECICA 40X30CM DJ ASS | 1,000x | 2,50 | 2,50E  |\\n\\nVE: 17,00%\\nOSN. E: 5,73\\nPDV E: 0,97\\nPDV: 0,97\\n\\nTOTAL: 6,70\\nUPLACENO:\\nKARTICA: 6,70\\nUkupna: 6,70\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3215103, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "BOMBONE GUMENE 100G HARIBO BE", "line_total": 2.1, "unit_price": 2.1}, {"qty": 1.0, "raw_label": "BOMBONE GUMENE 100G HARIBO BE", "line_total": 2.1, "unit_price": 2.1}, {"qty": 1.0, "raw_label": "SE7033 POKLON VRECICA 40X30CM DJ ASS", "line_total": 2.5, "unit_price": 2.5}], "totals": {"total": 6.7}, "merchant": {"name": "\\"BINGO\\" d.o.o. EXPORT-IMPORT TUZLA"}}, "received_at": "2026-01-09T14:02:17.461532Z", "valid_shape?": true}	2026-01-09 14:02:39.669455+00	6.70	\N	3215103	BAM	f65a59cb-16ac-4d7a-a252-7090916d4d1a	posted	defd494b3f5e4363e5168609c352992dbc658f7abbb3cbd3b9ba089d8d5e2c77	f22c3592-941a-46a8-9df9-63cb79c29e82	image/jpeg	\N	09e25c47-9428-4a3c-8289-87700be58868.jpg	\N	\N	"BINGO" d.o.o. EXPORT-IMPORT TUZLA	1	2026-01-09 14:01:58.396296+00	"BINGO" d.o.o. EXPORT-IMPORT TUZLA\nPJ br.213 "HIPERMARKET MERKUR" Sarajevo\nGradacacka broj 1\n71120 SARAJEVO NOVI GRAD\n\nJIB: 4209253454262\nPIB: 209253450003\n\nIBFM: BT011870\n\nFISKALNI RACUN\nBF: 420332\n25.12.2025. 17:36\n\n|  E51904 BOMBONE GUMENE 100G HARIBO BE | 1,000x | 2,10 | 2,10E  |\n| --- | --- | --- | --- |\n|  E51904 BOMBONE GUMENE 100G HARIBO BE | 1,000x | 2,10 | 2,10E  |\n|  SE7033 POKLON VRECICA 40X30CM DJ ASS | 1,000x | 2,50 | 2,50E  |\n\nVE: 17,00%\nOSN. E: 5,73\nPDV E: 0,97\nPDV: 0,97\n\nTOTAL: 6,70\nUPLACENO:\nKARTICA: 6,70\nUkupna: 6,70\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3613.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "dn drogerie markt d.n.o.\\nSarajevo\\nPodružnica Sarajevo 096\\nul. Dženala Bijedica broj 2\\n71000 Sarajevo\\n\\nJIB: 4201125901310\\nPIB: 201125900003\\n\\nIBFM: BN007630\\n\\n# FISKALNI RAČUN\\nBF: 215347\\n16.12.2025. 18:52\\n\\n|  Mivolis flasteri za djecu |  |   |\\n| --- | --- | --- |\\n|  1,000x | 1,85 | 1,85E  |\\n|  Maybelline Stay Matt tečni |  |   |\\n|  1,000x | 21,65 | 21,65E  |\\n|  Syoss oleo 8-05 |  |   |\\n|  1,000x | 11,45 | 11,45E  |\\n|  Profissimo salv. 33x33cm Do |  |   |\\n|  1,000x | 2,25 | 2,25E  |\\n\\nVE: 17,00%\\nOSN. E: 31,79\\nPDV E: 5,41\\nPDV: 5,41\\n\\nTOTAL: 37,20\\nUPLAČENO:\\nKARTICA: 37,20\\nUKUPNO: 37,20\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3263471, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "Mivolis flasteri za djecu", "line_total": 1.85, "unit_price": 1.85}, {"qty": 1.0, "raw_label": "Maybelline Stay Matt tečni", "line_total": 21.65, "unit_price": 21.65}, {"qty": 1.0, "raw_label": "Syoss oleo 8-05", "line_total": 11.45, "unit_price": 11.45}, {"qty": 1.0, "raw_label": "Profissimo salv. 33x33cm Do", "line_total": 2.25, "unit_price": 2.25}], "totals": {"total": 37.2}, "merchant": {"name": "dn drogerie markt d.n.o.", "address": "Podružnica Sarajevo 096, ul. Dženala Bijedica broj 2, 71000 Sarajevo", "store_name": "Sarajevo"}}, "received_at": "2026-01-09T15:21:22.259647Z", "valid_shape?": true}	2026-01-09 15:21:22.288661+00	37.20	\N	3263471	BAM	\N	extracted	b478258d3bb28a765cdd466530986b1cbf1cc1495a410840242b66a32af89e13	51b6b15b-485d-4a71-b409-f71e8d810b21	image/jpeg	\N	92a69fa9-ddcf-42c1-b531-56f6ef5d0ab3.jpg	\N	\N	dn drogerie markt d.n.o.	1	2026-01-09 15:20:57.961405+00	dn drogerie markt d.n.o.\nSarajevo\nPodružnica Sarajevo 096\nul. Dženala Bijedica broj 2\n71000 Sarajevo\n\nJIB: 4201125901310\nPIB: 201125900003\n\nIBFM: BN007630\n\n# FISKALNI RAČUN\nBF: 215347\n16.12.2025. 18:52\n\n|  Mivolis flasteri za djecu |  |   |\n| --- | --- | --- |\n|  1,000x | 1,85 | 1,85E  |\n|  Maybelline Stay Matt tečni |  |   |\n|  1,000x | 21,65 | 21,65E  |\n|  Syoss oleo 8-05 |  |   |\n|  1,000x | 11,45 | 11,45E  |\n|  Profissimo salv. 33x33cm Do |  |   |\n|  1,000x | 2,25 | 2,25E  |\n\nVE: 17,00%\nOSN. E: 31,79\nPDV E: 5,41\nPDV: 5,41\n\nTOTAL: 37,20\nUPLAČENO:\nKARTICA: 37,20\nUKUPNO: 37,20\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3609.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "\\"SAMON PROMET\\" doo Sarajevo\\nP.J.3 \\"HORECA SHOP I MARKET\\"\\nMARSALA TITA 7\\n71120 SARAJEVO CENTAR\\n\\nJIB: 4200397100042\\nPIB: 200397100000\\n\\nIBFM: AM045520\\n\\nFISKALNI RAOUN\\nBF: 97251\\n23.12.2025. 19:25\\n\\nSTRANGE LUVE GIN 40% 0,7L/KO\\n19,95E\\n\\nVE: 17,00%\\nOSN. E: 17,05\\nPDV E: 2,90\\nPDV: 2,90\\n\\nTOTAL: 19,95\\nUPLACENO:\\nKartica: 19,95\\nUkupno: 19,95\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3380062, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "STRANGE LUVE GIN 40% 0,7L/KO", "line_total": 19.95, "unit_price": 19.95}], "totals": {"total": 19.95}, "merchant": {"name": "\\"SAMON PROMET\\" doo Sarajevo"}}, "received_at": "2026-01-09T14:02:17.461326Z", "valid_shape?": true}	2026-01-09 14:02:17.481385+00	19.95	\N	3380062	BAM	\N	extracted	63344d52d8821f3c3dc31eb433462c8265ec12a97bb65989512f1a525e468c2a	735cd912-14ba-4ada-a897-f592fc43ca9a	image/jpeg	\N	12a5dade-3972-4ce1-9ee6-600a826efbef.jpg	\N	\N	"SAMON PROMET" doo Sarajevo	1	2026-01-09 14:01:58.30153+00	"SAMON PROMET" doo Sarajevo\nP.J.3 "HORECA SHOP I MARKET"\nMARSALA TITA 7\n71120 SARAJEVO CENTAR\n\nJIB: 4200397100042\nPIB: 200397100000\n\nIBFM: AM045520\n\nFISKALNI RAOUN\nBF: 97251\n23.12.2025. 19:25\n\nSTRANGE LUVE GIN 40% 0,7L/KO\n19,95E\n\nVE: 17,00%\nOSN. E: 17,05\nPDV E: 2,90\nPDV: 2,90\n\nTOTAL: 19,95\nUPLACENO:\nKartica: 19,95\nUkupno: 19,95\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3615.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "JU \\"APOTEKE SARAJEVO\\" SARAJEVO\\nAPOTEKA \\"KOSEVSKO BRDO\\"\\nBRACE BEGIC br.4\\n71000 Sarajevo\\n\\nJIB: 4200280090321\\nPIB: 200280090003\\n\\nIBFM: AH033550\\n\\nFISKALNI RAČUN\\nBF: 231838\\n19.12.2025. 15:21\\n\\nVOLTAREN RETARD TABLETE 100 MG A 2\\n0 SA P 172e 5,85E\\nPARACETAMOL TABLETE 500 MG A 10 BO\\nSNALIJ 577f 2,45E\\nANDOL TABLETE 300 MG A 20 5673\\n5,70E\\n\\nVE: 17,00%\\nOSN. E: 11,97\\nPDV E: 2,03\\nPDV: 2,03\\n\\nTOTAL: 14,00\\nUPLAĆENO:\\nKartica: 14,00\\nUkupno: 14,00\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3048908, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 1.0, "raw_label": "VOLTAREN RETARD TABLETE 100 MG A 2 0 SA P 172e", "line_total": 5.85, "unit_price": 5.85}, {"qty": 1.0, "raw_label": "PARACETAMOL TABLETE 500 MG A 10 BO SNALIJ 577f", "line_total": 2.45, "unit_price": 2.45}, {"qty": 1.0, "raw_label": "ANDOL TABLETE 300 MG A 20 5673", "line_total": 5.7, "unit_price": 5.7}], "totals": {"total": 14.0}, "merchant": {"name": "APOTEKE SARAJEVO", "address": "BRACE BEGIC br.4, 71000 Sarajevo", "store_name": "APOTEKA \\"KOSEVSKO BRDO\\""}}, "received_at": "2026-01-09T15:21:22.259776Z", "valid_shape?": true}	2026-01-09 15:21:22.306334+00	14.00	\N	3048908	BAM	\N	extracted	dfb00b4ca84499080e10c013f99900204533e94699dea248ab857a8d7b6382e9	5b84f146-8d1f-4f3c-8e34-33a865a442dd	image/jpeg	\N	282105c0-ac70-45f8-aa1b-504af44c7416.jpg	\N	\N	APOTEKE SARAJEVO	1	2026-01-09 15:20:58.13112+00	JU "APOTEKE SARAJEVO" SARAJEVO\nAPOTEKA "KOSEVSKO BRDO"\nBRACE BEGIC br.4\n71000 Sarajevo\n\nJIB: 4200280090321\nPIB: 200280090003\n\nIBFM: AH033550\n\nFISKALNI RAČUN\nBF: 231838\n19.12.2025. 15:21\n\nVOLTAREN RETARD TABLETE 100 MG A 2\n0 SA P 172e 5,85E\nPARACETAMOL TABLETE 500 MG A 10 BO\nSNALIJ 577f 2,45E\nANDOL TABLETE 300 MG A 20 5673\n5,70E\n\nVE: 17,00%\nOSN. E: 11,97\nPDV E: 2,03\nPDV: 2,03\n\nTOTAL: 14,00\nUPLAĆENO:\nKartica: 14,00\nUkupno: 14,00\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
IMG_3617.jpg	{"model": "mistral-ocr-2512", "provider": "mistral", "response": {"model": "mistral-ocr-2512", "pages": [{"index": 0, "images": [], "markdown": "\\"SAMON PROMET\\" doo Sarajevo\\nP.J.3 \\"HORECA SHOP I MARKET\\"\\nMARSALA TITA 7\\n71120 SARAJEVO CENTAR\\n\\nJIB: 4200397100042\\nPIB: 200397100000\\n\\nIBEM: AM045520\\n\\nFISKALNI RACUN\\nBF: 97249\\n23.12.2025. 19:23\\n\\nTUBORG 0,33 NEPOVRATNI/KO\\n24,000x 1,55 37,20E\\nSCHWEPPES TONIC 1L/KO\\n3,000x 2,00 6,00E\\nBULLDOG GIN SA CASOM 0,7/KO 42,00E\\n\\nVE: 17,00%\\nOSN. E: 72,82\\nPDV E: 12,38\\nPDV: 12,38\\n\\nTOTAL: 85,20\\nUPLACENO:\\nKartica: 85,20\\nUkupno: 85,20\\nPOVRAT: 0,00", "dimensions": {"dpi": 200, "width": 4032, "height": 3024}}], "usage_info": {"doc_size_bytes": 3110638, "pages_processed": 1}, "document_annotation": "{\\n  \\"title\\": \\"ReceiptMetaExtractionV1\\",\\n  \\"type\\": \\"object\\",\\n  \\"properties\\": {\\n    \\"merchant\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Seller/merchant printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"name\\": {\\n          \\"type\\": \\"string\\",\\n          \\"description\\": \\"Merchant/store name (as printed).\\"\\n        },\\n        \\"address\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Address if present.\\"\\n        },\\n        \\"tax_id\\": {\\n          \\"type\\": [\\"string\\", \\"null\\"],\\n          \\"description\\": \\"Merchant tax/VAT id if present.\\"\\n        }\\n      },\\n      \\"required\\": [\\"name\\"]\\n    },\\n    \\"purchased_at\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO-8601 timestamp (local time) if available, e.g. 2023-12-23T19:23:00\\"\\n    },\\n    \\"currency\\": {\\n      \\"type\\": [\\"string\\", \\"null\\"],\\n      \\"description\\": \\"ISO 4217 currency code (e.g. BAM/EUR/USD). Use null if unknown.\\"\\n    },\\n    \\"totals\\": {\\n      \\"type\\": \\"object\\",\\n      \\"description\\": \\"Totals printed on the receipt.\\",\\n      \\"properties\\": {\\n        \\"subtotal\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Subtotal before tax/fees if present.\\"\\n        },\\n        \\"tax\\": {\\n          \\"type\\": [\\"number\\", \\"null\\"],\\n          \\"description\\": \\"Total tax amount if present.\\"\\n        },\\n        \\"total\\": {\\n          \\"type\\": \\"number\\",\\n          \\"description\\": \\"Grand total paid; prefer the final total.\\"\\n        }\\n      },\\n      \\"required\\": [\\"total\\"]\\n    }\\n  },\\n  \\"required\\": [\\"totals\\"]\\n}"}, "extraction": {"items": [{"qty": 24.0, "raw_label": "TUBORG 0,33 NEPOVRATNI/KO", "line_total": 37.2, "unit_price": 1.55}, {"qty": 3.0, "raw_label": "SCHWEPPES TONIC 1L/KO", "line_total": 6.0, "unit_price": 2.0}, {"qty": 1.0, "raw_label": "BULLDOG GIN SA CASOM 0,7/KO", "line_total": 42.0, "unit_price": 42.0}], "totals": {"total": 85.2}, "merchant": {"name": "SAMON PROMET", "address": "MARSALA TITA 7, 71120 SARAJEVO CENTAR", "store_name": "P.J.3 \\"HORECA SHOP I MARKET\\""}}, "received_at": "2026-01-09T15:21:22.259831Z", "valid_shape?": true}	2026-01-09 15:21:22.311086+00	85.20	\N	3110638	BAM	\N	extracted	ed12a0e6706d9ea224837fe80355c518ade5f855d0f94e86028dc813576e30e4	9f360eb8-6370-455e-bfba-3592e894c9a9	image/jpeg	\N	4698e1a8-d296-4435-85ee-00fc4d7bd4e8.jpg	\N	\N	SAMON PROMET	1	2026-01-09 15:20:58.281156+00	"SAMON PROMET" doo Sarajevo\nP.J.3 "HORECA SHOP I MARKET"\nMARSALA TITA 7\n71120 SARAJEVO CENTAR\n\nJIB: 4200397100042\nPIB: 200397100000\n\nIBEM: AM045520\n\nFISKALNI RACUN\nBF: 97249\n23.12.2025. 19:23\n\nTUBORG 0,33 NEPOVRATNI/KO\n24,000x 1,55 37,20E\nSCHWEPPES TONIC 1L/KO\n3,000x 2,00 6,00E\nBULLDOG GIN SA CASOM 0,7/KO 42,00E\n\nVE: 17,00%\nOSN. E: 72,82\nPDV E: 12,38\nPDV: 12,38\n\nTOTAL: 85,20\nUPLACENO:\nKartica: 85,20\nUkupno: 85,20\nPOVRAT: 0,00	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6
\.


--
-- TOC entry 3416 (class 0 OID 16707)
-- Dependencies: 210
-- Data for Name: suppliers; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.suppliers (id, display_name, normalized_key, address, tax_id, created_at, updated_at) FROM stdin;
6057fb90-df80-416c-b1a0-5a9a9dfcc264	Bingo Centar	bingo-centar	Sarajevo, Bosnia	1234567898	2025-12-06 21:07:53.683291+00	2025-12-08 12:28:28.640776+00
32d65bb6-c7db-465a-a5fc-16ccccb853c3	Test Supplier	test-supplier	123 Test St	123456	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
5b7e78de-b195-4848-832c-d84f0e0631df	Hose Kosevsko Brdo	hose-kosevsko-brdo	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
36296fee-8aa6-4a34-87f6-f5672cabdd83	SAMON PROMET	samon-promet	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
fad06371-df63-4788-9f1f-6d24998e8e2e	"Pepco B-H" d.o.o.	pepco-b-h-doo	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
3e8cbf97-854d-4536-b366-ac2296910e85	"SAMON PROMET" doo Sarajevo	samon-promet-doo-sarajevo	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
3798e5f7-776b-462a-b182-55f4906272ce	dm drogerie markt	dm-drogerie-markt	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
cf83233f-316c-40f8-a022-b8b3c3c5df45	CM-COSMETIC MARKET	cm-cosmetic-market	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
7192d67a-8306-4bc2-a8d0-501782f3015b	JU APOTEKE SARAJEVO	ju-apoteke-sarajevo	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
ab2e9d22-1de2-400d-9e53-cdb87106568d	KONZUM	konzum	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
bdfe1baa-e999-4910-a71a-0a8594f2020a	PETROL BH OIL COMPANY	petrol-bh-oil-company	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
92a781b7-fb7d-4bde-b868-7868d4f918b0	Pepco B-H	pepco-b-h	\N	\N	2025-12-06 21:07:53.683291+00	2025-12-06 21:07:53.683291+00
\.


--
-- TOC entry 3409 (class 0 OID 16411)
-- Dependencies: 203
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.users (role, updated_at, email, avatar_url, password_hash, provider_user_id, last_login_at, status, id, full_name, auth_provider, created_at, email_verified) FROM stdin;
admin	2026-01-11 19:18:36.874892+00	enes.jakic@gmail.com	https://lh3.googleusercontent.com/a/ACg8ocJjYpNDm22dpDu9NR2QZzYt3lKysTwYIV94ucrm232EdtZmLA=s96-c	bcrypt+sha512$ca6e964322b050d51243ea253c04c95b$12$cb4a0be5ffe2835e8e0ca72f5b77f3d71ece01561ce4ac40	107292161221034741932	\N	active	e1ebcfb7-a453-4fdd-aae8-67ca561e8cf6	Enes Jakić	google	2025-12-08 08:41:00.287107+00	f
member	2025-12-25 13:23:11.321063+00	upload-test-1766668991@example.com	\N	bcrypt+sha512$2ad705b2ca0239df40a51a2e0797ee59$12$7aeef1243681439005319846241f864b11a2fdd7ec5e45ad	\N	\N	active	561c45b8-6e1f-4f97-bbf2-a086f61f3902	Upload Test	password	2025-12-25 13:23:11.321063+00	f
\.


--
-- TOC entry 3432 (class 0 OID 0)
-- Dependencies: 201
-- Name: automigrate_migrations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: app_user
--

SELECT pg_catalog.setval('public.automigrate_migrations_id_seq', 16, true);


--
-- TOC entry 3255 (class 2606 OID 16847)
-- Name: admin_sessions admin_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.admin_sessions
    ADD CONSTRAINT admin_sessions_pkey PRIMARY KEY (id);


--
-- TOC entry 3257 (class 2606 OID 16849)
-- Name: admin_sessions admin_sessions_token_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.admin_sessions
    ADD CONSTRAINT admin_sessions_token_key UNIQUE (token);


--
-- TOC entry 3198 (class 2606 OID 16469)
-- Name: admins admins_email_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_email_key UNIQUE (email);


--
-- TOC entry 3200 (class 2606 OID 16467)
-- Name: admins admins_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_pkey PRIMARY KEY (id);


--
-- TOC entry 3251 (class 2606 OID 16816)
-- Name: article_aliases article_aliases_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.article_aliases
    ADD CONSTRAINT article_aliases_pkey PRIMARY KEY (id);


--
-- TOC entry 3238 (class 2606 OID 16761)
-- Name: articles articles_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_pkey PRIMARY KEY (id);


--
-- TOC entry 3204 (class 2606 OID 16485)
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 3178 (class 2606 OID 16395)
-- Name: automigrate_migrations automigrate_migrations_name_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.automigrate_migrations
    ADD CONSTRAINT automigrate_migrations_name_key UNIQUE (name);


--
-- TOC entry 3180 (class 2606 OID 16393)
-- Name: automigrate_migrations automigrate_migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.automigrate_migrations
    ADD CONSTRAINT automigrate_migrations_pkey PRIMARY KEY (id);


--
-- TOC entry 3191 (class 2606 OID 16433)
-- Name: email_verification_tokens email_verification_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 3193 (class 2606 OID 16435)
-- Name: email_verification_tokens email_verification_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_token_key UNIQUE (token);


--
-- TOC entry 3242 (class 2606 OID 16770)
-- Name: expense_items expense_items_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expense_items
    ADD CONSTRAINT expense_items_pkey PRIMARY KEY (id);


--
-- TOC entry 3231 (class 2606 OID 16736)
-- Name: expenses expenses_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (id);


--
-- TOC entry 3210 (class 2606 OID 16503)
-- Name: login_events login_events_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.login_events
    ADD CONSTRAINT login_events_pkey PRIMARY KEY (id);


--
-- TOC entry 3215 (class 2606 OID 16651)
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 3217 (class 2606 OID 16653)
-- Name: password_reset_tokens password_reset_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_key UNIQUE (token);


--
-- TOC entry 3229 (class 2606 OID 16724)
-- Name: payers payers_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.payers
    ADD CONSTRAINT payers_pkey PRIMARY KEY (id);


--
-- TOC entry 3249 (class 2606 OID 16787)
-- Name: price_observations price_observations_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.price_observations
    ADD CONSTRAINT price_observations_pkey PRIMARY KEY (id);


--
-- TOC entry 3223 (class 2606 OID 16696)
-- Name: receipts receipts_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT receipts_pkey PRIMARY KEY (id);


--
-- TOC entry 3226 (class 2606 OID 16716)
-- Name: suppliers suppliers_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);


--
-- TOC entry 3187 (class 2606 OID 16426)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 3189 (class 2606 OID 16424)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 3258 (class 1259 OID 16856)
-- Name: idx_admin_sessions_admin_id; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_admin_sessions_admin_id ON public.admin_sessions USING btree (admin_id);


--
-- TOC entry 3259 (class 1259 OID 16857)
-- Name: idx_admin_sessions_expires; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_admin_sessions_expires ON public.admin_sessions USING btree (expires_at);


--
-- TOC entry 3260 (class 1259 OID 16855)
-- Name: idx_admin_sessions_token; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_admin_sessions_token ON public.admin_sessions USING btree (token);


--
-- TOC entry 3201 (class 1259 OID 16470)
-- Name: idx_admins_email; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_admins_email ON public.admins USING btree (email);


--
-- TOC entry 3202 (class 1259 OID 16488)
-- Name: idx_admins_status; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_admins_status ON public.admins USING btree (status);


--
-- TOC entry 3252 (class 1259 OID 16827)
-- Name: idx_article_aliases_article; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_article_aliases_article ON public.article_aliases USING btree (article_id);


--
-- TOC entry 3253 (class 1259 OID 16828)
-- Name: idx_article_aliases_supplier_label; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_article_aliases_supplier_label ON public.article_aliases USING btree (supplier_id, raw_label_normalized);


--
-- TOC entry 3239 (class 1259 OID 16805)
-- Name: idx_articles_category; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_articles_category ON public.articles USING btree (category);


--
-- TOC entry 3240 (class 1259 OID 16806)
-- Name: idx_articles_normalized_key; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_articles_normalized_key ON public.articles USING btree (normalized_key);


--
-- TOC entry 3205 (class 1259 OID 16506)
-- Name: idx_audit_logs_actor; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_audit_logs_actor ON public.audit_logs USING btree (actor_type, actor_id);


--
-- TOC entry 3206 (class 1259 OID 16486)
-- Name: idx_audit_logs_created_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_audit_logs_created_at ON public.audit_logs USING btree (created_at);


--
-- TOC entry 3194 (class 1259 OID 16508)
-- Name: idx_email_tokens_expires; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_email_tokens_expires ON public.email_verification_tokens USING btree (expires_at);


--
-- TOC entry 3195 (class 1259 OID 16441)
-- Name: idx_email_tokens_token; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_email_tokens_token ON public.email_verification_tokens USING btree (token);


--
-- TOC entry 3196 (class 1259 OID 16505)
-- Name: idx_email_tokens_user; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_email_tokens_user ON public.email_verification_tokens USING btree (user_id);


--
-- TOC entry 3243 (class 1259 OID 16808)
-- Name: idx_expense_items_article; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_expense_items_article ON public.expense_items USING btree (article_id);


--
-- TOC entry 3244 (class 1259 OID 16834)
-- Name: idx_expense_items_expense; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_expense_items_expense ON public.expense_items USING btree (expense_id);


--
-- TOC entry 3232 (class 1259 OID 16833)
-- Name: idx_expenses_is_posted; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_expenses_is_posted ON public.expenses USING btree (is_posted);


--
-- TOC entry 3233 (class 1259 OID 16836)
-- Name: idx_expenses_payer; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_expenses_payer ON public.expenses USING btree (payer_id);


--
-- TOC entry 3234 (class 1259 OID 16837)
-- Name: idx_expenses_purchased_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_expenses_purchased_at ON public.expenses USING btree (purchased_at);


--
-- TOC entry 3235 (class 1259 OID 16832)
-- Name: idx_expenses_supplier; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_expenses_supplier ON public.expenses USING btree (supplier_id);


--
-- TOC entry 3236 (class 1259 OID 16869)
-- Name: idx_expenses_user; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_expenses_user ON public.expenses USING btree (user_id);


--
-- TOC entry 3207 (class 1259 OID 16507)
-- Name: idx_login_events_created_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_login_events_created_at ON public.login_events USING btree (created_at);


--
-- TOC entry 3208 (class 1259 OID 16504)
-- Name: idx_login_events_principal; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_login_events_principal ON public.login_events USING btree (principal_type, principal_id);


--
-- TOC entry 3211 (class 1259 OID 16656)
-- Name: idx_password_reset_tokens_expires; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_password_reset_tokens_expires ON public.password_reset_tokens USING btree (expires_at);


--
-- TOC entry 3212 (class 1259 OID 16655)
-- Name: idx_password_reset_tokens_principal; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_password_reset_tokens_principal ON public.password_reset_tokens USING btree (principal_type, principal_id);


--
-- TOC entry 3213 (class 1259 OID 16654)
-- Name: idx_password_reset_tokens_token; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_password_reset_tokens_token ON public.password_reset_tokens USING btree (token);


--
-- TOC entry 3227 (class 1259 OID 16830)
-- Name: idx_payers_type; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_payers_type ON public.payers USING btree (type);


--
-- TOC entry 3245 (class 1259 OID 16831)
-- Name: idx_price_obs_article; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_price_obs_article ON public.price_observations USING btree (article_id);


--
-- TOC entry 3246 (class 1259 OID 16829)
-- Name: idx_price_obs_observed_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_price_obs_observed_at ON public.price_observations USING btree (observed_at);


--
-- TOC entry 3247 (class 1259 OID 16803)
-- Name: idx_price_obs_supplier; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_price_obs_supplier ON public.price_observations USING btree (supplier_id);


--
-- TOC entry 3218 (class 1259 OID 16807)
-- Name: idx_receipts_created_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_receipts_created_at ON public.receipts USING btree (created_at);


--
-- TOC entry 3219 (class 1259 OID 16804)
-- Name: idx_receipts_file_hash; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_receipts_file_hash ON public.receipts USING btree (file_hash);


--
-- TOC entry 3220 (class 1259 OID 16809)
-- Name: idx_receipts_status; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_receipts_status ON public.receipts USING btree (status);


--
-- TOC entry 3221 (class 1259 OID 16868)
-- Name: idx_receipts_user; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_receipts_user ON public.receipts USING btree (user_id);


--
-- TOC entry 3224 (class 1259 OID 16835)
-- Name: idx_suppliers_normalized_key; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_suppliers_normalized_key ON public.suppliers USING btree (normalized_key);


--
-- TOC entry 3181 (class 1259 OID 16471)
-- Name: idx_users_auth_provider_provider_user_id_external; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_users_auth_provider_provider_user_id_external ON public.users USING btree (auth_provider, provider_user_id) WHERE (auth_provider <> 'password'::text);


--
-- TOC entry 3182 (class 1259 OID 16510)
-- Name: idx_users_created_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_users_created_at ON public.users USING btree (created_at);


--
-- TOC entry 3183 (class 1259 OID 16487)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 3184 (class 1259 OID 16642)
-- Name: idx_users_email_verified; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_users_email_verified ON public.users USING btree (email_verified);


--
-- TOC entry 3185 (class 1259 OID 16509)
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- TOC entry 3276 (class 2620 OID 16640)
-- Name: admins admins_updated_at; Type: TRIGGER; Schema: public; Owner: app_user
--

CREATE TRIGGER admins_updated_at BEFORE UPDATE ON public.admins FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3275 (class 2620 OID 16639)
-- Name: users users_updated_at; Type: TRIGGER; Schema: public; Owner: app_user
--

CREATE TRIGGER users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3274 (class 2606 OID 16850)
-- Name: admin_sessions admin_sessions_admin_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.admin_sessions
    ADD CONSTRAINT admin_sessions_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES public.admins(id) ON DELETE CASCADE;


--
-- TOC entry 3273 (class 2606 OID 16822)
-- Name: article_aliases article_aliases_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.article_aliases
    ADD CONSTRAINT article_aliases_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;


--
-- TOC entry 3272 (class 2606 OID 16817)
-- Name: article_aliases article_aliases_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.article_aliases
    ADD CONSTRAINT article_aliases_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id) ON DELETE CASCADE;


--
-- TOC entry 3261 (class 2606 OID 16436)
-- Name: email_verification_tokens email_verification_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3268 (class 2606 OID 16776)
-- Name: expense_items expense_items_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expense_items
    ADD CONSTRAINT expense_items_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE SET NULL;


--
-- TOC entry 3267 (class 2606 OID 16771)
-- Name: expense_items expense_items_expense_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expense_items
    ADD CONSTRAINT expense_items_expense_id_fkey FOREIGN KEY (expense_id) REFERENCES public.expenses(id) ON DELETE CASCADE;


--
-- TOC entry 3264 (class 2606 OID 16742)
-- Name: expenses expenses_payer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_payer_id_fkey FOREIGN KEY (payer_id) REFERENCES public.payers(id);


--
-- TOC entry 3265 (class 2606 OID 16747)
-- Name: expenses expenses_receipt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_receipt_id_fkey FOREIGN KEY (receipt_id) REFERENCES public.receipts(id) ON DELETE SET NULL;


--
-- TOC entry 3263 (class 2606 OID 16737)
-- Name: expenses expenses_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);


--
-- TOC entry 3266 (class 2606 OID 16858)
-- Name: expenses expenses_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 3270 (class 2606 OID 16793)
-- Name: price_observations price_observations_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.price_observations
    ADD CONSTRAINT price_observations_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;


--
-- TOC entry 3269 (class 2606 OID 16788)
-- Name: price_observations price_observations_expense_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.price_observations
    ADD CONSTRAINT price_observations_expense_item_id_fkey FOREIGN KEY (expense_item_id) REFERENCES public.expense_items(id) ON DELETE SET NULL;


--
-- TOC entry 3271 (class 2606 OID 16798)
-- Name: price_observations price_observations_supplier_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.price_observations
    ADD CONSTRAINT price_observations_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id) ON DELETE CASCADE;


--
-- TOC entry 3262 (class 2606 OID 16863)
-- Name: receipts receipts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.receipts
    ADD CONSTRAINT receipts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


-- Completed on 2026-01-11 21:30:44 CET

--
-- PostgreSQL database dump complete
--

