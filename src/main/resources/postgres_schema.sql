--
-- PostgreSQL database dump
--

-- Dumped from database version 10.6 (Ubuntu 10.6-0ubuntu0.18.04.1)
-- Dumped by pg_dump version 11.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: businesses; Type: TABLE; Schema: public; Owner: sysdba
--

CREATE TABLE public.businesses
(
  id           character varying(32) NOT NULL,
  name         character varying(128),
  lat          double precision,
  lng          double precision,
  city         character varying(128),
  stars        double precision,
  review_count integer,
  address      character varying(128),
  postal_code  character varying(64),
  categories   character varying(1024)
);


ALTER TABLE public.businesses
  OWNER TO sysdba;

--
-- Name: reviews; Type: TABLE; Schema: public; Owner: sysdba
--

CREATE TABLE public.reviews
(
  id          character varying(32) NOT NULL,
  business_id character varying(32),
  stars       integer,
  date        date,
  text        text,
  useful      integer,
  funny       integer,
  cool        integer
);


ALTER TABLE public.reviews
  OWNER TO sysdba;

--
-- Name: tips; Type: TABLE; Schema: public; Owner: sysdba
--

CREATE TABLE public.tips
(
  id               bigint NOT NULL,
  text             text,
  date             date,
  compliment_count integer,
  business_id      character varying(32)
);


ALTER TABLE public.tips
  OWNER TO sysdba;

--
-- Name: tips_id_seq; Type: SEQUENCE; Schema: public; Owner: sysdba
--

CREATE SEQUENCE public.tips_id_seq
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;


ALTER TABLE public.tips_id_seq
  OWNER TO sysdba;

--
-- Name: tips_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sysdba
--

ALTER SEQUENCE public.tips_id_seq OWNED BY public.tips.id;


--
-- Name: tips id; Type: DEFAULT; Schema: public; Owner: sysdba
--

ALTER TABLE ONLY public.tips
  ALTER COLUMN id SET DEFAULT nextval('public.tips_id_seq'::regclass);


--
-- Name: businesses businesses_pkey; Type: CONSTRAINT; Schema: public; Owner: sysdba
--

ALTER TABLE ONLY public.businesses
  ADD CONSTRAINT businesses_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: sysdba
--

ALTER TABLE ONLY public.reviews
  ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: tips tips_pkey; Type: CONSTRAINT; Schema: public; Owner: sysdba
--

ALTER TABLE ONLY public.tips
  ADD CONSTRAINT tips_pkey PRIMARY KEY (id);


--
-- Name: businesses_address_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX businesses_address_idx ON public.businesses USING btree (address);


--
-- Name: businesses_categories_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX businesses_categories_idx ON public.businesses USING btree (categories);


--
-- Name: businesses_city_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX businesses_city_idx ON public.businesses USING btree (city);


--
-- Name: businesses_name_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX businesses_name_idx ON public.businesses USING btree (name);


--
-- Name: businesses_postal_code_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX businesses_postal_code_idx ON public.businesses USING btree (postal_code);


--
-- Name: businesses_review_count_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX businesses_review_count_idx ON public.businesses USING btree (review_count);


--
-- Name: businesses_stars_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX businesses_stars_idx ON public.businesses USING btree (stars);


--
-- Name: reviews_business_id_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX reviews_business_id_idx ON public.reviews USING btree (business_id);


--
-- Name: reviews_date_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX reviews_date_idx ON public.reviews USING btree (date);


--
-- Name: tips_business_id_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX tips_business_id_idx ON public.tips USING btree (business_id);


--
-- Name: tips_date_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX tips_date_idx ON public.tips USING btree (date);


--
-- Name: tips_text_idx; Type: INDEX; Schema: public; Owner: sysdba
--

CREATE INDEX tips_text_idx ON public.tips USING btree (text);


--
-- Name: reviews reviews_business_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sysdba
--

ALTER TABLE ONLY public.reviews
  ADD CONSTRAINT reviews_business_id_fkey FOREIGN KEY (business_id) REFERENCES public.businesses (id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: tips tips_business_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sysdba
--

ALTER TABLE ONLY public.tips
  ADD CONSTRAINT tips_business_id_fkey FOREIGN KEY (business_id) REFERENCES public.businesses (id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

