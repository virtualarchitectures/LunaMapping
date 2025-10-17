package paletai.mapping;

import controlP5.ControlP5;
import processing.core.*;
import java.util.ArrayList;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.io.File;
import processing.opengl.PGraphics2D;
import controlP5.*;
import paletai.generators.LunaContentGenerator;
import processing.data.XML;
import java.lang.reflect.*;
import java.util.Objects;

/**
 * The main container class that manages multiple scenes and transitions.
 * Handles the overall project structure, scene navigation, and rendering
 * pipeline.
 * 
 * <p>
 * Key features include:
 * </p>
 * <ul>
 * <li>Scene management and organization</li>
 * <li>Scene transitions with fade effects</li>
 * <li>Project serialization (future implementation)</li>
 * <li>Global project state management</li>
 * </ul>
 * 
 * @author Daniel Corbani
 * @version 1.0
 * @see Scene
 */
public class Project {
	PApplet mainApplet; // needed for control P5
	String projectName; // Name passed by the user
	ArrayList<Scene> scenes = new ArrayList<Scene>();
	int currentScene = 0;
	ArrayList<Screen> screens = new ArrayList<Screen>(); // Screens containing Scenes / external displays only, UI not
															// included here
	int currentScreen = 0;
	ArrayList<Rectangle> availableDisplays = new ArrayList<Rectangle>(); // Stores external displays config
	XML config;
	PGraphics2D canvaUI; // Main PGraphics for UI

	int mainWidth, mainHeight; // Used for UI drawings
	int hx1, hx2, hy1, hy2, r; // handles for separating panels
	ArrayList<String> mediaFiles = new ArrayList<String>();

	ControlP5 cp5;
	Group mediaList, screenList, displaysList, sceneList, generatorList;
	RadioButton screenRadio, sceneRadio;
	int screenButtonsArea = 30;
	boolean addSelectScreenBool = false, removeScreenBol; // to avoid creating a button inside another button
	boolean addSceneBool = false, removeSceneBol = false;

	// Preview are for Screen Panel
	float previewAreaX, previewAreaY, previewAreaWidth, previewAreaHeight, previewWidth, previewHeight, previewX,
			previewY;

	private int nextScene = -1;
	private boolean isTransitioning = false;
	private int transitionStartTime;
	private int transitionDuration = 2000; // 2 second transition
	private float transitionProgress = 0;

	Camera cam;

	private ArrayList<LunaContentGenerator> availableGenerators;

	public Project(PApplet p, String name) {
		mainApplet = p;
		projectName = (name == null || name.trim().isEmpty()) ? "untitled" : name.trim();
		cp5 = new ControlP5(mainApplet); // must be called before creating buttons;
		initializeDisplays();
		//cam = new Camera(mainApplet);
		mainApplet.image(canvaUI, 0, 0);
		scanMediaFiles();
        scanGenerators();
		initXMLconfig();
		initializeButtons();
		// PApplet.println("Finished Setup");
	}

	/////// External Displays Management
	void initializeDisplays() {
		// PApplet.println("Initializing Displays...");
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = ge.getScreenDevices();
		availableDisplays.clear(); // Clear previous display info

		for (int i = 0; i < devices.length; i++) {
			Rectangle bounds = devices[i].getDefaultConfiguration().getBounds();
			if (i == 0) { // initialize UI
				// PApplet.println("Main display");
				mainWidth = bounds.width;
				mainHeight = bounds.height;
				hx1 = mainWidth / 6;
				hx2 = mainWidth - hx1;
				hy1 = 30;
				hy2 = 2 * mainHeight / 4;
				r = 10;
				previewAreaX = hx1 + 20;
				previewAreaY = hy1 + 20;
				previewAreaWidth = hx2 - hx1 - 40;
				previewAreaHeight = hy2 - hy1 - 40 - screenButtonsArea; // Room for buttons
				// Create an offscreen buffer matching main screen size
				canvaUI = (PGraphics2D) mainApplet.createGraphics(mainWidth, mainHeight, PConstants.P2D);
				canvaUI.beginDraw();
				canvaUI.background(33);
				canvaUI.textSize(20);
				canvaUI.fill(200);
				canvaUI.textAlign(PConstants.CENTER, PConstants.CENTER);
				canvaUI.text("Luna Video Mapping", canvaUI.width / 2, canvaUI.height / 2);
				canvaUI.endDraw();
			} else {
				// Store external display info without creating screens
				availableDisplays.add(bounds);
				PApplet.println("Found external display #" + i + ": " + bounds.width + "x" + bounds.height);
			}
		}
	}

