module Pico8MapLoaderCustomPicoConsole

using ..Ahorn, Maple

@mapdef Entity "Pico8MapLoader/CustomPicoConsole" CustomPicoConsole(x::Integer, y::Integer, mapToLoad::String="")

const placements = Ahorn.PlacementDict(
    "Pico Console (Custom Map) (PICO-8 Map Loader)" => Ahorn.EntityPlacement(
        CustomPicoConsole
    )
)

sprite = "objects/pico8Console"

function Ahorn.selection(entity::CustomPicoConsole)
    x, y = Ahorn.position(entity)

    return Ahorn.getSpriteRectangle(sprite, x, y, jx=0.5, jy=1.0)
end

Ahorn.render(ctx::Ahorn.Cairo.CairoContext, entity::CustomPicoConsole, room::Maple.Room) = Ahorn.drawSprite(ctx, sprite, 0, 0, jx=0.5, jy=1.0)

end
