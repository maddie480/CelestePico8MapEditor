local picoConsole = {}

picoConsole.name = "Pico8MapLoader/CustomPicoConsole"
picoConsole.depth = 1000
picoConsole.justification = {0.5, 1.0}
picoConsole.texture = "objects/pico8Console"
picoConsole.placements = {
    name = "pico_console",
    data = {
        mapToLoad = ""
    }
}

return picoConsole