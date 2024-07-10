using Celeste.Mod.Entities;
using Microsoft.Xna.Framework;
using System.Collections;

namespace Celeste.Mod.Pico8MapLoader {
    [CustomEntity("Pico8MapLoader/CustomPicoConsole")]
    public class CustomPicoConsole : PicoConsole {
        private readonly string mapToLoad;

        public CustomPicoConsole(EntityData data, Vector2 offset) : base(data, offset) {
            mapToLoad = data.Attr("mapToLoad");
        }

        internal static IEnumerator hookInteractRoutine(On.Celeste.PicoConsole.orig_InteractRoutine orig, PicoConsole self, Player player) {
            Pico8MapLoaderModule.mapToLoad = (self is CustomPicoConsole customPicoConsole ? customPicoConsole.mapToLoad : null);
            return orig(self, player);
        }
    }
}
