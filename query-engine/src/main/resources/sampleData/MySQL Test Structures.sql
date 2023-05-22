-- Fields Table
-- Defines dynamic fields that rows in the Data table may, or may not, have.
CREATE TABLE IF NOT EXISTS `Fields` (
      `fieldId` INT NOT NULL
    , `name` VARCHAR(100) NOT NULL
    , `type` VARCHAR(100) NOT NULL
    , `valueField` VARCHAR(100) NOT NULL
    , CONSTRAINT PRIMARY KEY (`fieldId`)
);

-- Insert the one dynamic field definition for each possible type.
INSERT IGNORE INTO `Fields` (`fieldId`, `name`, `type`, `valueField`) VALUES (1, 'DateField', 'Date', 'dateValue');
INSERT IGNORE INTO `Fields` (`fieldId`, `name`, `type`, `valueField`) VALUES (2, 'TimeField', 'Time', 'timeValue');
INSERT IGNORE INTO `Fields` (`fieldId`, `name`, `type`, `valueField`) VALUES (3, 'DateTimeField', 'DateTime', 'dateTimeValue');
INSERT IGNORE INTO `Fields` (`fieldId`, `name`, `type`, `valueField`) VALUES (4, 'LongField', 'Long', 'longValue');
INSERT IGNORE INTO `Fields` (`fieldId`, `name`, `type`, `valueField`) VALUES (5, 'DoubleField', 'Double', 'doubleValue');
INSERT IGNORE INTO `Fields` (`fieldId`, `name`, `type`, `valueField`) VALUES (6, 'BoolField', 'Boolean', 'boolValue');
INSERT IGNORE INTO `Fields` (`fieldId`, `name`, `type`, `valueField`) VALUES (7, 'TextField', 'String', 'textValue');

-- RefData Table
-- A simple lookup table (by GUID) to provide textual values for fields.
-- Demonstrates lookoups in the UI.
-- This could just as easily use an INT for the id column, the only reason for choosing the GUID is to demonstrate the difference across platforms
CREATE TABLE IF NOT EXISTS `RefData` (
    `refId` BINARY(16) NOT NULL
    , `value` VARCHAR(100) NOT NULL UNIQUE
    , CONSTRAINT PRIMARY KEY (`refId`)
);

