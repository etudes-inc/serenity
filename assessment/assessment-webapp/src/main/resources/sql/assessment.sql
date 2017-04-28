-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/assessment/assessment-webapp/src/main/resources/sql/assessment.sql $
-- $Id: assessment.sql 11561 2015-09-06 00:45:58Z ggolden $
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
-- ASSESSMENT
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS ASMT_ASSESSMENT;
DROP TABLE IF EXISTS ASMT_SUBMISSION;
DROP TABLE IF EXISTS ASMT_ANSWER;

CREATE TABLE ASMT_ASSESSMENT
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE             BIGINT UNSIGNED NOT NULL,
  TITLE            VARCHAR (255),
  ASMTTYPE         CHAR (1) NOT NULL DEFAULT '0' CHECK (ASMTTYPE IN ('A','T','S','F','O','E')),
  OPEN             BIGINT UNSIGNED,
  DUE              BIGINT UNSIGNED,
  ALLOW            BIGINT UNSIGNED,
  HIDE             CHAR (1) NOT NULL DEFAULT '0' CHECK (HIDE IN ('0','1')),
  PUBLISHED        CHAR (1) NOT NULL DEFAULT '0' CHECK (PUBLISHED IN ('0','1')),
  INSTRUCTIONS     BIGINT UNSIGNED,
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  KEY ASSESSMENT_ST (SITE, TITLE)
);

CREATE TABLE ASMT_SUBMISSION
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  ASSESSMENT       BIGINT UNSIGNED NOT NULL,
  USER             BIGINT UNSIGNED NOT NULL,
  STARTED          BIGINT UNSIGNED,
  FINISHED         BIGINT UNSIGNED,
  KEY SUBMISSION_A (ASSESSMENT, USER)
);

CREATE TABLE ASMT_ANSWER
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SUBMISSION       BIGINT UNSIGNED NOT NULL,
  QUESTION         BIGINT UNSIGNED NOT NULL,
  ANSWERED         CHAR (1) NOT NULL DEFAULT '0' CHECK (ANSWERED IN ('0','1')),
  ANSWERED_ON      BIGINT,
  ANSWER_REF       BIGINT UNSIGNED,
  ANSWER_DATA      LONGTEXT,
  REVIEW           CHAR (1) NOT NULL DEFAULT '0' CHECK (REVIEW IN ('0','1')),
  KEY ANSWER_S     (SUBMISSION)
);
