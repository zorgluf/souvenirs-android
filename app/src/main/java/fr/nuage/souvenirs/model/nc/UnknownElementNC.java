package fr.nuage.souvenirs.model.nc;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import fr.nuage.souvenirs.model.Element;

public class UnknownElementNC extends ElementNC {

    private JSONObject jsonDescription;

    public UnknownElementNC() { super();}


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
