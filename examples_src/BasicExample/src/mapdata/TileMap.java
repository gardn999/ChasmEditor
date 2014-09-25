package mapdata;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

public class TileMap extends MapData{
  final int TILEMAP_TIMESTAMP = 20140322; // make sure loaded file is valid
  BufferedImage tileImage;  // image file used for drawing BaseTiles
  BaseTile[] baseTile = null; 
  final int nCompTiles = 512;
  CompTile[] compTile = new CompTile[nCompTiles];
  int firstLayer = 0, lastLayer = nLayers-1,
      tileW, tileH, nImageTilesX, nImageTilesY, nTilesMap;
  
  // initialization ////////////////////////////////////////////////////////////
  
  public TileMap(){ super(); initTiles(); }
  
  public TileMap(int totalLayers){ super(totalLayers); initTiles(); }
  
  public boolean initAll(java.net.URL mapURL, java.net.URL imageURL, 
                         int tileWidth, int tileHeight){
    try{
      load(new DataInputStream(mapURL.openStream()));
      initBaseTilesFromImage(
           javax.imageio.ImageIO.read(imageURL), tileWidth, tileHeight);
      return true;
    }catch(IOException e){
      return false;
    }
  }
  
  public final void initTiles(){
    nTilesMap = nImageTilesX = nImageTilesY = 1;
    baseTile = new BaseTile[1]; baseTile[0] = new BaseTile();
    for (int i = 0; i < nCompTiles; i++) compTile[i] = new CompTile();
    setLayerRange(0, nLayers-1);
  }
  
  // call each frame to update animated compTiles
  public void update(long dt){
    if (compTile[0] == null) return;
    for (int i = 0; i < nCompTiles; i++) compTile[i].update(dt);
  }
  
  // set first and last layers used for drawMap, drawAvgColors and getAvgColor
  public void setLayerRange(int first, int last){
    firstLayer = first; lastLayer = last;
  }
  
  // In pixel units, draw the map range:(mapX, mapY, mapW, mapH) to
  // Graphics g:(dstX, dstY) using layers: firstLayer to lastLayer
  // (draws whole tiles which may extend beyond the destination bounds)
  public void drawMap(int mapX, int mapY, int mapW, int mapH, 
                      Graphics g, int dstX, int dstY){
    if (firstLayer < 0) firstLayer = 0;
    if (lastLayer > nLayers-1) lastLayer = nLayers-1;
    int i0 = (mapX < 0) ? (mapX+1)/tileW - 1 : mapX/tileW,
        j0 = (mapY < 0) ? (mapY+1)/tileH - 1 : mapY/tileH;
    for (int j = j0; j*tileH < mapY+mapH; j++){
      int y = dstY + j*tileH-mapY;
      for (int i = i0; i*tileW < mapX+mapW; i++){
        int x = dstX + i*tileW-mapX;
        int[] layer = get(i, j);
        for (int iLayer = firstLayer; iLayer <= lastLayer; iLayer++)
          drawTile(g, layer[iLayer], x, y, tileW, tileH);
      }
    }
  }
  
  // In pixel units, draw the map range:(mapX, mapY, mapW, mapH) to
  // Graphics g:(dstX, dstY, dstW, dstW) using layers: firstLayer to lastLayer
  // (draws whole tiles which may extend beyond the destination bounds)
  public void drawMap(int mapX, int mapY, int mapW, int mapH, 
                      Graphics g, int dstX, int dstY, int dstW, int dstH){
    if ((mapW == 0) || (mapH == 0)) return;
    if (firstLayer < 0) firstLayer = 0;
    if (lastLayer > nLayers-1) lastLayer = nLayers-1;
    int i0 = (mapX < 0) ? (mapX+1)/tileW - 1 : mapX/tileW,
        j0 = (mapY < 0) ? (mapY+1)/tileH - 1 : mapY/tileH;
    for (int j = j0; j*tileH < mapY+mapH; j++){
      int y = dstY + (j*tileH - mapY)*dstH/mapH, 
          h = dstY + ((j+1)*tileH - mapY)*dstH/mapH - y;
      for (int i = i0; i*tileW < mapX+mapW; i++){
        int x = dstX + (i*tileW - mapX)*dstW/mapW, 
            w = dstX + ((i+1)*tileW - mapX)*dstW/mapW - x;
        int[] layer = get(i, j);
        for (int iLayer = firstLayer; iLayer <= lastLayer; iLayer++)
          drawTile(g, layer[iLayer], x, y, w, h);
      }
    }
  }
  
