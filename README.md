# PICO-8 Map Loader & Editor

This is a mod providing a _very_ basic editor for PICO-8 Celeste, along with a mod to load the map in-game without straight-up replacing the vanilla one, like putting a `Pico8Tilemap.txt` at the root of your mod would do. (Yes, this is a thing built in Everest. :p)

## Opening the map editor

In order to use the map editor, you must have Java 8 or more. You can download Java [here](https://jdk.java.net/) and extract it wherever you want, or use brew/apt/etc if you're on Mac or Linux.

Then, you should run the following command:
```sh
path/to/javaw.exe -jar path/to/celeste/Mods/Pico8MapLoaderEditor.zip
```

Hint: you can create a shortcut to `java.exe` in order to make running this command easier, and you can do Shift+Right Click => Copy as path in order to copy the full path to `Pico8MapLoaderEditor.zip`.

The editor will create a `picomapeditor-settings.txt` file in the working directory ("startup directory" in shortcut properties) to keep track of the last open map and graphics. Any changes to the map are instantly saved.

## Mod Structure

A PICO-8 map boils down to ... two files:
- `Pico8Maps/yourname_foldername/tilemap.txt`: the map itself. This is where you should save your map to when messing with it in the editor.
- `Pico8Maps/yourname_foldername/atlas.png`: the graphics for the map. The vanilla ones can be found at `Graphics/Atlases/Gameplay/pico8/atlas.png` in the graphics dump.

If you want to make a map with vanilla graphics, or just a skin mod, you can omit either of those files.

In order to give your map/skin a name, include this in your English.txt file (assuming your map/skin is in `Pico8Maps/yourname_foldername`):
```
pico8map_yourname_foldername= My Epic Map/Skin
```

## Releasing a map/skin

In order to release your map/skin, you should make an `everest.yaml` as any other Celeste mod. Make sure to require this mod as a dependency:
```yaml
- Name: MyPico8Map
  Version: 1.0.0
  Dependencies:
    - Name: Pico8MapLoaderEditor
      Version: 1.0.0
```

Also, like main game mods, make sure to zip the contents of the folder, not the folder itself!