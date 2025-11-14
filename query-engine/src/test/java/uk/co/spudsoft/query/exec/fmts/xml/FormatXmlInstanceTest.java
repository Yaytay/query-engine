/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.exec.fmts.xml;

import com.google.common.collect.ImmutableList;
import inet.ipaddr.IPAddressString;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.FormatXml;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.procs.ListReadStream;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.FormatXmlCharacterReference;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.fmts.ValueFormatters;
import uk.co.spudsoft.query.logging.Log;

/**
 *
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class FormatXmlInstanceTest {
  
  private static final Logger logger = LoggerFactory.getLogger(FormatXmlInstanceTest.class);

  private Types buildTypes() {
    Types types = new Types();
    types.putIfAbsent("Boolean", DataType.Boolean);
    types.putIfAbsent("Date", DataType.Date);
    types.putIfAbsent("DateTime", DataType.DateTime);
    types.putIfAbsent("Double", DataType.Double);
    types.putIfAbsent("Float", DataType.Float);
    types.putIfAbsent("Integer", DataType.Integer);
    types.putIfAbsent("Long", DataType.Long);
    types.putIfAbsent("String", DataType.String);
    types.putIfAbsent("Time", DataType.Time);
    return types;
  }

  private void deleteWithoutError(FileSystem fs, String path) {
    try {
      fs.deleteBlocking(path);
    } catch (Throwable ex) {
    }
  }
  
  @Test
  public void testDefaultStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    vertx.runOnContext(v1 -> {
      String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";    

      FormatXml defn = FormatXml.builder()
              .fieldInitialLetterFix(null)
              .fieldInvalidLetterFix(null)
              .build();

      FileSystem fs = vertx.fileSystem();
      if (!fs.existsBlocking("target/temp")) {
        fs.mkdirBlocking("target/temp");
      }
      deleteWithoutError(fs, outfile);
      RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
      PipelineContext pipelineContext = new PipelineContext("test", requestContext);
      
      fs.open(outfile, new OpenOptions().setCreate(true).setSync(true))
              .compose(writeStream -> {
                FormatXmlInstance instance = defn.createInstance(vertx, pipelineContext, writeStream);

                Types types = buildTypes();
                List<DataRow> rowsList = new ArrayList<>();
                for (int i = 0; i < 10000; ++i) {
                  rowsList.add(createDataRow(types, i));
                }
                return instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(pipelineContext, vertx.getOrCreateContext(), rowsList), types))
                        .compose(v2 -> instance.getFinalFuture());
              })
              .onComplete(ar -> {
                if (ar.failed()) {
                  testContext.failNow(ar.cause());
                } else {
                  testContext.verify(() -> {
                    String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
                    assertThat(outstring, startsWith(
                            """
                            <?xml version='1.0' encoding='utf-8'?><data><row><Date>1971-05-01</Date><DateTime>1971-05-01T00:00</DateTime><Double>0.0</Double><Float>0.0</Float><Integer>0</Integer><Long>0</Long><String>This is row \u2013 0</String><Time>00:00</Time><Telephonecontactdetails>01234</Telephonecontactdetails></row><row><Boolean>false</Boolean><DateTime>1971-05-02T01:01</DateTime><Double>1.1</Double><Float>1.1</Float><Integer>1</Integer><Long>10000000</Long><String>This is row \u2013 1</String><Time>01:01</Time></row><row><Boolean>true</Boolean><Date>1971-05-03</Date><Double>2.2</Double><Float>2.2</Float><Integer>2</Integer><Long>20000000</Long><String>This is row \u2013 2</String><Time>02:02</Time><Telephonecontactdetails>01234</Telephonecontactdetails></row>
                            """.trim()
                    ));
                  });
                  testContext.completeNow();
                }
              });
    });
  }

  @Test
  public void testCharacterReferenceReplacements(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";    

    FormatXml defn = FormatXml.builder()
            .fieldInitialLetterFix(null)
            .fieldInvalidLetterFix(null)
            .indent(true)
            .characterReferences(
                    ImmutableList.of(
                            FormatXmlCharacterReference.builder().replace("\u2013").with("#x2013").build()
                    )
            )
            .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    deleteWithoutError(fs, outfile);
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));

    FormatXmlInstance instance = defn.createInstance(vertx, pipelineContext, writeStream);

    Types types = buildTypes();
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(pipelineContext, vertx.getOrCreateContext(), rowsList), types))
            .compose(v -> {
              return instance.getFinalFuture();
            })
            .onComplete(ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                testContext.verify(() -> {
                  String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
                  assertThat(outstring, startsWith(
                          """
                          <?xml version='1.0' encoding='utf-8'?>
                          <data>
                            <row>
                              <Date>1971-05-01</Date>
                              <DateTime>1971-05-01T00:00</DateTime>
                              <Double>0.0</Double>
                              <Float>0.0</Float>
                              <Integer>0</Integer>
                              <Long>0</Long>
                              <String>This is row &#x2013; 0</String>
                              <Time>00:00</Time>
                              <Telephonecontactdetails>01234</Telephonecontactdetails>
                            </row>
                            <row>
                              <Boolean>false</Boolean>
                              <DateTime>1971-05-02T01:01</DateTime>
                              <Double>1.1</Double>
                              <Float>1.1</Float>
                              <Integer>1</Integer>
                              <Long>10000000</Long>
                              <String>This is row &#x2013; 1</String>
                              <Time>01:01</Time>
                            </row>
                          """.trim()
                  ));
                });
                testContext.completeNow();
              }
            });
  }

  @Test
  public void testIndentedStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";

    FormatXml defn = FormatXml.builder()
            .indent(true)
            .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    deleteWithoutError(fs, outfile);
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setSync(true));

    FormatXmlInstance instance = defn.createInstance(vertx, pipelineContext, writeStream);

    Types types = buildTypes();
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(pipelineContext, vertx.getOrCreateContext(), rowsList), types))
            .compose(v -> {
              return instance.getFinalFuture();
            })
            .onComplete(ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                testContext.verify(() -> {
                  String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
                  assertThat(outstring, startsWith(
                          """
              <?xml version='1.0' encoding='utf-8'?>
              <data>
                <row>
                  <Date>1971-05-01</Date>
                  <DateTime>1971-05-01T00:00</DateTime>
                  <Double>0.0</Double>
                  <Float>0.0</Float>
                  <Integer>0</Integer>
                  <Long>0</Long>
                  <String>This is row \u2013 0</String>
                  <Time>00:00</Time>
                  <Telephone_contact_details>01234</Telephone_contact_details>
                </row>
                <row>
                  <Boolean>false</Boolean>
                  <DateTime>1971-05-02T01:01</DateTime>
                  <Double>1.1</Double>
                  <Float>1.1</Float>
                  <Integer>1</Integer>
                  <Long>10000000</Long>
                  <String>This is row \u2013 1</String>
                  <Time>01:01</Time>
                </row>
              """.trim()
                  ));
                });
                testContext.completeNow();
              }
            });
  }

  @Test
  public void testIndentedFieldsAsAttributesStream(Vertx vertx, VertxTestContext testContext, TestInfo testInfo) throws IOException {

    String outfile = "target/temp/" + testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName() + ".xml";

    FormatXml defn = FormatXml.builder()
            .indent(true)
            .fieldsAsAttributes(true)
            .build();

    FileSystem fs = vertx.fileSystem();
    if (!fs.existsBlocking("target/temp")) {
      fs.mkdirBlocking("target/temp");
    }
    deleteWithoutError(fs, outfile);
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    WriteStream<Buffer> writeStream = fs.openBlocking(outfile, new OpenOptions().setCreate(true).setSync(true));

    FormatXmlInstance instance = defn.createInstance(vertx, pipelineContext, writeStream);

    Types types = buildTypes();
    List<DataRow> rowsList = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      rowsList.add(createDataRow(types, i));
    }

    instance.initialize(null, null, new ReadStreamWithTypes(new ListReadStream<>(pipelineContext, vertx.getOrCreateContext(), rowsList), types))
            .compose(v -> {
              return instance.getFinalFuture();
            })
            .onComplete(ar -> {
              if (ar.failed()) {
                testContext.failNow(ar.cause());
              } else {
                testContext.verify(() -> {
                  String outstring = FileUtils.readFileToString(new File(outfile), StandardCharsets.UTF_8);
                  assertThat(outstring, equalTo(
                          """
                          <?xml version='1.0' encoding='utf-8'?>
                          <data>
                            <row Date="1971-05-01" DateTime="1971-05-01T00:00" Double="0.0" Float="0.0" Integer="0" Long="0" String="This is row \u2013 0" Time="00:00" Telephone_contact_details="01234"/>
                            <row Boolean="false" DateTime="1971-05-02T01:01" Double="1.1" Float="1.1" Integer="1" Long="10000000" String="This is row \u2013 1" Time="01:01"/>
                            <row Boolean="true" Date="1971-05-03" Double="2.2" Float="2.2" Integer="2" Long="20000000" String="This is row \u2013 2" Time="02:02" Telephone_contact_details="01234"/>
                            <row Boolean="false" Date="1971-05-04" DateTime="1971-05-04T03:03" Float="3.3" Integer="3" Long="30000000" String="This is row \u2013 3" Time="03:03" Telephone_contact_details="None"/>
                            <row Boolean="true" Date="1971-05-05" DateTime="1971-05-05T04:04" Double="4.4" Integer="4" Long="40000000" String="This is row \u2013 4" Time="04:04" Telephone_contact_details="01234"/>
                            <row Boolean="false" Date="1971-05-06" DateTime="1971-05-06T05:05" Double="5.5" Float="5.5" Long="50000000" String="This is row \u2013 5" Time="05:05"/>
                            <row Boolean="true" Date="1971-05-07" DateTime="1971-05-07T06:06" Double="6.6" Float="6.6" Integer="6" String="This is row \u2013 6" Time="06:06" Telephone_contact_details="01234"/>
                            <row Boolean="false" Date="1971-05-08" DateTime="1971-05-08T07:07" Double="7.7" Float="7.7" Integer="7" Long="70000000" Time="07:07"/>
                            <row Boolean="true" Date="1971-05-09" DateTime="1971-05-09T08:08" Double="8.8" Float="8.8" Integer="8" Long="80000000" String="This is row \u2013 8" Telephone_contact_details="01234"/>
                            <row Date="1971-05-10" DateTime="1971-05-10T09:09" Double="9.9" Float="9.9" Integer="9" Long="90000000" String="This is row \u2013 9" Time="09:09" Telephone_contact_details="None"/>
                          </data>
                          """.trim()
                  ));
                });
                testContext.completeNow();
              }
            });
  }

  private DataRow createDataRow(Types types, int rowNum) {
    DataRow row = DataRow.create(types);
    row.put("Boolean", rowNum % 9 == 0 ? null : (rowNum % 2 == 0 ? Boolean.TRUE : Boolean.FALSE));
    row.put("Date", rowNum % 9 == 1 ? null : LocalDate.of(1971, Month.MAY, 1 + rowNum % 29));
    row.put("DateTime", rowNum % 9 == 2 ? null : LocalDateTime.of(1971, Month.MAY, 1 + rowNum % 29, rowNum % 24, rowNum % 60));
    row.put("Double", rowNum % 9 == 3 ? null : rowNum + (double) rowNum / 10);
    row.put("Float", rowNum % 9 == 4 ? null : rowNum + (float) rowNum / 10);
    row.put("Integer", rowNum % 9 == 5 ? null : rowNum);
    row.put("Long", rowNum % 9 == 6 ? null : rowNum * 10000000L);
    row.put("String", rowNum % 9 == 7 ? null : "This is row \u2013 " + rowNum);
    row.put("Time", rowNum % 9 == 8 ? null : LocalTime.of(rowNum % 24, rowNum % 60));
    if (rowNum % 2 == 0) {
      row.put("telephone contact details", "Telephone contact details", DataType.String, "01234");
    } else if (rowNum % 3 == 0) {
      row.put("telephone contact details", "Telephone contact details", DataType.String, "None");
    }
    return row;
  }

  @Test
  public void testGetName() {
    Map<String, String> nameMap = new HashMap<>();
    assertEquals("default", FormatXmlInstance.getName(nameMap, "F", "_", null, "default"));
    assertEquals("F", FormatXmlInstance.getName(nameMap, "F", "_", "   ", "default"));
    assertEquals("FA_", FormatXmlInstance.getName(nameMap, "F", "_", "  A ", "default"));
    assertEquals("Telephone_contact_details", FormatXmlInstance.getName(nameMap, "F", "_", "Telephone contact details", "default"));
  }

  private final ValueFormatters valueFormatters = new ValueFormatters("dd/MM/YYYY", "ss:mm:hh dd/MM/YYYY", "ss:mm:hh", "0.000", "['\"y\"', '\"n\"']",
           "\"", "\"", true, Collections.emptyList());

  @Test
  public void testFormatValue_Boolean() {
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    Log log = new Log(logger, pipelineContext);    
    
    String result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Boolean, true);
    assertEquals("\"y\"", result);

    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Boolean, false);
    assertEquals("\"n\"", result);

    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Boolean, "true");
    assertEquals("\"y\"", result);

    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Boolean, "false");
    assertEquals("\"n\"", result);

    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Boolean, 1);
    assertEquals("\"y\"", result);

    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Boolean, 0);
    assertEquals("\"n\"", result);
  }

  @Test
  public void testFormatValue_Numeric() {
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    Log log = new Log(logger, pipelineContext);    
    
    // Test Double
    String result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Double, 123.456);
    assertEquals("123.456", result);

    // Test Float
    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Float, 456.789f);
    assertEquals("456.789", result);

    // Test Integer
    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Integer, 12345);
    assertEquals("12345", result);

    // Test Long
    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Long, 123456789L);
    assertEquals("123456789", result);

    // Test BigDecimal as Integer
    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Integer, new BigDecimal("999"));
    assertEquals("999", result);
  }

  @Test
  public void testFormatValue_String() {
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    Log log = new Log(logger, pipelineContext);    
    
    String result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.String, "Hello World");
    assertEquals("Hello World", result);

    // Test non-string value converted to string
    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.String, 12345);
    assertEquals("12345", result);
  }

  @Test
  public void testFormatValue_Null() {
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    Log log = new Log(logger, pipelineContext);    
    
    String result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Null, null);
    assertNull(result);
  }

  @Test
  public void testFormatValue_Temporal() {
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    Log log = new Log(logger, pipelineContext);    
    
    // Test Date
    LocalDate date = LocalDate.of(2023, 12, 25);
    String result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Date, date);
    assertEquals("25/12/2023", result);

    // Test DateTime
    LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 45);
    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.DateTime, dateTime);
    assertEquals("45:30:10 25/12/2023", result);

    // Test Time
    LocalTime time = LocalTime.of(10, 30, 45);
    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Time, time);
    assertEquals("45:30:10", result);
  }

  @Test
  public void testFormatValue_IntegerWithNonNumberValue() {
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    Log log = new Log(logger, pipelineContext);    
    
    // Test when Integer type receives a non-Number value
    String result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Integer, "12345");
    assertEquals("12345", result);

    result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Integer, "Four");
    assertEquals("Four", result);
  }

  @Test
  public void testFormatValue_LongWithNonNumberValue() {
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    Log log = new Log(logger, pipelineContext);    
    
    // Test when Long type receives a non-Number value
    String result = FormatXmlInstance.formatValue(log, pipelineContext, valueFormatters, "testColumn", DataType.Long, "123456789");
    assertEquals("123456789", result);
  }
}
