-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/social/social-webapp/src/main/resources/sql/social.sql $
-- $Id: social.sql 11103 2015-06-13 04:19:29Z ggolden $
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

-- ---------------------------------------------------------------------------
-- HOME
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS HOME;

CREATE TABLE BLOG
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE             BIGINT UNSIGNED NOT NULL,
  OWNER            BIGINT UNSIGNED NOT NULL,
  NAME             VARCHAR (255),
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  KEY BLOG_S_O     (SITE, OWNER)
);

CREATE TABLE BLOG_ENTRY
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  BLOG_ID          BIGINT UNSIGNED NOT NULL,
  TITLE            VARCHAR (255),
  CONTENTS         BIGINT UNSIGNED,
  IMAGE            BIGINT UNSIGNED,
  PUBLISHED        CHAR (1) NOT NULL CHECK (PUBLISHED IN ('0','1')),
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  KEY BLOG_ENTRY_B (BLOG_ID)
);
