================================================================================
about ChasmEditor
================================================================================

Author:  John Gardner

This is a Java based tile editor which was initially created for an RPG I am 
currently developing called "The Chasm". Take a look at the game to get an idea 
of what it is capable of: 
http://gamejolt.com/games/rpg/the-chasm/17733/
Here is a platformer game which I also created using it:
http://gamejolt.com/games/platformer/regrowth/19175

Chasm editor supports multiple tile layers (soft limit of 100 set for now),
multiple maps per file, collision detection, animated tiles, tile transparency 
and tile flipping.  An autocompressing map format is used which eliminates the 
need to specify a map size.  All maps automatically have 2^32 by 2^32 dimensions 
and make use of tile repetition to greatly reduce storage requirements.

To try it out, unzip everything and run ChasmEditor.jar in the dist folder.  It
should automatically load up an example map.  Load new map and image files in
the "File Input and Settings" tab.  The Tile Width and Height must be specified
for the image file.  For the examples in dist/tilesets, they are: 
BasicExample.png: 32x32, Metroid.png: 16x16, Platformer.png: 32x32 and 
SimpleTown:64x64.

Take a look at dist/examples to see sample programs demonstrating how the 
maps can be used in Java.  In order of complexity they are: ShowMap.jar, 
BasicExample and Platformer.jar.  Take a look at their src files in the 
examples_src folder.

![Landscape](http://john-gardner.net/ChasmEditor/ChasmEditor.png)
![Landscape](http://john-gardner.net/ChasmEditor/ChasmEditor2.png)
    
