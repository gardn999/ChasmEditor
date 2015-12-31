package mapdata;

import java.io.*;
import java.util.*;

public class MapData{
  final int nLvls = 33, // size = 2^lvl, max size = 2^(nLvls-1)
            TIMESTAMP = 20140322; // make sure loaded file is valid
  int nLayers, // tile layers
     iTopMap = 0; // selected top level map
  ArrayList<Map2d> topMap = new ArrayList<Map2d>();
  Map2dSet[] map = new Map2dSet[nLvls-1];
  Map2d[] mLast = new Map2d[nLvls];
  int xLast = 0, yLast = 0;
  
  public MapData(){ initMapData(1); }
  
  public MapData(int totalLayers){ initMapData(totalLayers); }
  
  public final void initMapData(int totalLayers){
    nLayers = (totalLayers < 1) ? 1 : totalLayers;
    for (int lvl = 0; lvl < nLvls-1; lvl++) map[lvl] = new Map2dSet();
    topMap.clear();
    addTopMap();
    setTopMap(0);
  }
  
  // top level map methods /////////////////////////////////////////////////////

  // set current top level map used for get/set methods
  public void setTopMap(int i){ 
    if ((i < 0) || (i >= topMap.size())) return;
    iTopMap = i; 
    // init info about the last set/get, used to speed up get operations
    xLast = 0; yLast = 0;
    mLast[nLvls-1] = topMap.get(iTopMap);
    for (int lvl = nLvls-2; lvl >= 0; lvl--) 
      mLast[lvl] = mLast[lvl+1].submap[0];
  }
  
  // return index of current topMap in use
  public int currentTopMapIndex(){ return iTopMap; }
  
  // return Map2d object of the i'th topMap
  public Map2d getTopMap(int i){ 
    if ((i < 0) || (i >= topMap.size())) return null;
    else return topMap.get(iTopMap); 
  }
  
  public int totalTopMaps(){ return topMap.size(); }
  
  // add new top level map containing all 0's
  public void insertTopMap(int iAdd){
    if ((iAdd < 0) || (iAdd > topMap.size())) return;
    Map2d m, mPrev = null;
    for (int lvl = 0; lvl < nLvls-1; lvl++){
      m = new Map2d(lvl > 0);
      if (lvl > 0){ for (int i = 0; i < 4; i++) m.submap[i] = mPrev; }
      Map2d mEqual = map[lvl].add(m);
      if (mEqual != null) m = mEqual;
      m.nUsed+= 4;
      mPrev = m;
    }
    m = new Map2d(true); 
    for (int i = 0; i < 4; i++) m.submap[i] = mPrev;
    topMap.add(iAdd, m);
    if (iTopMap >= iAdd) setTopMap(iTopMap + 1);
  }
  
  public void addTopMap(){
    insertTopMap(topMap.size());
  }
  
  public void removeTopMap(int iRemove){
    if (topMap.size() <= 1) return; // need at least 1 topmap
    if ((iRemove < 0) || (iRemove >= topMap.size())) return;
    topMap.remove(iRemove);
    if (iTopMap >= iRemove) setTopMap(iTopMap - 1);
    // calc nUsed and remove if nUsed == 0 for lvls 0 to nLvls-2
    for (int lvl = nLvls-2; lvl >= 0; lvl--){
      // set nUsed to zero
      for (Iterator<Map2d> it = map[lvl].iterator(); it.hasNext(); )
        it.next().nUsed = 0;
      // count nUsed
      for (Map2d m : (lvl+1 == nLvls-1) ? topMap : map[lvl+1])
        for (int j = 0; j < 4; j++) m.submap[j].nUsed++;
      // remove if nUsed == 0
      for (Iterator<Map2d> it = map[lvl].iterator(); it.hasNext(); )
        if (it.next().nUsed == 0) it.remove();
    }
  }
  
  // get/set values ////////////////////////////////////////////////////////////
  
  // get a single layer at x, y
  public int get(int iLayer, int x, int y){ return get(x, y)[iLayer]; }
  
