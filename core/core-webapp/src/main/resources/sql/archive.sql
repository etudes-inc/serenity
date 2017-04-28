-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/archive.sql $
-- $Id: archive.sql 10052 2015-02-10 04:32:42Z ggolden $
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

DROP TABLE IF EXISTS ARCHIVE;
DROP TABLE IF EXISTS ARCHIVE_OWNER;

-- ---------------------------------------------------------------------------
-- ARCHIVE
-- ---------------------------------------------------------------------------

CREATE TABLE ARCHIVE
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  CLIENT_ID        BIGINT UNSIGNED NOT NULL,
  TERM_ID          BIGINT UNSIGNED NOT NULL,
  ARCHIVED_ON      BIGINT NOT NULL,
  NAME             VARCHAR (128),
  UNIQUE KEY ARCHIVE_S (SITE_ID)
);

CREATE TABLE ARCHIVE_OWNER
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  ARCHIVE_ID       BIGINT UNSIGNED NOT NULL,
  USER_ID          BIGINT UNSIGNED NOT NULL,
  KEY ARCHIVE_OWNER_U (USER_ID) 
);
