package markerboard.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import freemarker.template.*;
import lombok.NonNull;

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
public class ArrayNodeTemplateAdapter extends WrappingTemplateModel implements TemplateSequenceModel, AdapterTemplateModel, TemplateHashModel {

    private final ArrayNode arrayNode;

    public ArrayNodeTemplateAdapter(ArrayNode arrayNode, ObjectWrapper ow) {
        super(ow);
        this.arrayNode = arrayNode;
    }


    @Override
    public int size() throws TemplateModelException {
        return arrayNode.size();
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
        return wrap(arrayNode.get(index));
    }


    @Override
    public Object getAdaptedObject(Class hint) {
        return arrayNode;
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
        if(key.equals("iterator")) {
            return (TemplateMethodModelEx) args -> wrap(arrayNode.iterator());
        }
        else {
            // search for a the value in a name/value node that has a name matching the key
            Optional<JsonNode> matchingNameValuePairNode = this.findNameValuePair(key);
            if (matchingNameValuePairNode.isPresent()){
                return wrap(matchingNameValuePairNode.get());
            }
            else {
                throw new TemplateModelException(key + " is not a known element of this ArrayNode object");
            }
        }
    }


    @Override
    public boolean isEmpty() throws TemplateModelException {
        return arrayNode.size() == 0;
    }


    /**
     * Searches the ArrayNode for a JsonNode that has fields "name" and "value"
     * and searches
     * @param key
     * @return
     */
    Optional<JsonNode> findNameValuePair(@NonNull String key) {
        for(JsonNode jsonNode : this.arrayNode) {
            if ( !jsonNode.isObject() || !jsonNode.has("name") || !jsonNode.has("value")) {
                return Optional.empty(); // short circuit return false. This isn't an array of name/value pairs.
            }
            if (jsonNode.get("name").textValue().equals(key)){
                return Optional.of(jsonNode.get("value"));
            }
        }
        return Optional.empty();
    }
}
