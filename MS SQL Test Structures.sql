-- Fields Table
-- Defines dynamic fields that rows in the Data table may, or may not, have.
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Fields' AND xtype='U')
    CREATE TABLE "Fields" (
        "fieldId" INT NOT NULL,
        "name" VARCHAR(100) NOT NULL,
        "type" VARCHAR(100) NOT NULL,
        "valueField" VARCHAR(100) NOT NULL,
        PRIMARY KEY ("fieldId")
    );

-- Insert the one dynamic field definition for each possible type.
IF NOT EXISTS (SELECT * FROM Fields WHERE fieldId = 1)
    INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (1, 'DateField', 'Date', 'dateValue');
IF NOT EXISTS (SELECT * FROM Fields WHERE fieldId = 2)
    INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (2, 'TimeField', 'Time', 'timeValue');
IF NOT EXISTS (SELECT * FROM Fields WHERE fieldId = 3)
    INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (3, 'DateTimeField', 'DateTime', 'dateTimeValue');
IF NOT EXISTS (SELECT * FROM Fields WHERE fieldId = 4)
    INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (4, 'LongField', 'Long', 'longValue');
IF NOT EXISTS (SELECT * FROM Fields WHERE fieldId = 5)
    INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (5, 'DoubleField', 'Double', 'doubleValue');
IF NOT EXISTS (SELECT * FROM Fields WHERE fieldId = 6)
    INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (6, 'BoolField', 'Boolean', 'boolValue');
IF NOT EXISTS (SELECT * FROM Fields WHERE fieldId = 7)
    INSERT INTO "Fields" ("fieldId", "name", "type", "valueField") VALUES (7, 'TextField', 'String', 'textValue');

-- RefData Table
-- A simple lookup table (by GUID) to provide textual values for fields.
-- Demonstrates lookoups in the UI.
-- This could just as easily use an INT for the id column, the only reason for choosing the GUID is to demonstrate the difference across platforms
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='RefData' AND xtype='U')
    CREATE TABLE "RefData" (
        "refId" UNIQUEIDENTIFIER NOT NULL,
        "value" VARCHAR(100) NOT NULL CONSTRAINT UQ_value UNIQUE,
        PRIMARY KEY ("refId")
    );