  // set all layers at x, y.  Do not alter returned array! must be read only!
  public int[] get(int x, int y){
    int xor = ((x^xLast) | (y^yLast)) >>> 1;
    xLast = x; yLast = y;
    if (xor >= 8){
      for (int lvl = 32-Integer.numberOfLeadingZeros(xor); lvl >= 1; lvl--) 
        mLast[lvl] = mLast[lvl+1].submap[((x>>lvl)&1) + 2*((y>>lvl)&1)];
    }else if (xor >= 1){
      if (xor >= 4) mLast[3] = mLast[4].submap[((x>>3)&1) + ((y>>2)&2)];
      if (xor >= 2) mLast[2] = mLast[3].submap[((x>>2)&1) + ((y>>1)&2)];
      mLast[1] = mLast[2].submap[((x>>1)&1) + (y&2)];
    }
    mLast[0] = mLast[1].submap[(x&1) + ((y<<1)&2)];
    return mLast[0].layer;
  }
  
  public Map2d getMap(int x, int y, Map2d startMap, int startLvl, int stopLvl){
    Map2d m = startMap;
    for (int lvl = startLvl-1; lvl >= stopLvl; lvl--) 
      m = m.submap[((x>>lvl)&1) + 2*((y>>lvl)&1)];
    return m;
  }
  
  // set a single layer at x, y
  public void set(int val, int iLayer, int x, int y){
    int[] layer = get(x, y).clone();
    layer[iLayer] = val;
    set(layer, x, y);
  }
  
  // set all layers at x, y (as fast as setting a single layer)
  public void set(int[] layer, int x, int y){
    if (Arrays.equals(layer, get(x, y))) return; // exit if no change
    Map2d m = topMap.get(iTopMap); // m = old map at current level
    for (Map2d sub : m.submap) sub.nUsed--;
    // for each level, define mLast[lvl] = the new map at each level
    for (int lvl = nLvls-2; lvl >= 0; lvl--){
      int iSub = ((x>>lvl)&1) + 2*((y>>lvl)&1);  // index of submap to use
      m = m.submap[iSub];
      mLast[lvl] = new Map2d(m);
      mLast[lvl+1].submap[iSub] = mLast[lvl];
      for (Map2d sub : mLast[lvl+1].submap) sub.nUsed++;
      // remove old map if no longer used
      if (m.nUsed < 1){ 
        map[lvl].remove(m);
        if (lvl > 0) for (Map2d sub : m.submap) sub.nUsed--;
      }
    }
    mLast[0].layer = layer.clone(); 
    // for each level, see if a map equal to mLast already exists
    for (int lvl = 0; lvl <= nLvls-2; lvl++){
      // try to add mLast to the set
      Map2d mEqual = map[lvl].add(mLast[lvl]);
      // if an equivalent map exists replace mLast references with it
      if (mEqual != null){
        mLast[lvl] = mEqual;
        mLast[lvl+1].submap[((x>>lvl)&1) + 2*((y>>lvl)&1)] = mEqual;
        mEqual.nUsed++;
        if (lvl > 0) for (Map2d sub : mEqual.submap) sub.nUsed--;
      }
    }
    xLast = x; yLast = y;
  }
  
  // swap/replace indexes //////////////////////////////////////////////////////
  
  // swap i1 and i2 indexes for all layers in map[0]
  public void swapIndexes(int i1, int i2){
    if (i1 == i2) return;
    for (Map2d m : map[0])
      for (int i = 0; i < nLayers; i++)
        if (m.layer[i] == i1) m.layer[i] = i2; 
        else if (m.layer[i] == i2) m.layer[i] = i1;
    // rehash in case hash functions have been altered
    for (Map2dSet map2dSet : map) map2dSet.rehash();
  }
  
  // replace index i1 with i2 for all layers in map[0]
  public void replaceIndex(int i1, int i2){
    if (i1 == i2) return;
    for (Map2d m : map[0])
      for (int i = 0; i < nLayers; i++)
        if (m.layer[i] == i1) m.layer[i] = i2; 
    for (int lvl = 0; lvl < nLvls-1; lvl++){
      // rehash to remove any duplicate maps created
      map[lvl].rehash();
      // reassign submaps using removed duplicates to match kept
      for (Map2d m : (lvl+1 == nLvls-1) ? topMap : map[lvl+1])
        for (int i = 0; i < 4; i++){
          Map2d match = map[lvl].get(m.submap[i]);
          if (match != m.submap[i]){ 
            m.submap[i] = match;
            match.nUsed++;
          }
        }
    }
  }

  // add/insert/remove layers //////////////////////////////////////////////////
  
  public int totalLayers(){ return nLayers; }
  
  // add layer with value 0 to end of layer array
  public void addLayer(){ insertLayer(nLayers); }
  
