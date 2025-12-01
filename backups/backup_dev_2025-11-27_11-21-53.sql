--
-- PostgreSQL database dump
--

-- Dumped from database version 13.23 (Debian 13.23-1.pgdg13+1)
-- Dumped by pg_dump version 14.18 (Homebrew)

-- Started on 2025-11-27 11:21:54 CET

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

DROP DATABASE IF EXISTS single_tenant;
--
-- TOC entry 3277 (class 1262 OID 16897)
-- Name: single_tenant; Type: DATABASE; Schema: -; Owner: app_user
--

CREATE DATABASE single_tenant WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE = 'en_US.utf8';


ALTER DATABASE single_tenant OWNER TO app_user;

\connect single_tenant

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
-- TOC entry 2 (class 3079 OID 17025)
-- Name: hstore; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;


--
-- TOC entry 3278 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION hstore; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION hstore IS 'data type for storing sets of (key, value) pairs';


--
-- TOC entry 710 (class 1247 OID 16962)
-- Name: admin_role; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.admin_role AS ENUM (
    'admin',
    'support',
    'owner'
);


ALTER TYPE public.admin_role OWNER TO app_user;

--
-- TOC entry 707 (class 1247 OID 16957)
-- Name: admin_status; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.admin_status AS ENUM (
    'active',
    'suspended'
);


ALTER TYPE public.admin_status OWNER TO app_user;

--
-- TOC entry 717 (class 1247 OID 16987)
-- Name: audit_actor_type; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.audit_actor_type AS ENUM (
    'user',
    'admin'
);


ALTER TYPE public.audit_actor_type OWNER TO app_user;

--
-- TOC entry 724 (class 1247 OID 17004)
-- Name: login_principal_type; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.login_principal_type AS ENUM (
    'user',
    'admin'
);


ALTER TYPE public.login_principal_type OWNER TO app_user;

--
-- TOC entry 697 (class 1247 OID 16918)
-- Name: user_role; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.user_role AS ENUM (
    'admin',
    'member',
    'viewer'
);


ALTER TYPE public.user_role OWNER TO app_user;

--
-- TOC entry 694 (class 1247 OID 16910)
-- Name: user_status; Type: TYPE; Schema: public; Owner: app_user
--

CREATE TYPE public.user_status AS ENUM (
    'active',
    'inactive',
    'suspended'
);


ALTER TYPE public.user_status OWNER TO app_user;

--
-- TOC entry 278 (class 1255 OID 17152)
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
-- TOC entry 205 (class 1259 OID 16969)
-- Name: admins; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.admins (
    role public.admin_role DEFAULT 'admin'::public.admin_role NOT NULL,
    updated_at timestamp with time zone DEFAULT '2025-11-25 09:30:12.701341+00'::timestamp with time zone,
    email character varying(255) NOT NULL,
    password_hash text NOT NULL,
    last_login_at timestamp with time zone,
    status public.admin_status DEFAULT 'active'::public.admin_status NOT NULL,
    id uuid NOT NULL,
    full_name character varying(255),
    created_at timestamp with time zone DEFAULT '2025-11-25 09:30:12.701341+00'::timestamp with time zone,
    CONSTRAINT admins_email_check CHECK (((email)::text ~* '^[^\s@]+@[^\s@]+\.[^\s@]+$'::text))
);


ALTER TABLE public.admins OWNER TO app_user;

--
-- TOC entry 206 (class 1259 OID 16991)
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
    created_at timestamp with time zone DEFAULT '2025-11-25 09:30:12.701341+00'::timestamp with time zone,
    metadata jsonb
);


ALTER TABLE public.audit_logs OWNER TO app_user;

--
-- TOC entry 202 (class 1259 OID 16900)
-- Name: automigrate_migrations; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.automigrate_migrations (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.automigrate_migrations OWNER TO app_user;

--
-- TOC entry 201 (class 1259 OID 16898)
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
-- TOC entry 3279 (class 0 OID 0)
-- Dependencies: 201
-- Name: automigrate_migrations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: app_user
--

ALTER SEQUENCE public.automigrate_migrations_id_seq OWNED BY public.automigrate_migrations.id;


--
-- TOC entry 204 (class 1259 OID 16941)
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
    created_at timestamp with time zone DEFAULT '2025-11-25 09:30:12.701341+00'::timestamp with time zone
);


ALTER TABLE public.email_verification_tokens OWNER TO app_user;

--
-- TOC entry 207 (class 1259 OID 17009)
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
    created_at timestamp with time zone DEFAULT '2025-11-25 09:30:12.701341+00'::timestamp with time zone
);


ALTER TABLE public.login_events OWNER TO app_user;

--
-- TOC entry 203 (class 1259 OID 16925)
-- Name: users; Type: TABLE; Schema: public; Owner: app_user
--

