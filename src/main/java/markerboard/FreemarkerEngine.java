package markerboard;

import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.*;
import markerboard.util.ArrayNodeAwareObjectWrapper;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

import static freemarker.template.Configuration.VERSION_2_3_29;

public class FreemarkerEngine {

    public static Configuration freemarkerConfiguration = new Configuration(VERSION_2_3_29) {{
        setLogTemplateExceptions(false);

        setObjectWrapper(new ArrayNodeAwareObjectWrapper(getVersion()));
    }};


    public static String processTemplate(ObjectNode rootNode, String template) throws IOException, TemplateException {

        final Template t = new Template("ComponentTemplate", new StringReader(template), freemarkerConfiguration);
        final Writer w = new StringWriter();

        t.process(Collections.singletonMap("root", rootNode), w);
        return w.toString();
    }

    public static void setFreemarkerIncludeJacksonTextNodeQuotes(boolean includeQuotes) {
        ObjectWrapper wrapper = freemarkerConfiguration.getObjectWrapper();
        if(wrapper instanceof ArrayNodeAwareObjectWrapper) {
            ((ArrayNodeAwareObjectWrapper)wrapper).setIncludeQuotesOnTextNodes(includeQuotes);
        }
    }

}
