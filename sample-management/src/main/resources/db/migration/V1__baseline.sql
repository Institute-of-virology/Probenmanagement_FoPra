--
-- PostgreSQL database dump
--

-- Dumped from database version 17.6 (Debian 17.6-1.pgdg13+1)
-- Dumped by pg_dump version 17.6-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: address_store; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.address_store (
    id bigint NOT NULL,
    address character varying(255),
    address_key character varying(255)
);


ALTER TABLE public.address_store OWNER TO postgres;

--
-- Name: address_store_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.address_store_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.address_store_seq OWNER TO postgres;

--
-- Name: analysis; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis (
    id bigint NOT NULL,
    comments character varying(255),
    analysis_date timestamp without time zone,
    analysis_result character varying(255),
    analysis_type_id bigint NOT NULL,
    sample_id bigint NOT NULL
);


ALTER TABLE public.analysis OWNER TO postgres;

--
-- Name: analysis_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.analysis_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.analysis_seq OWNER TO postgres;

--
-- Name: analysis_type; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.analysis_type (
    id bigint NOT NULL,
    analysis_description text,
    analysis_name character varying(255) NOT NULL,
    analysis_unit character varying(255),
    analysis_origin character varying(255)
);


ALTER TABLE public.analysis_type OWNER TO postgres;

--
-- Name: analysis_type_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.analysis_type_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.analysis_type_seq OWNER TO postgres;

--
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


ALTER TABLE public.databasechangelog OWNER TO postgres;

--
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


ALTER TABLE public.databasechangeloglock OWNER TO postgres;



--
-- Name: report_authors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.report_authors (
    id bigint NOT NULL,
    name character varying(255),
    title character varying(255)
);


ALTER TABLE public.report_authors OWNER TO postgres;

--
-- Name: report_authors_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.report_authors_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.report_authors_seq OWNER TO postgres;

--
-- Name: sample; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sample (
    id bigint NOT NULL,
    coordinates character varying(255),
    sample_date timestamp without time zone,
    sample_amount character varying(255),
    sample_barcode character varying(255),
    sample_type character varying(255),
    validated boolean NOT NULL,
    visits integer NOT NULL,
    sample_delivery_id bigint NOT NULL,
    study_id bigint NOT NULL,
    subject_id bigint NOT NULL,
    date_of_shipment timestamp without time zone,
    validated_at timestamp without time zone
);


ALTER TABLE public.sample OWNER TO postgres;

--
-- Name: sample_delivery; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sample_delivery (
    id bigint NOT NULL,
    box_number character varying(255),
    delivery_date date,
    study_id bigint
);


ALTER TABLE public.sample_delivery OWNER TO postgres;

--
-- Name: sample_delivery_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.sample_delivery ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.sample_delivery_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: sample_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.sample ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.sample_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: study; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.study (
    id bigint NOT NULL,
    end_date date,
    expected_number_of_sample_deliveries character varying(255),
    expected_number_of_subjects character varying(255),
    remark character varying(255),
    sender1 character varying(255),
    sender2 character varying(255),
    sender3 character varying(255),
    sponsor character varying(255),
    start_date date,
    study_name character varying(255) NOT NULL
);


ALTER TABLE public.study OWNER TO postgres;

--
-- Name: study_analysis_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.study_analysis_types (
    study_id bigint NOT NULL,
    analysis_types_id bigint NOT NULL
);


ALTER TABLE public.study_analysis_types OWNER TO postgres;

--
-- Name: study_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.study_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.study_seq OWNER TO postgres;

--
-- Name: subject; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.subject (
    id bigint NOT NULL,
    alias bigint NOT NULL,
    study_id bigint NOT NULL
);


ALTER TABLE public.subject OWNER TO postgres;

--
-- Name: subject_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.subject ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.subject_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    roles character varying(255)
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    email character varying(255) NOT NULL,
    enabled boolean NOT NULL,
    last_login timestamp with time zone,
    otp_token character varying(255),
    otp_token_expiry timestamp with time zone,
    password character varying(255),
    username character varying(255) NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.users ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: address_store address_store_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.address_store
    ADD CONSTRAINT address_store_pkey PRIMARY KEY (id);


--
-- Name: analysis analysis_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT analysis_pkey PRIMARY KEY (id);


