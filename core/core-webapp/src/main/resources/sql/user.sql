-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/user.sql $
-- $Id: user.sql 10866 2015-05-15 23:19:38Z ggolden $
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

DROP TABLE IF EXISTS USER;
DROP TABLE IF EXISTS USER_IID;

-- ---------------------------------------------------------------------------
-- USER
-- ---------------------------------------------------------------------------

CREATE TABLE USER
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  AVATAR           BIGINT UNSIGNED,
  AIM              VARCHAR (255),
  FACEBOOK         VARCHAR (255),
  GOOGLEPLUS       VARCHAR (255),
  LINKEDIN         VARCHAR (255),
  SKYPE            VARCHAR (255),
  TWITTER          VARCHAR (255),
  WEB              VARCHAR (2048),
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  EID              VARCHAR (255),
  EMAIL_EXPOSED    CHAR (1) NOT NULL DEFAULT '0' CHECK (EMAIL_EXPOSED IN ('0','1')),
  EMAIL_OFFICIAL   VARCHAR (255),
  EMAIL_USER       VARCHAR (255),
  PASSWORD         VARCHAR (255),
  NAME_FIRST       VARCHAR (255),
  NAME_LAST        VARCHAR (255),
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  INTERESTS        VARCHAR (255),
  LOCATION         VARCHAR (255),
  OCCUPATION       VARCHAR (255),
  ROSTER           CHAR (1) NOT NULL DEFAULT '0' CHECK (ROSTER IN ('0','1')),
  SIGNATURE        BIGINT UNSIGNED,
  TIMEZONE         VARCHAR (255),
  KEY USER_EID     (EID),
  KEY USER_EMAIL_O (EMAIL_OFFICIAL),
  KEY USER_EMAIL_U (EMAIL_USER)
);

CREATE TABLE USER_IID
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  USER_ID          BIGINT UNSIGNED,
  IID              VARCHAR (255),
  CODE             VARCHAR (12),
  KEY USER_IID_USER (USER_ID),
  UNIQUE KEY USER_IID_ID (CODE, IID)
 );

-- populate with the admin, helpdesk user
INSERT INTO USER (NAME_LAST, NAME_FIRST, EID, PASSWORD) VALUES
("Admin", "Etudes", "admin", "WiRKGUT+KR+/cdOfE8VuXw=="),
("Helpdesk", "Etudes", "helpdesk", "WiRKGUT+KR+/cdOfE8VuXw=="),
("System", "Etudes", "system", "WiRKGUT+KR+/cdOfE8VuXw==");
