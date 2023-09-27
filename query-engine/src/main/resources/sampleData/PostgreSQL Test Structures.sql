-- Fields Table
-- Defines dynamic fields that rows in the Data table may, or may not, have.
CREATE TABLE IF NOT EXISTS "Fields" (
      "fieldId" INT NOT NULL
    , "name" VARCHAR(100) NOT NULL
    , "type" VARCHAR(100) NOT NULL
    , "valueField" VARCHAR(100) NOT NULL
    , PRIMARY KEY ("fieldId")
);

-- Insert the one dynamic field definition for each possible type.
INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (1, 'DateField', 'Date', 'dateValue') ON CONFLICT DO NOTHING;
INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (2, 'TimeField', 'Time', 'timeValue') ON CONFLICT DO NOTHING;
INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (3, 'DateTimeField', 'DateTime', 'dateTimeValue') ON CONFLICT DO NOTHING;
INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (4, 'LongField', 'Long', 'longValue') ON CONFLICT DO NOTHING;
INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (5, 'DoubleField', 'Double', 'doubleValue') ON CONFLICT DO NOTHING;
INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (6, 'BoolField', 'Boolean', 'boolValue') ON CONFLICT DO NOTHING;
INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (7, 'TextField', 'String', 'textValue') ON CONFLICT DO NOTHING;

-- RefData Table
-- A simple lookup table (by GUID) to provide textual values for fields.
-- Demonstrates lookoups in the UI.
-- This could just as easily use an INT for the id column, the only reason for choosing the GUID is to demonstrate the difference across platforms
CREATE TABLE IF NOT EXISTS "RefData" (
    "refId" UUID NOT NULL
    , "value" VARCHAR(100) NOT NULL UNIQUE
    , "ordering" INT NOT NULL UNIQUE
    , PRIMARY KEY ("refId")
);

