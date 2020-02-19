package markerboard.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.*;

public class ObjectNodeTemplateAdapter extends WrappingTemplateModel implements TemplateHashModel, AdapterTemplateModel {


    ObjectNode wrappedObjectNode;

    public ObjectNodeTemplateAdapter(ObjectNode objectNode, ObjectWrapper ow){
        super(ow);
        this.wrappedObjectNode = objectNode;
    }


    @Override
    public Object getAdaptedObject(Class<?> aClass) {
        return wrappedObjectNode;
    }


    @Override
    public TemplateModel get(String s) throws TemplateModelException {
        return wrap(this.wrappedObjectNode.get(s));
    }


    @Override
    public boolean isEmpty() throws TemplateModelException {
        return this.wrappedObjectNode.size() == 0;
    }
}
