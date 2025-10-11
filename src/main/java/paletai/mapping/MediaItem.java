package paletai.mapping;

import paletai.generators.LunaContentGenerator;
import processing.core.*;
import processing.data.XML;
import processing.video.*;
import processing.opengl.*;
import java.io.File;

import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Group;

/**
 * A class for managing media items (images/videos) with homography
 * transformation capabilities. Handles loading, playback, and rendering of
 * media files with perspective correction.
 * 
 * <p>
 * Key features include:
 * </p>
 * <ul>
 * <li>Automatic aspect ratio correction</li>
 * <li>Video playback control</li>
 * <li>Thumbnail generation</li>
 * <li>Homography transformation via {@link VidMap}</li>
 * <li>Media file management</li>
 * </ul>
 * 
 * @author Daniel Corbani
 * @version 1.0
 * @see VidMap
 * @see Movie
 */

public class MediaItem {
	// XML mediaXML;
	/** Parent Processing applet */
	private final PApplet p;
	/** Full path to media file */
	private String filePath;
	/** Base filename with scene index */
	public String fileName;
	/** Image object (for static images) */
	private PImage img;
	/** Thumbnail representation */
	public PImage thumbnail;
	public float thumbnailX, thumbnailY;
	/** Video flag */
	private final boolean isVideo;
	private boolean loaded = false;
	private boolean isLooping = true;
	/** Video object (for movies) */
	private Movie movie;
	/** Graphics buffer for rendering */
	private PGraphics2D mediaCanvas;
	/** Homography transformation handler */
	public VidMap vm; // Homography transformation
	/** Original media dimensions */
	public int mediaWidth, mediaHeight;
	public int assignedScreen, mediaId;
	int resolutionX, resolutionY;
	public boolean calibrate = false;
	// public boolean fromxml = false;
	boolean thumbnailGenerated = false;

	ControlP5 cp5;
	Group controlGroup;
	boolean controlsVisible = false;

    private LunaContentGenerator generator;
    private final boolean isGenerative;
    private float t;
	/**
	 * Constructs a new MediaItem.
	 *
	 * @param p          Parent Processing applet
	 * @param filePath   Path to media file
	 * @throws RuntimeException If media file cannot be loaded
	 */

	public MediaItem(PApplet p, String filePath, int screenIndex, int mediaId) {
		// PApplet.println("Start MediaItem");
		this.p = p;
		this.filePath = filePath;
		// println("filePath " + filePath);
		this.fileName = extractFileName(filePath); // NEED TO CHECK THIS!!!!!
		// PApplet.println(fileName);
		this.isVideo = isVideoFile(filePath);
		// this.sceneIndex = sceneIndex;
		this.assignedScreen = screenIndex;
		this.mediaId = mediaId;
		this.vm = new VidMap(p, fileName); // Pass fileName to vm
		initVariables();
        isGenerative = false;
	}

	public MediaItem(PApplet p, XML mediaXML) {
		// PApplet.println("Start MediaItem");
		this.p = p;
		this.filePath = mediaXML.getString("name");
		// PApplet.println("filePath " + filePath);
		this.fileName = extractFileName(filePath); // NEED TO CHECK THIS!!!!!
		// PApplet.println(fileName);
		this.isVideo = isVideoFile(filePath);
		// this.sceneIndex = sceneIndex;
		this.assignedScreen = mediaXML.getInt("Screen");
		this.mediaId = mediaXML.getInt("id");
		this.vm = new VidMap(p, fileName); // Pass fileName to vm
		initVariables();
		// this.mediaWidth = mediaXML.getInt("width");
		// this.mediaHeight = mediaXML.getInt("height");
		updateFromXML(mediaXML);
		loaded = true;
        isGenerative = false;
	}

