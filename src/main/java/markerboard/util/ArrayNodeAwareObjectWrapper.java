package markerboard.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;


/**
 * This is a custom freemarker object wrapper that will use a custom adapter
 * for Jackson ArrayNodes so that we can access them as proper sequences in a freemarker template.
 * It also lets you access values from arrays of name.value pairs as if the array was a hash of those
 * names to those values.
 * @see ArrayNodeTemplateAdapter
 */
public class ArrayNodeAwareObjectWrapper extends DefaultObjectWrapper {


    public ArrayNodeAwareObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }


    /**
     * When Freemarker is handling an Object that is an ArrayNode, this makes it use the custom adapter.
     * @see ArrayNodeTemplateAdapter
     * If it's not, then it goes back to using the default objectwrapper behavior.
     *
     * @param obj object to map to the template data model
     * @return template data model, with Jackson ArrayNodes handled correctly
     * @throws TemplateModelException if there was a problem adapting the object to a sequence model
     */
    @Override
    protected TemplateModel handleUnknownType(final Object obj) throws TemplateModelException {
        if(obj instanceof JsonNode){ // true for JsonNode and any sublcass (ArrayNode, ObjectNode, TextNode, etc...)
            JsonNode node = (JsonNode)obj;
            if (node.isArray()){
                return new ArrayNodeTemplateAdapter((ArrayNode)obj, this);
            }
            else if(node.isObject()) {
                return new ObjectNodeTemplateAdapter((ObjectNode)obj, this);
            }
        }
        // let the superclass handle it if we aren't using a custom adapter
        return super.handleUnknownType(obj);
    }
}
