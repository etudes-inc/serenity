-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/authentication.sql $
-- $Id: authentication.sql 10165 2015-02-26 23:24:48Z ggolden $
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

DROP TABLE IF EXISTS AUTHENTICATION;
DROP TABLE IF EXISTS AUTHENTICATION_BROWSER;

-- ---------------------------------------------------------------------------
-- AUTHENTICATION
-- ---------------------------------------------------------------------------

CREATE TABLE AUTHENTICATION
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  USER_ID          BIGINT UNSIGNED NOT NULL,
  AUTHENTICATED_ON BIGINT NOT NULL,
  RENEWED_ON       BIGINT NULL,
  IP               BINARY (16) NOT NULL,
  BROWSER_ID       BIGINT UNSIGNED NOT NULL,
  CLOSED           CHAR (1) NOT NULL DEFAULT '0' CHECK (CLOSED IN ('0','1')),
  KEY AUTHENTICATION_U (USER_ID)
);

-- SELECT A.ID, USER_ID, FROM_UNIXTIME(AUTHENTICATED_ON/1000) AS AUTHENTICATED_ON, FROM_UNIXTIME(RENEWED_ON/1000) AS RENEWED_ON, HEX(IP) AS IP, CLOSED, BROWSER_ID, B.USER_AGENT FROM AUTHENTICATION A JOIN AUTHENTICATION_BROWSER B ON A.BROWSER_ID = B.ID;

CREATE TABLE AUTHENTICATION_BROWSER
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  USER_AGENT       VARCHAR (255),
  UNIQUE KEY AUTHENTICATION_BROWSER_AGENT (USER_AGENT)
);
