-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/site.sql $
-- $Id: site.sql 12553 2016-01-14 20:03:28Z ggolden $
-- **********************************************************************************
--
-- Copyright (c) 2014, 2016 Etudes, Inc.
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

DROP TABLE IF EXISTS SITE;
DROP TABLE IF EXISTS SITE_TOOL;
DROP TABLE IF EXISTS SITE_LINK;
DROP TABLE IF EXISTS SKIN;

-- ---------------------------------------------------------------------------
-- SITE
-- ---------------------------------------------------------------------------

CREATE TABLE SITE
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  NAME             VARCHAR (128),
  CLIENT_ID        BIGINT UNSIGNED,
  TERM_ID          BIGINT UNSIGNED,
  PUBLISHED        CHAR (1) NOT NULL DEFAULT '0' CHECK (PUBLISHED IN ('0','1')),
  SKIN_ID          BIGINT UNSIGNED,
  CREATED_BY       BIGINT UNSIGNED,
  CREATED_ON       BIGINT,
  MODIFIED_BY      BIGINT UNSIGNED,
  MODIFIED_ON      BIGINT,
  PUBLISH_ON       BIGINT,
  UNPUBLISH_ON     BIGINT,
  UNIQUE KEY SITE_N (NAME),
  KEY SITE_TC      (TERM_ID, CLIENT_ID)
);

CREATE TABLE SITE_TOOL
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  TOOL_ID          INT UNSIGNED NOT NULL,
  KEY SITE_TOOLS_SITE (SITE_ID)
);

CREATE TABLE SITE_LINK
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  SITE_ID          BIGINT UNSIGNED NOT NULL,
  TITLE            VARCHAR (255),
  URL              VARCHAR (2048),
  POSITION         TINYINT,
  KEY SITE_LINK_S  (SITE_ID)
);

CREATE TABLE SKIN
(
  ID               BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
  NAME             VARCHAR(32),
  COLOR            VARCHAR(6),
  CLIENT_ID        BIGINT UNSIGNED
);

INSERT INTO SITE (NAME, CLIENT_ID, TERM_ID, PUBLISHED, SKIN_ID, CREATED_BY, MODIFIED_BY) VALUES
("Admin", 1, 4, '1', 1, 1, 1),
("Monitor", 1, 4, '1', 1, 1, 1),
("Helpdesk", 1, 4, '1', 1, 1, 1),
("Users Group", 1, 4, '1', 1, 1, 1);

INSERT INTO SITE_TOOL (SITE_ID, TOOL_ID) VALUES
(1, 1),
(1, 2),
(1, 6),
(2, 7),
(3, 1),
(3, 2),
(3, 6),
(4, 101);

INSERT INTO SITE_LINK (SITE_ID, TITLE, URL, POSITION) VALUES
(1, "Etudes", "http://www.etudes.org", 1),
(1, "JIRA", "https://jira.etudes.org", 2),
(3, "Etudes", "http://www.etudes.org", 1),
(3, "JIRA", "https://jira.etudes.org", 2);

INSERT INTO SKIN (NAME, COLOR, CLIENT_ID) VALUES
("Etudes", "730416", 0),
("Education", "730416", 0),
("Knowledge", "4E5375", 0),
("Excellence", "730416", 0),
("Invention", "070F06", 0),
("Questioning", "0F4440", 0),
("Anxiety", "0F4440", 0),
("Thinking", "730416", 0),
("Science", "4E5375", 0),
("Learning", "4E5375", 0),
("Fire", "070F06", 0),
("Acts", "0F4440", 0),
("Freedom", "4E5375", 0),
("Blackboard", "070F06", 0),
("Boat", "4E5375", 0),
("Calmwater", "496482", 0),
("Columns", "730416", 0),
("Country", "070F06", 0),
("Canals", "070F06", 0),
("Moon", "070F06", 0),
("Ocean", "35495F", 0),
("Petals", "070F06", 0),
("Pier", "070F06", 0),
("Raindrops", "022A62", 0),
("Ruins", "355D94", 0),
("Santorini", "730416", 0),
("Mountains", "070F06", 0),
("Space", "070F06", 0),
("Tree", "070F06", 0),
("Land", "4E5B88", 0),
("World", "1F3366", 0),
("fh", "730416", 2);
