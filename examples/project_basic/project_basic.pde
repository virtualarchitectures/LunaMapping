import paletai.mapping.*;

Project project;

void setup() {
  fullScreen(P2D, SPAN); //Always FullScreen, P2D and SPAN
  project = new Project(this, "NewProject");  //Name your project here
}

void draw() {
  background(0);
  project.render(mouseX,mouseY);
}

// press 'c' to toggle calibration mode
// press 'm' to switch media inside the current Scene if you have more than one
void keyReleased() {
  project.keyreleased(key, keyCode);  // call method on the instance
}

void mouseDragged() {
  project.moveHoverPoint(mouseX, mouseY);  // Move hovered point while dragging
}
