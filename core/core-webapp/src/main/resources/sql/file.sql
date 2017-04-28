-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/file.sql $
-- $Id: file.sql 10058 2015-02-11 03:31:49Z ggolden $
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

DROP TABLE IF EXISTS FILE;
DROP TABLE IF EXISTS FILE_REFERENCE;

-- ---------------------------------------------------------------------------
-- FILE
-- ---------------------------------------------------------------------------

CREATE TABLE FILE
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  DATE             BIGINT UNSIGNED NOT NULL,
  MODIFIEDON       BIGINT UNSIGNED NOT NULL,
  NAME             VARCHAR (255) NOT NULL,
  SIZE             BIGINT UNSIGNED,
  TYPE             VARCHAR (255)
);

CREATE TABLE FILE_REFERENCE
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  TOOL_ID          INT UNSIGNED NOT NULL,
  ITEM_ID          BIGINT UNSIGNED NOT NULL,
  FILE_ID          BIGINT UNSIGNED NOT NULL,
  SECURITY         TINYINT NOT NULL,
  KEY FILE_REFERENCE_F (FILE_ID),
  KEY FILE_REFERENCE_STE (SITE_ID, TOOL_ID, ITEM_ID)
);