CREATE TABLE public.users (
    role public.user_role DEFAULT 'member'::public.user_role NOT NULL,
    updated_at timestamp with time zone DEFAULT '2025-11-25 09:30:12.701341+00'::timestamp with time zone,
    email character varying(255) NOT NULL,
    avatar_url text,
    password_hash text NOT NULL,
    provider_user_id text,
    last_login_at timestamp with time zone,
    status public.user_status DEFAULT 'active'::public.user_status NOT NULL,
    id uuid NOT NULL,
    full_name character varying(255),
    auth_provider text DEFAULT 'password'::text,
    created_at timestamp with time zone DEFAULT '2025-11-25 09:30:12.701341+00'::timestamp with time zone,
    CONSTRAINT users_email_check CHECK (((email)::text ~* '^[^\s@]+@[^\s@]+\.[^\s@]+$'::text))
);


ALTER TABLE public.users OWNER TO app_user;

--
-- TOC entry 3082 (class 2604 OID 16903)
-- Name: automigrate_migrations id; Type: DEFAULT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.automigrate_migrations ALTER COLUMN id SET DEFAULT nextval('public.automigrate_migrations_id_seq'::regclass);


--
-- TOC entry 3269 (class 0 OID 16969)
-- Dependencies: 205
-- Data for Name: admins; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.admins (role, updated_at, email, password_hash, last_login_at, status, id, full_name, created_at) FROM stdin;
owner	2025-11-25 12:39:30.361252+00	admin@example.com	bcrypt+sha512$604531f02285a11cf0d3612b67bcb199$12$a8924fc3552ff2cb30d6743f13fa7d053cf86d2c2e025806	\N	active	a88373f4-fd85-491e-af4a-6572a5cf4f09	System Administrator	2025-11-25 12:39:30.361252+00
\.


--
-- TOC entry 3270 (class 0 OID 16991)
-- Dependencies: 206
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.audit_logs (ip, user_agent, actor_type, actor_id, target_id, target_type, id, action, created_at, metadata) FROM stdin;
\.


--
-- TOC entry 3266 (class 0 OID 16900)
-- Dependencies: 202
-- Data for Name: automigrate_migrations; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.automigrate_migrations (id, name, created_at) FROM stdin;
1	0001_schema	2025-11-25 10:30:12.779378
2	0002_enable_hstore_extension	2025-11-25 10:30:12.81355
3	0003_function_update-updated-at-column	2025-11-25 10:42:05.66498
4	0004_trigger_users-updated-at-trigger	2025-11-25 10:42:05.677509
5	0005_trigger_admins-updated-at-trigger	2025-11-25 10:42:05.687788
\.


--
-- TOC entry 3268 (class 0 OID 16941)
-- Dependencies: 204
-- Data for Name: email_verification_tokens; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.email_verification_tokens (id, user_id, token, expires_at, attempts, last_attempted_at, used_at, created_at) FROM stdin;
\.


--
-- TOC entry 3271 (class 0 OID 17009)
-- Dependencies: 207
-- Data for Name: login_events; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.login_events (id, principal_type, principal_id, success, reason, ip, user_agent, created_at) FROM stdin;
6fcf7e3e-6857-42a7-8c04-63e91ee10b58	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 14:09:24.614458+00
4e1d5a26-d8f9-42db-9395-ec3dc7873e4b	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 14:34:48.95809+00
f4375788-6d3e-435f-ae1c-6ebe28d768be	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 14:44:49.80888+00
b14d1ea2-2f9b-40cb-9ec5-38b4c3d232a8	user	a55fef53-f21e-4860-8c73-70bf7dc2ce57	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 14:52:06.684173+00
59fac204-30b6-4682-83c1-a86d712268a5	user	a55fef53-f21e-4860-8c73-70bf7dc2ce57	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 14:52:28.827988+00
bdb2cad2-18a6-4eec-b403-8b28ce52f70a	user	a55fef53-f21e-4860-8c73-70bf7dc2ce57	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 14:54:44.933999+00
f23abcb2-f236-430e-99d2-da158d7988e8	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 15:25:19.415627+00
edfe304c-bed3-4946-b734-2f039eddbcff	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-25 15:43:18.493309+00
6905a9fc-6395-4158-a36d-b80fa64beb64	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-26 13:26:42.777269+00
b790ad05-b7e5-45ac-863c-1a04ff4dbbc7	user	a55fef53-f21e-4860-8c73-70bf7dc2ce57	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-26 13:26:59.246244+00
4edcf88e-623a-4176-84e6-6b6f12f5dc49	user	a55fef53-f21e-4860-8c73-70bf7dc2ce57	t	oauth-google	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-26 13:32:06.179775+00
67b5357f-2c45-4d52-b2ba-21eaaefa8a36	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-26 13:32:14.369407+00
6bfd147f-ed13-4fad-8fe4-476fe3381936	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-27 08:31:57.026908+00
4c4ab02f-3837-4825-8a2e-a92fcf52e3cf	admin	a88373f4-fd85-491e-af4a-6572a5cf4f09	t	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-11-27 08:58:13.026957+00
\.


--
-- TOC entry 3267 (class 0 OID 16925)
-- Dependencies: 203
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: app_user
--

