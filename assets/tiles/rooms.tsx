<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10"
         tiledversion="1.11.2"
         name="rooms"
         tilewidth="16"
         tileheight="16"
         tilecount="16"
         columns="4">

 <properties>
  <property name="theme" value="room_debug"/>
 </properties>

 <image source="rooms.png" width="64" height="64"/>

  <tile id="0">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,start"/>
    <property name="tileId" value="floor.start"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="1">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,normal"/>
    <property name="tileId" value="floor.normal"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="4.0"/>
   </properties>
  </tile>
  <tile id="2">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,puzzle"/>
    <property name="tileId" value="floor.puzzle"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="3">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,empty"/>
    <property name="tileId" value="floor.empty"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="4">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,collectible"/>
    <property name="tileId" value="floor.collectible"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="5">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,power"/>
    <property name="tileId" value="floor.power"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="6">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,shop"/>
    <property name="tileId" value="floor.shop"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="7">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,miniboss"/>
    <property name="tileId" value="floor.miniboss"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="8">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,boss"/>
    <property name="tileId" value="floor.boss"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="9">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="FLOOR"/>
    <property name="tags" value="ground,room,vault"/>
    <property name="tileId" value="floor.vault"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="1.0"/>
   </properties>
  </tile>
  <tile id="10">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="WALL"/>
    <property name="tags" value="boundary,wall"/>
    <property name="tileId" value="wall.stone"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="3.0"/>
   </properties>
  </tile>
  <tile id="11">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="WALL"/>
    <property name="tags" value="boundary,wall,worn"/>
    <property name="tileId" value="wall.mossy"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.8"/>
   </properties>
  </tile>
  <tile id="12">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="DOOR"/>
    <property name="tags" value="connector,door"/>
    <property name="tileId" value="door.wood"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>
  <tile id="13">
   <properties>
    <property name="generationLayer" value="ground"/>
    <property name="role" value="DOOR"/>
    <property name="tags" value="connector,door,locked"/>
    <property name="tileId" value="door.locked"/>
    <property name="walkable" type="bool" value="false"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>
  <tile id="14">
   <properties>
    <property name="generationLayer" value="details"/>
    <property name="role" value="DECORATION"/>
    <property name="tags" value="detail,marker,walker"/>
    <property name="tileId" value="marker.walker"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>
  <tile id="15">
   <properties>
    <property name="generationLayer" value="details"/>
    <property name="role" value="DECORATION"/>
    <property name="tags" value="detail,marker,quest"/>
    <property name="tileId" value="marker.quest"/>
    <property name="walkable" type="bool" value="true"/>
    <property name="wfcWeight" type="float" value="0.1"/>
   </properties>
  </tile>
</tileset>
