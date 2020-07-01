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

public class ConfigLoadingFailedScreen extends Utils.ReturningScreen {

    private static final String I18N_KEY_BASE = "gui." + CompactOres.MODID + ".configloadfailure.";

    private final ITextComponent message;
    private final ITextComponent buttonTextQuit;
    private final ITextComponent buttonTextResetAndQuit;
    private final Button.IPressable onQuit;
    private final Button.IPressable onResetAndQuit;

    private final List<ITextProperties> wrappedMessage = new ArrayList<>();

    protected ConfigLoadingFailedScreen(String errorMessage, Button.IPressable onQuit, Button.IPressable onResetAndQuit) {
        super(new TranslationTextComponent(I18N_KEY_BASE + "title"));
        message = new TranslationTextComponent(I18N_KEY_BASE + "message", errorMessage);
        buttonTextQuit = new TranslationTextComponent(I18N_KEY_BASE + "deny");
        buttonTextResetAndQuit = new TranslationTextComponent(I18N_KEY_BASE + "confirm");
        this.onResetAndQuit = onResetAndQuit;
        this.onQuit = onQuit;
    }

    @Override
    protected void func_231160_c_() {
        super.func_231160_c_();
        wrappedMessage.clear();
        wrappedMessage.addAll(this.field_230712_o_.func_238425_b_(message, this.field_230708_k_ - 50));
        int i = (wrappedMessage.size() + 1) * 9;
        this.func_230480_a_(new Button(this.field_230708_k_ / 2 - 150, i + 100 - 30, 300, 20, buttonTextQuit, onQuit));
        this.func_230480_a_(new Button(this.field_230708_k_ / 2 - 150, i + 124 - 30, 300, 20, buttonTextResetAndQuit, onResetAndQuit));
    }

    @Override
    public void func_230430_a_(MatrixStack p_230430_1_, int p_render_1_, int p_render_2_, float p_render_3_) {
        this.func_230446_a_(p_230430_1_);
        this.func_238472_a_(p_230430_1_, this.field_230712_o_, this.field_230704_d_, this.field_230708_k_ / 2, 30, 0xFFFFFF);
        int y = 50;
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