-- Colours Table
-- A simple lookup table (by INT) to provide textual values for fields.
-- Demonstrates lookoups in the UI.
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Colours' AND xtype='U')
    CREATE TABLE "Colours" (
        "colourId" INT NOT NULL,
        "name" VARCHAR(100) NOT NULL CONSTRAINT UQ_name UNIQUE,
        "hex" VARCHAR(100) NOT NULL,
        PRIMARY KEY ("colourId")
    );
  
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 1) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (1, 'aliceblue', '#f0f8ff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 2) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (2, 'antiquewhite', '#faebd7');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 3) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (3, 'aqua', '#00ffff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 4) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (4, 'aquamarine', '#7fffd4');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 5) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (5, 'azure', '#f0ffff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 6) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (6, 'beige', '#f5f5dc');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 7) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (7, 'bisque', '#ffe4c4');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 8) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (8, 'black', '#000000');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 9) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (9, 'blanchedalmond', '#ffebcd');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 10) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (10, 'blue', '#0000ff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 11) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (11, 'blueviolet', '#8a2be2');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 12) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (12, 'brown', '#a52a2a');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 13) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (13, 'burlywood', '#deb887');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 14) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (14, 'cadetblue', '#5f9ea0');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 15) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (15, 'chartreuse', '#7fff00');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 16) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (16, 'chocolate', '#d2691e');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 17) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (17, 'coral', '#ff7f50');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 18) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (18, 'cornflowerblue', '#6495ed');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 19) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (19, 'cornsilk', '#fff8dc');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 20) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (20, 'crimson', '#dc143c');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 21) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (21, 'cyan', '#00ffff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 22) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (22, 'darkblue', '#00008b');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 23) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (23, 'darkcyan', '#008b8b');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 24) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (24, 'darkgoldenrod', '#b8860b');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 25) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (25, 'darkgray', '#a9a9a9');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 26) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (26, 'darkgreen', '#006400');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 27) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (27, 'darkgrey', '#a9a9a9');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 28) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (28, 'darkkhaki', '#bdb76b');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 29) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (29, 'darkmagenta', '#8b008b');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 30) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (30, 'darkolivegreen', '#556b2f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 31) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (31, 'darkorange', '#ff8c00');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 32) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (32, 'darkorchid', '#9932cc');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 33) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (33, 'darkred', '#8b0000');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 34) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (34, 'darksalmon', '#e9967a');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 35) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (35, 'darkseagreen', '#8fbc8f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 36) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (36, 'darkslateblue', '#483d8b');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 37) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (37, 'darkslategray', '#2f4f4f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 38) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (38, 'darkslategrey', '#2f4f4f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 39) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (39, 'darkturquoise', '#00ced1');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 40) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (40, 'darkviolet', '#9400d3');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 41) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (41, 'deeppink', '#ff1493');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 42) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (42, 'deepskyblue', '#00bfff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 43) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (43, 'dimgray', '#696969');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 44) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (44, 'dimgrey', '#696969');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 45) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (45, 'dodgerblue', '#1e90ff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 46) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (46, 'firebrick', '#b22222');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 47) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (47, 'floralwhite', '#fffaf0');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 48) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (48, 'forestgreen', '#228b22');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 49) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (49, 'fuchsia', '#ff00ff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 50) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (50, 'gainsboro', '#dcdcdc');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 51) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (51, 'ghostwhite', '#f8f8ff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 52) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (52, 'gold', '#ffd700');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 53) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (53, 'goldenrod', '#daa520');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 54) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (54, 'gray', '#808080');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 55) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (55, 'green', '#008000');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 56) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (56, 'greenyellow', '#adff2f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 57) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (57, 'grey', '#808080');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 58) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (58, 'honeydew', '#f0fff0');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 59) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (59, 'hotpink', '#ff69b4');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 60) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (60, 'indianred', '#cd5c5c');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 61) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (61, 'indigo', '#4b0082');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 62) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (62, 'ivory', '#fffff0');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 63) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (63, 'khaki', '#f0e68c');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 64) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (64, 'lavender', '#e6e6fa');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 65) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (65, 'lavenderblush', '#fff0f5');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 66) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (66, 'lawngreen', '#7cfc00');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 67) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (67, 'lemonchiffon', '#fffacd');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 68) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (68, 'lightblue', '#add8e6');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 69) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (69, 'lightcoral', '#f08080');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 70) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (70, 'lightcyan', '#e0ffff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 71) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (71, 'lightgoldenrodyellow', '#fafad2');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 72) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (72, 'lightgray', '#d3d3d3');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 73) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (73, 'lightgreen', '#90ee90');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 74) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (74, 'lightgrey', '#d3d3d3');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 75) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (75, 'lightpink', '#ffb6c1');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 76) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (76, 'lightsalmon', '#ffa07a');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 77) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (77, 'lightseagreen', '#20b2aa');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 78) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (78, 'lightskyblue', '#87cefa');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 79) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (79, 'lightslategray', '#778899');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 80) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (80, 'lightslategrey', '#778899');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 81) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (81, 'lightsteelblue', '#b0c4de');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 82) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (82, 'lightyellow', '#ffffe0');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 83) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (83, 'lime', '#00ff00');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 84) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (84, 'limegreen', '#32cd32');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 85) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (85, 'linen', '#faf0e6');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 86) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (86, 'magenta', '#ff00ff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 87) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (87, 'maroon', '#800000');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 88) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (88, 'mediumaquamarine', '#66cdaa');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 89) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (89, 'mediumblue', '#0000cd');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 90) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (90, 'mediumorchid', '#ba55d3');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 91) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (91, 'mediumpurple', '#9370db');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 92) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (92, 'mediumseagreen', '#3cb371');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 93) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (93, 'mediumslateblue', '#7b68ee');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 94) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (94, 'mediumspringgreen', '#00fa9a');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 95) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (95, 'mediumturquoise', '#48d1cc');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 96) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (96, 'mediumvioletred', '#c71585');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 97) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (97, 'midnightblue', '#191970');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 98) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (98, 'mintcream', '#f5fffa');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 99) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (99, 'mistyrose', '#ffe4e1');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 100) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (100, 'moccasin', '#ffe4b5');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 101) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (101, 'navajowhite', '#ffdead');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 102) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (102, 'navy', '#000080');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 103) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (103, 'oldlace', '#fdf5e6');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 104) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (104, 'olive', '#808000');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 105) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (105, 'olivedrab', '#6b8e23');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 106) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (106, 'orange', '#ffa500');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 107) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (107, 'orangered', '#ff4500');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 108) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (108, 'orchid', '#da70d6');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 109) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (109, 'palegoldenrod', '#eee8aa');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 110) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (110, 'palegreen', '#98fb98');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 111) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (111, 'paleturquoise', '#afeeee');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 112) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (112, 'palevioletred', '#db7093');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 113) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (113, 'papayawhip', '#ffefd5');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 114) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (114, 'peachpuff', '#ffdab9');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 115) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (115, 'peru', '#cd853f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 116) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (116, 'pink', '#ffc0cb');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 117) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (117, 'plum', '#dda0dd');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 118) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (118, 'powderblue', '#b0e0e6');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 119) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (119, 'purple', '#800080');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 120) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (120, 'red', '#ff0000');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 121) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (121, 'rosybrown', '#bc8f8f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 122) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (122, 'royalblue', '#4169e1');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 123) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (123, 'saddlebrown', '#8b4513');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 124) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (124, 'salmon', '#fa8072');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 125) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (125, 'sandybrown', '#f4a460');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 126) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (126, 'seagreen', '#2e8b57');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 127) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (127, 'seashell', '#fff5ee');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 128) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (128, 'sienna', '#a0522d');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 129) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (129, 'silver', '#c0c0c0');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 130) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (130, 'skyblue', '#87ceeb');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 131) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (131, 'slateblue', '#6a5acd');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 132) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (132, 'slategray', '#708090');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 133) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (133, 'slategrey', '#708090');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 134) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (134, 'snow', '#fffafa');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 135) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (135, 'springgreen', '#00ff7f');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 136) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (136, 'steelblue', '#4682b4');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 137) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (137, 'tan', '#d2b48c');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 138) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (138, 'teal', '#008080');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 139) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (139, 'thistle', '#d8bfd8');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 140) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (140, 'tomato', '#ff6347');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 141) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (141, 'turquoise', '#40e0d0');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 142) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (142, 'violet', '#ee82ee');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 143) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (143, 'wheat', '#f5deb3');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 144) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (144, 'white', '#ffffff');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 145) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (145, 'whitesmoke', '#f5f5f5');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 146) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (146, 'yellow', '#ffff00');
IF NOT EXISTS (SELECT * FROM Colours WHERE colourId = 147) INSERT INTO "Colours" ("colourId", "name", "hex") VALUES (147, 'yellowgreen', '#9acd32');
  

-- Data Table
-- Primary table for most queries, i.e. one row here corresponds to one output row.
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='Data' AND xtype='U')
    CREATE TABLE "Data" (
        "dataId" INT NOT NULL,
        "colourId" INT NOT NULL FOREIGN KEY REFERENCES "Colours"("colourId"),
        "instant" DATETIME2(7) NOT NULL,
        "value" VARCHAR(100) NOT NULL,
        PRIMARY KEY ("dataId")
    );
  