	/////// Check Data Folder for readable media files
	void scanMediaFiles() {
		// PApplet.println("Scan media files");
		File dataDir = new File(mainApplet.sketchPath("data"));
		if (dataDir.exists() && dataDir.isDirectory()) {
			File[] files = dataDir.listFiles();
			// PApplet.println("data folder contain: " + files.length + " files");
			for (File file : files) {
				if (isMediaFile(file.getName())) {
					mediaFiles.add(file.getName());
					// PApplet.println(file.getName() + " added to mediaFiles");
				}
			}
		}
	}

	// Helper method to check file extensions
	boolean isMediaFile(String filename) {
		String[] supportedExtensions = { ".mp4", ".mov", ".png", ".jpg", ".jpeg", ".gif" };
		String lowerName = filename.toLowerCase();
		for (String ext : supportedExtensions) {
			if (lowerName.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

    private void scanGenerators() {
        availableGenerators = new ArrayList<LunaContentGenerator>();
        PApplet.println("=== Luna Generator Discovery ===");

        try {
            Class<?>[] declaredClasses = mainApplet.getClass().getDeclaredClasses();
            PApplet.println("Scanning " + declaredClasses.length + " declared classes...");

            for (Class<?> clazz : declaredClasses) {
                //PApplet.println("Checking: " + clazz.getSimpleName());

                if (LunaContentGenerator.class.isAssignableFrom(clazz) &&
                        !clazz.isInterface() &&
                        !Modifier.isAbstract(clazz.getModifiers())) {
                    //PApplet.println("Checking first if");
                    try {
                        // Try with PApplet reference for inner classes
                        java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(mainApplet.getClass());
                        constructor.setAccessible(true); // This is the key!
                        LunaContentGenerator generator = (LunaContentGenerator) constructor.newInstance(mainApplet);
                        availableGenerators.add(generator);
                        //PApplet.println("✓ Generator: '" + generator.getName() + "'");

                    } catch (Exception e) {
                        PApplet.println("! Instantiation failed: " + e.getMessage());
                        e.printStackTrace(); // This will show the exact error
                    }
                }
            }

        } catch (Exception e) {
            PApplet.println("Error during discovery: " + e.getMessage());
        }

        PApplet.println("Total generators found: " + availableGenerators.size());
        PApplet.println("=== Discovery Complete ===\n");
    }

    public ArrayList<LunaContentGenerator> getGenerators(){
        return availableGenerators;
    }
    /////// Initilize from XML file
	void initXMLconfig() {
		// PApplet.println("init XML config");
		String filename = "data/" + projectName + ".xml";
		File file = new File(mainApplet.sketchPath(filename));
		if (file.exists()) {
			try {
				config = mainApplet.loadXML(filename);
				loadProjectFromXML(config);
				PApplet.println("Loaded existing project: " + projectName);
			} catch (Exception e) {
				PApplet.println("Error loading project: " + e);
			}
		} else {
			PApplet.println(filename + " doesn't exist");
			addNewScreen();
			addSelectScreenBool = false; // avoids duplicated button on initialization
			addNewScene();
			addSceneBool = false; // avoids duplicated button on initialization
			PApplet.println("New Project created: " + projectName);
		}
	}

	void loadProjectFromXML(XML xml) {
		// PApplet.println("Entered loadProjectFromXML");
		XML ScreensParent = xml.getChild("Screens");
		XML[] Screens = ScreensParent.getChildren("Screen");
		for (XML Screen : Screens) {
			addNewScreen();
			// PApplet.println("added Screen");
		}
		// PApplet.println("Done Screens");
		addSelectScreenBool = false;
		XML ScenesParent = xml.getChild("Scenes");
		XML[] Scenes = ScenesParent.getChildren("Scene");
		for (XML Scene : Scenes) {
			Scene newScene = new Scene();
			XML[] Medias = Scene.getChildren("MediaItem");
			// PApplet.printArray(Medias);
			for (XML Media : Medias) {
                int isGen = Media.getInt("isGenerator");
                if (isGen >0) {
                    PApplet.println("Found generator saved");
                    int newMediaId = newScene.mediaItems.size();
                    //PApplet.println("Add generator?" + newMediaId);
                    String MediaGeneratorName = Media.getString("name");
                    PApplet.println(MediaGeneratorName);
                    for (LunaContentGenerator generator : availableGenerators){
                        //PApplet.println(generator.getName());
                        if(Objects.equals(MediaGeneratorName, generator.getName())){
                            //PApplet.println("Match name: " + MediaGeneratorName + "=" + generator.getName());
                            GenerativeMediaItem newMedia = new GenerativeMediaItem(mainApplet, generator, Media);
                            int screenId = Media.getInt("Screen");
                            newMedia.assignToDisplay(screens.get(screenId).w, screens.get(screenId).h, screenId);
                            newScene.addMedia(newMedia);
                        }
                    }
                } else {
                    MediaItem newMedia = new MediaItem(mainApplet, Media);
                    int screenId = Media.getInt("Screen");
                    newMedia.assignToDisplay(screens.get(screenId).w, screens.get(screenId).h, screenId);
                    newScene.addMedia(newMedia);
                }
			}
			scenes.add(newScene);
		}
		addSceneBool = false; // avoids duplicated button on initialization
		// PApplet.println("Done Scenes");
	}

	void saveToFile() {
//		PApplet.println("save to file");
		config = new XML(projectName);
		config.setString("name", projectName);
		config.setString("type", "Luna Video Mapping project"); // May be used to check if the existing XML file was
																// created here
		config.addChild("Screens");
		XML ScreensParent = config.getChild("Screens");
		for (Screen screen : screens) {
			ScreensParent.addChild(screen.saveXML());
		}
		config.addChild("Scenes");
		XML ScenesParent = config.getChild("Scenes");
		for (Scene scene : scenes) {
			ScenesParent.addChild(scene.saveXML());
		}
		mainApplet.saveXML(config, "data/" + projectName + ".xml");
		// PApplet.println("Project saved");
	}

	///// Screen management
	// Create a new screen
	void addNewScreen() {
		int newScreenId = screens.size();
		Screen newScreen = new Screen(mainApplet, newScreenId);
		screens.add(newScreen); // add to ArrayList
		addSelectScreenBool = true; // tell ControlP5 to update next time
	}

    // Delete current new scene
    void DelCurScreen() {
        if (screens.size() > 1) {
            screens.remove(currentScreen);
            // RemoveSelectSceneButton(currentScene);
        }
        removeScreenBol = true;
        // Update current scene index if needed
        if (currentScreen >= screens.size()) {
            currentScreen = screens.size() - 1;
        }

        // Select the appropriate scene
        selectScreen(currentScreen);
        PApplet.println("Delete Screen " + currentScreen);

    }

	// Create a new scene
	void addNewScene() {
		// int newSceneId = scenes.size();
		// println("Adding a new Scene with index " + newId);
		Scene newScene = new Scene();
		scenes.add(newScene);
		// PApplet.println("New Scene added to current Screen");
		addSceneBool = true;
	}

	// Delete current new scene
	void DelCurScene() {
		if (scenes.size() > 1 && currentScene >= 0 && currentScene < scenes.size()) {
			scenes.get(currentScene).deactivate();
			scenes.remove(currentScene);
			// RemoveSelectSceneButton(currentScene);
		}
		// PApplet.println("New Scene added to current Screen");
		removeSceneBol = true;

		// Update current scene index if needed
		if (currentScene >= scenes.size()) {
			currentScene = scenes.size() - 1;
		}

		// Select the appropriate scene
		selectScene(currentScene);
	}

	void addMedia(String name) {
		// PApplet.println("Add media?");
		int newMediaId = scenes.get(currentScene).mediaItems.size();
		MediaItem newMedia = new MediaItem(mainApplet, name, currentScreen, newMediaId);
		newMedia.assignToDisplay(screens.get(currentScreen).w, screens.get(currentScreen).h, currentScreen);
		scenes.get(currentScene).addMedia(newMedia);
		scenes.get(currentScreen).setThumbnailPosition(2 * r, hy2);
		// saveToFile();
		// PApplet.println(name + " added to current Scene");
	}

    void addGenerator(LunaContentGenerator newGenerator) {
        int newMediaId = scenes.get(currentScene).mediaItems.size();
        //PApplet.println("Add generator?" + newMediaId);
        GenerativeMediaItem newMedia = new GenerativeMediaItem(mainApplet, newGenerator, currentScreen, newMediaId);
        newMedia.assignToDisplay(screens.get(currentScreen).w, screens.get(currentScreen).h, currentScreen);
        scenes.get(currentScene).addMedia(newMedia);
        scenes.get(currentScene).setThumbnailPosition(2 * r, hy2);
        // saveToFile();
        // PApplet.println(name + " added to current Scene");
    }
	// This functions updates all buttons
	void updateCP5() {
		if (addSelectScreenBool) {
			addSelectScreenButton(screens.size() - 1);
			addSelectScreenBool = false;
		}
		if (addSceneBool) {
			addSelectSceneButton(scenes.size() - 1);
			addSceneBool = false;
		}
		if (removeSceneBol) {
			// RemoveSelectSceneButton(currentScene);
			createSceneButtons();
			removeSceneBol = false;
		}
        if (removeScreenBol) {
            createScreenButtons();
            removeScreenBol = false;
        }

	}

	void initializeButtons() {
		// PApplet.println("Initializing buttons");
		createAssignDisplayButtons();
		createScreenButtons();
		createSceneButtons();
		createMediaButtons();
        createGeneratorButtons();
	}

	void createAssignDisplayButtons() {  
	    int DisplayButtonX = hx2 - 10 * r;
	    int DisplayButtonHeight = 30;

	    // group container
	    if (displaysList != null) displaysList.remove();
	    displaysList = cp5.addGroup("Display List")
	        .setPosition(DisplayButtonX, hy1 + 2 * r)
	        .setBackgroundHeight(hy2 - hy1)
	        .disableCollapse()
	        .hideBar();

	    // font (could be declared once globally instead)
	    PFont myFont = mainApplet.createFont("NeueMachina-Regular.otf", 14, true);

	    // add one button per available display
	    for (int i = 0; i < availableDisplays.size(); i++) {
	        Rectangle b = availableDisplays.get(i);

	        Bang btn = cp5.addBang("Display " + i)
	            .setPosition(0, DisplayButtonHeight * i * 2)
	            .setSize(70, DisplayButtonHeight)  // wider
	            .setGroup(displaysList);

	        // styling
	        btn.getCaptionLabel()
	            .setText("Display " + i)
	            .setFont(myFont)
	            .align(ControlP5.CENTER, ControlP5.CENTER);
	        btn.setColorBackground(mainApplet.color(60, 60, 60));
		    btn.setColorForeground(mainApplet.color(90, 90, 90));
		    btn.setColorActive(mainApplet.color(120, 200, 120));
		    btn.setColorLabel(mainApplet.color(255));

	        // callback
	        btn.addCallback(new CallbackListener() {
	            public void controlEvent(CallbackEvent event) {
	                if (event.getAction() == ControlP5.ACTION_RELEASE) {
	                    assignDisplay(b);
	                }
	            }
	        });
	    }
	}
	
	void assignDisplay(Rectangle b) {
		if (screens.get(currentScreen).isAssigned == false) {
			for (Screen screen : screens) {
				if (screen.x == b.x) { // check if this screen is assigned to display in question
					screen.unassign();
				}
			}
			screens.get(currentScreen).assignToDisplay(b);
		} else {
			screens.get(currentScreen).unassign();
		}
	}

	void createScreenButtons() {
		if (screenList != null) screenList.remove();
		
		screenList = cp5.addGroup("Screen List")
				.setPosition(hx1 + r, hy2 - 2 * r - screenButtonsArea)
				.setBackgroundHeight(screenButtonsArea)
				.disableCollapse();
				//.hideBar();

		PFont myFont = mainApplet.createFont("Arial", 14, true);
		
		 // ---- Add Screen button ----
	    Button addBtn = cp5.addButton("Add Screen")
	        .setPosition(r, r)
	        .setSize(40, screenButtonsArea)   // wider so label fits nicely
	        .setCaptionLabel("+")
	        .setGroup(screenList);

	    addBtn.getCaptionLabel()
	          .setFont(myFont)
	          .align(ControlP5.CENTER, ControlP5.CENTER);
        addBtn.setColorBackground(mainApplet.color(60, 120, 60));
        addBtn.setColorForeground(mainApplet.color(90, 160, 90));
        addBtn.setColorActive(mainApplet.color(120, 200, 120));
        addBtn.setColorLabel(mainApplet.color(255));

	    addBtn.addCallback(new CallbackListener() {
	        public void controlEvent(CallbackEvent event) {
	            if (event.getAction() == ControlP5.ACTION_RELEASE) {
	                addNewScreen();
	            }
	        }
	    });

        // ---- Add Screen button ----
        Button delBtn = cp5.addButton("Del Screen")
                .setPosition(r+50, r)
                .setSize(40, screenButtonsArea)   // wider so label fits nicely
                .setCaptionLabel("-")
                .setGroup(screenList);

        delBtn.getCaptionLabel()
                .setFont(myFont)
                .align(ControlP5.CENTER, ControlP5.CENTER);
//        addBtn.setColorBackground(mainApplet.color(60, 60, 60));
//        addBtn.setColorForeground(mainApplet.color(90, 90, 90));
//        addBtn.setColorActive(mainApplet.color(120, 200, 120));
//        addBtn.setColorLabel(mainApplet.color(255));

        delBtn.setColorBackground(mainApplet.color(150, 60, 60));
        delBtn.setColorForeground(mainApplet.color(200, 90, 90));
        delBtn.setColorActive(mainApplet.color(255, 120, 120));
        delBtn.setColorLabel(mainApplet.color(255));

        delBtn.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                if (event.getAction() == ControlP5.ACTION_RELEASE) {
                    DelCurScreen();
                }
            }
        });
	 // ---- Screen selector (radio buttons) ----
	    if (screenRadio != null) screenRadio.remove();
	    
	    screenRadio = cp5.addRadioButton("ScreenSelector")
	            .setPosition(hx1 + 200, hy2 - r - screenButtonsArea)
	            .setSize(60, screenButtonsArea) // width x height for toggles
	            .setItemsPerRow(6)
	            .setSpacingColumn(70)
	            .setColorBackground(mainApplet.color(80))
	            .setColorForeground(mainApplet.color(120))
	            .setColorActive(mainApplet.color(0, 200, 100))
	            .setNoneSelectedAllowed(true);

		for (Screen screen : screens) {
			addSelectScreenButton(screen.id);
		}
		// PApplet.println("Create Screen buttons");
	}

