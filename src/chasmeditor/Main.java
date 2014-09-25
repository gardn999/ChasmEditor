package chasmeditor;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.*;
import mapdata.TileMap;

public class Main extends JFrame implements ActionListener{
  final int INI_TIMESTAMP = 20140803, // make sure .ini file is valid
            DEFAULT_LAYERS = 1, MAX_LAYERS = 100;
  String iniFileName = "ChasmEditor.ini",
         autoBackupName = "ChasmEditorAutoBackup.map",
         mapFilePath = "", tileImagePath = "";
  int tileW = 16, tileH = 16;
  Timer timer; 
  TileMap tileMap = new TileMap(DEFAULT_LAYERS);
  JTabbedPane mainTabbedPane;
  FileSettings fileSettings;
  MapEditor mapEditor;
  TileEditor tileEditor;
  Controls controls;
  CloseOperation closeOperation = new CloseOperation();
  boolean firstTime = true;
  long nanoTime, repaintTimer = 0, 
       repaintDelay = (long)(1.0e9/40); // 40 max fps
  int pulse = 0; 
  Color selectColor = Color.black, solidColor = Color.black,
        bg1 = new Color(140, 150, 160), bg2 = new Color(110, 120, 130);
  Font monoFont = new Font("monospaced", Font.BOLD, 14);
  
  public static void main(String[] args){
    Main main = new Main();
    main.start();
  }
  
  void start(){
    setTitle("Chasm Map Editor");
    fileSettings = new FileSettings(this);
    mapEditor = new MapEditor(this);
    tileEditor = new TileEditor(this);
    controls = new Controls(this);
    int tabIndex = 0;
    
    // initialize using the .ini file
    if (new File(iniFileName).exists()){
      try{
        // read from .ini file
        BufferedReader br = new BufferedReader(new FileReader(iniFileName));
        if (Integer.parseInt(br.readLine()) != INI_TIMESTAMP)
          throw new IOException("invalid TIMESTAMP");
        loadMap(br.readLine());
        loadTileImage(br.readLine(), Integer.parseInt(br.readLine()), 
             Integer.parseInt(br.readLine()));
        setPreferredSize(new Dimension(Integer.parseInt(br.readLine()), 
                                       Integer.parseInt(br.readLine()) ));
        tabIndex = Integer.parseInt(br.readLine());
        mapEditor.mapTabbedPane.setSelectedIndex(
          Integer.parseInt(br.readLine()));
        mapEditor.tilesMapSplitPane.setDividerLocation(
          Integer.parseInt(br.readLine()));
        mapEditor.mainSplitPane.setDividerLocation(
          Integer.parseInt(br.readLine()));
        tileEditor.splitPane.setDividerLocation(
          Integer.parseInt(br.readLine()));
        mapEditor.selectionTiles[0][0] = Integer.parseInt(br.readLine());
        mapEditor.viewPanel.setView(
             Integer.parseInt(br.readLine()), // x
             Integer.parseInt(br.readLine()), // y
             Double.parseDouble(br.readLine())); // scale
        tileEditor.baseTilesPanel.tile = 
          Integer.parseInt(br.readLine());
        tileEditor.compTilesPanel.tile = 
          Integer.parseInt(br.readLine());
        setTopMap(Integer.parseInt(br.readLine()));
        br.close();
      }catch(IOException e){ 
        System.err.println(e.getMessage()); 
      } catch (NumberFormatException e){
        System.err.println(e.getMessage());
      }
    }else{
      mapFilePath = "";
      initMap();
      loadTileImage("", 16, 16);
      setPreferredSize(new Dimension(1200, 800));
    }
    
    mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
    mainTabbedPane.add("File Input and Settings", fileSettings);
    mainTabbedPane.add("       Map Editor      ", mapEditor);
    mainTabbedPane.add("      Tile Editor      ", tileEditor);
    mainTabbedPane.add("  Summary of Controls  ", controls);
    mainTabbedPane.setSelectedIndex(tabIndex);
    add(mainTabbedPane);
    
    pack();
    mapEditor.scrollToSelectedTile();
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(closeOperation);
    setLocationRelativeTo(null);
    setVisible(true);

    timer = new Timer(4, this);
    timer.start();
  }
  
  @Override public void actionPerformed(ActionEvent e){
    if (firstTime){ nanoTime = System.nanoTime(); firstTime = false; }
    fileSettings.update();
    mapEditor.update();
    tileEditor.update();
    long dt = -nanoTime; nanoTime = System.nanoTime(); dt+= nanoTime;
    repaintTimer+= dt;
    if (repaintTimer >= repaintDelay){
      tileMap.update(repaintTimer);
      repaintTimer = 0;
      pulse = 255*((int)(nanoTime/4e8)%2);
      selectColor = new Color(pulse, pulse, pulse);
      solidColor = new Color(pulse, 0, 255-pulse, 128);
      repaint();
    }
  }
  
  void initMap(){
    tileMap.initMapData(DEFAULT_LAYERS);
    tileMap.initTiles();
    initBaseTilesFromImage();
    resetChangeMade();
    mapEditor.layersPanel.initLayers();
    setTopMap(0);
    fileSettings.layerField.setValue(tileMap.totalLayers());
  }
  
  final boolean loadMap(String path){
    try{
      mapFilePath = path;
      FileInputStream in = new FileInputStream(mapFilePath);
      tileMap.load(new DataInputStream(in));
      in.close();
      resetChangeMade();
      // make backup of map if loaded successfully
      FileOutputStream out = new FileOutputStream(autoBackupName);
      tileMap.save(new DataOutputStream(out));
      out.close();
      mapEditor.layersPanel.initLayers();
      fileSettings.layerField.setValue(tileMap.totalLayers());
      return true;
    }catch(IOException e){ 
      mapFilePath = "";
      initMap();
      return false;
    }
  }
  