-- FieldValues Table
-- Provides the values that the dynamic fields have.
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='FieldValues' AND xtype='U')
    CREATE TABLE "FieldValues" (
        "dataId" INT NOT NULL FOREIGN KEY REFERENCES "Data"("dataId"),
        "fieldId" INT NOT NULL FOREIGN KEY REFERENCES "Fields"("fieldId"),
        "dateValue" DATE NULL DEFAULT NULL,
        "timeValue" TIME NULL DEFAULT NULL,
        "dateTimeValue" DATETIME2(7) NULL DEFAULT NULL,
        "longValue" BIGINT NULL DEFAULT NULL,
        "doubleValue" REAL NULL DEFAULT NULL,
        "boolValue" BIT NULL DEFAULT NULL,
        "textValue" VARCHAR(1000) NULL DEFAULT NULL,
        PRIMARY KEY ("dataId", "fieldId")
    );

-- ManyData Table
-- A many-to-one table to provide a field for the Data rows that has multiple values
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='ManyData' AND xtype='U')
    CREATE TABLE "ManyData" (
        "dataId" INT NOT NULL FOREIGN KEY REFERENCES "Data"("dataId"),
        "sort" INT NOT NULL,
        "refId" UNIQUEIDENTIFIER NOT NULL FOREIGN KEY REFERENCES "RefData"("refId"),
        PRIMARY KEY ("dataId", "refId")
    );

  