	void addSelectScreenButton(int index) {
		// PApplet.println("Entered addSelectScreenButtons");
		String optionName = "" + index;
		screenRadio.addItem(optionName, index).setSize(30, 20).setColorActive(mainApplet.color(0, 150, 255))
				.setColorBackground(mainApplet.color(100)).setColorForeground(mainApplet.color(60));

		PFont myFont = mainApplet.createFont("Arial", 14, true);
		
		// Style the new radio button's label
		screenRadio.getItem(optionName)
			.getCaptionLabel()
			.setFont(myFont)
			.align(ControlP5.CENTER, ControlP5.CENTER)
			.setColor(mainApplet.color(255))
			.setSize(14);
		Toggle t = screenRadio.getItem(optionName);
		if (index == 0) {
			screenRadio.activate(index); // ensures this Toggle is ON
			selectScene(index); // call your logic as if it was clicked
		}
		t.addCallback(new CallbackListener() {
			public void controlEvent(CallbackEvent ev) {
				if (ev.getAction() == ControlP5.ACTION_RELEASE && t.getState()) {
					// println("Screen " + index + " " + t.getState());
					selectScreen(index);
				}
			}
		});
		// PApplet.println("Add select Screen button");
	}

	void selectScreen(int index) {
		if (index >= 0 && index < screens.size()) {
			currentScreen = index;
			// println("Select Screen: " + currentScreen);
		}
	}

