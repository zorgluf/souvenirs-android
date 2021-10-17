package fr.nuage.souvenirs.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.UUID;

import fr.nuage.souvenirs.model.Element;

public abstract class ElementViewModel extends ViewModel {


    protected Element element;
    private LiveData<Integer> left;
    private LiveData<Integer> right;
    private LiveData<Integer> top;
    private LiveData<Integer> bottom;
    private LiveData<UUID> id;
    private MutableLiveData<Boolean> ldIsSelected;

    public ElementViewModel(Element e) {
        super();
        this.element = e;
        left = Transformations.map(e.getLiveDataLeft(), left -> {
            return left;
        });
        right = Transformations.map(e.getLiveDataRight(), right -> {
            return right;
        });
        top = Transformations.map(e.getLiveDataTop(), top -> {
            return top;
        });
        bottom = Transformations.map(e.getLiveDataBottom(), bottom -> {
            return bottom;
        });
        id = Transformations.map(e.getLiveDataId(), id -> {
            return id;
        });
        ldIsSelected = new MutableLiveData<>();
        ldIsSelected.postValue(false);
    }

    public LiveData<Integer> getLeft() {
        return left;
    }

    public LiveData<Integer> getBottom() {
        return bottom;
    }

    public LiveData<Integer> getRight() {
        return right;
    }

    public LiveData<Integer> getTop() {
        return top;
    }

    public LiveData<UUID> getId() {
        return id;
    }

    public LiveData<Boolean> getIsSelected() {
        return ldIsSelected;
    }

    public void delete() {
        element.delete();
    }

    public void setSelected(boolean isSelected) {
        ldIsSelected.postValue(isSelected);
    }

    public void moveToPreviousPage() {
        element.moveToPreviousPage();
    }

    public void moveToNextPage() {
        element.moveToNextPage();
    }

    public void setPosition(int top, int left, int bottom, int right) {
        element.setTop(top);
        element.setLeft(left);
        element.setBottom(bottom);
        element.setRight(right);
    }

    public void bringToFront() {
        element.bringToFront();
    }

    public Element getElement() {
        return element;
    }

}
