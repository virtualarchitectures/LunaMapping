package paletai.mapping;

import processing.core.*;
import processing.video.Capture;
import processing.opengl.*;


public class Camera {
	public String[] devices;
	private PApplet p;
	Capture cam;
	public boolean hasStarted = false;
	
	Camera(PApplet p){
		this.p = p;
		devices = Capture.list();
		PApplet.printArray(devices);
	}
}