	void createSceneButtons() {
	    // remove previous group if it exists
	    if (sceneList != null) sceneList.remove();

	    // group container
	    sceneList = cp5.addGroup("Scenes")
	        .setPosition(r, mainHeight - 8 * r)
	        .setBackgroundHeight(200)
	        .disableCollapse();

	 // load a font (you could make this global so it’s reused)
	    PFont myFont = mainApplet.createFont("Arial", 14, true);
	    sceneList.getCaptionLabel()
	    .setFont(myFont)
	    .setColor(120)
	    .setColorBackground(44);
	    

	    // ---- Add Scene button ----
	    Button addBtn = cp5.addButton("Add Scene")
	        .setPosition(r, r)
	        .setSize(40, screenButtonsArea)    // wider for readability
	        .setCaptionLabel("+")
	        .setGroup(sceneList);

	    addBtn.getCaptionLabel()
	        .setFont(myFont)
	        .align(ControlP5.CENTER, ControlP5.CENTER);
	    addBtn.setColorBackground(mainApplet.color(60, 120, 60));
	    addBtn.setColorForeground(mainApplet.color(90, 160, 90));
	    addBtn.setColorActive(mainApplet.color(120, 200, 120));
	    addBtn.setColorLabel(mainApplet.color(255));

	    addBtn.addCallback(new CallbackListener() {
	        public void controlEvent(CallbackEvent event) {
	            if (event.getAction() == ControlP5.ACTION_RELEASE) {
	                addNewScene();
	            }
	        }
	    });

	    // ---- Delete Scene button ----
	    Button delBtn = cp5.addButton("Del Scene")
	        .setPosition(r + 60, r)  // spaced apart
	        .setSize(40, screenButtonsArea)
	        .setCaptionLabel("-")
	        .setGroup(sceneList);

	    delBtn.getCaptionLabel()
	        .setFont(myFont)
	        .align(ControlP5.CENTER, ControlP5.CENTER);
	    delBtn.setColorBackground(mainApplet.color(150, 60, 60));
	    delBtn.setColorForeground(mainApplet.color(200, 90, 90));
	    delBtn.setColorActive(mainApplet.color(255, 120, 120));
	    delBtn.setColorLabel(mainApplet.color(255));

	    delBtn.addCallback(new CallbackListener() {
	        public void controlEvent(CallbackEvent event) {
	            if (event.getAction() == ControlP5.ACTION_RELEASE) {
	                DelCurScene();
	            }
	        }
	    });

	    // ---- Scene selector (radio button) ----
	    if (sceneRadio != null) sceneRadio.remove();

	    sceneRadio = cp5.addRadioButton("SceneSelector")
	        .setPosition(r + 200, mainHeight - 4 * r)
	        .setSize(40, 25) // width, height of each toggle
	        .setItemsPerRow(20)
	        .setSpacingColumn(50)
	        .setNoneSelectedAllowed(false)
	        .setColorBackground(mainApplet.color(80))
	        .setColorForeground(mainApplet.color(120))
	        .setColorActive(mainApplet.color(0, 200, 100));

	    // apply font/color to all toggles once they’re created
	    for (int i = 0; i < scenes.size(); i++) {
	        addSelectSceneButton(i);
	        Toggle t = sceneRadio.getItem("Scene " + i);
	        t.getCaptionLabel()
	            .setFont(myFont)
	            .align(ControlP5.CENTER, ControlP5.CENTER);
	        t.setColorLabel(mainApplet.color(255));
	    }
	}

