-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/announcement/announcement-webapp/src/main/resources/sql/annoumcement.sql $
-- $Id: annoumcement.sql 10482 2015-04-16 02:56:07Z ggolden $
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
-- ANNOUNCEMENT
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS ANNOUNCEMENT;

CREATE TABLE ANNOUNCEMENT
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE             BIGINT UNSIGNED NOT NULL,
  SUBJECT          VARCHAR (255),
  CONTENT          BIGINT UNSIGNED,
  PUBLISHED        CHAR (1) NOT NULL CHECK (PUBLISHED IN ('0','1')),
  ISPUBLIC         CHAR (1) NOT NULL CHECK (ISPUBLIC IN ('0','1')),
  RELEASE_ON       BIGINT,
  SITEORDER        BIGINT UNSIGNED,
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  KEY ANNOUNCEMENT_S   (SITE)
);