--
-- Name: analysis_type analysis_type_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_type
    ADD CONSTRAINT analysis_type_pkey PRIMARY KEY (id);


--
-- Name: databasechangeloglock databasechangeloglock_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);



--
-- Name: report_authors report_authors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_authors
    ADD CONSTRAINT report_authors_pkey PRIMARY KEY (id);


--
-- Name: sample_delivery sample_delivery_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample_delivery
    ADD CONSTRAINT sample_delivery_pkey PRIMARY KEY (id);


--
-- Name: sample sample_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT sample_pkey PRIMARY KEY (id);


--
-- Name: study_analysis_types study_analysis_types_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.study_analysis_types
    ADD CONSTRAINT study_analysis_types_pkey PRIMARY KEY (study_id, analysis_types_id);


--
-- Name: study study_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.study
    ADD CONSTRAINT study_pkey PRIMARY KEY (id);


--
-- Name: subject subject_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subject
    ADD CONSTRAINT subject_pkey PRIMARY KEY (id);


--
-- Name: study uk214saucfr6b9hv3hhqqckjbbe; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.study
    ADD CONSTRAINT uk214saucfr6b9hv3hhqqckjbbe UNIQUE (study_name);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: address_store uk7nhqxxjia1nj4nudqau7oxago; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.address_store
    ADD CONSTRAINT uk7nhqxxjia1nj4nudqau7oxago UNIQUE (address_key);


--
-- Name: subject ukab3m38ainea1d51txmj0ycpd5; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subject
    ADD CONSTRAINT ukab3m38ainea1d51txmj0ycpd5 UNIQUE (alias, study_id);


--
-- Name: analysis_type ukca76v9n2xv9twdr7mcfvfieag; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis_type
    ADD CONSTRAINT ukca76v9n2xv9twdr7mcfvfieag UNIQUE (analysis_name);


--
-- Name: sample ukd3aahl87fayfy2v7l6cbsnp1u; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT ukd3aahl87fayfy2v7l6cbsnp1u UNIQUE (sample_barcode, study_id);


--
-- Name: users ukr43af9ap4edm43mmtq01oddj6; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);



--
-- Name: analysis fk1t32pjnbnid69ppq5f25bam0r; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT fk1t32pjnbnid69ppq5f25bam0r FOREIGN KEY (sample_id) REFERENCES public.sample(id);


--
-- Name: sample_delivery fk2t40qjnvuqs14viarlnhvxsyv; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample_delivery
    ADD CONSTRAINT fk2t40qjnvuqs14viarlnhvxsyv FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: study_analysis_types fk3n652x8l0vgbrr63mxv0nmgk7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.study_analysis_types
    ADD CONSTRAINT fk3n652x8l0vgbrr63mxv0nmgk7 FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: sample fk4dan4xtkevyamd49gy0ch1ek5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT fk4dan4xtkevyamd49gy0ch1ek5 FOREIGN KEY (subject_id) REFERENCES public.subject(id);


--
-- Name: sample fk9xnu0qrl111upir1bo9r80q51; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT fk9xnu0qrl111upir1bo9r80q51 FOREIGN KEY (sample_delivery_id) REFERENCES public.sample_delivery(id);


--
-- Name: analysis fkd5b84a621ulrsrxrtildhqg9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.analysis
    ADD CONSTRAINT fkd5b84a621ulrsrxrtildhqg9 FOREIGN KEY (analysis_type_id) REFERENCES public.analysis_type(id);


--
-- Name: subject fkfpft5emcv1sy9yurbavia0vjf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.subject
    ADD CONSTRAINT fkfpft5emcv1sy9yurbavia0vjf FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: user_roles fkhfh9dx7w3ubf1co1vdev94g3f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: sample fkkbdme00c5ep0xx088cjvgh4i7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sample
    ADD CONSTRAINT fkkbdme00c5ep0xx088cjvgh4i7 FOREIGN KEY (study_id) REFERENCES public.study(id);


--
-- Name: study_analysis_types fkpank7cofce0g5g10it3h5sarn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.study_analysis_types
    ADD CONSTRAINT fkpank7cofce0g5g10it3h5sarn FOREIGN KEY (analysis_types_id) REFERENCES public.analysis_type(id);


--
-- PostgreSQL database dump complete
--