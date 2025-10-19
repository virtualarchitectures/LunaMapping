import paletai.generators.LunaContentGenerator;
import processing.opengl.PGraphics2D;


public class BouncingBall implements LunaContentGenerator {

  PGraphics2D pgPaint;
  float t,x,y,vx,vy;

  public String getName() {
    return "Bouncing Ball";
  }


  public void setup(int w, int h) {
    pgPaint = (PGraphics2D) createGraphics(w, h, P2D);
    t = 0;
    x = 10;
    y = 10;
    vx = 2;
    vy = 2;
  }

  public String[] getParameters() {
    return new String[]{"speed", "height"};
  }

  public void update() {
    pgPaint.beginDraw();
    pgPaint.background(80,20,20);
    pgPaint.noStroke();
    pgPaint.fill(200);
    pgPaint.ellipse(x,y,40,40);
    pgPaint.endDraw();
    x += vx;
    y += vy;
    
    if (x>pgPaint.width || x < 0) vx = -vx;
    if (y>pgPaint.height || y < 0) vy = -vy;
  }

  public PGraphics2D getGraphics() {
    return pgPaint;
  }

  public void setParameter(String name, float value) {
  }
}
