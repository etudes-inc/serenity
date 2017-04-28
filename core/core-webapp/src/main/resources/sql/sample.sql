-- *********************************************************************************
-- $URL: https://source.etudes.org/svn/serenity/trunk/core/core-webapp/src/main/resources/sql/sample.sql $
-- $Id: sample.sql 8484 2014-08-19 19:12:42Z ggolden $
-- **********************************************************************************
--
-- Copyright (c) 2014 Etudes, Inc.
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

INSERT INTO SITE (NAME, CLIENT_ID, TERM_ID, PUBLISHED, CREATED_BY, MODIFIED_BY) VALUES
("Users Group", 1, 5, '0', 1, 1),
("ETU SAMPLE 101 1001 GGOLD DEV", 1, 6, '1', 1, 1),
("ETU SAMPLE 222 1002 GGOLD DEV", 1, 6, '0', 1, 1),
("ETU SAMPLE 300 1003 GGOLD DEV", 1, 6, '0', 1, 1),
("ETU SAMPLE 310 1004 GGOLD DEV", 1, 6, '1', 1, 1),
("ETU SAMPLE 320 DEV", 1, 6, '1', 1, 1),
("ETU SAMPLE 400 2314 VSINO F14", 1, 42, '1', 1, 1),
("ETU SAMPLE 410 3122 MTANU F14", 1, 42, '1', 1, 1),
("ETU SAMPLE 420 2238 RMARE F14", 1, 42, '1', 1, 1),
("ETU SAMPLE 430 2238 MTHOP F14", 1, 42, '1', 1, 1);

INSERT INTO SITE_TOOLS(SITE_ID, TOOL_ID) VALUES
(4, 100),
(4, 101),
(4, 102),
(4, 112),
(5, 100),
(6, 100),
(7, 100),
(8, 100),
(9, 100),
(10, 100),
(11, 100),
(12, 100);

UPDATE SITE SET PUBLISHED = '0', PUBLISH_ON = 1411415880000 WHERE ID = 5;

INSERT INTO USER (NAME_LAST, NAME_FIRST, EID, PASSWORD) VALUES
("Golden", "Glenn", "ggolden", ""),
("Student", "One", "s1", ""),
("Student", "Two", "s2", ""),
("Student", "Three", "s3", ""),
("Student", "Four", "s4", ""),
("Student", "Five", "s5", ""),
("Guest", "One", "g1@guest.org", "");

INSERT INTO USER_IID (USER_ID, IID, CODE) VALUES
(2, "ggolden", "etu"),
(3, "s1", "etu"),
(4, "s2", "etu"),
(5, "s3", "etu"),
(6, "s4", "etu"),
(7, "s5", "etu");

INSERT INTO ROSTER (OFFICIAL, NAME, CLIENT_ID, TERM_ID) VALUES
('1', "101001", 1, 6),
('1', "101002", 1, 6),
('1', "222001", 1, 6),
('1', "300001", 1, 6),
('1', "310001", 1, 6),
('1', "320001", 1, 6),
('1', "UG", 1, 5),
('1', "XXXX", 1, 42),
('0', "Adhoc4", 1, 6);

INSERT INTO ROSTER_SITE (ROSTER_ID, SITE_ID) VALUES
(2, 4),
(3, 4),
(4, 5),
(5, 6),
(6, 7),
(7, 8),
(8, 3),
(9, 9),
(9, 10),
(9, 11),
(9,12),
(10, 4);

INSERT INTO ROSTER_MEMBER (ROSTER_ID, USER_ID, ROLE, ACTIVE) VALUES
(2, 2, 4, '1'),
(2, 3, 2, '1'),
(2, 4, 2, '1'),
(3, 5, 2, '1'),
(3, 6, 2, '1'),
(3, 7, 2, '0'),
(4, 2, 4, '1'),
(4, 3, 2, '1'),
(5, 2, 4, '1'),
(5, 4, 2, '1'),
(6, 2, 4, '1'),
(6, 5, 2, '1'),
(7, 2, 4, '1'),
(7, 6, 2, '1'),
(8, 2, 2, '1'),
(8, 3, 2, '1'),
(8, 4, 2, '1'),
(8, 5, 2, '1'),
(8, 6, 2, '1'),
(8, 7, 2, '0'),
(9, 2, 4, '1'),
(9, 3, 2, '1'),
(10, 8, 1, '1');