  // draw average colors in the map range:(mapX, mapY, w, h) to 
  // dst:(dstX, dstY) using layers: firstLayer to lastLayer
  public void drawAvgColors(int mapX, int mapY, int w, int h, 
                            BufferedImage dst, int dstX, int dstY){
    int[] pixels = ((DataBufferInt)dst.getRaster().getDataBuffer()).getData();
    if (pixels == null) return;
    int dstW = dst.getWidth(),
        xi8 = (mapX < 0) ? (mapX/8)*8 : ((mapX+7)/8)*8, 
        yi8 = (mapY < 0) ? (mapY/8)*8 : ((mapY+7)/8)*8,
        xf8 = xi8 + ((mapX+w - xi8)/8)*8,
        yf8 = yi8 + ((mapY+h - yi8)/8)*8,
        i0 = (dstY-mapY)*dstW + dstX-mapX;
    // interior whole 8x8 blocks
    for (int y8 = yi8; y8 < yf8; y8+= 8){
      for (int x8 = xi8; x8 < xf8; x8+= 8){
        Map2d m8 = getMap(x8, y8, topMap.get(iTopMap), 32, 3);
        int i8 = i0 + x8 + y8*dstW;
        for (int s4 = 0; s4 < 4; s4++){
          Map2d m4 = m8.submap[s4];
          int i4 = i8 + (s4&1)*4 + (s4&2)*2*dstW;
          for (int s2 = 0; s2 < 4; s2++){
            Map2d[] m2sub = m4.submap[s2].submap;
            int i2 = i4  + (s2&1)*2 + (s2&2)*dstW;
            pixels[i2] = getAvgColor(m2sub[0].layer);
            pixels[i2+1] = getAvgColor(m2sub[1].layer);
            pixels[i2+dstW] = getAvgColor(m2sub[2].layer);
            pixels[i2+dstW+1] = getAvgColor(m2sub[3].layer);
          }  
        }
      }
    }
    // vertical edges
    for (int x = mapX; x < xi8; x++)
      for (int y = mapY; y < mapY+h; y++)
        pixels[i0 + x + y*dstW] = getAvgColor(get(x, y)); 
    for (int x = xf8; x < mapX+w; x++)
      for (int y = mapY; y < mapY+h; y++)
        pixels[i0 + x + y*dstW] = getAvgColor(get(x, y)); 
    // horizontal edges
    for (int y = mapY; y < yi8; y++)
      for (int x = mapX; x < mapX+w; x++)
        pixels[i0 + x + y*dstW] = getAvgColor(get(x, y)); 
    for (int y = yf8; y < mapY+h; y++)
      for (int x = mapX; x < mapX+w; x++)
        pixels[i0 + x + y*dstW] = getAvgColor(get(x, y));
  }
  
  // calculate an average color using layers: firstLayer to lastLayer
  public int getAvgColor(int[] layer){
    if (firstLayer < 0) firstLayer = 0;
    if (lastLayer > nLayers-1) lastLayer = nLayers-1;
    int i = firstLayer,
        avg = ((layer[i]&4) == 4) ? compTile[layer[i] >> 3].avgColor :
                                    baseTile[layer[i] >> 3].avgColor,
       rb = avg&0xff00ff, g = avg&0xff00;
    while (++i <= lastLayer){
      if (layer[i] >= 4){  // skip empty tile (ID < 4)
        avg = ((layer[i]&4) == 4) ? compTile[layer[i] >> 3].avgColor :
                                    baseTile[layer[i] >> 3].avgColor;
        int a = avg>>>24, a0 = 255-a;
        rb = ( ((avg&0xff00ff)*a + rb*a0) >>> 8 ) & 0xff00ff;
        g = ( ((avg&0xff00)*a + g*a0) >>> 8 ) & 0xff00;
      }
    }
    return rb | g | 0xff000000;
  }
                      
  // draw tile at x,y
  public void drawTile(Graphics g, int ID, int x, int y){
    drawTile(g, ID, x, y, tileW, tileH);
  }
  
