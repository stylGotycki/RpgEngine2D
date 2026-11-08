<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10"
         tiledversion="1.11.2"
         name="interior-placeholder"
         tilewidth="16"
         tileheight="16"
         tilecount="23"
         columns="8">

 <properties>
  <property name="theme" value="placeholder"/>
 </properties>

 <image source="interior-placeholder.png" width="128" height="48"/>

  <tile id="0">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,nature,grass"/>
    <property name="tileId" value="floor.grass"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="4"/>
   </properties>
  </tile>

  <tile id="1">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,nature,dirt,path"/>
    <property name="tileId" value="floor.dirt"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="2.5"/>
   </properties>
  </tile>

  <tile id="2">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,nature,sand,coast"/>
    <property name="tileId" value="floor.sand"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.4"/>
   </properties>
  </tile>

  <tile id="3">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="PIT"/>
    <property name="tags" value="ground,nature,water,obstacle"/>
    <property name="tileId" value="pit.water"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="1.2"/>
   </properties>
  </tile>

  <tile id="4">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,ruins,stone"/>
    <property name="tileId" value="floor.stone"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="3"/>
   </properties>
  </tile>

  <tile id="5">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,ruins,stone,cracked"/>
    <property name="tileId" value="floor.stone_cracked"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.8"/>
   </properties>
  </tile>

  <tile id="6">
   <properties>
    <property name="blocksSight" type="bool" value="true"/>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="WALL"/>
    <property name="tags" value="boundary,ruins,stone"/>
    <property name="tileId" value="wall.stone"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="2.5"/>
   </properties>
  </tile>

  <tile id="7">
   <properties>
    <property name="blocksSight" type="bool" value="true"/>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="WALL"/>
    <property name="tags" value="boundary,ruins,stone,moss"/>
    <property name="tileId" value="wall.mossy"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.7"/>
   </properties>
  </tile>

  <tile id="8">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,bridge,connector,horizontal"/>
    <property name="tileId" value="floor.bridge_horizontal"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.35"/>
   </properties>
  </tile>

  <tile id="9">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,bridge,connector,vertical"/>
    <property name="tileId" value="floor.bridge_vertical"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.35"/>
   </properties>
  </tile>

  <tile id="10">
   <properties>
    <property name="damage" type="int" value="1"/>
    <property name="generationLayer" value="details"/>
    <property name="role" value="HAZARD"/>
    <property name="tags" value="detail,hazard,trap,spikes"/>
    <property name="tileId" value="hazard.spikes"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.25"/>
   </properties>
  </tile>

  <tile id="11">
   <properties>
    <property name="blocksSight" type="bool" value="true"/>
    <property name="generationLayer" value="details"/>
    <property name="role" value="OBSTACLE"/>
    <property name="tags" value="detail,obstacle,rubble"/>
    <property name="tileId" value="obstacle.rubble"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.55"/>
   </properties>
  </tile>

  <tile id="12">
   <properties>
    <property name="generationLayer" value="details"/>
    <property name="role" value="DECORATION"/>
    <property name="tags" value="detail,decoration,nature,flowers"/>
    <property name="tileId" value="decoration.flowers"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.6"/>
   </properties>
  </tile>

  <tile id="13">
   <properties>
    <property name="generationLayer" value="details"/>
    <property name="role" value="DECORATION"/>
    <property name="tags" value="detail,decoration,bones"/>
    <property name="tileId" value="decoration.bones"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.35"/>
   </properties>
  </tile>

  <tile id="14">
   <properties>
    <property name="generationLayer" value="details"/>
    <property name="role" value="DECORATION"/>
    <property name="tags" value="detail,decoration,light,torch"/>
    <property name="tileId" value="decoration.torch"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.25"/>
   </properties>
  </tile>

  <tile id="15">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="DOOR"/>
    <property name="tags" value="boundary,door,connector"/>
    <property name="tileId" value="door.wood"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.12"/>
   </properties>
  </tile>

  <tile id="16">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="DOOR"/>
    <property name="tags" value="boundary,door,connector,locked"/>
    <property name="tileId" value="door.locked"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.12"/>
   </properties>
  </tile>

  <tile id="17">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="OBSTACLE"/>
    <property name="tags" value="furniture,shop,counter"/>
    <property name="tileId" value="furniture.counter"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>

  <tile id="18">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="OBSTACLE"/>
    <property name="tags" value="feature,power,core"/>
    <property name="tileId" value="feature.power_core"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>

  <tile id="19">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,power,rune"/>
    <property name="tileId" value="floor.power_rune"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>

  <tile id="20">
   <properties>
    <property name="blocksSight" type="bool" value="true"/>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="OBSTACLE"/>
    <property name="tags" value="structure,building,roof"/>
    <property name="tileId" value="structure.roof"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>

  <tile id="21">
   <properties>
    <property name="blocksSight" type="bool" value="true"/>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="WALL"/>
    <property name="tags" value="structure,building,wall"/>
    <property name="tileId" value="structure.hut_wall"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>

  <tile id="22">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="OBSTACLE"/>
    <property name="tags" value="structure,building,door"/>
    <property name="tileId" value="structure.hut_door"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>
</tileset>