-- DynamicEndpoint Table
-- Provides details of all the endpoints that the queries can point to.
-- For demo purposes these could be multiple databases on the same server, or on different servers.
-- Each database must have an equivalent structure.
-- This script isn't going to try to load data into this table, to demonstrate dynamic endpoints you will have to do that manually.
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='DynamicEndpoint' AND xtype='U')
    CREATE TABLE "DynamicEndpoint" (
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
    
DECLARE @sql NVARCHAR(4000)

-- NumberToWords Function
-- This function exists solely to give our ref data something interesting to say.
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='NumberToWords' AND xtype='FN')
BEGIN
  SET @sql = 'CREATE FUNCTION NumberToWords (@Number as INT) 
    RETURNS VARCHAR(1024)
    AS
    BEGIN   
      DECLARE @Word varchar(1024)
      SET @Word = CASE
        WHEN @Number = 0 THEN ''zero''
        WHEN @Number = 1 THEN ''one''
        WHEN @Number = 2 THEN ''two''
        WHEN @Number = 3 THEN ''three''
        WHEN @Number = 4 THEN ''four''
        WHEN @Number = 5 THEN ''five''
        WHEN @Number = 6 THEN ''six''
        WHEN @Number = 7 THEN ''seven''
        WHEN @Number = 8 THEN ''eight''
        WHEN @Number = 9 THEN ''nine''
        WHEN @Number = 10 THEN ''ten''
        WHEN @Number = 11 THEN ''eleven''
        WHEN @Number = 12 THEN ''twelve''
        WHEN @Number = 13 THEN ''thirteen''
        WHEN @Number = 14 THEN ''fourteen''
        WHEN @Number = 15 THEN ''fifteen''
        WHEN @Number = 16 THEN ''sixteen''
        WHEN @Number = 17 THEN ''seventeen''
        WHEN @Number = 18 THEN ''eighteen''
        WHEN @Number = 19 THEN ''nineteen''

        WHEN @Number = 20 THEN ''twenty''
        WHEN @Number BETWEEN 21 AND 29 THEN ''twenty '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number = 30 THEN ''thirty'' 
        WHEN @Number BETWEEN 31 AND 39 THEN ''thirty '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number = 40 THEN ''forty''
        WHEN @Number BETWEEN 41 AND 49 THEN ''forty '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number = 50 THEN ''fifty'' 
        WHEN @Number BETWEEN 51 AND 59 THEN ''fifty '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number = 60 THEN ''sixty''
        WHEN @Number BETWEEN 61 AND 69 THEN ''sixty '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number = 70 THEN ''seventy''
        WHEN @Number BETWEEN 71 AND 79 THEN ''seventy '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number = 80 THEN ''eighty''
        WHEN @Number BETWEEN 81 AND 89 THEN ''eighty '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number = 90 THEN ''ninety''
        WHEN @Number BETWEEN 91 AND 99 THEN ''ninety '' + dbo.NumberToWords(@Number % 10)

        WHEN @Number BETWEEN 100 AND 999 AND @Number % 100 = 0 THEN dbo.NumberToWords(@Number / 100) + '' hundred''
        WHEN @Number BETWEEN 100 AND 999 AND @Number % 100 != 0 THEN dbo.NumberToWords(@Number / 100) + '' hundred and '' + dbo.NumberToWords(@Number % 100)

        WHEN @Number BETWEEN 1000 AND 999999 AND @Number % 1000 = 0 THEN dbo.NumberToWords(@Number / 1000) + '' thousand''
        WHEN @Number BETWEEN 1000 AND 999999 AND @Number % 1000 != 0 AND @Number % 100 = 0 THEN dbo.NumberToWords(@Number / 1000) + '' thousand and '' + dbo.NumberToWords(@Number % 1000)
        WHEN @Number BETWEEN 1000 AND 999999 AND @Number % 1000 != 0 AND @Number % 100 != 0 THEN dbo.NumberToWords(@Number / 1000) + '' thousand '' + dbo.NumberToWords(@Number % 1000)
      END

      SELECT @Word = RTRIM(@Word)

      RETURN @Word

    END'
  EXEC sp_executesql @sql