  // draw stretched tile at x,y
  public void drawTile(Graphics g, int ID, int x, int y, int w, int h){
    if (ID < 4) return; // do nothing for empty tile
    boolean hFlip = ((ID&1) == 1), vFlip = ((ID&2) == 2);
    if ((ID&4) == 4){ // CompTile
      CompTile c = compTile[ID>>3];
      if (!c.info.isEmpty()){
        if (c.baseID < 4) return; // do nothing for empty tile
        hFlip = hFlip ^ ((c.baseID&1)==1); vFlip = vFlip ^ ((c.baseID&2)==2);
        float alpha = c.info.get(c.index).alpha;
        if (alpha < 1f) 
          ((Graphics2D)g).setComposite(
               AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        baseTile[c.baseID>>3].draw(g, hFlip ? x+w : x, vFlip ? y+h : y, 
                                   hFlip ? x : x+w, vFlip ? y : y+h);
        if (alpha < 1f) ((Graphics2D)g).setComposite(AlphaComposite.SrcOver);
      }
    }else{
      baseTile[ID>>3].draw(g, hFlip ? x+w : x, vFlip ? y+h : y, 
                              hFlip ? x : x+w, vFlip ? y : y+h);
    }
  }
  
  // file i/o //////////////////////////////////////////////////////////////////
  
  @Override public void save(DataOutputStream out) throws IOException{
    if (out == null) return;
    out.writeInt(TILEMAP_TIMESTAMP);
    // write base tiles
    out.writeInt(baseTile.length);
    for (BaseTile b : baseTile){ 
      out.writeShort(b.solid); out.writeUTF(b.name); out.writeUTF(b.type);
    }
    // write composite tiles
    out.writeInt(compTile.length);
    for (CompTile c : compTile){
      out.writeUTF(c.name); 
      out.writeUTF(c.type);
      out.writeInt(c.info.size());
      for (int j = 0; j < c.info.size(); j++) {
        out.writeInt(c.info.get(j).baseID);
        out.writeInt(c.info.get(j).time);
        out.writeFloat(c.info.get(j).alpha);
      }
    }
    // save map data
    super.save(out);
  }
  
  @Override public void load(DataInputStream in) throws IOException{
    if (in == null) return;
    if (in.readInt() != TILEMAP_TIMESTAMP)
      throw new IOException("invalid TIMESTAMP");
    // load base tiles
    nTilesMap = in.readInt();
    checkBaseTileLength();
    for (int i = 0; i < nTilesMap; i++){
      baseTile[i].solid = in.readShort(); 
      baseTile[i].name = in.readUTF(); 
      baseTile[i].type = in.readUTF();
    }
    baseTile[0].solid = 0; // make sure empty tile isn't solid
    // load composite tiles
    int nComp = in.readInt();
    for (int i = 0; i < nComp; i++){
      compTile[i].name = in.readUTF(); 
      compTile[i].type = in.readUTF();
      compTile[i].info.clear();
      compTile[i].reset();
      int size = in.readInt();
      for (int j = 0; j < size; j++)
        compTile[i].info.add(
             new CompInfo(in.readInt(), in.readInt(), in.readFloat()));
    }
    // load map data
    super.load(in);
    setLayerRange(0, nLayers-1);
  }
  
  public void initBaseTilesFromImage(BufferedImage src, int tw, int th){
    tileImage = src; tileW = tw; tileH = th;
    if ((src == null) || (src.getWidth() < tw) || (src.getHeight() < th)){
      tileImage = null;
      nImageTilesX = nImageTilesY = 1;
    }else{
      nImageTilesX = tileImage.getWidth()/tileW;
      nImageTilesY = tileImage.getHeight()/tileH;
    }
    checkBaseTileLength();
    if (tileImage != null){
      for (int j = 0; j < nImageTilesY; j++)
        for (int i = 0; i < nImageTilesX; i++)
          baseTile[i + j*nImageTilesX].initTileFromImage(
               i*tileW, j*tileH, tileW, tileH);
    }
    // if no image is defined for baseTile, avgColor must be clear
    for (int i = nImageTiles(); i < nTilesMap; i++) 
      baseTile[i].avgColor = 0;
    // for 0 = empty(null) tile, avgColor and solid must be clear
    baseTile[0].avgColor = 0; baseTile[0].solid = 0; 
  }
  
  private void checkBaseTileLength(){
    int oldLength = baseTile.length, 
        newLength = Math.max(nTilesMap, nImageTiles());
    if (newLength != oldLength) 
      baseTile = Arrays.copyOf(baseTile, newLength);
    for (int i = oldLength; i < newLength; i++) 
      baseTile[i] = new BaseTile();
  }
  
  // access BaseTiles and CompTiles ////////////////////////////////////////////
  public BaseTile getBaseTile(int iTile){ return baseTile[iTile]; }
  public CompTile getCompTile(int iTile){ return compTile[iTile]; }
 
  // the number of BaseTiles represented by the tileImage //////////////////////
  public int nImageTiles(){ return nImageTilesX*nImageTilesY; }
  public int nImageTilesX(){ return nImageTilesX; }
  public int nImageTilesY(){ return nImageTilesY; }

  // tile ID conversion methods
  // ID = iTile*8 + isCompTile*4 + vFlip*2 + hFlip
  
  public static int getID(int iTile, boolean isComp, 
                          boolean vFlip, boolean hFlip){
    return iTile*8 + (isComp ? 4 : 0) + (vFlip ? 2 : 0) + (hFlip ? 1 : 0);
  }
  
  public static int getTileIndex(int ID){ return ID/8; }
  public static boolean isCompTile(int ID){ return ((ID&4) == 4); }
  public static boolean isVFlipped(int ID){ return ((ID&2) == 2); }
  public static boolean isHFlipped(int ID){ return ((ID&1) == 1); }
  
  public int getBaseTileID(int ID){
    return ((ID&4) == 4) ? compTile[ID>>3].baseID : ID;
  }
  
  public boolean isValidID(int ID){
    return ((ID&4) == 4) ? (ID < nCompTiles*8) : (ID < baseTile.length*8);
  }
  
  // collision detection using tile solid bits
  
  // return true if map at point x,y is solid
  public boolean collisionAt(double x, double y){
    int i = xToI(x*4/tileW), j = xToI(y*4/tileH), ii = i&3, jj = j&3;
    int[] layer = get(i>>2, j>>2);
    for (int ID : layer)
      if (isSolid(ID, ii, jj)) return true;
    return false;
  }  
    
  // return true if any part of the map within (x, y, w, h) is solid
  public boolean collisionAt(double x, double y, double w, double h){
    int i1 = xToI(x*4/tileW), i2 = xToI((x+w)*4/tileW),
        j1 = xToI(y*4/tileH), j2 = xToI((y+h)*4/tileH);
    for (int j = j1>>2; j <= j2>>2; j++){
      int jj1 = (j*4 >= j1) ? 0 : j1-j*4, jj2 = (j2-j*4 >= 3) ? 4 : j2-j*4+1;
      for (int i = i1>>2; i <= i2>>2; i++){
        int ii1 = (i*4 >= i1) ? 0 : i1-i*4, ii2 = (i2-i*4 >= 3) ? 4 : i2-i*4+1;
        int[] layer = get(i, j);
        for (int ID : layer) 
          if (isSolid(ID, ii1, jj1, ii2, jj2)) return true;
      }
    }
    return false;
  }
  
  static final int xToI(double x){ return (x < 0) ? (int)x - 1 : (int)x; }
  
  // tile solid methods for collision detection ////////////////////////////////
  // short solid bits represents a 4x4 solid grid, for each bit 1=solid
  
  // return if bit representing i,j is solid
  public boolean isSolid(int ID, int i, int j){ 
    if (ID < 4) return false; // empty tile can't be solid
    int baseID = ((ID&4) == 4) ? compTile[ID>>3].baseID : ID,
        xorID = ((ID&4) == 4) ? ID^baseID : ID;
    return baseTile[baseID>>3].isSolid( ((xorID&1) == 1) ? 3-i : i, 
                                        ((xorID&2) == 2) ? 3-j : j);
  }
    
  // must have 0 <= i1, j1, i2, j2 <= 4 and i2 > i1, j2 > j1
  public boolean isSolid(int ID, int i1, int j1, int i2, int j2){
    if (ID < 4) return false; // empty tile can't be solid
    int baseID = ((ID&4) == 4) ? compTile[ID>>3].baseID : ID,
        xorID = ((ID&4) == 4) ? ID^baseID : ID;
    boolean hFlip = (xorID&1) == 1, vFlip = (xorID&2) == 2;
    return baseTile[baseID>>3].isSolid(hFlip ? 4-i2 : i1, vFlip ? 4-j2 : j1, 
                                       hFlip ? 4-i1 : i2, vFlip ? 4-j1 : j2);
  }

  // return short solid for all layers in an array or'ed together
  public short getLayerSolid(int[] layer){
    short solid = 0;
    for (int ID : layer){
      solid |= getSolid(ID);
      if (solid == -1) return -1;
    }
    return solid;
  }
  
  // return short solid corrected for horizontal and vertical flipping
  public short getSolid(int ID){
    if (ID < 4) return 0; // empty tile can't be solid
    int baseID = ((ID&4) == 4) ? compTile[ID>>3].baseID : ID;
    short s = baseTile[baseID>>3].solid;
    if (s == 0) return 0; else if (s == -1) return -1;
    
    int xorID = ((ID&4) == 4) ? ID^baseID : ID;
    if ((xorID & 1) == 1){ // hFlip
      s = (short)(((s << 3) & 0x8888) + ((s << 1) & 0x4444) +
                  ((s >> 1) & 0x2222) + ((s >> 3) & 0x1111));
    }
    if ((xorID & 2) == 2){ // vFlip
      s = (short)(((s << 12) & 0xF000) + ((s <<  4) & 0x0F00) +
                  ((s >>  4) & 0x00F0) + ((s >> 12) & 0x000F));
    }
    return s;
  }

  // draw the 4x4 grid represented by short solid
  public void drawSolid(Graphics g, short solid, Color solidColor, 
                        int x, int y, int w, int h){
    if (solid == 0) return; 
    g.setColor(solidColor);
    if (solid == -1){ g.fillRect(x, y, w, h); return; }
    
    for (int j = 0, y4 = y*4 + 2; j < 4; j++, y4+= h){
      int yj = y4>>2, hj = ((y4+h)>>2) - yj, s = solid >> (j*4);
      for (int i = 0, x4 = x*4 + 2; i < 4; i++, x4+= w)
        if ((s & (1<<i)) != 0) 
          g.fillRect(x4>>2, yj, ((x4+w)>>2) - (x4>>2), hj);
    }
  }

  // swap or replace all tile ID references in the map and compTiles ///////////
  
  // swap tiles i1 and i2
  public void swapTiles(int i1, int i2){
    if (i1 == i2) return;
    swapIndexes(i1, i2);
    for (CompTile tile : compTile)
      for (CompInfo info : tile.info)
        if (info.baseID == i1) info.baseID = i2;
        else if (info.baseID == i2) info.baseID = i1;
  }
  
  // replace tile i1 with i2
  public void replaceTile(int i1, int i2){
    if (i1 == i2) return;
    replaceIndex(i1, i2);
    for (CompTile tile : compTile)
      for (CompInfo info : tile.info)
        if (info.baseID == i1) info.baseID = i2;
  }
  
  
  // tile name and type methods ////////////////////////////////////////////////
  
  public String getName(int ID){
    return ((ID&4) == 4) ? compTile[ID>>3].name : baseTile[ID>>3].name; 
  }
  
  public String getType(int ID){
    return ((ID&4) == 4) ? compTile[ID>>3].type : baseTile[ID>>3].type; 
  }
  
  public void setName(int ID, String newName){
    if ((ID&4) == 4) compTile[ID>>3].name = newName;
    else baseTile[ID>>3].name = newName;
  }
  
  public void setType(int ID, String newType){
    if ((ID&4) == 4) compTile[ID>>3].type = newType;
    else baseTile[ID>>3].type = newType;
  }

  // BaseTile //////////////////////////////////////////////////////////////////
  
  public class BaseTile{
    int x1, y1, x2, y2, avgColor;
    short solid;
    String name = "", type = "";
    
    public void initTileFromImage(int srcX, int srcY, int w, int h){
      x1 = srcX; y1 = srcY; x2 = srcX+w; y2 = srcY+h;
      // calc avgColor
      if (tileImage != null){
        int aSum = 0, rSum = 0, gSum = 0, bSum = 0, n = w*h;
        for (int y = y1; y < y2; y++)
          for (int x = x1; x < x2; x++){
            int c = tileImage.getRGB(x, y);
            aSum+= c >>> 24; 
            rSum+= (c >>> 16) & 0xFF;
            gSum+= (c >>> 8) & 0xFF; 
            bSum+= c & 0xFF;
          }
        avgColor = ((aSum+n/2)/n << 24) + ((rSum+n/2)/n << 16) + 
                   ((gSum+n/2)/n << 8) + (bSum+n/2)/n;
      }
    }
    
    public short getSolid(){ return solid; }
    
    public void setSolid(short newSolid){ solid = newSolid; }
    
    public void setSolid(int i, int j){ solid |= (1 << (i+j*4)); }
    
    public void clearSolid(int i, int j){ solid &= ~(1 << (i+j*4)); }
    
    public boolean isSolid(int i, int j){ 
      return (solid & (1 << (i + j*4))) != 0; 
    }
    
    // must have 0 <= i1, j1, i2, j2 <= 4 and i2 > i1, j2 > j1
    public boolean isSolid(int i1, int j1, int i2, int j2){
      if (solid == 0) return false; else if (solid == -1) return true;
      for (int j = j1; j < j2; j++)
        for (int i = i1; i < i2; i++)
          if ((solid & (1 << (i + j*4))) != 0) return true;
      return false;
    }
    
    public void draw(Graphics g, int x, int y){
      if (tileImage == null) return;
      g.drawImage(tileImage, x, y, x+x2-x1, y+y2-y1, x1, y1, x2, y2, null);
    }
    
    public void draw(Graphics g, int dx1, int dy1, int dx2, int dy2){
      if (tileImage == null) return;
      g.drawImage(tileImage, dx1, dy1, dx2, dy2, x1, y1, x2, y2, null);
    }
  }
  
  // CompTile //////////////////////////////////////////////////////////////////
  // composed of BaseTiles, used for animation and transparency effects
  
  // reset when an animation is changed to make sure everything is in sync
  public void resetCompTimes(){
    if (compTile[0] == null) return;
    for (int i = 0; i < nCompTiles; i++) compTile[i].reset();
  }

  public void copyCompTile(int srcTile, int dstTile){
    CompTile src = compTile[srcTile], dst = compTile[dstTile];
    dst.ctr = src.ctr; dst.index = src.index; 
    dst.baseID = src.baseID; dst.avgColor = src.avgColor;
    dst.name = src.name; dst.type = src.type;
    dst.info = new ArrayList<CompInfo>();
    for (int i = 0; i < src.info.size(); i++){
      CompInfo c = src.info.get(i);
      dst.add(c.baseID, c.time, c.alpha);
    }
  }
    
  public class CompInfo{ 
    public int baseID, time; 
    public float alpha; 
    CompInfo(int ID, int ms, float a){ baseID = ID; time = ms; alpha = a; }
  }
  
  public class CompTile{
    long ctr = 0;  // counter in nanoseconds for frame timing
    int index = 0, // current compInfo index
        baseID = 0, avgColor = 0;
    String name = "", type = "";
    ArrayList<CompInfo> info = new ArrayList<CompInfo>();  // i'th tile
    
    public int infoSize(){ return info.size(); }
    
    public CompInfo getInfo(int i){ return info.get(i); }
    
    public void add(int b, int t, float a){ info.add(new CompInfo(b, t, a)); }
    
    public void remove(int i){ info.remove(i); }
    
    public void insert(int i, int b, int t, float a){ 
      info.add(i, new CompInfo(b, t, a));
    }
    
    public void reset(){ ctr = 0; index = 0; }
    
    public void update(long dt){ // dt = time in nanoseconds
      if (info.isEmpty()){ baseID = 0; avgColor = 0; return; }
      ctr+= dt;
      index = index%info.size();
      while (ctr > (long)info.get(index).time*1000000){ 
        ctr-= (long)info.get(index).time*1000000; 
        index = (index+1)%info.size(); 
      }
      baseID = info.get(index).baseID;
      if (baseID >= (baseTile.length<<3)) baseID = 0;
      avgColor = baseTile[baseID>>3].avgColor; 
    }
  }
}
