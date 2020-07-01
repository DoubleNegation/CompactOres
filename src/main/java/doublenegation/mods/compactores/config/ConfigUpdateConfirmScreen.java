package doublenegation.mods.compactores.config;

import com.mojang.blaze3d.matrix.MatrixStack;
import doublenegation.mods.compactores.CompactOres;
import doublenegation.mods.compactores.Utils;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;

public class ConfigUpdateConfirmScreen extends Utils.ReturningScreen {

    private static final String I18N_KEY_BASE = "gui." + CompactOres.MODID + ".configupdate.";

    private final ITextComponent message;
    private final ITextComponent buttonTextYes;
    private final ITextComponent buttonTextNo;
    private final Button.IPressable onConfirm;
    private final Button.IPressable onDeny;

    private final List<ITextProperties> wrappedMessage = new ArrayList<>();

    protected ConfigUpdateConfirmScreen(String configCreateVer, String currentVer, Button.IPressable onConfirm, Button.IPressable onDeny) {
        super(new TranslationTextComponent(I18N_KEY_BASE + "title"));
        message = new TranslationTextComponent(I18N_KEY_BASE + "message", currentVer, configCreateVer);
        buttonTextYes = new TranslationTextComponent(I18N_KEY_BASE + "confirm");
        buttonTextNo = new TranslationTextComponent(I18N_KEY_BASE + "deny");
        this.onConfirm = onConfirm;
        this.onDeny = onDeny;
    }

    @Override
    protected void func_231160_c_() {
        super.func_231160_c_();
        wrappedMessage.clear();
        wrappedMessage.addAll(this.field_230712_o_.func_238425_b_(message, this.field_230708_k_ - 50));
        int i = (wrappedMessage.size() + 1) * 9 - 80;
        this.func_230480_a_(new Button(this.field_230708_k_ / 2 - 150, i + 100, 300, 20, buttonTextYes, onConfirm));
        this.func_230480_a_(new Button(this.field_230708_k_ / 2 - 150, i + 124, 300, 20, buttonTextNo, onDeny));
    }

    @Override
    public void func_230430_a_(MatrixStack p_230430_1_, int p_render_1_, int p_render_2_, float p_render_3_) {
        this.func_230446_a_(p_230430_1_);
        this.func_238472_a_(p_230430_1_, this.field_230712_o_, this.field_230704_d_, this.field_230708_k_ / 2, 9, 0xFFFFFF);
        int y = 3 * 9;
        for(ITextProperties s : wrappedMessage) {
            this.func_238472_a_(p_230430_1_, this.field_230712_o_, s, this.field_230708_k_ / 2, y, 0xFFFFFF);
            y += 9;
        }
        super.func_230430_a_(p_230430_1_, p_render_1_, p_render_2_, p_render_3_);
    }

    @Override
    public boolean func_231178_ax__() {
        return false;
    }

}
