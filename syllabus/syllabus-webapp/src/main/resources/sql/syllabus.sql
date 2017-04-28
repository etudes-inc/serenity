-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/syllabus/syllabus-webapp/src/main/resources/sql/syllabus.sql $
-- $Id: syllabus.sql 9981 2015-02-01 20:34:07Z ggolden $
-- **********************************************************************************
--
-- Copyright (c) 2015 Etudes, Inc.
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

-- ---------------------------------------------------------------------------
-- SYLLABUS
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS SYLLABUS;
DROP TABLE IF EXISTS SYLLABUS_SECTION;
DROP TABLE IF EXISTS SYLLABUS_ACCEPTANCE;

CREATE TABLE SYLLABUS
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE             BIGINT UNSIGNED NOT NULL,
  SOURCE           CHAR (1) NOT NULL CHECK (SOURCE IN ('S','E')),
  URL              VARCHAR (2048),
  HEIGHT           INT,
  NEWWINDOW        CHAR (1) NOT NULL CHECK (NEWWINDOW IN ('0','1')),
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  KEY SYLLABUS_S   (SITE)
);

CREATE TABLE SYLLABUS_SECTION
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SYLLABUS_ID      BIGINT UNSIGNED NOT NULL,
  TITLE            VARCHAR (255),
  CONTENT          BIGINT UNSIGNED,
  PUBLISHED        CHAR (1) NOT NULL CHECK (PUBLISHED IN ('0','1')),
  ISPUBLIC         CHAR (1) NOT NULL CHECK (ISPUBLIC IN ('0','1')),
  SECTIONORDER     BIGINT UNSIGNED,
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  KEY SYLLABUS_SECTION_S (SYLLABUS_ID)
);

CREATE TABLE SYLLABUS_ACCEPTANCE
(
  SYLLABUS_ID      BIGINT UNSIGNED NOT NULL,
  ACCEPTED_BY      BIGINT UNSIGNED,
  ACCEPTED_ON      BIGINT,
  UNIQUE KEY SYLLABUS_ACCEPTANCE_P (SYLLABUS_ID, ACCEPTED_BY)
);