  // insert layer with value 0 at position in layer array
  public void insertLayer(int position){
    if ((position < 0) || (position > nLayers)) return;
    nLayers++;
    for (Map2d m : map[0]){ 
      m.layer = Arrays.copyOf(m.layer, nLayers);
      for (int i = nLayers-1; i > position; i--) m.layer[i] = m.layer[i-1];
      m.layer[position] = 0;
    }
    // rehash in case hash functions have been altered
    for (Map2dSet map2dSet : map) map2dSet.rehash();
  }

  // remove layer at position
  public void removeLayer(int position){
    if ((position < 0) || (position > nLayers-1)) return;
    nLayers--;
    for (Map2d m : map[0]){ 
      for (int i = position; i < nLayers; i++) m.layer[i] = m.layer[i+1];
      m.layer = Arrays.copyOf(m.layer, nLayers);
    }
    for (int lvl = 0; lvl < nLvls-1; lvl++){
      // rehash to remove any duplicate maps created
      map[lvl].rehash();
      // reassign submaps using removed duplicates to match kept
      for (Map2d m : (lvl+1 == nLvls-1) ? topMap : map[lvl+1])
        for (int i = 0; i < 4; i++){
          Map2d match = map[lvl].get(m.submap[i]);
          if (match != m.submap[i]){ 
            m.submap[i] = match;
            match.nUsed++;
          }
        }
    }
  }
  
  // save and load map data ////////////////////////////////////////////////////
  
  public void save(DataOutputStream out) throws IOException{
    if (out == null) return;
    out.writeInt(TIMESTAMP);
    out.writeInt(nLayers);
    // write level 0 map data
    out.writeInt(map[0].size());
    int max = 1;
    for (Map2d m : map[0]){
      for (int layer : m.layer) if (layer > max) max = layer;
    }
    int bitSize = 32 - Integer.numberOfLeadingZeros(max);
    out.writeInt(bitSize);
    BitWriter bitWriter = new BitWriter(out, bitSize);
    for (Map2d m : map[0]){
      for (int j = 0; j < nLayers; j++) bitWriter.write(m.layer[j]);
    }
    bitWriter.endWrite();
    // write level > 0 map data
    HashMap<Map2d, Integer> indexMap = new HashMap<Map2d, Integer>();
    for (int lvl = 1; lvl < nLvls; lvl++){
      indexMap.clear();
      int index = 0;
      for (Map2d m : map[lvl-1]){
        indexMap.put(m, index);
        index++;
      }
      out.writeInt((lvl == nLvls-1) ? topMap.size() : map[lvl].size());
      bitSize = 32 - Integer.numberOfLeadingZeros(map[lvl-1].size());
      out.writeInt(bitSize);
      bitWriter = new BitWriter(out, bitSize);
      for (Map2d m : (lvl == nLvls-1) ? topMap : map[lvl]){
        for (int j = 0; j < 4; j++) 
          bitWriter.write(indexMap.get(m.submap[j]));
      }
      bitWriter.endWrite();
    }
  }
  
  public void load(DataInputStream in) throws IOException{
    if (in == null) return;
    if (in.readInt() != TIMESTAMP) throw new IOException("invalid TIMESTAMP");
    nLayers = in.readInt();
    // load level 0 map data              
    HashMap<Integer, Map2d> indexMap = new HashMap<Integer, Map2d>();
    map[0].clear();
    int size = in.readInt();
    int bitSize = in.readInt();
    BitReader bitReader = new BitReader(in, bitSize);
    for (int i = 0; i < size; i++){
      Map2d m = new Map2d(false);
      for (int j = 0; j < nLayers; j++) m.layer[j] = bitReader.read();
      map[0].add(m);
      indexMap.put(i, m);
    }
    // load level > 0 map data
    for (int lvl = 1; lvl < nLvls; lvl++){
      if (lvl == nLvls-1) topMap.clear(); else map[lvl].clear();
      size = in.readInt();
      bitSize = in.readInt();
      bitReader = new BitReader(in, bitSize);
      HashMap<Integer, Map2d> indexMap2 = new HashMap<Integer, Map2d>();
      for (int i = 0; i < size; i++){
        Map2d m = new Map2d(true);
        for (int j = 0; j < 4; j++) 
          m.submap[j] = indexMap.get(bitReader.read());
        if (lvl == nLvls-1) topMap.add(m); else map[lvl].add(m);
        indexMap2.put(i, m);
      }
      indexMap = indexMap2;
    }
    // calc nUsed
    for (int lvl = 1; lvl < nLvls; lvl++){
      for (Map2d m : (lvl == nLvls-1) ? topMap : map[lvl])
        for (int j = 0; j < 4; j++) m.submap[j].nUsed++;
    }
    setTopMap( 
         (currentTopMapIndex() >= totalTopMaps()) ? 0 : currentTopMapIndex() );
  }
  