	void addSelectSceneButton(int index) {
	    String optionName = "Scene " + index;

	    // Create toggle inside the RadioButton group
	    sceneRadio.addItem(optionName, index)
	        .setSize(40, 25)  // width, height of each toggle button
	        .setColorBackground(mainApplet.color(80))   // normal background
	        .setColorForeground(mainApplet.color(120))  // hover
	        .setColorActive(mainApplet.color(171, 128, 255)) // active/selected
	        ; 

	    // Access the toggle just created
	    Toggle t = sceneRadio.getItem(optionName);

	    // ---- Styling label ----
	    PFont myFont = mainApplet.createFont("Arial", 20, true); // load once globally if you prefer
	    t.getCaptionLabel().setFont(myFont);
	    t.setColorLabel(mainApplet.color(255)); // label text color (white)
	    t.getCaptionLabel()
	     .setText(""+ index)
	     .align(ControlP5.CENTER, ControlP5.CENTER);  // horizontal + vertical center
	    
	    // ---- Behavior ----
	    if (index == 0) {
	        sceneRadio.activate(index); // ensures first button is ON
	        selectScene(index);         // trigger selection
	    }

	    t.addCallback(new CallbackListener() {
	        public void controlEvent(CallbackEvent ev) {
	            if (ev.getAction() == ControlP5.ACTION_RELEASE && t.getState()) {
	                selectScene(index);
	            }
	        }
	    });
	}

