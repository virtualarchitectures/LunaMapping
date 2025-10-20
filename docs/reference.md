# API Reference

Complete documentation for Luna Video Mapping Library's classes and methods.

## Overview

Luna provides a comprehensive API for video mapping, scene management, and generative content integration. This reference covers all public classes and methods available to developers.

## Core Classes

### Project Class
The main container that manages multiple scenes, screens, and the overall project structure.

**Key Methods:**
- `Project(PApplet, String)` - Create a new project
- `addNewScene()` - Add a scene to the project
- `addNewScreen()` - Add a screen configuration
- `render()` - Render the entire project
- `startTransition()` - Begin scene transition

### Scene Class
Manages collections of media items as individual scenes with activation controls.

**Key Methods:**
- `addMedia(MediaItem)` - Add media to scene
- `activate()` / `deactivate()` - Control scene playback
- `render()` - Render scene content
- `saveXML()` - Serialize scene configuration

### Screen Class
Represents physical or virtual displays for video output.

**Key Methods:**
- `assignToDisplay(Rectangle)` - Assign to physical display
- `updateMediaList()` - Update displayed media
- `render()` - Render screen output
- `setTransitionState()` - Handle scene transitions

### MediaItem Class
Base class for all media types (videos, images, generative content).

**Key Methods:**
- `render()` - Render media with transformations
- `toggleCalibration()` - Toggle mapping calibration
- `assignToDisplay()` - Assign to specific screen
- `playMedia()` / `stopMedia()` - Control playback

### GenerativeMediaItem Class
Specialized MediaItem for programmatically generated content.

**Key Methods:**
- Extends all MediaItem functionality
- Integrates with `LunaContentGenerator` implementations

## Utility Classes

### VidMap Class
Handles homography transformations and point calibration.

**Key Methods:**
- `updateHomography()` - Update transformation matrix
- `toggleCalibration()` - Toggle calibration mode
- `render()` - Apply transformations

### LunaContentGenerator Interface
Interface for creating custom generative content.

**Required Methods:**
- `getName()` - Return generator name
- `setup(width, height)` - Initialize generator
- `update()` - Update generator state
- `getGraphics()` - Return current frame

## Complete Documentation

For detailed method signatures, parameters, and usage examples, see the full Javadoc documentation:

[View Complete Javadoc](../reference/index.html)

## Usage Patterns

### Basic Project Setup
```java
import paletai.mapping.*;

Project project = new Project(this, "MyMappingProject");
// Add scenes and media...
project.render(mouseX, mouseY);
```
### Custom Generator Implementation
Implement the LunaContentGenerator interface to create your own dynamic visual content that integrates seamlessly with the mapping system.

## Next Steps

- Explore the [examples](examples.md) for practical implementations
- Check [troubleshooting](troubleshooting.md) for common issues
- Review the [source code](../src/) for advanced customization