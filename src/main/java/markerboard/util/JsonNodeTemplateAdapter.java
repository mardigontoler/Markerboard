package markerboard.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.*;
import lombok.NonNull;
import lombok.Setter;

import java.util.Optional;


/**
 * We have code that's using Jackson tree-model classes as the data model for freemarker,
 * but it doesn't seem like they're correctly handling array nodes inside of other objects.
 * This class adapts Jackson ArrayNodes to the TemplateSequenceModel that Freemarker expects,
 * se we can treat them as a sequence in freemarker:
 * if the data is an object node containing an ArrayNode like this:
 * {
 *    "items": [1,2,3]
 * }
 *
 * then the template can reference an item with ${items[0]}
 *
 *
 * This model also implements TemplateHashModel,
 * so if you try to use this model in a template as a hash, then this model will try to find an internal
 * value in a name/value pair with a name matching the key to the hash.
 *
 * For example,
 * if the data model looked like this:
 * {
 *     "valuePair": [
 *          {"name": "x", "value":"foo"},
 *          {"name": "y", "value":"bar"},
 *          {"name": "z", "value":"yak"}
 *     ]
 * }
 * Then this adapter would let us refer to the value of y
 * by using this:
 * ${valuePair.y}
 * when normally you would have to access it like this:
 * ${valuePair?filter(p->p.name="\"y\"")?first.value}
 *
 *
 * In addition, if this object exposes an "iterator" method that you can call from
 * freemarker that yields an iterator over the JsonNodes in the ArrayNode
 */
public class JsonNodeTemplateAdapter extends WrappingTemplateModel implements TemplateSequenceModel, AdapterTemplateModel, TemplateHashModel {

    private boolean isArrayNode;
    private final JsonNode jsonNode;

    @Setter
    private boolean includeQuotesOnTextNode = true;  // TextNodes have quotes when evaluated, by default


    public JsonNodeTemplateAdapter(JsonNode jsonNode, ObjectWrapper ow, boolean includeQuotesOnTextNode) {
        super(ow);
        this.jsonNode = jsonNode;
        this.includeQuotesOnTextNode = includeQuotesOnTextNode;
        this.isArrayNode = jsonNode instanceof ArrayNode;
    }


    @Override
    public int size() throws TemplateModelException {
        if(isArrayNode)
            return jsonNode.size();
        else throw new TemplateModelException("Cannot access size. Not an array node.");
    }


    /**
     * Gets a wrapped JsonNode from the ArrayNode with an index.
     * This lets the ArrayNode items be accessed with the [] operator.
     * @param index index into the ArrayNode
     * @return wrapped JsonNode
     * @throws TemplateModelException if it can't wrap the item at the index
     */
    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        // make sure that the object returned is also wrapped to the freemarker model
        if(isArrayNode) {
            JsonNode node = jsonNode.get(index);
            return getWrappedValue(node);
        }
        else throw new TemplateModelException("Cannot get with index, not an array node!");
    }


    @Override
    public Object getAdaptedObject(Class hint) {
        return jsonNode;
    }


    /**
     * Gets a value from the array node by a string key.
     * This searches through the underlying arraynode and returns the value
     * from a name/value pair object with a name equal to the key.
     *
     * If the key is "iterator" then this exposes an iterator over the underlying ArrayNode
     */
    @Override
    public TemplateModel get(String key) throws TemplateModelException {

        // Adding this because some templates already exist that use an iterator on the array node.
        // That seems to have worked even though the ArrayNodes weren't natively wrapped as sequences.
        // Essentially, if this model _seems_ to be used as a hash but the property is
        // called iterator, then we instead return a TemplateMethodModelEx that's the Iterator<JsonNode>
        // of the ArrayNode
        if(jsonNode instanceof ObjectNode) return getWrappedValue(jsonNode.get(key));
        if(key.equals("iterator") && isArrayNode) {
            return (TemplateMethodModelEx) args -> wrap(jsonNode.iterator());
        }
        else {
            // search for a the value in a name/value node that has a name matching the key
            Optional<JsonNode> matchingValue = this.findAsNameValuePairValue(key);
            if (matchingValue.isPresent()){
                return getWrappedValue(matchingValue.get());
            }
            else {
                throw new TemplateModelException(key + " is not a known element of this ArrayNode object");
            }
        }
    }


    @Override
    public boolean isEmpty() throws TemplateModelException {
        try {
        return jsonNode.size() == 0;
        } catch (Exception e){
            throw new TemplateModelException("Error getting size of node");
        }
    }


    /**
     * Searches the ArrayNode for a JsonNode that has fields "name" and "value"
     * and searches
     * @param key
     * @return
     */
    Optional<JsonNode> findAsNameValuePairValue(@NonNull String key) {
        if(!isArrayNode) return Optional.empty();
        for(JsonNode currentNode : this.jsonNode) {
            if ( !currentNode.isObject() || !currentNode.has("name") || !currentNode.has("value")) {
                return Optional.empty(); // short circuit return false. This isn't an array of name/value pairs.
            }
            if (currentNode.get("name").textValue().equals(key)){
                return Optional.of(currentNode.get("value"));
            }
        }
        return Optional.empty();
    }


    TemplateModel getWrappedValue(JsonNode node) throws TemplateModelException {
        if(node.isTextual() && !includeQuotesOnTextNode){
            return wrap(node.textValue());
        }
        return wrap(node);
    }
}