    public MediaItem(PApplet p, LunaContentGenerator generator, int screenIndex, int mediaId) {
        // PApplet.println("Start MediaItem");
        this.p = p;
        this.fileName = generator.getName();
        this.isVideo = false;
        isGenerative = true;
        this.generator = generator;
        t = 0;
        // this.sceneIndex = sceneIndex;
        this.assignedScreen = screenIndex;
        this.mediaId = mediaId;
        this.vm = new VidMap(p, fileName); // Pass fileName to vm
        initVariables();
    }
    boolean isGenerative(){
        return isGenerative;
    }

	XML saveXML() {
		XML xml = new XML("MediaItem");
		xml.setString("name", fileName);
		xml.setInt("Screen", assignedScreen);
		// xml.setInt("Scene", sceneIndex);
		xml.setInt("id", mediaId);
		xml.setInt("width", mediaWidth);
		xml.setInt("height", mediaHeight);
		if (vm.xyN[0] != null)
			xml.addChild(arrayToXML("xyN", vm.xyN));
		if (vm.uvN[0] != null)
			xml.addChild(arrayToXML("uvN", vm.uvN));
		return xml;
	}

	// Convert a PVector[] into XML
	XML arrayToXML(String tag, PVector[] arr) {
		XML arrayXML = new XML(tag);
		for (int i = 0; i < arr.length; i++) {
			XML v = new XML("point");
			v.setInt("index", i);
			v.setFloat("x", arr[i].x);
			v.setFloat("y", arr[i].y);
			arrayXML.addChild(v);
		}
		return arrayXML;
	}

	private void createControlGroup() {
		cp5 = new ControlP5(p);
		// Create a group for this media item's controls
		controlGroup = cp5.addGroup("media_" + System.currentTimeMillis()) // Unique name
				.setPosition(thumbnailX, thumbnailY).setWidth(150) // Match thumbnail width
				.setHeight(50) // Slightly taller to fit controls
				.setBackgroundColor(p.color(50, 220)) // Semi-transparent background
				.setBackgroundHeight(20).disableCollapse().hideBar();
		// .hide(); // Start hidden

		// Add controls to the group
		cp5.addButton("calibrate" + controlGroup.getName()).setPosition(10, 10).setSize(60, 20)
				.setCaptionLabel("Calibrate").setGroup(controlGroup).addCallback(new CallbackListener() {
					public void controlEvent(CallbackEvent event) {
						if (event.getAction() == ControlP5.ACTION_RELEASE) {
							// println("Bang released: " + event.getController().getName());
							// Your function call here
							toggleCalibration();
						}
					}
				});

		cp5.addButton("input" + controlGroup.getName()).setPosition(80, 10).setSize(60, 20).setCaptionLabel("Input")
				.setGroup(controlGroup).addCallback(new CallbackListener() {
					public void controlEvent(CallbackEvent event) {
						if (event.getAction() == ControlP5.ACTION_RELEASE) {
							// println("Bang released: " + event.getController().getName());
							// Your function call here
							toggleInput();
						}
					}
				});

		// Video controls

		if (isVideo) {
			cp5.addButton("play" + controlGroup.getName()).setPosition(10, 80).setSize(60, 20).setCaptionLabel("Play")
					.setGroup(controlGroup).addCallback(new CallbackListener() {
						public void controlEvent(CallbackEvent event) {
							if (event.getAction() == ControlP5.ACTION_RELEASE) {
								// println("Bang released: " + event.getController().getName());
								// Your function call here
								playMedia();
							}
						}
					});
		}

		if (isVideo) {
			cp5.addButton("stop" + controlGroup.getName()).setPosition(80, 80).setSize(60, 20).setCaptionLabel("Stop")
					.setGroup(controlGroup).addCallback(new CallbackListener() {
						public void controlEvent(CallbackEvent event) {
							if (event.getAction() == ControlP5.ACTION_RELEASE) {
								// println("Bang released: " + event.getController().getName());
								// Your function call here
								stopMedia();
							}
						}
					});
		}
	}

