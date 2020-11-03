package fr.nuage.souvenirs.viewmodel;

import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.TextElement;
import fr.nuage.souvenirs.view.TextElementView;

public class TextElementViewModel extends ElementViewModel {
    private LiveData<String> text;

    public TextElementViewModel(TextElement e) {
        super(e);
        text = Transformations.map(e.getLiveDataText(), text -> {
            return text;
        });
    }

    public LiveData<String> getText() {
        return text;
    }

    public void setText(String t) {
        ((TextElement)element).setText(t);
    }

    public void del() {
        element.delete();
    }
}