-- Colours Table
-- A simple lookup table (by INT) to provide textual values for fields.
-- Demonstrates lookoups in the UI.
CREATE TABLE IF NOT EXISTS `Colours` (
    `colourId` INT NOT NULL
    , `name` VARCHAR(100) NOT NULL UNIQUE
    , `hex` VARCHAR(100) NOT NULL
    , CONSTRAINT PRIMARY KEY (`colourId`)
);
  
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (1, 'aliceblue', '#f0f8ff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (2, 'antiquewhite', '#faebd7');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (3, 'aqua', '#00ffff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (4, 'aquamarine', '#7fffd4');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (5, 'azure', '#f0ffff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (6, 'beige', '#f5f5dc');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (7, 'bisque', '#ffe4c4');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (8, 'black', '#000000');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (9, 'blanchedalmond', '#ffebcd');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (10, 'blue', '#0000ff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (11, 'blueviolet', '#8a2be2');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (12, 'brown', '#a52a2a');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (13, 'burlywood', '#deb887');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (14, 'cadetblue', '#5f9ea0');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (15, 'chartreuse', '#7fff00');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (16, 'chocolate', '#d2691e');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (17, 'coral', '#ff7f50');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (18, 'cornflowerblue', '#6495ed');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (19, 'cornsilk', '#fff8dc');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (20, 'crimson', '#dc143c');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (21, 'cyan', '#00ffff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (22, 'darkblue', '#00008b');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (23, 'darkcyan', '#008b8b');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (24, 'darkgoldenrod', '#b8860b');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (25, 'darkgray', '#a9a9a9');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (26, 'darkgreen', '#006400');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (27, 'darkgrey', '#a9a9a9');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (28, 'darkkhaki', '#bdb76b');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (29, 'darkmagenta', '#8b008b');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (30, 'darkolivegreen', '#556b2f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (31, 'darkorange', '#ff8c00');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (32, 'darkorchid', '#9932cc');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (33, 'darkred', '#8b0000');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (34, 'darksalmon', '#e9967a');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (35, 'darkseagreen', '#8fbc8f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (36, 'darkslateblue', '#483d8b');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (37, 'darkslategray', '#2f4f4f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (38, 'darkslategrey', '#2f4f4f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (39, 'darkturquoise', '#00ced1');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (40, 'darkviolet', '#9400d3');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (41, 'deeppink', '#ff1493');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (42, 'deepskyblue', '#00bfff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (43, 'dimgray', '#696969');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (44, 'dimgrey', '#696969');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (45, 'dodgerblue', '#1e90ff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (46, 'firebrick', '#b22222');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (47, 'floralwhite', '#fffaf0');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (48, 'forestgreen', '#228b22');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (49, 'fuchsia', '#ff00ff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (50, 'gainsboro', '#dcdcdc');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (51, 'ghostwhite', '#f8f8ff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (52, 'gold', '#ffd700');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (53, 'goldenrod', '#daa520');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (54, 'gray', '#808080');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (55, 'green', '#008000');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (56, 'greenyellow', '#adff2f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (57, 'grey', '#808080');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (58, 'honeydew', '#f0fff0');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (59, 'hotpink', '#ff69b4');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (60, 'indianred', '#cd5c5c');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (61, 'indigo', '#4b0082');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (62, 'ivory', '#fffff0');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (63, 'khaki', '#f0e68c');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (64, 'lavender', '#e6e6fa');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (65, 'lavenderblush', '#fff0f5');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (66, 'lawngreen', '#7cfc00');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (67, 'lemonchiffon', '#fffacd');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (68, 'lightblue', '#add8e6');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (69, 'lightcoral', '#f08080');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (70, 'lightcyan', '#e0ffff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (71, 'lightgoldenrodyellow', '#fafad2');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (72, 'lightgray', '#d3d3d3');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (73, 'lightgreen', '#90ee90');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (74, 'lightgrey', '#d3d3d3');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (75, 'lightpink', '#ffb6c1');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (76, 'lightsalmon', '#ffa07a');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (77, 'lightseagreen', '#20b2aa');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (78, 'lightskyblue', '#87cefa');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (79, 'lightslategray', '#778899');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (80, 'lightslategrey', '#778899');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (81, 'lightsteelblue', '#b0c4de');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (82, 'lightyellow', '#ffffe0');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (83, 'lime', '#00ff00');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (84, 'limegreen', '#32cd32');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (85, 'linen', '#faf0e6');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (86, 'magenta', '#ff00ff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (87, 'maroon', '#800000');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (88, 'mediumaquamarine', '#66cdaa');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (89, 'mediumblue', '#0000cd');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (90, 'mediumorchid', '#ba55d3');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (91, 'mediumpurple', '#9370db');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (92, 'mediumseagreen', '#3cb371');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (93, 'mediumslateblue', '#7b68ee');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (94, 'mediumspringgreen', '#00fa9a');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (95, 'mediumturquoise', '#48d1cc');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (96, 'mediumvioletred', '#c71585');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (97, 'midnightblue', '#191970');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (98, 'mintcream', '#f5fffa');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (99, 'mistyrose', '#ffe4e1');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (100, 'moccasin', '#ffe4b5');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (101, 'navajowhite', '#ffdead');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (102, 'navy', '#000080');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (103, 'oldlace', '#fdf5e6');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (104, 'olive', '#808000');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (105, 'olivedrab', '#6b8e23');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (106, 'orange', '#ffa500');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (107, 'orangered', '#ff4500');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (108, 'orchid', '#da70d6');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (109, 'palegoldenrod', '#eee8aa');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (110, 'palegreen', '#98fb98');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (111, 'paleturquoise', '#afeeee');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (112, 'palevioletred', '#db7093');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (113, 'papayawhip', '#ffefd5');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (114, 'peachpuff', '#ffdab9');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (115, 'peru', '#cd853f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (116, 'pink', '#ffc0cb');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (117, 'plum', '#dda0dd');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (118, 'powderblue', '#b0e0e6');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (119, 'purple', '#800080');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (120, 'red', '#ff0000');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (121, 'rosybrown', '#bc8f8f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (122, 'royalblue', '#4169e1');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (123, 'saddlebrown', '#8b4513');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (124, 'salmon', '#fa8072');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (125, 'sandybrown', '#f4a460');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (126, 'seagreen', '#2e8b57');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (127, 'seashell', '#fff5ee');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (128, 'sienna', '#a0522d');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (129, 'silver', '#c0c0c0');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (130, 'skyblue', '#87ceeb');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (131, 'slateblue', '#6a5acd');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (132, 'slategray', '#708090');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (133, 'slategrey', '#708090');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (134, 'snow', '#fffafa');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (135, 'springgreen', '#00ff7f');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (136, 'steelblue', '#4682b4');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (137, 'tan', '#d2b48c');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (138, 'teal', '#008080');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (139, 'thistle', '#d8bfd8');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (140, 'tomato', '#ff6347');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (141, 'turquoise', '#40e0d0');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (142, 'violet', '#ee82ee');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (143, 'wheat', '#f5deb3');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (144, 'white', '#ffffff');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (145, 'whitesmoke', '#f5f5f5');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (146, 'yellow', '#ffff00');
INSERT IGNORE INTO `Colours` (`colourId`, `name`, `hex`) VALUES (147, 'yellowgreen', '#9acd32');
  

-- Data Table
-- Primary table for most queries, i.e. one row here corresponds to one output row.
CREATE TABLE IF NOT EXISTS `Data` (
      `dataId` INT NOT NULL
    , `colourId` INT NOT NULL
    , `instant` TIMESTAMP NOT NULL
    , `value` VARCHAR(100) NOT NULL
    , CONSTRAINT PRIMARY KEY (`dataId`)
    , FOREIGN KEY (colourId) REFERENCES `Colours`(`colourId`)
);
  
-- FieldValues Table
-- Provides the values that the dynamic fields have.
CREATE TABLE IF NOT EXISTS `FieldValues` (
      `dataId` INT NOT NULL 
    , `fieldId` INT NOT NULL 
    , `dateValue` DATE NULL DEFAULT NULL
    , `timeValue` TIME NULL DEFAULT NULL
    , `dateTimeValue` TIMESTAMP NULL DEFAULT NULL
    , `longValue` BIGINT NULL DEFAULT NULL
    , `doubleValue` REAL NULL DEFAULT NULL
    , `boolValue` BIT NULL DEFAULT NULL
    , `textValue` VARCHAR(1000) NULL DEFAULT NULL
    , CONSTRAINT PRIMARY KEY (`dataId`, `fieldId`)
    , FOREIGN KEY (`dataId`) REFERENCES `Data`(`dataId`)
    , FOREIGN KEY (`fieldId`) REFERENCES `Fields`(`fieldId`)
);

-- ManyData Table
-- A many-to-one table to provide a field for the Data rows that has multiple values
CREATE TABLE IF NOT EXISTS `ManyData` (
      `dataId` INT NOT NULL 
    , `sort` INT NOT NULL
    , `refId` BINARY(16) NOT NULL 
    , PRIMARY KEY (`dataId`, `refId`)
    , FOREIGN KEY (`dataId`) REFERENCES `Data`(`dataId`)
    , FOREIGN KEY (`refId`) REFERENCES `RefData`(`refId`)
);

  

-- DynamicEndpoint Table
-- Provides details of all the endpoints that the queries can point to.
-- For demo purposes these could be multiple databases on the same server, or on different servers.
-- Each database must have an equivalent structure.
-- This script isn't going to try to load data into this table, to demonstrate dynamic endpoints you will have to do that manually.
CREATE TABLE IF NOT EXISTS `DynamicEndpoint` (
    `endpointKey` VARCHAR(50) NOT NULL,
    `type` VARCHAR(10) NULL DEFAULT NULL,
    `url` VARCHAR(1000) NULL DEFAULT NULL,
    `urlTemplate` VARCHAR(1000) NULL DEFAULT NULL,
    `secret` VARCHAR(100) NULL DEFAULT NULL,
    `username` VARCHAR(100) NULL DEFAULT NULL,
    `password` VARCHAR(100) NULL DEFAULT NULL,
    `useCondition` VARCHAR(1000) NULL DEFAULT NULL,
    PRIMARY KEY (`endpointKey`)
);
    
-- NumberToWords Function
-- These function exists solely to give our ref data something interesting to say.
DELIMITER //
CREATE FUNCTION IF NOT EXISTS NumberToWordsUpToTwenty (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE Num
      WHEN 0 THEN 'zero'
      WHEN 1 THEN 'one'
      WHEN 2 THEN 'two'
      WHEN 3 THEN 'three'
      WHEN 4 THEN 'four'
      WHEN 5 THEN 'five'
      WHEN 6 THEN 'six'
      WHEN 7 THEN 'seven'
      WHEN 8 THEN 'eight'
      WHEN 9 THEN 'nine'
      WHEN 10 THEN 'ten'
      WHEN 11 THEN 'eleven'
      WHEN 12 THEN 'twelve'
      WHEN 13 THEN 'thirteen'
      WHEN 14 THEN 'fourteen'
      WHEN 15 THEN 'fifteen'
      WHEN 16 THEN 'sixteen'
      WHEN 17 THEN 'seventeen'
      WHEN 18 THEN 'eighteen'
      WHEN 19 THEN 'nineteen'  
      ELSE '?'
    END
  );
END; //
CREATE FUNCTION IF NOT EXISTS NumberToWordsUpToHundred (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE
      WHEN Num < 20 THEN NumberToWordsUpToTwenty(Num)
      
      WHEN Num = 20 THEN 'twenty'
      WHEN Num <= 29 THEN CONCAT('twenty ', NumberToWordsUpToTwenty(Num % 10))
  
      WHEN Num = 30 THEN 'thirty' 
      WHEN Num <= 39 THEN CONCAT('thirty ', NumberToWordsUpToTwenty(Num % 10))
  
      WHEN Num = 40 THEN 'forty'
      WHEN Num <= 49 THEN CONCAT('forty ', NumberToWordsUpToTwenty(Num % 10))
  
      WHEN Num = 50 THEN 'fifty' 
      WHEN Num <= 59 THEN CONCAT('fifty ', NumberToWordsUpToTwenty(Num % 10))
  
      WHEN Num = 60 THEN 'sixty'
      WHEN Num <= 69 THEN CONCAT('sixty ', NumberToWordsUpToTwenty(Num % 10))
  
      WHEN Num = 70 THEN 'seventy'
      WHEN Num <= 79 THEN CONCAT('seventy ', NumberToWordsUpToTwenty(Num % 10))
  
      WHEN Num = 80 THEN 'eighty'
      WHEN Num <= 89 THEN CONCAT('eighty ', NumberToWordsUpToTwenty(Num % 10))
  
      WHEN Num = 90 THEN 'ninety'
      WHEN Num <= 99 THEN CONCAT('ninety ', NumberToWordsUpToTwenty(Num % 10))

      ELSE '??'
    END
  );
END; //
CREATE FUNCTION IF NOT EXISTS NumberToWordsUpToThousand (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE
      WHEN Num < 100 THEN NumberToWordsUpToHundred(Num)
  
      WHEN Num <= 999 THEN CASE 
        WHEN Num % 100 = 0 THEN CONCAT(NumberToWordsUpToTwenty(Num / 100), ' hundred')
        ELSE CONCAT(NumberToWordsUpToTwenty(Num / 100), ' hundred and ', NumberToWordsUpToHundred(Num % 100))
      END
      ELSE '???'
    END
  );
END; //
CREATE FUNCTION IF NOT EXISTS NumberToWords (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE
      WHEN Num < 1000 THEN NumberToWordsUpToThousand(Num)
  
      WHEN Num < 999999 THEN CASE
        WHEN Num % 1000 = 0 THEN CONCAT(NumberToWordsUpToThousand(Num / 1000), ' thousand')
        ELSE CONCAT(NumberToWordsUpToThousand(Num / 1000), ' thousand ', NumberToWordsUpToThousand(Num % 1000))
      END
      ELSE '????'
    END
  );
END; //
DELIMITER ;
    
-- NumberToOrdinal Function
-- This function exists solely to give our ref data something interesting to say.
DELIMITER //
CREATE FUNCTION IF NOT EXISTS NumberToOrdinalUpToTwenty (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE Num
      WHEN 0 THEN 'zeroth'
      WHEN 1 THEN 'first'
      WHEN 2 THEN 'second'
      WHEN 3 THEN 'third'
      WHEN 4 THEN 'fourth'
      WHEN 5 THEN 'fifth'
      WHEN 6 THEN 'sixth'
      WHEN 7 THEN 'seventh'
      WHEN 8 THEN 'eighth'
      WHEN 9 THEN 'nineth'
      WHEN 10 THEN 'tenth'
      WHEN 11 THEN 'eleventh'
      WHEN 12 THEN 'twelfth'
      WHEN 13 THEN 'thirteenth'
      WHEN 14 THEN 'fourteenth'
      WHEN 15 THEN 'fifteenth'
      WHEN 16 THEN 'sixteenth'
      WHEN 17 THEN 'seventeenth'
      WHEN 18 THEN 'eighteenth'
      WHEN 19 THEN 'nineteenth'  
      ELSE '?'
    END
  );
END; //
CREATE FUNCTION IF NOT EXISTS NumberToOrdinalUpToHundred (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE
      WHEN Num < 20 THEN NumberToOrdinalUpToTwenty(Num)
      
      WHEN Num = 20 THEN 'twentieth'
      WHEN Num <= 29 THEN CONCAT('twenty ', NumberToOrdinalUpToTwenty(Num % 10))
  
      WHEN Num = 30 THEN 'thirtieth' 
      WHEN Num <= 39 THEN CONCAT('thirty ', NumberToOrdinalUpToTwenty(Num % 10))
  
      WHEN Num = 40 THEN 'fortieth'
      WHEN Num <= 49 THEN CONCAT('forty ', NumberToOrdinalUpToTwenty(Num % 10))
  
      WHEN Num = 50 THEN 'fiftieth' 
      WHEN Num <= 59 THEN CONCAT('fifty ', NumberToOrdinalUpToTwenty(Num % 10))
  
      WHEN Num = 60 THEN 'sixtieth'
      WHEN Num <= 69 THEN CONCAT('sixty ', NumberToOrdinalUpToTwenty(Num % 10))
  
      WHEN Num = 70 THEN 'seventieth'
      WHEN Num <= 79 THEN CONCAT('seventy ', NumberToOrdinalUpToTwenty(Num % 10))
  
      WHEN Num = 80 THEN 'eightieth'
      WHEN Num <= 89 THEN CONCAT('eighty ', NumberToOrdinalUpToTwenty(Num % 10))
  
      WHEN Num = 90 THEN 'ninetieth'
      WHEN Num <= 99 THEN CONCAT('ninety ', NumberToOrdinalUpToTwenty(Num % 10))

      ELSE '??'
    END
  );
END; //
CREATE FUNCTION IF NOT EXISTS NumberToOrdinalUpToThousand (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE
      WHEN Num < 100 THEN NumberToOrdinalUpToHundred(Num)
  
      WHEN Num <= 999 THEN CASE 
        WHEN Num % 100 = 0 THEN CONCAT(NumberToWordsUpToTwenty(Num / 100), ' hundredth')
        ELSE CONCAT(NumberToWordsUpToTwenty(Num / 100), ' hundred and ', NumberToOrdinalUpToHundred(Num % 100))
      END
      ELSE '???'
    END
  );
END; //
CREATE FUNCTION IF NOT EXISTS NumberToOrdinal (Num INT) 
RETURNS VARCHAR(1024) DETERMINISTIC
BEGIN
  RETURN RTRIM(
    CASE
      WHEN Num < 1000 THEN NumberToOrdinalUpToThousand(Num)
  
      WHEN Num < 999999 THEN CASE
        WHEN Num % 1000 = 0 THEN CONCAT(NumberToWordsUpToThousand(Num / 1000), ' thousandth')
        ELSE CONCAT(NumberToWordsUpToThousand(Num / 1000), ' thousand ', NumberToOrdinalUpToThousand(Num % 1000))
      END
      ELSE '????'
    END
  );
END; //
CREATE PROCEDURE IF NOT EXISTS LoadData()
BEGIN
  START TRANSACTION;
  
    SET @i = -1;
    WHILE @i < 1000 DO
      SET @i = @i + 1;
      SET @val = NumberToWords(@i);
      SET @guid = CAST(SHA(CAST(@i AS BINARY(16))) AS BINARY(16));
      INSERT IGNORE INTO `RefData` (`refId`, `value`) VALUES (@guid, @val);
    END WHILE;
  
    SET @i = 0;
    WHILE @i < 10000 DO
  
      SET @i = @i + 1;
      INSERT IGNORE INTO `Data` (`dataId`, `colourId`, `instant`, `value`) VALUES (@i, 1 + (@i % 147), DATE_ADD('1971-05-06', interval @i * 27 hour ), NumberToOrdinal(@i));
          
      SET @j = 0;
      WHILE @j < @i % 7 DO
        SET @j = @j + 1;
        SET @guid = CAST(SHA(CAST((@i * @j) % 1000 AS BINARY(16))) AS BINARY(16));
        INSERT IGNORE INTO `ManyData` (`refId`, `dataId`, `sort`) VALUES (@guid, @i, @j);
      END WHILE;
        
      SET @j = 0;
      WHILE @j < 7 DO
        SET @j = @j + 1;
        IF (@i % @j) = 0 THEN
          INSERT IGNORE INTO `FieldValues` (`dataId`, `fieldId`, `dateValue`, `timeValue`, `dateTimeValue`, `longValue`, `doubleValue`, `boolValue`, `textValue`)
              VALUES(
                @i
                , @j
                , CASE WHEN @j = 1 THEN DATE_ADD('2023-05-06', INTERVAL 0 - @i DAY) END
                , CASE WHEN @j = 2 THEN DATE_ADD('2023-05-06', INTERVAL 0 - @i MINUTE) END 
                , CASE WHEN @j = 3 THEN DATE_ADD('2023-05-06', INTERVAL 0 - @i MINUTE) END 
                , CASE WHEN @j = 4 THEN (@i * @j) END 
                , CASE WHEN @j = 5 THEN (1.0 / @i) END 
                , CASE WHEN @j = 6 THEN ((@i / @j) % 2) END 
                , CASE WHEN @j = 7 THEN NumberToWords(@i) END 
              );
        END IF;
      END WHILE;
    END WHILE;
  COMMIT;
END; //

CALL LoadData();