	public void showControls() {
		controlGroup.show();
	}

	public void hideControls() {
		controlGroup.hide();
	}

	public void setThumbnailPosition(int x, int y) {
		this.thumbnailX = x;
		this.thumbnailY = y;
		// PApplet.println("Thumnail position: " + thumbnailX + "," + thumbnailY);
		controlGroup.setPosition(x, y); // Position above thumbnail
	}

	void initVariables() {
		// PApplet.println("initVariables");
		if (isVideo) {
			// PApplet.println("is video");
			this.movie = new Movie(p, filePath);
			movie.loop(); // Preload the movie (optional)
			mediaWidth = movie.width;
			mediaHeight = movie.height;
			thumbnail = p.createImage(150, 100, PConstants.RGB);

		} else if (isGenerative){
            // PApplet.println("MediaItem is picture");
            mediaWidth = 0;
            mediaHeight = 0;
            thumbnail = p.createImage(150, 100, PConstants.RGB);
        } else{
			// PApplet.println("MediaItem is picture");
			img = p.loadImage(filePath);
			mediaWidth = img.width;
			mediaHeight = img.height;
			thumbnail = p.createImage(150, 100, PConstants.RGB);
		}
		thumbnailX = 0;// (thumbnail.width+50)*mediaId;
		thumbnailY = 0;
		createControlGroup();
	}

	void updateFromXML(XML xml) {
		PVector[] xyNew = arrayFromXML(xml.getChild("xyN"));
		PVector[] uvNew = arrayFromXML(xml.getChild("uvN"));
		vm.xyN = arrayFromXML(xml.getChild("xyN"));
		vm.uvN = arrayFromXML(xml.getChild("uvN"));
		updateHomography(xyNew, uvNew);
	}

	// Convert XML back into a PVector[]
	PVector[] arrayFromXML(XML arrayXML) {
		XML[] points = arrayXML.getChildren("point");
		PVector[] arr = new PVector[points.length];
		for (int i = 0; i < points.length; i++) {
			// PApplet.println(points[i].getName());
			float x = points[i].getFloat("x");
			float y = points[i].getFloat("y");
			// float z = points[i].hasAttribute("z") ? points[i].getFloat("z") : 0;
			arr[i] = new PVector(x, y);
			// PApplet.println(arr[i]);
		}
		return arr;
	}

	// Update an existing XML node with new PVector values
	void updateArrayXML(XML arrayXML, PVector[] arr) {
		XML[] points = arrayXML.getChildren("point");
		for (int i = 0; i < arr.length && i < points.length; i++) {
			// println(arrayXML.getName() + " " + points[i]);
			points[i].setFloat("x", arr[i].x);
			points[i].setFloat("y", arr[i].y);
		}
	}

	void assignToDisplay(int w, int h, int screenIndex) {
		// PApplet.println("MediaItem Assigned to Display: " + screenIndex);
		this.resolutionX = w;
		this.resolutionY = h;
		this.mediaCanvas = (PGraphics2D) p.createGraphics(resolutionX, resolutionY, PConstants.P2D);
		this.mediaCanvas.beginDraw();
		this.mediaCanvas.clear();
		this.mediaCanvas.endDraw();
		this.assignedScreen = screenIndex;
        if (isGenerative){
            this.generator.setup(resolutionX,resolutionY);
        }
		vm.assignToDisplay(resolutionX, resolutionY);
	}

