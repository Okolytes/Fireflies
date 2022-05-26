package fireflies.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import fireflies.Fireflies;
import fireflies.entity.FireflyEntity;
import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

public final class DebugStuff {
    public static boolean debugScreenOpen() {
        return Objects.equals(Minecraft.getInstance().currentScreen, DebugScreen.getInstance());
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT, modid = Fireflies.MODID)
    public static class DebugScreen extends Screen { // https://github.com/codingminecraft/IntegratingImGui
        private static final ImGuiImplGlfw IMGUI_GLFW = new ImGuiImplGlfw();
        private static final ImGuiImplGl3 IMGUI_GL3 = new ImGuiImplGl3();
        private static DebugScreen instance;
        @Nullable
        public static FireflyEntity selectedFirefly;

        private DebugScreen() {
            super(StringTextComponent.EMPTY);
            ImGui.createContext();
            IMGUI_GLFW.init(Minecraft.getInstance().getMainWindow().getHandle(), false);
            IMGUI_GL3.init();
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.KeyInputEvent event) {
            if (event.getKey() == GLFW.GLFW_KEY_RIGHT_CONTROL && event.getAction() == GLFW.GLFW_PRESS) {
                final Minecraft mc = Minecraft.getInstance();
                if (mc.world != null) {
                    mc.displayGuiScreen(debugScreenOpen() ? null : getInstance());
                }
            }
        }

        @Override
        public void render(MatrixStack pMatrixStack, int pMouseX, int pMouseY, float pPartialTicks) {
            if (this.minecraft == null || this.minecraft.world == null) return;
            try {
                IMGUI_GLFW.newFrame();
                ImGui.newFrame();
                ImGui.styleColorsClassic();
                ImGui.showDemoWindow();
                ImGui.begin("Fireflies debug screen");

                ImGui.beginGroup();
                ImGui.beginChild("Fireflies selection", 200, 200, true, ImGuiWindowFlags.HorizontalScrollbar);
                for (Entity entity : this.minecraft.world.getAllEntities()) {
                    if (!(entity instanceof FireflyEntity)) continue;
                    final FireflyEntity firefly = (FireflyEntity) entity;

                    final boolean flag = firefly.equals(selectedFirefly);
                    if (ImGui.selectable(firefly.getName().getString() + " - " + firefly.getPosition().getCoordinatesAsString(), flag)) {
                        selectedFirefly = flag ? null : firefly;
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(firefly.toString());
                    }
                }
                ImGui.endChild();


                ImGui.text("WANTS_IN: " + FireflyAbdomenAnimationManager.WANTS_IN.size());
                ImGui.text("WANTS_OUT: " + FireflyAbdomenAnimationManager.WANTS_OUT.size());
                ImGui.text("ANIMATIONS");
                ImGui.indent();
                for (FireflyAbdomenAnimation animation : FireflyAbdomenAnimationManager.ANIMATIONS) {
                    ImGui.text(animation.name);
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip(animation.toString());
                    }
                }
                ImGui.unindent();
                ImGui.endGroup();


                ImGui.sameLine();

                ImGui.beginChild("Selected firefly", 0, 0, true);

                if (selectedFirefly != null) {
                    // uhghghghghghgh java please give me nameof()
                    textValue("rainedOnTicks", selectedFirefly.rainedOnTicks);
                    textValue("underWaterTicks", selectedFirefly.underWaterTicks);

                    if (ImGui.checkbox("hasIllumerin", selectedFirefly.hasIllumerin())) {
                        selectedFirefly.setHasIllumerin(!selectedFirefly.hasIllumerin());
                    }

                    ImGui.separator();

                    textValue("abdomenParticle", selectedFirefly.particleManager.abdomenParticle);
                    textValue("abdomenParticlePositionOffset", selectedFirefly.particleManager.abdomenParticlePositionOffset);
                    textValue("getAbdomenParticlePos", Arrays.toString(selectedFirefly.particleManager.getAbdomenParticlePos()));
                    if (ImGui.button("spawnDustParticle()")) {
                        selectedFirefly.particleManager.spawnDustParticle();
                    }

                    ImGui.separator();

                    textValue("curAnimation", selectedFirefly.animationManager.curAnimation);
                    textValue("prevAnimation", selectedFirefly.animationManager.prevAnimation);

                    if (ImGui.treeNode("setAnimation()")) {
                        for (FireflyAbdomenAnimation animation : FireflyAbdomenAnimationManager.ANIMATIONS) {
                        final boolean flag = Objects.equals(animation.name, selectedFirefly.animationManager.curAnimation);
                            if (ImGui.selectable(animation.name, flag)) {
                                selectedFirefly.animationManager.setAnimation(flag ? null : animation.name);
                            }
                        }
                        ImGui.treePop();
                    }

                    textValue("animationProperties", selectedFirefly.animationManager.animationProperties);
                    if (ImGui.button("resetAnimationProperties()")) {
                        selectedFirefly.animationManager.resetAnimationProperties();
                    }
                }

                ImGui.endChild();

                ImDrawList drawList = ImGui.getForegroundDrawList();
                drawList.addRectFilled(20, 20, 80, 80,
                        ImColor.intToColor(50, 50, 50, 255));

                ImGui.end();
                ImGui.render();
                IMGUI_GL3.renderDrawData(ImGui.getDrawData());
            } catch (Exception e) {
                e.printStackTrace(); //todo remove
            }
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        public static DebugScreen getInstance() {
            if (instance == null) instance = new DebugScreen();
            return instance;
        }

        private static void textValue(String s, @Nullable Object o) {
            ImGui.text(s + ":");
            ImGui.sameLine(250);
            ImGui.text(o == null ? "null" : o.toString());
        }
    }
}
