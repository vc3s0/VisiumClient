package dev.cweldlc.client.module.impl.render;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;

public class NoHurtCamModule extends Module {

    public NoHurtCamModule() {
        super("NoHurtCam", "Disables camera shake when taking damage", Category.RENDER);
    }
}
