package doublenegation.mods.compactores;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IBidiRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class SimpleChoiceMessageScreen extends Utils.ReturningScreen {

    private final ITextComponent message;
    private final ITextComponent buttonTextFirst;
    private final ITextComponent buttonTextSecond;
    private final Button.IPressable onFirstOption;
    private final Button.IPressable onSecondOption;

    private IBidiRenderer textRenderer = IBidiRenderer.field_243257_a;

    public SimpleChoiceMessageScreen(String i18nBase, Button.IPressable onFirstOption, Button.IPressable onSecondOption, Object... messageParams) {
        super(new TranslationTextComponent(i18nBase + ".title"));
        message = new TranslationTextComponent(i18nBase + ".message", messageParams);
        buttonTextFirst = new TranslationTextComponent(i18nBase + ".option1");
        buttonTextSecond = new TranslationTextComponent(i18nBase + ".option2");
        this.onFirstOption = onFirstOption;
        this.onSecondOption = onSecondOption;
    }

    @Override
    protected void init() {
        super.init();
        textRenderer = IBidiRenderer.func_243258_a(this.font, message, this.width - 50);
        int i = 50 + (textRenderer.func_241862_a() + 1) * 9;
        this.addButton(new Button(this.width / 2 - 150, i + 5, 300, 20, buttonTextFirst, onFirstOption));
        this.addButton(new Button(this.width / 2 - 150, i + 30, 300, 20, buttonTextSecond, onSecondOption));
    }

    @Override
    public void render(MatrixStack p_230430_1_, int p_render_1_, int p_render_2_, float p_render_3_) {
        this.renderBackground(p_230430_1_);
        drawCenteredString(p_230430_1_, this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        textRenderer.func_241863_a(p_230430_1_, this.width / 2, 50);
        super.render(p_230430_1_, p_render_1_, p_render_2_, p_render_3_);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
