-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/blog/blog-webapp/src/main/resources/sql/blog.sql $
-- $Id: blog.sql 10060 2015-02-11 22:02:24Z ggolden $
-- **********************************************************************************
--
-- Copyright (c) 2014, 2015 Etudes, Inc.
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
-- BLOG
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS BLOG_ENTRY;
DROP TABLE IF EXISTS BLOG;

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
  CONTENT          BIGINT UNSIGNED,
  IMAGE            BIGINT UNSIGNED,
  PUBLISHED        CHAR (1) NOT NULL CHECK (PUBLISHED IN ('0','1')),
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  KEY BLOG_ENTRY_B (BLOG_ID)
);
