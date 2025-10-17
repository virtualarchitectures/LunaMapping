package paletai.mapping;

import java.util.ArrayList;

import processing.data.XML;

/**
 * A class that manages a collection of MediaItems as a single scene. Handles
 * rendering, media control, and calibration for multiple media elements.
 * 
 * <p>
 * Key features include:
 * </p>
 * <ul>
 * <li>Media item management and organization</li>
 * <li>Active media selection for calibration</li>
 * <li>Scene activation/deactivation</li>
 * <li>Batch operations on all media items</li>
 * </ul>
 * 
 * @author Daniel Corbani
 * @version 1.0
 * @see MediaItem
 */
public class Scene {
	//int id;
	public ArrayList<MediaItem> mediaItems = new ArrayList<MediaItem>();
    //public ArrayList<MediaItem> generatorItems = new ArrayList<MediaItem>();
	public XML sceneXML;
	boolean isActive;
	int currentMediaCalibration = -1;
	private int alpha = 255;

	public Scene() {
		//this.id = id;
		sceneXML = new XML("Scene");
		//sceneXML.setInt("id", id);
		sceneXML.addChild("Medias");
		isActive = false;
	}

	Scene(XML scene) {
		//id = scene.getInt("id");
		sceneXML = scene;
		isActive = false;

	}
	
	void setThumbnailPosition(int x, int y) {
		for (MediaItem media : mediaItems) {
			int tX = 1+media.mediaId*200;
			//PApplet.println(media.fileName + " tX = " + tX);
			media.setThumbnailPosition((int)(x+tX), y+30);
		}
	}

	void render() {
		if (isActive) {
			for (MediaItem media : mediaItems) {
				media.render();
			}
		}
	}

	void addMedia(MediaItem newMedia) {
		mediaItems.add(newMedia);
		if (currentMediaCalibration < 0)
			currentMediaCalibration = 0;
	}

    void delMedia(int index) {
        mediaItems.remove(index);
    }

	XML saveXML() {
		XML xml = new XML("Scene");
		for (MediaItem media : mediaItems) {
			xml.addChild(media.saveXML());
		}
		return xml;
	}

	public void deactivate() {
		for (MediaItem media : mediaItems) {
			media.stopMedia();
			media.offCalibration();
			media.hideControls();
		}
		isActive = false;
	}

	public void activate() {
		for (MediaItem media : mediaItems) {
			media.playMedia();
			media.showControls();
		}
		isActive = true;
	}

	public void toggleActivation() {
		isActive = !isActive;
		if (isActive) {
			for (MediaItem media : mediaItems) {
				media.playMedia();
			}
		} else {
			for (MediaItem media : mediaItems) {
				media.stopMedia();
			}
		}
	}
}