	void selectScene(int index) {
		if (index >= 0 && index < scenes.size()) {
			currentScene = index;
			// PApplet.println("Scene " + index + " selected");
			for (Scene scene : scenes) {
				scene.deactivate();
			}
			scenes.get(currentScene).activate();
			// int numMedias = scenes.get(currentScene).mediaItems.size();
			// PApplet.println("has " + numMedias + " medias");
			for (Screen screen : screens) {
				screen.currentScene = currentScene;
			}
		}
	}

	void createMediaButtons() {
	    // Create the group container
	    mediaList = cp5.addGroup("Media List")
	        .setPosition(r, hy1 + r)
	        .setBackgroundHeight(hy2 - hy1 - 2 * r)
	        .disableCollapse();
	    
	    mediaList.getCaptionLabel().setVisible(false);
	    mediaList.hideBar();

	    // Load a custom font (adjust name/size to your liking)
	    PFont myFont = mainApplet.createFont("Arial", 12, true);

	    for (int i = 0; i < mediaFiles.size(); i++) {
	        String name = mediaFiles.get(i);
	        int buttonHeight = 30;             // custom height
	        int buttonWidth  = hx1 - 2 * r;    // custom width
	        int buttonY      = i * buttonHeight;

	        Button b = cp5.addButton(name)
	            .setPosition(0, buttonY)              // relative to the group
	            .setSize(buttonWidth, buttonHeight)   // size of button
	            .setCaptionLabel(name)                // label text
	            .setGroup(mediaList);                 // attach to group

	        // ---- Styling ----
	        b.getCaptionLabel().setFont(myFont);      // font
	        b.setColorBackground(mainApplet.color(91, 91, 91)); // normal background
	        b.setColorForeground(mainApplet.color(202, 195, 226)); // hover color
	        b.setColorActive(mainApplet.color(171, 128, 255));   // pressed color
	        b.setColorLabel(mainApplet.color(255));              // text color

	        // ---- Callback ----
	        b.addCallback(new CallbackListener() {
	            public void controlEvent(CallbackEvent event) {
	                if (event.getAction() == ControlP5.ACTION_RELEASE) {
	                    addMedia(name);
	                }
	            }
	        });
	    }
	}

