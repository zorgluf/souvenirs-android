package fr.nuage.souvenirs.model.nc;

import androidx.lifecycle.MutableLiveData;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import fr.nuage.souvenirs.model.Element;

public class TextElementNC extends ElementNC {

    private MutableLiveData<String> ldText = new MutableLiveData<String>();
    private String text;

    public TextElementNC() {
        this("");
    }

    public TextElementNC(String text) {
        this(text,10,10,90,90);
    }

    public TextElementNC(String text, int left, int top, int right, int bottom) {
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
        setText(jsonObject.getString("text"));
    }

    @Override
    public void load(APIProvider.ElementResp elementResp) {
        super.load(elementResp);
        setText(elementResp.text);
    }

    @Override
    public APIProvider.ElementResp generateElementResp() {
        APIProvider.ElementResp elementResp = super.generateElementResp();
        elementResp.text = getText();
        return elementResp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.ldText.postValue(this.text);
        onChange();
    }

    public MutableLiveData<String> getLiveDataText() {
        return ldText;
    }

}
