package paletai.mapping;

import paletai.generators.LunaContentGenerator;
import processing.core.*;
import processing.core.PApplet;
import processing.data.XML;

public class GenerativeMediaItem extends MediaItem {
    public GenerativeMediaItem(PApplet p, LunaContentGenerator generator, int screenIndex, int mediaId) {
        super(p, generator, screenIndex, mediaId);
    }

    public GenerativeMediaItem(PApplet p, LunaContentGenerator generator, XML mediaXML) {
        // Call the parent constructor that handles XML loading
        super(p, generator, mediaXML);
    }

    @Override
    public void render() {
        // Use the proven VidMap approach that worked in your test
        this.generator.update();
        if (vm != null) {
            vm.render(this.generator.getGraphics());
        }

        // Thumbnail generation can stay the same
        if (!thumbnailGenerated) {
            generateThumbnail();
        }
    }
}