	/**
	 * Checks if media is successfully loaded.
	 * 
	 * @return true if media is ready for display
	 */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * Adjusts media display to maintain aspect ratio. Automatically updates
	 * homography points to fit media properly.
	 *
	 * @param mediaWidth  Original media width
	 * @param mediaHeight Original media height
	 */
	public void applyAspectRatioCorrection(int mediaWidth, int mediaHeight) {
		// PApplet.println("applyAspectRatioCorrection");
		float screenAspect = (float) mediaCanvas.width / mediaCanvas.height;
		// System.out.println("screenAspect = " + screenAspect); //1.3334
		float mediaAspect = (float) mediaWidth / mediaHeight;
		// System.out.println("mediaAspect = " + mediaAspect); //0.5625
		float newWidth, newHeight;
		float offsetX = 0, offsetY = 0;

		if (mediaAspect > screenAspect) {
			// Fit to width
			newWidth = mediaCanvas.width;
			newHeight = mediaCanvas.width / mediaAspect;
			offsetY = (mediaCanvas.height - newHeight) / 2;
		} else {
			// Fit to height
			newHeight = mediaCanvas.height;
			newWidth = mediaCanvas.height * mediaAspect;
			offsetX = (mediaCanvas.width - newWidth) / 2;
		}

		// Update homography points
		PVector[] uvP = { new PVector(offsetX, offsetY), new PVector(offsetX + newWidth, offsetY),
				new PVector(offsetX + newWidth, offsetY + newHeight), new PVector(offsetX, offsetY + newHeight) };

		PVector[] xyP = { new PVector(0, 0), new PVector(mediaCanvas.width, 0),
				new PVector(mediaCanvas.width, mediaCanvas.height), new PVector(0, mediaCanvas.height) };

		vm.updateHomographyFromPixel(xyP, uvP);
//		if (loaded) {
//			// updateFromXML(fromXML);
//		}
	}
	// **🔹 vm Wrapper Methods**

	public void updateHomography(PVector[] xyNew, PVector[] uvNew) {
		vm.updateHomography(xyNew, uvNew);
	}

	public void toggleCalibration() {
		vm.toggleCalibration();
		this.calibrate = vm.calibrate;
	}

	public void offCalibration() {
		vm.offCalibration();
		this.calibrate = vm.calibrate;
	}

	public void onCalibration() {
		vm.onCalibration();
		this.calibrate = vm.calibrate;
	}

	public void toggleInput() {
		vm.checkInput = !vm.checkInput;
		// System.out.println("checkInput = " + vm.checkInput);
	}

	public void checkHover(float x, float y) {
		vm.checkHover(x, y);
	}

	public void moveHoverPoint(float x, float y) {
		vm.moveHoverPoint(x, y);
	}

	public void mouseReleased() {
		vm.mouseReleased();
	}

	public void resetHomography() {
		vm.resetHomography();
		applyAspectRatioCorrection(mediaWidth, mediaHeight);
	}

	// Extracts the file name from the full path
	private String extractFileName(String path) {
		File file = new File(path);
		return file.getName();
	}

	// Check if a file is a video
	private boolean isVideoFile(String filename) {
		filename = filename.toLowerCase();
		return filename.endsWith(".mp4") || filename.endsWith(".avi") || filename.endsWith(".mov");
	}

	// Generate a thumbnail from a video (first frame) - Now manually callable
	public void generateThumbnail() {
        if (isGenerative) {
            thumbnail = p.createImage(mediaCanvas.width, mediaCanvas.height, PConstants.RGB);
            thumbnail.copy(this.generator.getGraphics(), 0, 0, this.generator.getGraphics().width, this.generator.getGraphics().height, 0, 0, thumbnail.width, thumbnail.height);
            thumbnail.loadPixels();
            thumbnail.resize(150, 100);
            PApplet.println("Generative thumbnail");
        } else if (!isVideo) {
			img.loadPixels();
			thumbnail = p.createImage(img.width, img.height, PConstants.RGB);
			thumbnail.copy(img, 0, 0, img.width, img.height, 0, 0, thumbnail.width, thumbnail.height);
			thumbnail.loadPixels();
			thumbnail.resize(150, 100);
		} else if (isVideo && movie != null) {
			// Check if video has pixels available
			movie.loadPixels();
			thumbnail = p.createImage(movie.width, movie.height, PConstants.RGB);
			thumbnail.copy(movie, 0, 0, movie.width, movie.height, 0, 0, thumbnail.width, thumbnail.height);
			thumbnail.loadPixels();
			thumbnail.resize(150, 100);
			// PApplet.println(thumbnail.width);
		}
		thumbnailGenerated = true;
	}