END
    
-- NumberToOrdinal Function
-- This function exists solely to give our ref data something interesting to say.
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='NumberToOrdinal' AND xtype='FN')
BEGIN
  SET @sql = 'CREATE FUNCTION NumberToOrdinal (@Number as INT) 
    RETURNS VARCHAR(1024)
    AS
    BEGIN   
      DECLARE @Word varchar(1024)
      SET @Word = CASE
        WHEN @Number = 0 THEN ''zeroth''
        WHEN @Number = 1 THEN ''first''
        WHEN @Number = 2 THEN ''second''
        WHEN @Number = 3 THEN ''third''
        WHEN @Number = 4 THEN ''fourth''
        WHEN @Number = 5 THEN ''fifth''
        WHEN @Number = 6 THEN ''sixth''
        WHEN @Number = 7 THEN ''seventh''
        WHEN @Number = 8 THEN ''eighth''
        WHEN @Number = 9 THEN ''nineth''
        WHEN @Number = 10 THEN ''tenth''
        WHEN @Number = 11 THEN ''eleventh''
        WHEN @Number = 12 THEN ''twelfth''
        WHEN @Number = 13 THEN ''thirteenth''
        WHEN @Number = 14 THEN ''fourteenth''
        WHEN @Number = 15 THEN ''fifteenth''
        WHEN @Number = 16 THEN ''sixteenth''
        WHEN @Number = 17 THEN ''seventeenth''
        WHEN @Number = 18 THEN ''eighteenth''
        WHEN @Number = 19 THEN ''nineteenth''

        WHEN @Number = 20 THEN ''twentieth''
        WHEN @Number BETWEEN 21 AND 29 THEN ''twenty '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number = 30 THEN ''thirtieth'' 
        WHEN @Number BETWEEN 31 AND 39 THEN ''thirty '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number = 40 THEN ''fortieth''
        WHEN @Number BETWEEN 41 AND 49 THEN ''forty '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number = 50 THEN ''fiftieth'' 
        WHEN @Number BETWEEN 51 AND 59 THEN ''fifty '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number = 60 THEN ''sixtieth''
        WHEN @Number BETWEEN 61 AND 69 THEN ''sixty '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number = 70 THEN ''seventieth''
        WHEN @Number BETWEEN 71 AND 79 THEN ''seventy '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number = 80 THEN ''eightieth''
        WHEN @Number BETWEEN 81 AND 89 THEN ''eighty '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number = 90 THEN ''ninetieth''
        WHEN @Number BETWEEN 91 AND 99 THEN ''ninety '' + dbo.NumberToOrdinal(@Number % 10)

        WHEN @Number BETWEEN 100 AND 999 AND @Number % 100 = 0 THEN dbo.NumberToWords(@Number / 100) + '' hundredth''
        WHEN @Number BETWEEN 100 AND 999 AND @Number % 100 != 0 THEN dbo.NumberToWords(@Number / 100) + '' hundred and '' + dbo.NumberToOrdinal(@Number % 100)

        WHEN @Number BETWEEN 1000 AND 999999 AND @Number % 1000 = 0 THEN dbo.NumberToWords(@Number / 1000) + '' thousandth''
        WHEN @Number BETWEEN 1000 AND 999999 AND @Number % 1000 != 0 AND @Number % 100 = 0 THEN dbo.NumberToWords(@Number / 1000) + '' thousand and '' + dbo.NumberToOrdinal(@Number % 1000)
        WHEN @Number BETWEEN 1000 AND 999999 AND @Number % 1000 != 0 AND @Number % 100 != 0 THEN dbo.NumberToWords(@Number / 1000) + '' thousand '' + dbo.NumberToOrdinal(@Number % 1000)
      END

      SELECT @Word = RTRIM(@Word)

      RETURN @Word
    
    END'
  EXEC sp_executesql @sql
