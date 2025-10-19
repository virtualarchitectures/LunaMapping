void initCam() {
  String[] cameras = Capture.list();
  for (int i=0; i< cameras.length; i++) println(cameras[i]);
  cam = new Capture(this, cameras[1]);
  cam.start();
  camWidth = cam.width;
  camHeight = cam.height;
  //println(camWidth+","+camHeight);
  pgCamA = (PGraphics2D) createGraphics(camWidth, camHeight, P2D);
  
}

void updateCamera() {
  cam.read();
  pgCamA.beginDraw();
  pgCamA.blendMode(REPLACE);
  pgCamA.image(cam, 0, 0);
  pgCamA.endDraw();
  
}