    void createGeneratorButtons() {
        // Create the group container
        generatorList = cp5.addGroup("Generator List")
                .setPosition(hx2+r, hy1 + r)
                .setBackgroundHeight(hy2 - hy1 - 2 * r)
                .disableCollapse();

        generatorList.getCaptionLabel().setVisible(false);
        generatorList.hideBar();

        // Load a custom font (adjust name/size to your liking)
        PFont myFont = mainApplet.createFont("Arial", 12, true);

        for (int i = 0; i < availableGenerators.size(); i++) {
            LunaContentGenerator generator = availableGenerators.get(i);
            String name = generator.getName();
            int buttonHeight = 30;             // custom height
            int buttonWidth  = mainWidth - hx2 - 2 * r;    // custom width
            int buttonY      = i * buttonHeight;

            Button b = cp5.addButton(name)
                    .setPosition(0, buttonY)              // relative to the group
                    .setSize(buttonWidth, buttonHeight)   // size of button
                    .setCaptionLabel(name)                // label text
                    .setGroup(generatorList);                 // attach to group

            // ---- Styling ----
            b.getCaptionLabel().setFont(myFont);      // font
            b.setColorBackground(mainApplet.color(91, 91, 91)); // normal background
            b.setColorForeground(mainApplet.color(202, 195, 226)); // hover color
            b.setColorActive(mainApplet.color(171, 128, 255));   // pressed color
            b.setColorLabel(mainApplet.color(255));              // text color

            // ---- Callback ----
            b.addCallback(new CallbackListener() {
                public void controlEvent(CallbackEvent event) {
                    if (event.getAction() == ControlP5.ACTION_RELEASE) {
                        addGenerator(generator);
                        PApplet.println("Button pressed: Generator " + name + " added");
                    }
                }
            });
        }
    }

	public void render(int mousex, int mousey) {
		drawUI();
		updateCP5();

		if (isTransitioning) {
			// Update transition progress
			transitionProgress = (float) (mainApplet.millis() - transitionStartTime) / transitionDuration;
			transitionProgress = PApplet.constrain(transitionProgress, 0, 1);

			// Render both scenes during transition
			scenes.get(currentScene).render(); // Current scene
			scenes.get(nextScene).render(); // Next scene

			// Complete transition
			if (transitionProgress >= 1.0f) {
				completeTransition();
			}
		} else {
			// Normal rendering
			scenes.get(currentScene).render();
		}

		// Update screens with appropriate media list based on transition state
		if (isTransitioning) {
			// During transition, screens need access to both scenes' media
			screens.get(currentScreen).setTransitionState(scenes.get(currentScene).mediaItems,
					scenes.get(nextScene).mediaItems, transitionProgress);
		} else {
			screens.get(currentScreen).updateMediaList(scenes.get(currentScene).mediaItems);
		}

		for (Screen screen : screens) {
			screen.render(mousex, mousey);
		}
	}

