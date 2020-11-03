package fr.nuage.souvenirs.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class UnknownElement extends Element {

    private JSONObject jsonDescription;

    public UnknownElement() { super();}


    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        Iterator<String> keys = jsonDescription.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (! json.has(key)) {
                json.put(key,jsonDescription.get(key));
            }
        }
        return json;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) {
        jsonDescription = jsonObject;
    }
}
