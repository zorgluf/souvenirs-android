package fr.nuage.souvenirs.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Element;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.PageBuilder;
import fr.nuage.souvenirs.model.PaintElement;
import fr.nuage.souvenirs.model.TextElement;
import fr.nuage.souvenirs.model.TilePageBuilder;
import fr.nuage.souvenirs.model.UnknownElement;

public class PageViewModel extends ViewModel {

    private Page page;
    private LiveData<ArrayList<ElementViewModel>> elements;
    private MutableLiveData<Boolean> paintMode = new MutableLiveData<>();

    public PageViewModel(Page page) {
        super();
        this.page = page;
        paintMode.postValue(false);
        elements = Transformations.map(page.getLiveDataElements(), elements -> {
            ArrayList<ElementViewModel> out = new ArrayList<ElementViewModel>();
            for (Element element : elements) {
                ElementViewModel eVM = null;
                //check if already exists
                ArrayList<ElementViewModel> els = getElements().getValue();
                if (els != null) {
                    for (ElementViewModel elVM : els) {
                        if (element.getId().equals(elVM.getId().getValue())) {
                            eVM = elVM;
                            break;
                        }
                    }
                }
                if (eVM == null) {
                    if (element.getClass() == TextElement.class) {
                        eVM = new TextElementViewModel((TextElement) element);
                    } else if (element.getClass() == ImageElement.class) {
                        eVM = new ImageElementViewModel((ImageElement) element);
                    } else if (element.getClass() == PaintElement.class) {
                        eVM = new PaintElementViewModel((PaintElement) element);
                    } else {
                        eVM = new UnknownElementViewModel((UnknownElement)element);
                    }
                }
                out.add(eVM);
            }
            return out;
        });
    }

    public LiveData<ArrayList<ElementViewModel>> getElements() {
        return elements;
    }

    public UUID getId() {
        return page.getId();
    }

    public void delete() {
        page.delete();
    }

    public void moveUp() { page.moveUp(); }

    public void moveDown() { page.moveDown(); }

    public MutableLiveData<Boolean> getLdPaintMode() {
        return paintMode;
    }

    public Page getPage() {
        return page;
    }

    public void addImage(InputStream input, String mime) {
        ImageElement imageElement = page.createImageElement();
        imageElement.setImage(input,mime);
        PageBuilder pageBuilder = (page.getAlbum().getDefaultStyle().equals(Album.STYLE_TILE)) ? new TilePageBuilder() : new PageBuilder();
        pageBuilder.applyDefaultStyle(page);
    }

    public void addText() {
        page.createTextElement();
        PageBuilder pageBuilder = (page.getAlbum().getDefaultStyle().equals(Album.STYLE_TILE)) ? new TilePageBuilder() : new PageBuilder();
        pageBuilder.applyDefaultStyle(page);
    }

    public void swapElements(UUID oriElementUUID, UUID destElementUUID) {
        //swap positions of elements
        Element ori = page.getElement(oriElementUUID);
        Element dest = page.getElement(destElementUUID);
        int t_top = ori.getTop();
        int t_bottom = ori.getBottom();
        int t_right = ori.getRight();
        int t_left = ori.getLeft();
        ori.setTop(dest.getTop());
        ori.setBottom(dest.getBottom());
        ori.setLeft(dest.getLeft());
        ori.setRight(dest.getRight());
        dest.setTop(t_top);
        dest.setBottom(t_bottom);
        dest.setLeft(t_left);
        dest.setRight(t_right);
    }


    public int getNbImage() {
        return page.getNbImage();
    }

    public int getNbText() {
        return page.getNbTxt();
    }

    public MutableLiveData<Boolean> getPaintMode() {
        return paintMode;
    }

    public void setPaintMode(boolean b) {
        paintMode.postValue(b);
    }

    public PaintElementViewModel getPaintElement() {
        if (elements.getValue() != null) {
            for (ElementViewModel e : elements.getValue()) {
                if (e.getClass().equals(PaintElementViewModel.class)) {
                    return (PaintElementViewModel)e;
                }
            }
        }
        return null;
    }

    public void startPaintMode() {
        //create paint element if needed
        if (!page.hasPaintElement()) {
            page.createPaintElement();
        }
        //set draw mode on view model
        setPaintMode(true);
    }
}
