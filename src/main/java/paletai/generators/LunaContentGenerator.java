// LunaContentGenerator.java - Place this in your Luna library
package paletai.generators;

import processing.opengl.PGraphics2D;

public interface LunaContentGenerator {
    String getName();
    void setup(int width, int height);  // Remove PGraphics parameter
    void update();            // Rename from draw to update
    PGraphics2D getGraphics();          // Add this method
    String[] getParameters();
    void setParameter(String name, float value);
}