	void drawUI() {
		canvaUI.beginDraw();
		canvaUI.background(33);

		// title
		canvaUI.textSize(20);
		canvaUI.fill(200);
		canvaUI.textAlign(PConstants.CENTER, PConstants.CENTER);
		canvaUI.text(projectName, mainWidth / 2, hy1 / 2);
		canvaUI.text("fps: " + (int) (mainApplet.frameRate), mainWidth / 3, hy1 / 2);

		// media
		drawPanel(0, hy1, hx1, hy2, canvaUI);

		// stage
		drawStagePanel(currentScreen); // Pass the index here

		// effects
		drawPanel(hx2, hy1, mainWidth, hy2, canvaUI);

		// timeline
		drawTimelinePanel(currentScene);

        //drawGenerativePanel();

		canvaUI.endDraw();
		mainApplet.image(canvaUI, 0, 0);
	}

	void drawPanel(int x1, int y1, int x2, int y2, PGraphics2D input) {
		input.noStroke();
		input.fill(44);
		input.rect(x1 + r, y1 + r, x2 - x1 - 2 * r, y2 - y1 - 2 * r, r);
	}

	void updatePreviewArea() {
		// Calculate scale to fit
		float scale = PApplet.min(previewAreaWidth / screens.get(currentScreen).w,
				previewAreaHeight / screens.get(currentScreen).h);

		previewWidth = screens.get(currentScreen).w * scale;
		previewHeight = screens.get(currentScreen).h * scale;

		// Center in preview area
		previewX = previewAreaX + (previewAreaWidth - previewWidth) / 2;
		previewY = previewAreaY + (previewAreaHeight - previewHeight) / 2;
	}

	void drawStagePanel(int index) {
		// 1. Draw panel background
		drawPanel(hx1, hy1, hx2, hy2, canvaUI);

		// 3. Draw preview content
		if (index >= 0 && index < screens.size()) {
			Screen screen = screens.get(index);
			updatePreviewArea();
			screens.get(currentScreen).setPreviewArea(previewX, previewY, previewWidth, previewHeight);
			// Draw (with border)
			canvaUI.fill(0);
			canvaUI.rect(previewX - 2, previewY - 2, previewWidth + 4, previewHeight + 4);
			canvaUI.image(screen.getScreen(), previewX, previewY, previewWidth, previewHeight);
			canvaUI.fill(200, 100);
			canvaUI.textSize(48);
			canvaUI.textAlign(PConstants.CENTER, PConstants.CENTER);
			canvaUI.text(index, (hx2 + hx1) / 2, (hy1 + hy2) / 2);
			// canvaUI.text(previewAreaWidth, (hx2 + hx1)/2, (hy1 +hy2)/2);
		}

		// 4. Draw UI elements on top
		canvaUI.fill(200);
		canvaUI.textSize(16);
		canvaUI.textAlign(PConstants.LEFT, PConstants.TOP);
		canvaUI.text("Stage Preview", hx1 + 20, hy1 + 15);
	}

	void drawTimelinePanel(int index) {
		drawPanel(0, hy2, mainWidth, mainHeight, canvaUI);
		// 4. Draw UI elements on top
		for (int i = 0; i < scenes.get(currentScene).mediaItems.size(); i++) {
			MediaItem media = scenes.get(currentScene).mediaItems.get(i);
			scenes.get(currentScene).setThumbnailPosition(2 * r, hy2);
			if (media.thumbnailGenerated) {
				canvaUI.image(media.thumbnail, media.thumbnailX, media.thumbnailY);
			}
		}
	}

	public void moveHoverPoint(float mousex, float mousey) {
		for (Screen screen : screens) {
			screen.moveHoverPoint(mousex, mousey);
		}
	}

	public void keyreleased(char k, int kc) {
		saveToFile();
		if (k == ' ' && !isTransitioning) {
//			sceneRadio.deactivate(currentScene);
//			currentScene = PApplet.constrain(currentScene+1,0,scenes.size()-1);
//			sceneRadio.activate(currentScene);
//			selectScene(currentScene);
			startTransition(currentScene + 1);
			PApplet.println("Next scene : currentScene + 1");
		}
	}

	private void startTransition(int targetScene) {
		if (targetScene >= 0 && targetScene < scenes.size() && targetScene != currentScene) {
			isTransitioning = true;
			nextScene = targetScene;
			transitionStartTime = mainApplet.millis();
			transitionProgress = 0;

			// Activate both scenes during transition
			scenes.get(nextScene).activate();
		}
	}

	private void completeTransition() {
		// Clean up transition state
		scenes.get(currentScene).deactivate();
		sceneRadio.deactivate(currentScene);

		currentScene = nextScene;
		nextScene = -1;
		isTransitioning = false;
		transitionProgress = 0;

		sceneRadio.activate(currentScene);
	}


}