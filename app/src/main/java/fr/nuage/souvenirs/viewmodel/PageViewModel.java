package fr.nuage.souvenirs.viewmodel;

import android.graphics.Bitmap;
import android.webkit.MimeTypeMap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.IntStream;

import fr.nuage.souvenirs.model.AudioElement;
import fr.nuage.souvenirs.model.Element;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.PaintElement;
import fr.nuage.souvenirs.model.TextElement;
import fr.nuage.souvenirs.model.TilePageBuilder;
import fr.nuage.souvenirs.model.UnknownElement;
import fr.nuage.souvenirs.model.VideoElement;

public class PageViewModel extends ViewModel {

    public static final int AUDIO_MODE_NONE = 0;
    public static final int AUDIO_MODE_ON = 1;
    public static final int AUDIO_MODE_OFF = 2;

    private final Page page;
    private final LiveData<ArrayList<ElementViewModel>> ldElements;
    private final ArrayList<ElementViewModel> elements = new ArrayList<>();
    private final MutableLiveData<Boolean> paintMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> editMode = new MutableLiveData<>();
    private final LiveData<Integer> audioMode;


    public PageViewModel(Page page) {
        super();
        this.page = page;
        editMode.postValue(false);
        paintMode.postValue(false);
        ldElements = Transformations.map(page.getLiveDataElements(), elementsModel -> {
            updateElements(elementsModel);
            return new ArrayList<>(elements);
        });
        audioMode = Transformations.map(page.getLiveDataElements(), elements -> {
            int mode = AUDIO_MODE_NONE;
            for (Element element : elements) {
                if (element.getClass().equals(AudioElement.class)) {
                    AudioElement audioElement = (AudioElement) element;
                    if (audioElement.isStop()) {
                        mode = AUDIO_MODE_OFF;
                    } else {
                        mode = AUDIO_MODE_ON;
                    }
                }
            }
            return mode;
        });
    }

    private void updateElements(ArrayList<Element> elements) {
        //remove deleted pages
        int i = 0;
        while (i < this.elements.size()) {
            ElementViewModel evm = this.elements.get(i);
            if (elements.stream().filter(element -> element.getId().equals(evm.getId())).count() == 0) {
                this.elements.remove(i);
            } else {
                i++;
            }
        }
        //create new elements
        for (int j = 0; j < elements.size(); j++) {
            Element e = elements.get(j);
            int vmIndex = IntStream.range(0, this.elements.size())
                    .filter(k -> this.elements.get(k).getId().equals(e.getId()))
                    .findFirst()
                    .orElse(-1);
            if (vmIndex == -1) {
                ElementViewModel eVM;
                if (e.getClass() == TextElement.class) {
                    eVM = new TextElementViewModel((TextElement) e);
                } else if (e.getClass() == ImageElement.class) {
                    eVM = new ImageElementViewModel((ImageElement) e);
                } else if (e.getClass() == PaintElement.class) {
                    eVM = new PaintElementViewModel((PaintElement) e);
                } else if (e.getClass() == AudioElement.class) {
                    eVM = new AudioElementViewModel((AudioElement) e);
                } else if (e.getClass() == VideoElement.class) {
                    eVM = new VideoElementViewModel((VideoElement) e);
                } else {
                    eVM = new UnknownElementViewModel((UnknownElement)e);
                }
                this.elements.add(j,eVM);
            } else {
                Collections.swap(this.elements,vmIndex,j);
            }
        }
    }

    public LiveData<ArrayList<ElementViewModel>> getLdElements() {
        return ldElements;
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

    public MutableLiveData<Boolean> getLdEditMode() {
        return editMode;
    }

    public LiveData<Integer> getLdAudioMode() {
        return audioMode;
    }

    public Page getPage() {
        return page;
    }

    public void addImage(InputStream input, String mime, String displayName, int size) {
        ImageElement imageElement = page.createImageElement();
        imageElement.setImage(input,mime);
        imageElement.setName(displayName);
        imageElement.setSize(size);
        addImage(imageElement);
    }

    public void addImage(ImageElement imageElement) {
        imageElement.setTransformType(ImageElement.ZOOM_OFFSET);
        TilePageBuilder pageBuilder = new TilePageBuilder();
        pageBuilder.applyDefaultStyle(page);
    }

    /**
     * Import existing file
     * @param pendingPhotoFile
     */
    public void addImage(File pendingPhotoFile) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(pendingPhotoFile.getAbsolutePath()));
        ImageElement imageElement = page.createImageElement();
        imageElement.setImagePath(pendingPhotoFile.getPath());
        imageElement.setMimeType(mimeType);
        addImage(imageElement);
    }

    public void addText() {
        page.createTextElement();
        TilePageBuilder pageBuilder = new TilePageBuilder();
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

    public boolean getPaintMode() {
        return paintMode.getValue();
    }

    public void setPaintMode(boolean b) {
        paintMode.postValue(b);
    }

    public PaintElementViewModel getPaintElement() {
        if (ldElements.getValue() != null) {
            for (ElementViewModel e : ldElements.getValue()) {
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
            PaintElement paintElement = page.createPaintElement();
            PaintElementViewModel paintElementViewModel = new PaintElementViewModel(paintElement);
            paintElementViewModel.setImage(Bitmap.createBitmap(10,10, Bitmap.Config.ARGB_8888));
        }
        //set draw mode on view model
        setPaintMode(true);
    }

    public ElementViewModel getElement(UUID elementId) {
        for (ElementViewModel elementViewModel: elements) {
            if (elementId.equals(elementViewModel.getId().getValue())) {
                return  elementViewModel;
            }
        }
        return null;
    }

    public void addAudio(InputStream inputAudio, String mimeType) {
        AudioElement audioElement = page.createAudioElement();
        if (inputAudio == null) {
            //insert stop audio
            audioElement.setStop(true,true);
        } else {
            audioElement.setAudio(inputAudio,mimeType);
        }
    }

    public void removeAudio() {
        page.removeAudio();
    }

    public AudioElement getAudioElement() {
        for (Element element : page.getElements()) {
            if (element.getClass().equals(AudioElement.class)) {
                return (AudioElement)element;
            }
        }
        return null;
    }

    public void addVideo(InputStream input, String mime, String displayName, int size) {
        VideoElement videoElement = page.createVideoElement();
        videoElement.setVideo(input,mime);
        videoElement.setName(displayName);
        videoElement.setSize(size);
        addImage(videoElement);
    }

    public ArrayList<VideoElementViewModel> getVideoElements() {
        ArrayList<VideoElementViewModel> out = new ArrayList<>();
        for (ElementViewModel element : elements) {
            if (element.getClass().equals(VideoElementViewModel.class)) {
                out.add((VideoElementViewModel) element);
            }
        }
        return out;
    }
}
