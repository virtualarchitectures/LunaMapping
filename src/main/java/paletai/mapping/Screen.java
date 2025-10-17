package paletai.mapping;

import java.awt.Rectangle;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.data.XML;
import processing.opengl.PGraphics2D;

public class Screen {
	private PApplet p;
	int id;
	int w, h, x, y; // Screen dimensions and position in virtual space
	XML screenXML;
	ArrayList<MediaItem> mediaItems = new ArrayList<MediaItem>();
	ArrayList<MediaItem> nextMediaItems = new ArrayList<MediaItem>();
	public PGraphics2D pg, nextPg; // Offscreen buffer for this screen
	boolean isAssigned;
	int currentScene;
	private boolean inTransition = false;
	private float transitionAlpha = 1.0f;

	public Screen(PApplet p, int id) {
		this.p = p;
		this.id = id;
		unassign();
		screenXML = new XML("Screen");
		screenXML.setInt("id", id);
		// screenXML.addChild("Medias");
		currentScene = 0;
	}

	Screen(PApplet p, XML screen) {
		this.p = p;
		this.id = screen.getInt("id");
		unassign();
		screenXML = screen;
		currentScene = 0;
	}

	XML saveXML() {
		XML xml = new XML("Screen");
		xml.setInt("id", id);
		return xml;
	}

	public void updateMediaList(ArrayList<MediaItem> m) {
		this.mediaItems = new ArrayList<MediaItem>(m);
		this.inTransition = false;
	}

	public void setTransitionState(ArrayList<MediaItem> currentMedia, ArrayList<MediaItem> nextMedia, float progress) {
		this.mediaItems = new ArrayList<MediaItem>(currentMedia);
		this.nextMediaItems = new ArrayList<MediaItem>(nextMedia);
		this.inTransition = true;
		this.transitionAlpha = progress;
	}

	void unassign() {
		this.w = 1280;
		this.h = 720;
		this.x = 0;
		this.y = 0;
		isAssigned = false;
		pg = (PGraphics2D) p.createGraphics(w, h, PConstants.P2D);
		nextPg = (PGraphics2D) p.createGraphics(w, h, PConstants.P2D);
		for (MediaItem media : mediaItems) {
			media.assignToDisplay(w, h, id);
		}
	}

	public void assignToDisplay(Rectangle bounds) {
		this.w = bounds.width;
		this.h = bounds.height;
		this.x = bounds.x;
		this.y = bounds.y;
		isAssigned = true;
		pg = (PGraphics2D) p.createGraphics(w, h, PConstants.P2D);
		nextPg = (PGraphics2D) p.createGraphics(w, h, PConstants.P2D);
		for (MediaItem media : mediaItems) {
			media.assignToDisplay(w, h, id);
		}
	}


	public void render(int mousex, int mousey) {
		int localMousex = mousex;
		int localMousey = mousey;
		// Update the offscreen buffer
		updateGraphics(localMousex, localMousey);
		show();
	}

	public void show() {
		// Draw the buffer at the correct position
		if (isAssigned) {
			p.image(pg, x, y);
		}
	}

	void updateGraphics(int mousex, int mousey) {
		pg.beginDraw();
		pg.background(0);
		if (inTransition) {
            // Render transition: current scene fading out, next scene fading in
            renderTransitionMedia(mousex, mousey);
        } else {
            // Normal rendering
            renderNormalMedia(mousex, mousey);
        }
		
		pg.endDraw();
	}
	
	private void renderNormalMedia(int mousex, int mousey) {
        for (MediaItem media : mediaItems) {
            if (media.assignedScreen == id) {
                media.checkHover(mousex, mousey);
                //PApplet.println("Screen pg: " + pg.width + "x" + pg.height);
                //PApplet.println("Media canvas: " + media.getMediaCanvas().width + "x" + media.getMediaCanvas().height);
                pg.image(media.getMediaCanvas(), 0, 0, pg.width, pg.height);
            }
        }
    }
	
	private void renderTransitionMedia(int mousex, int mousey) {
        // Render current scene media (fading out)
        for (MediaItem media : mediaItems) {
            if (media.assignedScreen == id) {
                media.checkHover(mousex, mousey);
                pg.tint(255, 255 * (1 - transitionAlpha)); // Fade out
                pg.image(media.getMediaCanvas(), 0, 0, pg.width, pg.height);
                pg.noTint();
            }
        }
        
        // Render next scene media (fading in)
        for (MediaItem media : nextMediaItems) {
            if (media.assignedScreen == id) {
                media.checkHover(mousex, mousey);
                pg.tint(255, 255 * transitionAlpha); // Fade in
                pg.image(media.getMediaCanvas(), 0, 0, pg.width, pg.height);
                pg.noTint();
            }
        }
    }

	public void setPreviewArea(float px, float py, float pw, float ph) {
		for (MediaItem media : mediaItems) {
			media.setPreviewArea(px, py, pw, ph);
			// media.moveHoverPoint(mousex, mousey);
		}
	}

	public void moveHoverPoint(float mousex, float mousey) {
		for (MediaItem media : mediaItems) {
			media.moveHoverPoint(mousex, mousey);
		}
	}

	PGraphics2D getScreen() {
		return pg;
	}

}