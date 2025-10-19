/**
 *
 * Luna Video Mapping | https://luna.art.br/
 * A Processing/Java library for Video Mapping.
 *
 * Created by Daniel Corbani
 * GPL 2.0 licence: https://www.gnu.org/licenses/old-licenses/gpl-2.0.html.en
 *
 */


/* In this example, a few files were included in the data folder.
 *        
 */
 
import paletai.mapping.*;

//Luna need this two complementary libraries to work
//You can install them from the Processing Contrinution Manager
import controlP5.*;
import processing.video.*;


Project project;

void setup() {
  fullScreen(P2D, SPAN); //This should always be FullScreen, P2D and SPAN
  project = new Project(this, "NewProject");  //Name your project here
}

void draw() {
  background(0);
  project.render(mouseX, mouseY);
}

// press any key to save your project
// press 'space bar' to go to next scene
void keyReleased() {
  project.keyreleased(key, keyCode);  // call method on the instance
}

void mouseDragged() {
  project.moveHoverPoint(mouseX, mouseY);  // Move hovered point while dragging
}