  // print out all the Map2d info
  public void printMapInfo(){
    int nMaps = 0;
    System.out.println("lvl( nMaps):  ID:(submap ID's, nUsed),  ...");
    for (int lvl = nLvls-1; lvl >= 0; lvl--){
      int size = (lvl == nLvls-1) ? topMap.size() : map[lvl].size();
      System.out.print(String.format("%2d (%6d):", lvl, size));
      nMaps+= size;
      for (Map2d m : (lvl == nLvls-1) ? topMap : map[lvl]){
        Map2d[] s = m.submap;
        System.out.print(String.format("%4d:(", m.hashCode()%1000));
        if (lvl > 0)
          System.out.print(String.format("%3d,%3d/%3d,%3d", 
                  s[0].hashCode()%1000, s[1].hashCode()%1000, 
                  s[2].hashCode()%1000, s[3].hashCode()%1000 ));
        else{
          System.out.print("' ");
          for (int l : m.layer) System.out.print(l + " ");
          System.out.print("'");
        }
        System.out.print(String.format(",%2d), ", m.nUsed));
      }
      System.out.println();
    }
    System.out.println("total maps: " + nMaps);
  }

  // see if Map2dSet hashtable bucket sizes are reasonable
  public void testBucketSizes(){
    for (int lvl = nLvls-2; lvl >= 0; lvl--){
      System.out.println("\nbucket sizes for level " + lvl + ":");
      map[lvl].testBucketSizes();
    }
  }
  
  // make sure map is valid
  public void testMap(){
    // make sure all map[lvl] submaps exist in the lvl-1 maps
    int nNotFound = 0;
    for (int lvl = 1; lvl < nLvls; lvl++)
      for (Map2d m : (lvl == nLvls-1) ? topMap : map[lvl])
        for (Map2d s : m.submap){
          boolean found = false;
          for (Map2d m2 : map[lvl-1]) if (s == m2){ found = true; break; }
          if (!found) nNotFound++;
        }
    if (nNotFound > 0) System.out.println(nNotFound + " submaps not found.");
    // make sure all maps are used in the map[lvl+1] submaps
    int nUnused = 0;
    for (int lvl = 0; lvl < nLvls-1; lvl++){
      for (Map2d m : map[lvl]){
        boolean used = false;
        for (Map2d m2 : (lvl+1 == nLvls-1) ? topMap : map[lvl+1]){
          for (Map2d s : m2.submap) if (s == m) used = true;
          if (used) break;
        }
        if (!used) nUnused++;
      }
    }
    if (nUnused > 0) System.out.println(nUnused + " maps are unused.");
    if ((nUnused == 0) && (nNotFound == 0))
      System.out.println("Map has no errors.");
  }
  
  // Map2d /////////////////////////////////////////////////////////////////////
  public class Map2d{
    int[] layer = null;
    Map2d[] submap = null;
    Map2d next = null;  // used by Map2dSet
    int nUsed = 0;
  
    public Map2d(boolean useSubmaps){
      if (useSubmaps) submap = new Map2d[4]; else layer = new int[nLayers];
    }
  
    public Map2d(Map2d m){
      if (m.layer != null) layer = Arrays.copyOf(m.layer, nLayers);
      else submap = Arrays.copyOf(m.submap, 4);
    }
  
    public boolean equals(Map2d m){
      if (m == null) return false;
      else if (layer != null) return Arrays.equals(layer, m.layer);
      else return Arrays.equals(submap, m.submap);
    }
  
    public int hash(){
      int h = 1315423911;  // JSHash - Justin Sobel
      if (layer != null) for (int i : layer) h^= (h<<5) + i + (h>>2);
      else for (Map2d i : submap) h^= (h<<5) + i.hashCode() + (h>>2);
      return h;
    }
  }

  // Map2dSet //////////////////////////////////////////////////////////////////
  public class Map2dSet implements Iterable<Map2d>{
    int tableSize = 16, nEntries = 0;
    Map2d[] table = new Map2d[tableSize];
  