	public void setPreviewArea(float px, float py, float pw, float ph) {
		vm.setPreviewArea(px, py, pw, ph);
	}

	/**
	 * Renders the media with homography transformation. Handles both static images
	 * and video playback.
	 */


	public void render() {
		mediaCanvas.beginDraw();
		mediaCanvas.background(0); // Clear previous frame

		if (isVideo && movie != null) { // Check for null FIRST
			if (movie.available()) {
				movie.read();
				if (mediaHeight == 0) {
					mediaWidth = movie.width;
					mediaHeight = movie.height;
					if (!loaded)
						applyAspectRatioCorrection(mediaWidth, mediaHeight);
					loaded = true;
				}
				if (movie.time() > 0.5f && !thumbnailGenerated) {
					generateThumbnail();
					//PApplet.println("Video thumbnail generated");
				}
			}

			// Only try to draw if movie is not null
			mediaCanvas.image(movie, 0, 0, mediaCanvas.width, mediaCanvas.height);

		} else if (isGenerative) {
            this.generator.update();
            //generator.draw(mediaCanvas,t);
            if (generator.getGraphics() != null) {
                mediaCanvas.image(this.generator.getGraphics(), 0, 0, mediaCanvas.width, mediaCanvas.height);
                if (!thumbnailGenerated) {
                    generateThumbnail();
                    //PApplet.println("Video thumbnail generated");
                }
            }
        } else if (!isVideo){
			// Handle non-video or stopped video case
			if (!thumbnailGenerated) {
				generateThumbnail();
			}
			if (img != null) {
				mediaCanvas.image(img, 0, 0, mediaCanvas.width, mediaCanvas.height);
			}
		}

		mediaCanvas.endDraw();

		// Apply homography transformation using vm
		if (vm != null) {
			vm.render(mediaCanvas);
		}
	}

	/**
	 * Toggles video playback state. No effect on static images.
	 */
	public void togglePlayback() {
		if (isVideo) {
			if (movie.isPlaying()) {
				movie.pause();
			} else {
				playMedia();
			}
		}
	}

	/**
	 * Toggles video loop mode.
	 */
	public void toggleLoop() {
		isLooping = !isLooping;
		// System.out.println("isLooping = " + isLooping);
	}

	/**
	 * Starts media playback. For videos: begins playback according to loop mode.
	 */
	public void playMedia() {
		if (isVideo) {
			stopMedia(); // clean up old one first
			movie = new Movie(p, filePath);
			if (isLooping) {
				movie.loop();
			} else {
				movie.play();
			}
		}
	}

	/**
	 * Stops media playback. For videos: stops and clears the display.
	 */
	public void stopMedia() {
		if (isVideo && movie != null) {
			//PApplet.println("Stop Media");
			movie.stop();
			movie.dispose(); // force GStreamer cleanup. It is crucial to force GStreamer to release the
								// native pipeline before reusing
			movie = null;
			// movie = new Movie(p, filePath);
			mediaCanvas.beginDraw();
			mediaCanvas.clear();
			mediaCanvas.endDraw();
			//PApplet.println("finished Stop Media");
		}
	}

	public void muteMedia() {
		if (isVideo && movie != null && !movie.isPlaying()) {
			movie.volume(0);
		}
	}

	// Getters
	public String getFilePath() {
		return filePath;
	}

	public String getFileName() {
		return fileName;
	}

	public PImage getThumbnail() {
		return thumbnail;
	}

	public boolean isVideo() {
		return isVideo;
	}

	public PGraphics2D getMediaCanvas() {
		return vm.getMediaCanvas();
	}
}