  final void saveMap(){
    try{
      FileOutputStream out = new FileOutputStream(mapFilePath);
      tileMap.save(new DataOutputStream(out));
      out.close();
      resetChangeMade();
    }catch(IOException e){ System.err.println(e.getMessage()); }
  }
  
  void loadTileImage(String path, int w, int h){
    tileImagePath = path;
    tileW = (w < 1) ? 1 : w; tileH = (h < 1) ? 1 : h;
    fileSettings.tileWField.setValue(tileW); 
    fileSettings.tileHField.setValue(tileH);
    initBaseTilesFromImage();
    initPanels();
    mapEditor.viewPanel.rescale();
  }
  
  // load mapFilePath map or reset if unable to
  final void initBaseTilesFromImage(){
    try{
      BufferedImage tileImage = 
           javax.imageio.ImageIO.read(new File(tileImagePath)); 
      if (tileImage == null) throw new IOException();
      tileMap.initBaseTilesFromImage(tileImage, tileW, tileH);
    }catch(IOException e){
      tileImagePath = "";
      tileMap.initBaseTilesFromImage(null, tileW, tileH);
    }
  }
  
  void initPanels(){
    mapEditor.baseTilesPanel.init(tileMap.nImageTilesX(), 
                                  tileMap.nImageTilesY());
    mapEditor.compTilesPanel.init(16, 32);
    mapEditor.mapSelectPanel.updateMapList();
    mapEditor.scrollToSelectedTile();
    tileEditor.baseTilesPanel.init(tileMap.nImageTilesX(), 
                                   tileMap.nImageTilesY());
    tileEditor.compTilesPanel.init(16, 32);
    tileEditor.rightEditPanel.tile = -1;
  }
  
  boolean confirm(String message){
    return (JOptionPane.showConfirmDialog(null, message, "", 
         JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
  }
  
  void confirmRefreshImage(){
    if (confirm("Refresh image?")) initBaseTilesFromImage();
  }
  
  void confirmExit(){
    if (changeMade()){
      if (mapFilePath.equals("")){
        if (confirm("Exit without saving?")) exit();
      }else{
        int answer = 
          JOptionPane.showConfirmDialog(null, "Save to \"" + mapFilePath + 
               "\" before exiting?", "", JOptionPane.YES_NO_CANCEL_OPTION);
        if (answer == JOptionPane.YES_OPTION){
          saveMap();
          exit();
        }else if (answer == JOptionPane.NO_OPTION){
          exit();
        }
      }
    }else{ 
      exit();
    }
  }
  
  void confirmSave(){
    if (mapFilePath.equals("")) doSaveAsDialog();
    else if (confirm("Save changes to \"" + mapFilePath + "\"?")) saveMap();
  }
  
  void confirmRestore(){
    if (mapFilePath.equals("")){
      if (confirm("Reset all changes?")){ 
        initMap();
        initPanels();
      }
    }else{
      if (confirm("Restore from \"" + mapFilePath + "\"?")){
        loadMap(mapFilePath);
        initPanels();
      }
    }
  }
  
  void doSaveAsDialog(){
    String path = fileSettings.selectFile("Select name to save map file as.");
    if (path != null){ 
      File mapFile = new File(path);
      if (mapFile.exists())
        if (!confirm("Overwrite file: \"" + path + "\"?")) return;
      mapFilePath = path;
      saveMap();
    }
  }
  
  void exit(){
    try{
      BufferedWriter br = new BufferedWriter(new FileWriter(iniFileName));
      br.write(INI_TIMESTAMP + "\n");
      br.write(mapFilePath + "\n");
      br.write(tileImagePath + "\n");
      br.write(tileW + "\n");
      br.write(tileH + "\n");
      br.write(getWidth() + "\n");
      br.write(getHeight() + "\n");
      br.write(mainTabbedPane.getSelectedIndex() + "\n");
      br.write(mapEditor.mapTabbedPane.getSelectedIndex() + "\n");
      br.write(mapEditor.tilesMapSplitPane.getDividerLocation() + "\n");
      br.write(mapEditor.mainSplitPane.getDividerLocation() + "\n");
      br.write(tileEditor.splitPane.getDividerLocation() + "\n");
      br.write(mapEditor.selectionTiles[0][mapEditor.selectedLayer] + "\n");
      br.write(mapEditor.viewPanel.x0 + "\n");
      br.write(mapEditor.viewPanel.y0 + "\n");
      br.write(mapEditor.viewPanel.scale + "\n");
      br.write(tileEditor.baseTilesPanel.tile + "\n");
      br.write(tileEditor.compTilesPanel.tile + "\n");
      br.write(tileMap.currentTopMapIndex() + "\n");
      br.close();
    }catch(IOException e){ 
      System.err.println(e.getMessage()); 
    }
    System.exit(0);
  }
  
  boolean changeMade(){ 
    return fileSettings.changed || mapEditor.hist.isChanged() || 
           mapEditor.changed || tileEditor.changed; 
  }
  
  void resetChangeMade(){
    fileSettings.changed = false;
    mapEditor.hist.reset();
    mapEditor.changed = false;
    tileEditor.changed = false;
  }
  
  void setTopMap(int i){
    tileMap.setTopMap(i);
    mapEditor.mapSelectPanel.updateMapList();
  }
  
  class CloseOperation extends WindowAdapter{
    @Override public void windowClosing(WindowEvent e){
      confirmExit();
    }
  }  
}