    public void clear(){
      tableSize = 16; nEntries = 0; table = new Map2d[tableSize];
    }
  
    // if match found, return reference else add data and return null
    public Map2d add(Map2d data){
      data.next = null;
      int hash = data.hash()&(tableSize-1);
      if (table[hash] == null){ 
        table[hash] = data;
      }else{
        Map2d e = table[hash];
        while ((e.next != null) && (!e.equals(data))) e = e.next;
        if (e.equals(data)) return e; else e.next = data;
      }
      nEntries++;
      if (nEntries > tableSize*2) rehash();
      return null;
    }
  
    // if match found, return reference else return null
    public Map2d get(Map2d data){
      int hash = data.hash()&(tableSize-1);
      if (table[hash] == null) return null;
      else{
        Map2d e = table[hash];
        while ((e != null) && (!e.equals(data))) e = e.next;
        if (e == null) return null; else return e;
      }
    }
  
    public void remove(Map2d data){
      int hash = data.hash()&(tableSize-1);
      if (table[hash] != null){
        Map2d ePrev = null, e = table[hash];
        while ((e.next != null) && (!e.equals(data))){ ePrev = e; e = e.next; }
        if (e.equals(data)){
          if (ePrev == null) table[hash] = e.next; else ePrev.next = e.next;
          nEntries--;
        }
      }
    }

    public int size(){ return nEntries; }
  
    public void rehash(){
      Map2d[] oldTable = table;
      tableSize = 1 << (32 - Integer.numberOfLeadingZeros(nEntries));
      nEntries = 0; 
      table = new Map2d[tableSize];
      for (Map2d e : oldTable) 
        while (e != null){ Map2d next = e.next; add(e); e = next; }
    }
    
    @Override public Map2dSetIterator iterator(){ 
      return new Map2dSetIterator(); 
    }
  
    public class Map2dSetIterator implements Iterator<Map2d>{
      int visited = 0, index = -1;
      Map2d currentEntry = new Map2d(true);
    
      @Override public boolean hasNext(){ return visited < nEntries; }
    
      @Override public Map2d next(){
        if (visited >= nEntries) return null;
        if (currentEntry.next == null){
          do index++; while (table[index] == null);
          currentEntry = table[index];
        }else{ 
          currentEntry = currentEntry.next;
        }
        visited++; 
        return currentEntry;
      }
    
      @Override public void remove(){
        Map2dSet.this.remove(currentEntry);
        visited--;
      }
    }

    public void testBucketSizes(){
      int sum2 = 0;
      for (int i = 0; i < tableSize; i++){
        int size = 0; 
        Map2d e = table[i];
        while (e != null){ size++; e = e.next; }
        sum2+= size*size;
        System.out.print(String.format("%4d:%2d  ", i, size));
        if (i%10 == 9) System.out.println();
      }
      System.out.println("\nratio: " + 
           Math.sqrt((double)sum2*tableSize/nEntries/nEntries));
    }
  }

  // BitReader /////////////////////////////////////////////////////////////////
  public class BitReader{
    DataInputStream in;
    int bitSize, readBit, bitAnd, readInt;
    
    public BitReader(DataInputStream In, int BitSize) throws IOException{
      in = In; bitSize = BitSize; 
      readBit = 0; bitAnd = (1<<bitSize) - 1;
    }
    
    public int read() throws IOException{
      if (readBit == 0) readInt = in.readInt();
      int val = readInt >>> readBit;
      if (readBit + bitSize > 32){
        readInt = in.readInt();
        val+= (readInt << 32-readBit);
      }
      readBit = (readBit+bitSize)%32;
      return val & bitAnd;
    }
  }
  
  // BitWriter /////////////////////////////////////////////////////////////////
  public class BitWriter{
    DataOutputStream out;
    int bitSize, writeBit, writeInt;
    
    public BitWriter(DataOutputStream Out, int BitSize){
      out = Out; bitSize = BitSize; writeBit = 0; writeInt = 0;
    }
    
    public void write(int val) throws IOException{
      writeInt+=  val << writeBit;
      if (writeBit + bitSize > 31) out.writeInt(writeInt);
      writeBit = (writeBit+bitSize)%32;
      if (writeBit < bitSize) writeInt = val >>> bitSize-writeBit;
    }
    
    public void endWrite() throws IOException{
      if (writeBit > 0) out.writeInt(writeInt);
    }
  }
}
