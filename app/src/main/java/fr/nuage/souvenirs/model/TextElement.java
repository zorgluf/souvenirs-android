package fr.nuage.souvenirs.model;

import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

public class TextElement extends Element {

    private MutableLiveData<String> ldText = new MutableLiveData<String>();
    private String text;

    public TextElement() {
        this("");
    }

    public TextElement(String text) {
        this(text,10,10,90,90);
    }

    public TextElement(String text, int left, int top, int right, int bottom) {
        super(left, top, right, bottom);
        setText(text);
    }


    @Override
    public JSONObject completeToJSON(JSONObject json) throws JSONException {
        json.put("text",text);
        return json;
    }

    @Override
    public void completeFromJSON(JSONObject jsonObject) throws JSONException {
        setText(jsonObject.getString("text"),false);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        setText(text,true);
    }

    public void setText(String text, boolean save) {
        this.text = text;
        this.ldText.postValue(this.text);
        if (save) {
            onChange();
        }
    }

    public MutableLiveData<String> getLiveDataText() {
        return ldText;
    }

}
