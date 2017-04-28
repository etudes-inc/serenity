-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/tracking.sql $
-- $Id: tracking.sql 10165 2015-02-26 23:24:48Z ggolden $
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

DROP TABLE IF EXISTS TRACKING;
DROP TABLE IF EXISTS PRESENCE;

-- ---------------------------------------------------------------------------
-- TRACKING
-- ---------------------------------------------------------------------------

CREATE TABLE TRACKING
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  USER_ID          BIGINT UNSIGNED NOT NULL,
  FIRST_VISIT      BIGINT NOT NULL,
  LAST_VISIT       BIGINT NOT NULL,
  VISITS           BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY TRACKING_KEY (SITE_ID, USER_ID)
);

-- SELECT ID, SITE_ID, USER_ID, FROM_UNIXTIME(FIRST_VISIT/1000) AS FIRST_VISIT, FROM_UNIXTIME(LAST_VISIT/1000) AS LAST_VISIT, VISITS FROM TRACKING ORDER BY SITE_ID, USER_ID;

-- ---------------------------------------------------------------------------
-- PRESENCE
-- ---------------------------------------------------------------------------

CREATE TABLE PRESENCE
(
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  TOOL_ID          BIGINT UNSIGNED NOT NULL,
  ITEM_ID          BIGINT UNSIGNED NOT NULL,
  USER_ID          BIGINT UNSIGNED NOT NULL,
  ASOF             BIGINT NOT NULL,
  PRIMARY KEY      (SITE_ID, TOOL_ID, ITEM_ID, USER_ID)
);

-- SELECT SITE_ID, TOOL_ID, ITEM_ID, USER_ID, FROM_UNIXTIME(ASOF/1000) AS ASOF FROM PRESENCE ORDER BY SITE_ID, TOOL_ID, ITEM_ID, USER_ID;
