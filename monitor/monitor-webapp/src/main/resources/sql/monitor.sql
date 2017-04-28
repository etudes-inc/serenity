-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/monitor/monitor-webapp/src/main/resources/sql/monitor.sql $
-- $Id: monitor.sql 12036 2015-11-08 01:49:32Z ggolden $
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
-- MONITOR
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS MONITOR_OPTIONS;

CREATE TABLE MONITOR_OPTIONS
(
  OPEN_FILES           INT UNSIGNED NOT NULL,
  OPEN_APACHE          INT UNSIGNED NOT NULL,
  OPEN_MYSQL           INT UNSIGNED NOT NULL,
  SINCE_REPORT         BIGINT UNSIGNED NOT NULL,
  SINCE_DB_BACKUP      BIGINT UNSIGNED NOT NULL,
  SINCE_FS_BACKUP      BIGINT UNSIGNED NOT NULL,
  SINCE_OFFSITE_BACKUP BIGINT UNSIGNED NOT NULL,
  LOAD_AVG             FLOAT NOT NULL,
  DISK_USED_PCT        INT UNSIGNED NOT NULL,
  APPSERVER_RESPONSE   FLOAT NOT NULL,
  QUERIES_TOTAL        INT UNSIGNED NOT NULL,
  QUERIES_ACTIVE       INT UNSIGNED NOT NULL
);

INSERT INTO MONITOR_OPTIONS (OPEN_FILES, OPEN_APACHE, OPEN_MYSQL, SINCE_REPORT, SINCE_DB_BACKUP, SINCE_FS_BACKUP, SINCE_OFFSITE_BACKUP, LOAD_AVG, DISK_USED_PCT, APPSERVER_RESPONSE, QUERIES_TOTAL, QUERIES_ACTIVE) VALUES
(
    1500,
    200,
    20,
    120000, -- 2l * 60l * 1000l
    86400000, -- 24l * 60l * 60l * 1000l
    7200000, -- 2l * 60l * 60l * 1000l
    86400000, -- 24l * 60l * 60l * 1000l
    5.0,
    90,
    10.0,
    180,
    20
);