END

DECLARE @i int
DECLARE @j int
DECLARE @guid uniqueidentifier

SET @i = -1
WHILE @i < 1000
BEGIN
  SET @i = @i + 1
  DECLARE @val VARCHAR(200) = dbo.NumberToWords(@i)
  IF NOT EXISTS (SELECT * FROM "RefData" WHERE "value" = @val)
    INSERT INTO "RefData" ("refId", "value") VALUES (convert(uniqueidentifier, hashbytes('SHA', convert(varbinary(128), @i))), @val)
END

SET @i = 0
WHILE @i < 10000
BEGIN
  SET @i = @i + 1  
  IF NOT EXISTS (SELECT * FROM "Data" WHERE "dataId" = @i)
    INSERT INTO "Data" ("dataId", "colourId", "instant", "value") VALUES (@i, 1 + (@i % 147), DATEADD(hour, @i * 27, '1971-05-06'), dbo.NumberToOrdinal(@i))  
    
  SET @j = 0
  WHILE @j < @i % 7
  BEGIN
    SET @j = @j + 1
    SET @guid = convert(uniqueidentifier, hashbytes('SHA', convert(varbinary(128), (@i * @j) % 1000)))
    IF NOT EXISTS (SELECT * FROM "ManyData" WHERE "dataId" = @i AND "refId" = @guid)
    BEGIN
      INSERT INTO "ManyData" ("refId", "dataId", "sort") VALUES (@guid, @i, @j)
    END
  END
  
  SET @j = 0
  WHILE @j < 7
  BEGIN
    SET @j = @j + 1
    IF (@i % @j) = 0
    BEGIN
      IF NOT EXISTS (SELECT * FROM "FieldValues" WHERE "dataId" = @i AND "fieldId" = @j)
        INSERT INTO "FieldValues" ("dataId", "fieldId", "dateValue", "timeValue", "dateTimeValue", "longValue", "doubleValue", "boolValue", "textValue")
          VALUES(
            @i
            , @j
            , CASE WHEN @j = 1 THEN DATEADD(DAY, (0 - @i), '2023-05-06') END 
            , CASE WHEN @j = 2 THEN DATEADD(MINUTE, (0 - @i), '2023-05-06') END 
            , CASE WHEN @j = 3 THEN DATEADD(MINUTE, (0 - @i), '2023-05-06') END 
            , CASE WHEN @j = 4 THEN @i * @j END 
            , CASE WHEN @j = 5 THEN 1 / convert(float, @i) END 
            , CASE WHEN @j = 6 THEN (@i / @j) % 2 END 
            , CASE WHEN @j = 7 THEN dbo.NumberToWords(@i) END 
          )
    END  
  END
END 