COPY public.users (role, updated_at, email, avatar_url, password_hash, provider_user_id, last_login_at, status, id, full_name, auth_provider, created_at) FROM stdin;
admin	2025-11-26 13:32:06.174373+00	enes.jakic@gmail.com	https://lh3.googleusercontent.com/a/ACg8ocJjYpNDm22dpDu9NR2QZzYt3lKysTwYIV94ucrm232EdtZmLA=s96-c	hashed-oauth-google-user	107292161221034741932	\N	active	a55fef53-f21e-4860-8c73-70bf7dc2ce57	Enes Jakić	google	2025-11-25 12:48:13.495404+00
\.


--
-- TOC entry 3280 (class 0 OID 0)
-- Dependencies: 201
-- Name: automigrate_migrations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: app_user
--

SELECT pg_catalog.setval('public.automigrate_migrations_id_seq', 5, true);


--
-- TOC entry 3119 (class 2606 OID 16983)
-- Name: admins admins_email_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_email_key UNIQUE (email);


--
-- TOC entry 3121 (class 2606 OID 16981)
-- Name: admins admins_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_pkey PRIMARY KEY (id);


--
-- TOC entry 3125 (class 2606 OID 16999)
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- TOC entry 3100 (class 2606 OID 16908)
-- Name: automigrate_migrations automigrate_migrations_name_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.automigrate_migrations
    ADD CONSTRAINT automigrate_migrations_name_key UNIQUE (name);


--
-- TOC entry 3102 (class 2606 OID 16906)
-- Name: automigrate_migrations automigrate_migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.automigrate_migrations
    ADD CONSTRAINT automigrate_migrations_pkey PRIMARY KEY (id);


--
-- TOC entry 3112 (class 2606 OID 16947)
-- Name: email_verification_tokens email_verification_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 3114 (class 2606 OID 16949)
-- Name: email_verification_tokens email_verification_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_token_key UNIQUE (token);


--
-- TOC entry 3131 (class 2606 OID 17017)
-- Name: login_events login_events_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.login_events
    ADD CONSTRAINT login_events_pkey PRIMARY KEY (id);


--
-- TOC entry 3108 (class 2606 OID 16940)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 3110 (class 2606 OID 16938)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 3122 (class 1259 OID 16984)
-- Name: idx_admins_email; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_admins_email ON public.admins USING btree (email);


--
-- TOC entry 3123 (class 1259 OID 17002)
-- Name: idx_admins_status; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_admins_status ON public.admins USING btree (status);


--
-- TOC entry 3126 (class 1259 OID 17020)
-- Name: idx_audit_logs_actor; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_audit_logs_actor ON public.audit_logs USING btree (actor_type, actor_id);


--
-- TOC entry 3127 (class 1259 OID 17000)
-- Name: idx_audit_logs_created_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_audit_logs_created_at ON public.audit_logs USING btree (created_at);


--
-- TOC entry 3115 (class 1259 OID 17022)
-- Name: idx_email_tokens_expires; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_email_tokens_expires ON public.email_verification_tokens USING btree (expires_at);


--
-- TOC entry 3116 (class 1259 OID 16955)
-- Name: idx_email_tokens_token; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_email_tokens_token ON public.email_verification_tokens USING btree (token);


--
-- TOC entry 3117 (class 1259 OID 17019)
-- Name: idx_email_tokens_user; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_email_tokens_user ON public.email_verification_tokens USING btree (user_id);


--
-- TOC entry 3128 (class 1259 OID 17021)
-- Name: idx_login_events_created_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_login_events_created_at ON public.login_events USING btree (created_at);


--
-- TOC entry 3129 (class 1259 OID 17018)
-- Name: idx_login_events_principal; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_login_events_principal ON public.login_events USING btree (principal_type, principal_id);


--
-- TOC entry 3103 (class 1259 OID 16985)
-- Name: idx_users_auth_provider_provider_user_id_external; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_users_auth_provider_provider_user_id_external ON public.users USING btree (auth_provider, provider_user_id) WHERE (auth_provider <> 'password'::text);


--
-- TOC entry 3104 (class 1259 OID 17024)
-- Name: idx_users_created_at; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_users_created_at ON public.users USING btree (created_at);


--
-- TOC entry 3105 (class 1259 OID 17001)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: app_user
--

CREATE UNIQUE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 3106 (class 1259 OID 17023)
-- Name: idx_users_status; Type: INDEX; Schema: public; Owner: app_user
--

CREATE INDEX idx_users_status ON public.users USING btree (status);


--
-- TOC entry 3134 (class 2620 OID 17154)
-- Name: admins admins_updated_at; Type: TRIGGER; Schema: public; Owner: app_user
--

CREATE TRIGGER admins_updated_at BEFORE UPDATE ON public.admins FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3133 (class 2620 OID 17153)
-- Name: users users_updated_at; Type: TRIGGER; Schema: public; Owner: app_user
--

CREATE TRIGGER users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3132 (class 2606 OID 16950)
-- Name: email_verification_tokens email_verification_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app_user
--

ALTER TABLE ONLY public.email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


-- Completed on 2025-11-27 11:21:54 CET

--
-- PostgreSQL database dump complete
--

