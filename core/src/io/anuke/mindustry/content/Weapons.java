package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.ShootFx;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.type.Weapon;

public class Weapons implements ContentList {
    public static Weapon blaster, blaster2, blaster3, blaster4;

    @Override
    public void load() {

        blaster = new Weapon("blaster") {{
            length = 1.5f;
            reload = 15f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletLead);
        }};

        blaster2 = new Weapon("clustergun") {{
            length = 1.5f;
            reload = 13f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletLead);
        }};

        blaster3 = new Weapon("shockgun") {{
            length = 1.5f;
            reload = 12f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletCarbide);
        }};

        blaster4 = new Weapon("vulcan") {{
            length = 1.5f;
            reload = 10f;
            roundrobin = true;
            ejectEffect = ShootFx.shellEjectSmall;
            setAmmo(AmmoTypes.bulletThermite);
        }};
    }

    @Override
    public Array<? extends Content> getAll() {
        return Upgrade.all();
    }
}
