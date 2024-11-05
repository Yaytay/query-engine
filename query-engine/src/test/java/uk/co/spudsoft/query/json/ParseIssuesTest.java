/*
 * Copyright (C) 2024 njt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.json;

import io.vertx.core.json.Json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Pipeline;

/**
 * A collection of tests of specific pipelines that have caused issues.
 * 
 * @author njt
 */
public class ParseIssuesTest {
  
  private static final String pipeline1 = """
                                          {
                                            "source": {
                                              "type": "SQL",
                                              "endpoint": "demo-uat6.targetconnect.net",
                                              "query": "",
                                              "queryTemplate": "SELECT\\\\n  actor.name AS User_Id\\\\n  , student_details.student_number AS Student_Number\\\\n  , actor.active AS Student_Active\\\\n  , actor.first_name\\\\n  , actor.last_name\\\\n  , actor.telephone\\\\n  , actor.email\\\\n<if(args.studentdetails)>\\\\n  , student_details.preferred_telephone\\\\n  , student_details.preferred_email_address\\\\n  , student_details.gender AS Gender\\\\n  , student_details.date_of_birth AS Date_Of_Birth\\\\n  , student_details.mode_of_study\\\\n<endif>\\\\nFROM \\\\n  actor AS actor\\\\n  LEFT JOIN student_details ON actor.student_details = student_details.id\\\\nWHERE\\\\n  actor.type in ('student','graduate')\\\\nGROUP BY \\\\n  actor.name \\\\nORDER BY \\\\n  actor.last_name, actor.first_name;"
                                            },
                                            "formats": [{
                                                "type": "Delimited"
                                              }, {
                                                "type": "HTML"
                                              }, {
                                                "type": "XLSX",
                                                "headerFont": {
                                                  "fontName": "Times New Roman"
                                                },
                                                "bodyFont": {
                                                  "fontName": "Arial"
                                                }
                                              }
                                            ],
                                            "title": "Students",
                                            "description": "Feed of student data from targetconnect",
                                            "sourceEndpoints": [{
                                                "url": "mysql://mysql.uat-swarm-01.gti-prod.zone/targetconnect_master_uat6",
                                                "secret": "tc-master",
                                                "name": "master"
                                              }
                                            ],
                                            "dynamicEndpoints": [{
                                                "input": {
                                                  "source": {
                                                    "type": "SQL",
                                                    "endpoint": "master",
                                                    "query": "select \\\\n  'SQL' as type\\\\n  , domain as 'key'\\\\n  , replace(substring(url, 1, position('?' in url) - 1), 'jdbc:', '') as url\\\\n  , 'tc-demo-uat6' as secret\\\\nfrom\\\\n  dynamic_data_source\\\\nwhere\\\\n  domain = 'demo-uat6.targetconnect.net'"
                                                  }
                                                },
                                                "keyField": "key"
                                              }
                                            ],
                                            "arguments": [{
                                                "name": "studentdetails",
                                                "type": "Integer",
                                                "optional": true,
                                                "validate": true,
                                                "title": "Student Details",
                                                "prompt": "arg1"
                                              }
                                            ],
                                            "processors": [{
                                                "type": "LOOKUP",
                                                "map": {
                                                  "source": {
                                                    "type": "SQL",
                                                    "endpoint": "demo-uat6.targetconnect.net",
                                                    "name": "LKP",
                                                    "query": "select \\\\n\\\\tid\\\\n\\\\t, name\\\\nfrom \\\\n\\\\tlookup_data\\\\nwhere\\\\n\\\\ttype in ('course', 'disability', 'ethnicity', 'faculty', 'feestatus', 'levelOfStudy', 'modeOfStudy', 'nationality', 'qualificationAchievedCode', 'studentCountry', 'studentDepartment', 'studentType', 'yearOfStudy')\\\\n"
                                                  }
                                                },
                                                "lookupKeyField": "id",
                                                "lookupValueField": "name",
                                                "lookupFields": [{
                                                    "valueField": "ModeOfStudy",
                                                    "keyField": "mode_of_study"
                                                  }
                                                ],
                                                "condition": {
                                                  "expression": "hello"
                                                },
                                                "name": ""
                                              }
                                            ]
                                          }
                                          """;
  
  
  @Test
  public void test1() {
    Pipeline pipeline = Json.decodeValue(pipeline1, Pipeline.class);
    assertEquals(1, pipeline.getProcessors().size());
  }
  
}