-- Colours Table
-- A simple lookup table (by INT) to provide textual values for fields.
-- Demonstrates lookoups in the UI.
CREATE TABLE IF NOT EXISTS "Colours" (
    "colourId" INT NOT NULL
    , "name" VARCHAR(100) NOT NULL UNIQUE
    , "hex" VARCHAR(100) NOT NULL
    , PRIMARY KEY ("colourId")
);
  
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (1, 'aliceblue', '#f0f8ff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (2, 'antiquewhite', '#faebd7') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (3, 'aqua', '#00ffff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (4, 'aquamarine', '#7fffd4') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (5, 'azure', '#f0ffff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (6, 'beige', '#f5f5dc') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (7, 'bisque', '#ffe4c4') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (8, 'black', '#000000') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (9, 'blanchedalmond', '#ffebcd') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (10, 'blue', '#0000ff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (11, 'blueviolet', '#8a2be2') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (12, 'brown', '#a52a2a') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (13, 'burlywood', '#deb887') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (14, 'cadetblue', '#5f9ea0') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (15, 'chartreuse', '#7fff00') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (16, 'chocolate', '#d2691e') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (17, 'coral', '#ff7f50') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (18, 'cornflowerblue', '#6495ed') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (19, 'cornsilk', '#fff8dc') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (20, 'crimson', '#dc143c') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (21, 'cyan', '#00ffff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (22, 'darkblue', '#00008b') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (23, 'darkcyan', '#008b8b') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (24, 'darkgoldenrod', '#b8860b') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (25, 'darkgray', '#a9a9a9') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (26, 'darkgreen', '#006400') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (27, 'darkgrey', '#a9a9a9') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (28, 'darkkhaki', '#bdb76b') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (29, 'darkmagenta', '#8b008b') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (30, 'darkolivegreen', '#556b2f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (31, 'darkorange', '#ff8c00') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (32, 'darkorchid', '#9932cc') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (33, 'darkred', '#8b0000') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (34, 'darksalmon', '#e9967a') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (35, 'darkseagreen', '#8fbc8f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (36, 'darkslateblue', '#483d8b') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (37, 'darkslategray', '#2f4f4f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (38, 'darkslategrey', '#2f4f4f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (39, 'darkturquoise', '#00ced1') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (40, 'darkviolet', '#9400d3') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (41, 'deeppink', '#ff1493') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (42, 'deepskyblue', '#00bfff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (43, 'dimgray', '#696969') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (44, 'dimgrey', '#696969') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (45, 'dodgerblue', '#1e90ff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (46, 'firebrick', '#b22222') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (47, 'floralwhite', '#fffaf0') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (48, 'forestgreen', '#228b22') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (49, 'fuchsia', '#ff00ff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (50, 'gainsboro', '#dcdcdc') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (51, 'ghostwhite', '#f8f8ff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (52, 'gold', '#ffd700') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (53, 'goldenrod', '#daa520') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (54, 'gray', '#808080') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (55, 'green', '#008000') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (56, 'greenyellow', '#adff2f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (57, 'grey', '#808080') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (58, 'honeydew', '#f0fff0') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (59, 'hotpink', '#ff69b4') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (60, 'indianred', '#cd5c5c') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (61, 'indigo', '#4b0082') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (62, 'ivory', '#fffff0') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (63, 'khaki', '#f0e68c') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (64, 'lavender', '#e6e6fa') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (65, 'lavenderblush', '#fff0f5') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (66, 'lawngreen', '#7cfc00') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (67, 'lemonchiffon', '#fffacd') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (68, 'lightblue', '#add8e6') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (69, 'lightcoral', '#f08080') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (70, 'lightcyan', '#e0ffff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (71, 'lightgoldenrodyellow', '#fafad2') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (72, 'lightgray', '#d3d3d3') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (73, 'lightgreen', '#90ee90') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (74, 'lightgrey', '#d3d3d3') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (75, 'lightpink', '#ffb6c1') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (76, 'lightsalmon', '#ffa07a') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (77, 'lightseagreen', '#20b2aa') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (78, 'lightskyblue', '#87cefa') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (79, 'lightslategray', '#778899') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (80, 'lightslategrey', '#778899') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (81, 'lightsteelblue', '#b0c4de') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (82, 'lightyellow', '#ffffe0') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (83, 'lime', '#00ff00') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (84, 'limegreen', '#32cd32') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (85, 'linen', '#faf0e6') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (86, 'magenta', '#ff00ff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (87, 'maroon', '#800000') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (88, 'mediumaquamarine', '#66cdaa') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (89, 'mediumblue', '#0000cd') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (90, 'mediumorchid', '#ba55d3') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (91, 'mediumpurple', '#9370db') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (92, 'mediumseagreen', '#3cb371') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (93, 'mediumslateblue', '#7b68ee') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (94, 'mediumspringgreen', '#00fa9a') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (95, 'mediumturquoise', '#48d1cc') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (96, 'mediumvioletred', '#c71585') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (97, 'midnightblue', '#191970') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (98, 'mintcream', '#f5fffa') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (99, 'mistyrose', '#ffe4e1') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (100, 'moccasin', '#ffe4b5') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (101, 'navajowhite', '#ffdead') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (102, 'navy', '#000080') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (103, 'oldlace', '#fdf5e6') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (104, 'olive', '#808000') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (105, 'olivedrab', '#6b8e23') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (106, 'orange', '#ffa500') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (107, 'orangered', '#ff4500') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (108, 'orchid', '#da70d6') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (109, 'palegoldenrod', '#eee8aa') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (110, 'palegreen', '#98fb98') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (111, 'paleturquoise', '#afeeee') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (112, 'palevioletred', '#db7093') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (113, 'papayawhip', '#ffefd5') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (114, 'peachpuff', '#ffdab9') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (115, 'peru', '#cd853f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (116, 'pink', '#ffc0cb') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (117, 'plum', '#dda0dd') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (118, 'powderblue', '#b0e0e6') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (119, 'purple', '#800080') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (120, 'red', '#ff0000') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (121, 'rosybrown', '#bc8f8f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (122, 'royalblue', '#4169e1') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (123, 'saddlebrown', '#8b4513') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (124, 'salmon', '#fa8072') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (125, 'sandybrown', '#f4a460') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (126, 'seagreen', '#2e8b57') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (127, 'seashell', '#fff5ee') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (128, 'sienna', '#a0522d') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (129, 'silver', '#c0c0c0') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (130, 'skyblue', '#87ceeb') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (131, 'slateblue', '#6a5acd') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (132, 'slategray', '#708090') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (133, 'slategrey', '#708090') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (134, 'snow', '#fffafa') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (135, 'springgreen', '#00ff7f') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (136, 'steelblue', '#4682b4') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (137, 'tan', '#d2b48c') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (138, 'teal', '#008080') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (139, 'thistle', '#d8bfd8') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (140, 'tomato', '#ff6347') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (141, 'turquoise', '#40e0d0') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (142, 'violet', '#ee82ee') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (143, 'wheat', '#f5deb3') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (144, 'white', '#ffffff') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (145, 'whitesmoke', '#f5f5f5') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (146, 'yellow', '#ffff00') ON CONFLICT DO NOTHING;
INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (147, 'yellowgreen', '#9acd32') ON CONFLICT DO NOTHING;
  

-- Data Table
-- Primary table for most queries, i.e. one row here corresponds to one output row.
CREATE TABLE IF NOT EXISTS "Data" (
      "dataId" INT NOT NULL
    , "colourId" INT NOT NULL
    , "instant" TIMESTAMP NOT NULL
    , "value" VARCHAR(100) NOT NULL
    , PRIMARY KEY ("dataId")
    , FOREIGN KEY ("colourId") REFERENCES "Colours"("colourId")
);
  
-- FieldValues Table
-- Provides the values that the dynamic fields have.
CREATE TABLE IF NOT EXISTS "FieldValues" (
      "dataId" INT NOT NULL 
    , "fieldId" INT NOT NULL 
    , "dateValue" DATE NULL DEFAULT NULL
    , "timeValue" TIME NULL DEFAULT NULL
    , "dateTimeValue" TIMESTAMP NULL DEFAULT NULL
    , "longValue" BIGINT NULL DEFAULT NULL
    , "doubleValue" REAL NULL DEFAULT NULL
    , "boolValue" BOOLEAN NULL DEFAULT NULL
    , "textValue" VARCHAR(1000) NULL DEFAULT NULL
    , PRIMARY KEY ("dataId", "fieldId")
    , FOREIGN KEY ("dataId") REFERENCES "Data"("dataId")
    , FOREIGN KEY ("fieldId") REFERENCES "Fields"("fieldId")
);

-- ManyData Table
-- A many-to-one table to provide a field for the Data rows that has multiple values
CREATE TABLE IF NOT EXISTS "ManyData" (
      "dataId" INT NOT NULL 
    , "sort" INT NOT NULL
    , "refId" UUID NOT NULL 
    , PRIMARY KEY ("dataId", "refId")
    , FOREIGN KEY ("dataId") REFERENCES "Data"("dataId")
    , FOREIGN KEY ("refId") REFERENCES "RefData"("refId")
);

  

-- DynamicEndpoint Table
-- Provides details of all the endpoints that the queries can point to.
-- For demo purposes these could be multiple databases on the same server, or on different servers.
-- Each database must have an equivalent structure.
-- This script isn't going to try to load data into this table, to demonstrate dynamic endpoints you will have to do that manually.
CREATE TABLE IF NOT EXISTS "DynamicEndpoint" (
    "endpointKey" VARCHAR(50) NOT NULL,
    "type" VARCHAR(10) NULL DEFAULT NULL,
    "url" VARCHAR(1000) NULL DEFAULT NULL,
    "urlTemplate" VARCHAR(1000) NULL DEFAULT NULL,
    "secret" VARCHAR(100) NULL DEFAULT NULL,
    "username" VARCHAR(100) NULL DEFAULT NULL,
    "password" VARCHAR(100) NULL DEFAULT NULL,
    "useCondition" VARCHAR(1000) NULL DEFAULT NULL,
    PRIMARY KEY ("endpointKey")
);
    
  
-- NumberToWords Function
-- These function exists solely to give our ref data something interesting to say.
CREATE OR REPLACE FUNCTION "NumberToWords" (Num INT)
RETURNS VARCHAR(1024) AS $fn$
BEGIN
  RETURN RTRIM(
    CASE 
      WHEN Num = 0 THEN 'zero'
      WHEN Num = 1 THEN 'one'
      WHEN Num = 2 THEN 'two'
      WHEN Num = 3 THEN 'three'
      WHEN Num = 4 THEN 'four'
      WHEN Num = 5 THEN 'five'
      WHEN Num = 6 THEN 'six'
      WHEN Num = 7 THEN 'seven'
      WHEN Num = 8 THEN 'eight'
      WHEN Num = 9 THEN 'nine'
      WHEN Num = 10 THEN 'ten'
      WHEN Num = 11 THEN 'eleven'
      WHEN Num = 12 THEN 'twelve'
      WHEN Num = 13 THEN 'thirteen'
      WHEN Num = 14 THEN 'fourteen'
      WHEN Num = 15 THEN 'fifteen'
      WHEN Num = 16 THEN 'sixteen'
      WHEN Num = 17 THEN 'seventeen'
      WHEN Num = 18 THEN 'eighteen'
      WHEN Num = 19 THEN 'nineteen'  

      WHEN Num = 20 THEN 'twenty'
      WHEN Num <= 29 THEN CONCAT('twenty ', "NumberToWords"(Num % 10))

      WHEN Num = 30 THEN 'thirty' 
      WHEN Num <= 39 THEN CONCAT('thirty ', "NumberToWords"(Num % 10))

      WHEN Num = 40 THEN 'forty'
      WHEN Num <= 49 THEN CONCAT('forty ', "NumberToWords"(Num % 10))

      WHEN Num = 50 THEN 'fifty' 
      WHEN Num <= 59 THEN CONCAT('fifty ', "NumberToWords"(Num % 10))

      WHEN Num = 60 THEN 'sixty'
      WHEN Num <= 69 THEN CONCAT('sixty ', "NumberToWords"(Num % 10))

      WHEN Num = 70 THEN 'seventy'
      WHEN Num <= 79 THEN CONCAT('seventy ', "NumberToWords"(Num % 10))

      WHEN Num = 80 THEN 'eighty'
      WHEN Num <= 89 THEN CONCAT('eighty ', "NumberToWords"(Num % 10))

      WHEN Num = 90 THEN 'ninety'
      WHEN Num <= 99 THEN CONCAT('ninety ', "NumberToWords"(Num % 10))

      WHEN Num <= 999 THEN CASE 
        WHEN Num % 100 = 0 THEN CONCAT("NumberToWords"(Num / 100), ' hundred')
        ELSE CONCAT("NumberToWords"(Num / 100), ' hundred and ', "NumberToWords"(Num % 100))
      END

      WHEN Num < 999999 THEN CASE
        WHEN Num % 1000 = 0 THEN CONCAT("NumberToWords"(Num / 1000), ' thousand')
        ELSE CONCAT("NumberToWords"(Num / 1000), ' thousand ', "NumberToWords"(Num % 1000))
      END
      ELSE '????'
    END
  );
END
$fn$ LANGUAGE plpgsql IMMUTABLE LEAKPROOF;

-- NumberToOrdinal Function
-- This function exists solely to give our ref data something interesting to say.
CREATE OR REPLACE FUNCTION "NumberToOrdinal" (Num INT) 
RETURNS VARCHAR(1024) AS $fn$
BEGIN
  RETURN RTRIM(
    CASE 
      WHEN Num = 0 THEN 'zeroth'
      WHEN Num = 1 THEN 'first'
      WHEN Num = 2 THEN 'second'
      WHEN Num = 3 THEN 'third'
      WHEN Num = 4 THEN 'fourth'
      WHEN Num = 5 THEN 'fifth'
      WHEN Num = 6 THEN 'sixth'
      WHEN Num = 7 THEN 'seventh'
      WHEN Num = 8 THEN 'eighth'
      WHEN Num = 9 THEN 'nineth'
      WHEN Num = 10 THEN 'tenth'
      WHEN Num = 11 THEN 'eleventh'
      WHEN Num = 12 THEN 'twelfth'
      WHEN Num = 13 THEN 'thirteenth'
      WHEN Num = 14 THEN 'fourteenth'
      WHEN Num = 15 THEN 'fifteenth'
      WHEN Num = 16 THEN 'sixteenth'
      WHEN Num = 17 THEN 'seventeenth'
      WHEN Num = 18 THEN 'eighteenth'
      WHEN Num = 19 THEN 'nineteenth'  

      WHEN Num = 20 THEN 'twentieth'
      WHEN Num <= 29 THEN CONCAT('twenty ', "NumberToOrdinal"(Num % 10))

      WHEN Num = 30 THEN 'thirtieth' 
      WHEN Num <= 39 THEN CONCAT('thirty ', "NumberToOrdinal"(Num % 10))

      WHEN Num = 40 THEN 'fortieth'
      WHEN Num <= 49 THEN CONCAT('forty ', "NumberToOrdinal"(Num % 10))

      WHEN Num = 50 THEN 'fiftieth' 
      WHEN Num <= 59 THEN CONCAT('fifty ', "NumberToOrdinal"(Num % 10))

      WHEN Num = 60 THEN 'sixtieth'
      WHEN Num <= 69 THEN CONCAT('sixty ', "NumberToOrdinal"(Num % 10))

      WHEN Num = 70 THEN 'seventieth'
      WHEN Num <= 79 THEN CONCAT('seventy ', "NumberToOrdinal"(Num % 10))

      WHEN Num = 80 THEN 'eightieth'
      WHEN Num <= 89 THEN CONCAT('eighty ', "NumberToOrdinal"(Num % 10))

      WHEN Num = 90 THEN 'ninetieth'
      WHEN Num <= 99 THEN CONCAT('ninety ', "NumberToOrdinal"(Num % 10))

      WHEN Num <= 999 THEN CASE 
        WHEN Num % 100 = 0 THEN CONCAT("NumberToWords"(Num / 100), ' hundredth')
        ELSE CONCAT("NumberToWords"(Num / 100), ' hundred and ', "NumberToOrdinal"(Num % 100))
      END

      WHEN Num < 999999 THEN CASE
        WHEN Num % 1000 = 0 THEN CONCAT("NumberToWords"(Num / 1000), ' thousandth')
        ELSE CONCAT("NumberToWords"(Num / 1000), ' thousand ', "NumberToOrdinal"(Num % 1000))
      END
      ELSE '????'
    END
  );
END
$fn$ LANGUAGE plpgsql IMMUTABLE LEAKPROOF;
  
CREATE OR REPLACE FUNCTION int2uuid (x INT)
RETURNS UUID AS $fn$
BEGIN
  RETURN ENCODE(SUBSTRING(SHA256(DECODE(LPAD(TO_HEX(x), 32, '0'), 'hex')), 0, 17), 'hex')::UUID;
END
$fn$ LANGUAGE plpgsql IMMUTABLE LEAKPROOF;
  
DO
$do$
DECLARE
  i INTEGER := -1;
  j INTEGER := -1;
  val VARCHAR := NULL;
  guid UUID := null;
BEGIN
  WHILE i < 1000 LOOP
    i = i + 1;
    val = "NumberToWords"(i);
    INSERT INTO "RefData" ("refId", "value", "ordering") VALUES (int2uuid(i), val, i) ON CONFLICT DO NOTHING;
  END LOOP;

  i = 0;
  WHILE i < 10000 LOOP
    i = i + 1;
    INSERT INTO "Data" ("dataId", "colourId", "instant", "value") VALUES (i, 1 + (i % 147), make_date(1971, 5, 6) + make_interval(hours => i * 27), "NumberToOrdinal"(i)) ON CONFLICT DO NOTHING;

    j = 0;
    WHILE j < i % 7 LOOP
      j = j + 1;
      guid = int2uuid((i * j) % 1000);
      INSERT INTO "ManyData" ("refId", "dataId", "sort") VALUES (guid, i, j) ON CONFLICT DO NOTHING;
    END LOOP;

    j = 0;
    WHILE j < 7 LOOP
      j = j + 1;
      IF (i % j) = 0 THEN
      INSERT INTO "FieldValues" ("dataId", "fieldId", "dateValue", "timeValue", "dateTimeValue", "longValue", "doubleValue", "boolValue", "textValue")
        VALUES(
          i
          , j
          , CASE WHEN j = 1 THEN make_date(2023, 5, 6) - make_interval(days => i) END
          , CASE WHEN j = 2 THEN make_date(2023, 5, 6) - make_interval(mins => i) END 
          , CASE WHEN j = 3 THEN make_date(2023, 5, 6) - make_interval(mins => i) END 
          , CASE WHEN j = 4 THEN (i * j) END 
          , CASE WHEN j = 5 THEN (1.0 / i) END 
          , CASE WHEN j = 6 THEN ((i / j) % 2) = 0 END 
          , CASE WHEN j = 7 THEN "NumberToWords"(i) END 
        ) ON CONFLICT DO NOTHING;
      END IF;
    END LOOP;
  END LOOP;
END;
$do$
