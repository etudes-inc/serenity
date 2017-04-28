-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/home/home-webapp/src/main/resources/sql/home.sql $
-- $Id: home.sql 11984 2015-11-03 20:57:48Z ggolden $
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
-- HOME
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS HOME_ITEM;

CREATE TABLE HOME_ITEM
(
    ID              BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
    SITE            BIGINT UNSIGNED NOT NULL,   
    PUBLISHED       CHAR (1) NOT NULL CHECK (PUBLISHED IN ('0','1')),
    RELEASE_DATE    BIGINT NULL,
    SOURCE          CHAR (1) NOT NULL CHECK (SOURCE IN ('W','A','F','Y')),
    TITLE           VARCHAR (255) NULL,
    CONTENT         BIGINT UNSIGNED NULL,
    URL             VARCHAR (2048) NULL,
    DIMENSIONS      VARCHAR(12) NULL,
    ALT             VARCHAR (2048) NULL,
    CREATED_BY      BIGINT UNSIGNED NOT NULL,
    CREATED_ON      BIGINT NOT NULL,
    MODIFIED_BY     BIGINT UNSIGNED NOT NULL,
    MODIFIED_ON     BIGINT NOT NULL,
    KEY HOME_ITEM_S	(SITE)
);
