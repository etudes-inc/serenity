-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/roster.sql $
-- $Id: roster.sql 12553 2016-01-14 20:03:28Z ggolden $
-- **********************************************************************************
--
-- Copyright (c) 2014 Etudes, Inc.
-- 
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--      http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
-- *********************************************************************************/

DROP TABLE IF EXISTS ROSTER;
DROP TABLE IF EXISTS ROSTER_MEMBER;
DROP TABLE IF EXISTS ROSTER_SITE;
DROP TABLE IF EXISTS ROSTER_BLOCKED;
DROP TABLE IF EXISTS CLIENT;
DROP TABLE IF EXISTS TERM;

-- ---------------------------------------------------------------------------
-- ROSTER
-- ---------------------------------------------------------------------------

CREATE TABLE ROSTER
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  OFFICIAL         CHAR (1) NOT NULL DEFAULT '0' CHECK (OFFICIAL IN ('0','1')),
  NAME             VARCHAR (128),
  CLIENT_ID        BIGINT UNSIGNED,
  TERM_ID          BIGINT UNSIGNED,
  UNIQUE KEY ROSTER_IDENT (CLIENT_ID, TERM_ID, NAME)
);

CREATE TABLE ROSTER_MEMBER
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  ROSTER_ID        BIGINT UNSIGNED NOT NULL,
  USER_ID          BIGINT UNSIGNED NOT NULL,
  ROLE             TINYINT NOT NULL,
  ACTIVE           CHAR (1) NOT NULL DEFAULT '0' CHECK (ACTIVE IN ('0','1')),
  UNIQUE KEY ROSTER_MEMBER_UR (USER_ID, ROSTER_ID),
  KEY ROSTER_MEMBER_R (ROSTER_ID)
);

CREATE TABLE ROSTER_SITE
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  ROSTER_ID        BIGINT UNSIGNED NOT NULL,
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY ROSTER_SITE_RS (ROSTER_ID, SITE_ID),
  KEY ROSTER_SITE_S (SITE_ID)
);

CREATE TABLE ROSTER_BLOCKED
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  USER_ID          BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY ROSTER_BLOCKED_UNIQUE (SITE_ID, USER_ID)
);

-- ---------------------------------------------------------------------------
-- CLIENT
-- ---------------------------------------------------------------------------

CREATE TABLE CLIENT
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  NAME             VARCHAR (128),
  ABBREVIATION     VARCHAR (12),
  IID_CODE         VARCHAR (12)
);

-- populate client
INSERT INTO CLIENT (NAME, ABBREVIATION, IID_CODE) VALUES
('Etudes', 'ETU', 'etu'),
('Foothill College', 'FH', 'fh');

-- ---------------------------------------------------------------------------
-- TERM
-- ---------------------------------------------------------------------------

CREATE TABLE TERM
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  NAME             VARCHAR (12),
  ABBREVIATION     VARCHAR (12)
);

-- populate term
INSERT INTO TERM (NAME, ABBREVIATION) VALUES
('Unknown', 'UNK'),
('Deleted', 'XXX'),
('User', 'USR'),
('System', 'SYS'),
('Project', 'PRJ'),
('Development','DEV'),
('Winter 2006', 'W06'),
('Spring 2006', 'SP06'),
('Summer 2006', 'SU06'),
('Fall 2006', 'F06'),
('Winter 2007', 'W07'),
('Spring 2007', 'SP07'),
('Summer 2007', 'SU07'),
('Fall 2007', 'F07'),
('Winter 2008', 'W08'),
('Spring 2008', 'SP08'),
('Summer 2008', 'SU08'),
('Fall 2008', 'F08'),
('Winter 2009', 'W09'),
('Spring 2009', 'SP09'),
('Summer 2009', 'SU09'),
('Fall 2009', 'F09'),
('Winter 2010', 'W10'),
('Spring 2010', 'SP10'),
('Summer 2010', 'SU10'),
('Fall 2010', 'F10'),
('Winter 2011', 'W11'),
('Spring 2011', 'SP11'),
('Summer 2011', 'SU11'),
('Fall 2011', 'F11'),
('Winter 2012', 'W12'),
('Spring 2012', 'SP12'),
('Summer 2012', 'SU12'),
('Fall 2012', 'F12'),
('Winter 2013', 'W13'),
('Spring 2013', 'SP13'),
('Summer 2013', 'SU13'),
('Fall 2013', 'F13'),
('Winter 2014', 'W14'),
('Spring 2014', 'SP14'),
('Summer 2014', 'SU14'),
('Fall 2014', 'F14'),
('Winter 2015', 'W15'),
('Spring 2015', 'SP15'),
('Summer 2015', 'SU15'),
('Fall 2015', 'F15'),
('Winter 2016', 'W16'),
('Spring 2016', 'SP16'),
('Summer 2016', 'SU16'),
('Fall 2016', 'F16'),
('Winter 2017', 'W17'),
('Spring 2017', 'SP17'),
('Summer 2017', 'SU17'),
('Fall 2017', 'F17'),
('Winter 2018', 'W18'),
('Spring 2018', 'SP18'),
('Summer 2018', 'SU18'),
('Fall 2018', 'F18'),
('Winter 2019', 'W19'),
('Spring 2019', 'SP19'),
('Summer 2019', 'SU19'),
('Fall 2019', 'F19'),
('Winter 2020', 'W20'),
('Spring 2020', 'SP20'),
('Summer 2020', 'SU20'),
('Fall 2020', 'F20'),
('Winter 2021', 'W21'),
('Spring 2021', 'SP21'),
('Summer 2021', 'SU21'),
('Fall 2021', 'F21'),
('Winter 2022', 'W22'),
('Spring 2022', 'SP22'),
('Summer 2022', 'SU22'),
('Fall 2022', 'F22');

INSERT INTO ROSTER (OFFICIAL, NAME, CLIENT_ID, TERM_ID) VALUES
-- for admin site
('1', "Admin", 1, 4),
-- for monitor site, a master and adhoc roster
('1', "Master2", 1, 4),
('0', "Adhoc2", 1, 4),
-- for helpdesk site, a master and adhoc roster
('1', "Master3", 1, 4),
('0', "Adhoc3", 1, 4),
-- for user group site, a master and adhoc roster
('1', "Master4", 1, 4),
('0', "Adhoc4", 1, 4);

INSERT INTO ROSTER_SITE (ROSTER_ID, SITE_ID) VALUES
-- admin site, admin roster
(1, 1),
-- monitor site, monitor's master and adhoc rosters
(2, 2),
(3, 2),
-- helpdesk site, helpdesk's master and adhoc rosters
(4, 3),
(5, 3),
-- user group site, user group's master and adhoc rosters
(6, 4),
(7, 4);

INSERT INTO ROSTER_MEMBER (ROSTER_ID, USER_ID, ROLE, ACTIVE) VALUES
-- for admin's roster, admin user
(1, 1, 6, '1'),
-- for monitor's master roster, admin user
(2, 1, 6, '1'),
-- for helpdesk's master roster, admin and helpdesk users
(4, 1, 6, '1'),
(4, 2, 6, '1'),
-- for user group's master roster, admin and helpdesk users
(6, 1, 6, '1'),
(6, 2, 6, '1');
