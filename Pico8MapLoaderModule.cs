using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Text.RegularExpressions;
using Celeste.Mod.UI;
using Celeste.Pico8;
using Microsoft.Xna.Framework;
using Mono.Cecil.Cil;
using Monocle;
using MonoMod.Cil;
using MonoMod.RuntimeDetour;
using MonoMod.Utils;

namespace Celeste.Mod.Pico8MapLoader {
    public class Pico8MapLoaderModule : EverestModule {
        internal static string mapToLoad = null;
        internal static HashSet<string> maps = new HashSet<string>();

        private Regex pico8OptionRegex = new Regex("^Pico8Maps/([^/]+)/(?:tilemap|atlas)$");

        private static ILHook hookOrigCtor;
        private static MTexture loadedAtlas;

        public override void Load() {
            Everest.Content.OnUpdate += onModAssetUpdate;
            On.Celeste.OuiMainMenu.OnPico8 += onPico8InMainMenu;
            IL.Celeste.Pico8.Emulator.ctor += hookEmulatorConstructor;

            hookOrigCtor = new ILHook(typeof(Emulator).GetMethod("orig_ctor"), hookEmulatorOrigConstructor);
        }

        public override void Unload() {
            Everest.Content.OnUpdate -= onModAssetUpdate;
            On.Celeste.OuiMainMenu.OnPico8 -= onPico8InMainMenu;
            IL.Celeste.Pico8.Emulator.ctor -= hookEmulatorConstructor;

            hookOrigCtor?.Dispose();
            hookOrigCtor = null;
        }

        public override void Initialize() {
            base.Initialize();

            foreach (ModAsset asset in Everest.Content.Map.Values) {
                assetAdded(asset);
            }
        }

        private void onModAssetUpdate(ModAsset oldAsset, ModAsset newAsset) {
            if (newAsset != null) {
                assetAdded(newAsset);

                if (Engine.Scene is Emulator pico8 && mapToLoad != null && newAsset.PathVirtual == "Pico8Maps/" + mapToLoad + "/tilemap") {
                    // hot reload!
                    string input = Encoding.UTF8.GetString(newAsset.Data);
                    byte[] tilemap = new byte[input.Length / 2];
                    int length = input.Length;
                    for (int index = 0; index < length; index += 2) {
                        char c1 = input[index];
                        char c2 = input[index + 1];
                        string tile = (index < length / 2) ? (c1.ToString() + c2) : (c2.ToString() + c1);
                        tilemap[index / 2] = (byte) int.Parse(tile, NumberStyles.HexNumber);
                    }
                    new DynData<Emulator>(pico8)["tilemap"] = tilemap;
                }
            }
        }

        private void assetAdded(ModAsset asset) {
            Match match = pico8OptionRegex.Match(asset.PathVirtual);
            if (match.Success) {
                maps.Add(match.Groups[1].Value);
            }
        }

        private void onPico8InMainMenu(On.Celeste.OuiMainMenu.orig_OnPico8 orig, OuiMainMenu self) {
            Audio.Play("event:/ui/main/button_select");
            OuiGenericMenu.Goto<PicoPicker>(overworld => overworld.Goto<OuiMainMenu>(), new object[0]);
        }

        private void hookEmulatorConstructor(ILContext il) {
            ILCursor cursor = new ILCursor(il);

            // fun fact: Everest supports replacing the PICO-8 tilemap by putting a Pico8Tilemap.txt file at the root of your mod.
            // This was added in Everest 611, before I even joined the community, and no-one ever used it as far as I'm aware.
            while (cursor.TryGotoNext(MoveType.After, instr => instr.MatchLdstr("Pico8Tilemap"))) {
                Logger.Log(LogLevel.Verbose, "Pico8MapLoader", $"Hooking tilemap path at " + cursor.Index + " in IL for Emulator constructor");
                cursor.EmitDelegate<Func<string, string>>(orig => mapToLoad != null ? "Pico8Maps/" + mapToLoad + "/tilemap" : orig);
            }
        }

        private void hookEmulatorOrigConstructor(ILContext il) {
            ILCursor cursor = new ILCursor(il);

            // This, however, is not an Everest feature.
            while (cursor.TryGotoNext(MoveType.After, instr => instr.MatchLdstr("pico8/atlas"), instr => instr.MatchCallvirt<Atlas>("get_Item"))) {
                Logger.Log(LogLevel.Verbose, "Pico8MapLoader", $"Hooking atlas at " + cursor.Index + " in IL for Emulator orig_constructor");
                cursor.EmitDelegate<Func<MTexture, MTexture>>(orig => {
                    // unload the previous atlas, if any
                    loadedAtlas?.Unload();
                    loadedAtlas = null;

                    if (mapToLoad != null && Everest.Content.Map.TryGetValue("Pico8Maps/" + mapToLoad + "/atlas", out ModAsset atlas)) {
                        // the "atlas" is, uh, a single texture actually
                        loadedAtlas = new MTexture(VirtualContent.CreateTexture(atlas));
                        return loadedAtlas;
                    }

                    return orig;
                });
            }
        }
    }
}
