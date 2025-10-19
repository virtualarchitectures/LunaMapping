import paletai.generators.LunaContentGenerator;
import processing.opengl.PGraphics2D;

public class ShowCamera implements LunaContentGenerator {

  PGraphics2D pgPaint, cameraBuffer;
  public String getName() {
    return "Camera";
  }

  public void setup(int w, int h) {
    pgPaint = (PGraphics2D) createGraphics(w, h, P2D);
  }

  public String[] getParameters() {
    return new String[]{"speed", "height"};
  }

  public void update() {
    pgPaint.beginDraw();
    pgPaint.background(0);
    pgPaint.image(pgCamA, 0, 0, pgPaint.width, pgPaint.height);
    pgPaint.endDraw();
  }

  public PGraphics2D getGraphics() {
    return pgPaint;
  }

  public void setParameter(String name, float value) {
  }
}
