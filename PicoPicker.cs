using Celeste.Mod.UI;
using Celeste.Pico8;
using Monocle;
using System.Collections.Generic;

namespace Celeste.Mod.Pico8MapLoader {
    public class PicoPicker : OuiGenericMenu {
        public override string MenuName => Dialog.Clean("MENU_PICO8");

        protected override void addOptionsToMenu(TextMenu menu) {
            menu.Add(new TextMenu.SubHeader(Dialog.Clean("pico8maploader_pickamap")));
            menu.Add(new TextMenu.Button(Dialog.Clean("pico8maploader_vanilla")).Pressed(() => {
                Pico8MapLoaderModule.mapToLoad = null;
                goToPico8();
            }));

            List<string> sortedMaps = new List<string>(Pico8MapLoaderModule.maps);
            sortedMaps.Sort();

            foreach (string map in sortedMaps) {
                menu.Add(new TextMenu.Button(Dialog.Clean("pico8map_" + map)).Pressed(() => {
                    Pico8MapLoaderModule.mapToLoad = map;
                    goToPico8();
                }));
            }
        }

        private void goToPico8() {
            Focused = false;
            new FadeWipe(Scene, wipeIn: false, delegate {
                Focused = true;
                Overworld.EnteringPico8 = true;
                SaveData.Instance = null;
                SaveData.NoFileAssistChecks();
                Engine.Scene = new Emulator(Overworld);
            });
        }
    }
}
