package io.fixprotocol.orchestra2md;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkdownGeneratorTest {

  private MarkdownGenerator generator;
  private ByteArrayOutputStream jsonOutputStream;
  
  @BeforeEach
  void setUp() throws Exception {
    jsonOutputStream = new ByteArrayOutputStream(8096);
    generator = new MarkdownGenerator("/P/", false, false);
  }

  @Test
  void roundTrip() throws Exception {
    String text =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<fixr:repository xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:fixr=\"http://fixprotocol.io/2020/orchestra/repository\">\n"
        + "    <fixr:metadata/>\n"
        + "    <fixr:datatypes/>\n"
        + "    <fixr:codeSets/>\n"
        + "    <fixr:fields>\n"
        + "        <fixr:field type=\"String\" discriminatorId=\"22\" id=\"48\" name=\"SecurityID\" abbrName=\"ID\" added=\"FIX.2.7\">\n"
        + "            <fixr:annotation>\n"
        + "                <fixr:documentation purpose=\"SYNOPSIS\">\n"
        + "         Security identifier value of SecurityIDSource (22) type (e.g. CUSIP, SEDOL, ISIN, etc). Requires SecurityIDSource.\n"
        + "      </fixr:documentation>\n"
        + "            </fixr:annotation>\n"
        + "        </fixr:field>\n"
        + "        <fixr:field type=\"SecurityIDSourceCodeSet\" unionDataType=\"Reserved100Plus\" id=\"22\" name=\"SecurityIDSource\" abbrName=\"Src\" added=\"FIX.2.7\" updated=\"FIX.5.0SP2\" updatedEP=\"161\">\n"
        + "            <fixr:annotation>\n"
        + "                <fixr:documentation purpose=\"SYNOPSIS\">\n"
        + "         Identifies class or source of the SecurityID(48) value.\n"
        + "      </fixr:documentation>\n"
        + "            </fixr:annotation>\n"
        + "        </fixr:field>\n"
        + "        <fixr:field type=\"SecurityStatusCodeSet\" id=\"965\" name=\"SecurityStatus\" abbrName=\"Status\" added=\"FIX.4.4\" addedEP=\"4\">\n"
        + "            <fixr:annotation>\n"
        + "                <fixr:documentation purpose=\"SYNOPSIS\">\n"
        + "         Used for derivatives. Denotes the current state of the Instrument.\n"
        + "      </fixr:documentation>\n"
        + "            </fixr:annotation>\n"
        + "        </fixr:field>\n"
        + "    </fixr:fields>\n"
        + "    <fixr:components>\n"
        + "        <fixr:component id=\"1003\" name=\"Instrument\">\n"
        + "            <fixr:fieldRef id=\"48\" presence=\"required\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:appinfo purpose=\"notes\">blabla</fixr:appinfo>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"22\"/>\n"
        + "            <fixr:fieldRef id=\"965\"/>\n"
        + "            <fixr:componentRef presence=\"required\" id=\"4162\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:appinfo purpose=\"notes\">yada yada</fixr:appinfo>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:componentRef>\n"
        + "            <fixr:groupRef presence=\"required\" id=\"2070\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:appinfo purpose=\"notes\">nonsense</fixr:appinfo>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:groupRef>\n"
        + "        </fixr:component>\n"
        + "        <fixr:component category=\"Common\" added=\"FIX.5.0SP2\" addedEP=\"169\" updated=\"FIX.5.0SP2\" updatedEP=\"211\" id=\"4162\" name=\"OptionExercise\" abbrName=\"OptExer\">\n"
        + "            <fixr:fieldRef id=\"41106\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41107\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Must be set if EncodedExerciseDesc(41108) field is specified and must immediately precede it.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41108\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Encoded (non-ASCII characters) representation of the ExerciseDesc(41106) field in the encoded format specified via the MessageEncoding(347) field.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41109\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41110\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41111\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41112\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41113\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41114\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"41115\" added=\"FIX.5.0SP2\" addedEP=\"169\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"42590\" added=\"FIX.5.0SP2\" addedEP=\"208\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:componentRef added=\"FIX.5.0SP2\" addedEP=\"208\" id=\"4386\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:componentRef>\n"
        + "            <fixr:componentRef added=\"FIX.5.0SP2\" addedEP=\"169\" id=\"4164\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:componentRef>\n"
        + "            <fixr:componentRef added=\"FIX.5.0SP2\" addedEP=\"169\" id=\"4167\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:componentRef>\n"
        + "            <fixr:componentRef added=\"FIX.5.0SP2\" addedEP=\"208\" id=\"4362\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:componentRef>\n"
        + "            <fixr:annotation>\n"
        + "                <fixr:documentation purpose=\"SYNOPSIS\">\n"
        + "         The OptionExercise component is a subcomponent of the Instrument component used to specify option exercise provisions. Its purpose is to identify the opportunities and conditions for exercise, e.g. the schedule of dates on which exercise is allowed. The embedded OptionExerciseExpiration component is used to terminate the opportunity for exercise.\n"
        + "      </fixr:documentation>\n"
        + "                <fixr:documentation purpose=\"ELABORATION\"/>\n"
        + "            </fixr:annotation>\n"
        + "        </fixr:component>\n"
        + "    </fixr:components>\n"
        + "    <fixr:groups>\n"
        + "        <fixr:group category=\"Common\" added=\"FIX.4.4\" id=\"2070\" name=\"EvntGrp\" abbrName=\"Evnt\">\n"
        + "            <fixr:numInGroup id=\"864\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:numInGroup>\n"
        + "            <fixr:fieldRef id=\"865\" added=\"FIX.4.4\" updated=\"FIX.5.0SP2\" updatedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Required if NoEvents(864) &gt; 0.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"866\" added=\"FIX.4.4\" updated=\"FIX.5.0SP2\" updatedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Conditionally required when EventTime(1145) is specified.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"1145\" added=\"FIX.5.0\" updated=\"FIX.5.0SP2\" updatedEP=\"132\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"1827\" added=\"FIX.5.0SP2\" addedEP=\"132\" updated=\"FIX.5.0SP2\" updatedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Conditionally required when EventTimePeriod(1826) is specified.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"1826\" added=\"FIX.5.0SP2\" addedEP=\"132\" updated=\"FIX.5.0SP2\" updatedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Conditionally required when EventTimeUnit(1827) is specified.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"2340\" added=\"FIX.5.0SP2\" addedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"867\" added=\"FIX.4.4\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"868\" added=\"FIX.4.4\" updated=\"FIX.5.0SP2\" updatedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation/>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"1578\" added=\"FIX.5.0SP2\" addedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Must be set if EncodedEventText(1579) field is specified and must immediately precede it.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:fieldRef id=\"1579\" added=\"FIX.5.0SP2\" addedEP=\"161\">\n"
        + "                <fixr:annotation>\n"
        + "                    <fixr:documentation>\n"
        + "         Encoded (non-ASCII characters) representation of the EventText(868) field in the encoded format specified via the MessageEncoding(347) field.\n"
        + "      </fixr:documentation>\n"
        + "                </fixr:annotation>\n"
        + "            </fixr:fieldRef>\n"
        + "            <fixr:annotation>\n"
        + "                <fixr:documentation purpose=\"SYNOPSIS\">\n"
        + "         The EvntGrp is a repeating subcomponent of the Instrument component used to specify straightforward events associated with the instrument. Examples include put and call dates for bonds and options; first exercise date for options; inventory and delivery dates for commodities; start, end and roll dates for swaps. Use ComplexEvents for more advanced dates such as option, futures, commodities and equity swap observation and pricing events.\n"
        + "      </fixr:documentation>\n"
        + "                <fixr:documentation purpose=\"ELABORATION\">\n"
        + "         The EvntGrp contains three different methods to express a \"time\" associated with the event using the EventDate(866) and EventTime(1145) pair of fields or the EventTimeUnit(1827) and EventTimePeriod(1826) pair of fields or EventMonthYear(2340).\n"
        + "         The EventDate(866), and optional EventTime(1145), may be used to specify an exact date and optional time for the event. The EventTimeUnit(1827) and EventTimePeriod(1826) may be used to express a time period associated with the event, e.g. 3-month, 4-years, 2-weeks. The EventMonthYear(2340), and optional EventTime(1145), may be used to express the event as a month of year, with optional day of month or week of month.\n"
        + "         Either EventDate(866) or EventMonthYear(2340), and the optional EventTime(1145), must be specified or EventTimeUnit(1827) and EventTimePeriod(1826) must be specified.\n"
        + "         The EventMonthYear(2340) may be used instead of EventDate(866) when month-year, with optional day of month or week of month, is required instead of a date.\n"
        + "      </fixr:documentation>\n"
        + "            </fixr:annotation>\n"
        + "        </fixr:group>\n"
        + "    </fixr:groups>\n"
        + "    <fixr:messages/>\n"
        + "</fixr:repository>";   
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    ByteArrayOutputStream mdStream = new ByteArrayOutputStream(8096);
    OutputStreamWriter outputWriter = new OutputStreamWriter(mdStream, StandardCharsets.UTF_8);
    generator.generate(inputStream, outputWriter, jsonOutputStream);
    outputWriter.close();
    String md = mdStream.toString();
    //System.out.println(md);
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test
  void table() throws Exception {
    String text ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<fixr:repository xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
        + "                 xmlns:fixr=\"http://fixprotocol.io/2020/orchestra/repository\"\n"
        + "                 xmlns:functx=\"http://www.functx.com\"\n"
        + "                 xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
        + "                 name=\"FIX.5.0SP2\"\n"
        + "                 version=\"FIX.5.0SP2_EP258\">\n"
        + "   <fixr:metadata>\n"
        + "      <dc:title>Orchestra</dc:title>\n"
        + "      <dc:creator>unified2orchestra.xslt script</dc:creator>\n"
        + "      <dc:publisher>FIX Trading Community</dc:publisher>\n"
        + "      <dc:date>2020-07-19T09:28:08.308Z</dc:date>\n"
        + "      <dc:format>Orchestra schema</dc:format>\n"
        + "      <dc:source>FIX Unified Repository</dc:source>\n"
        + "      <dc:rights>Copyright (c) FIX Protocol Ltd. All Rights Reserved.</dc:rights>\n"
        + "   </fixr:metadata>\n"
        + "   <fixr:codeSets>"
        + "     <fixr:codeSet name=\"CommTypeCodeSet\" id=\"13\" type=\"char\">\n"
        + "         <fixr:code name=\"AmountPerContract\"\n"
        + "                    id=\"13008\"\n"
        + "                    value=\"8\"\n"
        + "                    sort=\"8\"\n"
        + "                    added=\"FIX.5.0SP2\"\n"
        + "                    addedEP=\"204\">\n"
        + "            <fixr:annotation>\n"
        + "               <fixr:documentation purpose=\"SYNOPSIS\">\n"
        + "         Amount per contract\n"
        + "      </fixr:documentation>\n"
        + "               <fixr:documentation purpose=\"ELABORATION\">\n"
        + "         Specify ContractMultiplier(231) in the Instrument component if the security is denominated in a size other than the market convention.\n"
        + "This is the second line of an elaboration\n"
        + "      </fixr:documentation>\n"
        + "            </fixr:annotation>\n"
        + "         </fixr:code>\n"
        + "         <fixr:annotation>\n"
        + "            <fixr:documentation purpose=\"SYNOPSIS\">\n"
        + "         Specifies the basis or unit used to calculate the total commission based on the rate.\n"
        + "      </fixr:documentation>\n"
        + "         </fixr:annotation>\n"
        + "      </fixr:codeSet>\n"
        + "   </fixr:codeSets>"
        + "  <fixr:datatypes/>"
        + "</fixr:repository>";
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    ByteArrayOutputStream mdStream = new ByteArrayOutputStream(8096);
    OutputStreamWriter outputWriter = new OutputStreamWriter(mdStream, StandardCharsets.UTF_8);
    generator.generate(inputStream, outputWriter, jsonOutputStream);
    outputWriter.close();
    //String md = mdStream.toString();
    //System.out.println(md);
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test // ODOC-64
  void noFields() throws Exception {
    String text ="<fixr:repository xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:fixr=\"http://fixprotocol.io/2020/orchestra/repository\">\n"
        + "    <fixr:metadata/>\n"
        + "    <fixr:datatypes/>\n"
        + "    <fixr:codeSets>\n"
        + "        <fixr:codeSet type=\"char\" id=\"10001\" name=\"SecurityStatusCodeset\">\n"
        + "            <fixr:code value=\"1\" id=\"10002\" name=\"Active\"/>\n"
        + "        </fixr:codeSet>\n"
        + "        <fixr:codeSet type=\"char\" id=\"10004\" name=\"SecurityStatusCodeset\">\n"
        + "            <fixr:code value=\"1\" id=\"10005\" name=\"Active\"/>\n"
        + "        </fixr:codeSet>\n"
        + "    </fixr:codeSets>\n"
        + "    <fixr:groups/>\n"
        + "    <fixr:messages/>\n"
        + "</fixr:repository>";
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    ByteArrayOutputStream mdStream = new ByteArrayOutputStream(8096);
    OutputStreamWriter outputWriter = new OutputStreamWriter(mdStream, StandardCharsets.UTF_8);
    generator.generate(inputStream, outputWriter, jsonOutputStream);
    outputWriter.close();
    //String md = mdStream.toString();
    //System.out.println(md);
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }
  
  @Test // ODOC-74
  void paragraphBreak() throws Exception {
    String text ="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<fixr:repository xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:fixr=\"http://fixprotocol.io/2020/orchestra/repository\">\n"
        + "    <fixr:metadata/>\n"
        + "    <fixr:datatypes>\n"
        + "        <fixr:datatype name=\"String\"/>\n"
        + "    </fixr:datatypes>\n"
        + "    <fixr:codeSets/>\n"
        + "    <fixr:fields>\n"
        + "        <fixr:field type=\"String\" id=\"48\" name=\"SecurityID\">\n"
        + "            <fixr:annotation>\n"
        + "                <fixr:documentation purpose=\"SYNOPSIS\" contentType=\"text/markdown\">Line 1\n"
        + "\n"
        + "Line 2</fixr:documentation>\n"
        + "            </fixr:annotation>\n"
        + "        </fixr:field>\n"
        + "    </fixr:fields>\n"
        + "    <fixr:components/>\n"
        + "    <fixr:groups/>\n"
        + "    <fixr:messages/>\n"
        + "</fixr:repository>\n";
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    ByteArrayOutputStream mdStream = new ByteArrayOutputStream(8096);
    OutputStreamWriter outputWriter = new OutputStreamWriter(mdStream, StandardCharsets.UTF_8);
    generator.generate(inputStream, outputWriter, jsonOutputStream);
    outputWriter.close();
    String md = mdStream.toString();
    assertTrue(md.contains("Line 1/P/Line 2"));
    //System.out.println(md);
    //String errors = jsonOutputStream.toString();
    //System.out.println(errors);
  }

  
  @Test // ODOC-33
  void missingNumInGroup() throws Exception {
    String text ="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<fixr:repository xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:fixr=\"http://fixprotocol.io/2020/orchestra/repository\">\n"
        + "    <fixr:metadata/>\n"
        + "    <fixr:datatypes>\n"
        + "        <fixr:datatype name=\"String\"/>\n"
        + "    </fixr:datatypes>\n"
        + "    <fixr:codeSets/>\n"
        + "    <fixr:fields>\n"
        + "        <fixr:field type=\"String\" id=\"48\" name=\"SecurityID\"/>\n"
        + "    </fixr:fields>\n"
        + "    <fixr:components/>\n"
        + "    <fixr:groups>\n"
        + "        <fixr:group id=\"1012\" name=\"Parties\"/>"
        + "    </fixr:groups>\n"
        + "    <fixr:messages/>\n"
        + "</fixr:repository>\n";
    
    InputStream inputStream = new ByteArrayInputStream(text.getBytes());
    ByteArrayOutputStream mdStream = new ByteArrayOutputStream(8096);
    OutputStreamWriter outputWriter = new OutputStreamWriter(mdStream, StandardCharsets.UTF_8);
    generator.generate(inputStream, outputWriter, jsonOutputStream);
    outputWriter.close();
    //String md = mdStream.toString();
    //System.out.println(md);
    String errors = jsonOutputStream.toString();
    //System.out.println(errors);
    assertTrue(errors.contains("Unknown numInGroup for group"));
    assertTrue(errors.contains("Group has no members"));
  }
}
