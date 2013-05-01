package strings;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResValuesFile;
import brut.androlib.res.data.value.ResScalarValue;
import brut.androlib.res.util.ExtFile;
import brut.androlib.res.util.ExtMXSerializer;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;

public class StringsXML {
  final static boolean           debug         = true;
  final static AndrolibResources res           = new AndrolibResources();
  final static ExtMXSerializer   xmlSerializer = AbstractAndrolibResources
                                                   .getResXmlSerializer();
  final static JsonFactory       json          = new JsonFactory();

  public static void p(final Object msg) {
    if (debug) {
      System.out.println(msg.toString());
    }
  }

  public static void toJSON(final ResValuesFile input,
      final File outputDirectory) throws Exception {
    String[] paths = input.getPath().split(File.separator);
    final String outName = paths[paths.length - 1];
    final File outFile = new File(outputDirectory, outName);
    p("Saving to: " + outFile);
    JsonGenerator generator = json.createGenerator(new FileOutputStream(
        outFile), JsonEncoding.UTF8);

    // Ensure output stream is auto closed when generator.close() is called.
    generator.configure(Feature.AUTO_CLOSE_TARGET, true);
    generator.configure(Feature.AUTO_CLOSE_JSON_CONTENT, true);
    generator.configure(Feature.FLUSH_PASSED_TO_STREAM, true);
    generator.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
    generator.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
    generator.configure(Feature.QUOTE_FIELD_NAMES, true);
    // generator.configure(Feature.ESCAPE_NON_ASCII, true); // don't escape non ascii
    generator.useDefaultPrettyPrinter();

    // ResStringValue extends ResScalarValue which has field mRawValue
    final Field valueField = ResScalarValue.class.getDeclaredField("mRawValue");
    valueField.setAccessible(true);

    generator.writeStartObject();
    for (ResResource resource : input.listResources()) {
      if (input.isSynthesized(resource)) {
        continue;
      }

      final String name = resource.getResSpec().getName();
      // Get the value field from the ResStringValue object.
      final String value = (String) valueField.get(resource.getValue());
      generator.writeStringField(name, value);
    }
    generator.writeEndObject();
    generator.flush();
    generator.close();
  }

  public static void run(final File input, final File outputDirectory)
      throws Exception {
    final ExtFile apkFile = new ExtFile(input);
    ResTable table = res.getResTable(apkFile, true);
    ResValuesFile stringsXML = null;
    for (ResPackage pkg : table.listMainPackages()) {
      p(pkg);
      for (ResValuesFile values : pkg.listValuesFiles()) {
        p(values.getPath());
        if (values.getPath().endsWith("/strings.xml")) {
          stringsXML = values;
          break;
        }
      }
      if (stringsXML != null) {
        break;
      }
    }

    toJSON(stringsXML, outputDirectory);
    p("complete");
  }

  public static void main(String[] args) throws Exception {
    final File input = new File("/tmp/apk/apk.apk");
    final File outputDirectory = new File("/tmp/apk/");
    run(input, outputDirectory);
  }
}