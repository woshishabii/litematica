package fi.dy.masa.litematica.gui;

import java.io.File;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.button.ButtonOnOff;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.data.DataDump;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetInfoIcon;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiMaterialList extends GuiListBase<MaterialListEntry, WidgetMaterialListEntry, WidgetListMaterialList>
{
    private final MaterialListBase materialList;
    private int id;

    public GuiMaterialList(MaterialListBase materialList)
    {
        super(10, 44);

        this.materialList = materialList;
        this.title = this.materialList.getTitle();
        this.useTitleHierarchy = false;

        Minecraft mc = Minecraft.getMinecraft();

        MaterialListUtils.updateAvailableCounts(this.materialList.getMaterialsAll(), mc.player);
        WidgetMaterialListEntry.setMaxNameLength(materialList.getMaterialsAll(), mc);

        // Remember the last opened material list, for the hotkey
        if (DataManager.getMaterialList() == null)
        {
            DataManager.setMaterialList(materialList);
        }
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = 24;
        int buttonWidth;
        this.id = 0;
        String label;
        ButtonGeneric button;

        String str = I18n.format("litematica.gui.label.material_list.multiplier", this.materialList.getMultiplier());
        int w = this.fontRenderer.getStringWidth(str) + 6;
        this.addLabel(this.width - w - 6, y + 4, w, 12, 0xFFFFFFFF, str);
        this.createButton(this.width - w - 26, y + 2, -1, ButtonListener.Type.CHANGE_MULTIPLIER);

        this.addWidget(new WidgetInfoIcon(this.width - 23, 12, this.zLevel, Icons.INFO_11, "litematica.info.material_list"));

        int gap = 2;
        x += this.createButton(x, y, -1, ButtonListener.Type.REFRESH_LIST) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.LIST_TYPE) + gap;
        x += this.createButtonOnOff(x, y, -1, this.materialList.getHideAvailable(), ButtonListener.Type.HIDE_AVAILABLE) + gap;
        x += this.createButtonOnOff(x, y, -1, this.materialList.getHudRenderer().getShouldRender(), ButtonListener.Type.TOGGLE_INFO_HUD) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.CLEAR_IGNORED) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.WRITE_TO_FILE) + gap;
        y += 22;

        y = this.height - 36;
        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
    }

    private int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this);
        String label = "";

        if (type == ButtonListener.Type.LIST_TYPE)
        {
            label = type.getDisplayName(this.materialList.getMaterialListType().getDisplayName());
        }
        else if (type == ButtonListener.Type.CHANGE_MULTIPLIER)
        {
            String hover = I18n.format("litematica.gui.button.hover.plus_minus_tip");
            ButtonGeneric button = new ButtonGeneric(0, x, y, Icons.BUTTON_PLUS_MINUS_16, hover);
            this.addButton(button, listener);
            return button.getButtonWidth();
        }
        else
        {
            label = type.getDisplayName();
        }

        if (width == -1)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        this.addButton(button, listener);

        return width;
    }

    private int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        ButtonOnOff button = ButtonOnOff.create(x, y, width, false, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this));
        return button.getButtonWidth();
    }

    public MaterialListBase getMaterialList()
    {
        return this.materialList;
    }

    @Override
    protected WidgetListMaterialList createListWidget(int listX, int listY)
    {
        return new WidgetListMaterialList(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiMaterialList parent;
        private final Type type;

        public ButtonListener(Type type, GuiMaterialList parent)
        {
            this.parent = parent;
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            MaterialListBase materialList = this.parent.materialList;

            switch (this.type)
            {
                case REFRESH_LIST:
                    materialList.recreateMaterialList();
                    WidgetMaterialListEntry.setMaxNameLength(materialList.getMaterialsAll(), this.parent.mc);
                    break;

                case LIST_TYPE:
                    BlockInfoListType type = materialList.getMaterialListType();
                    materialList.setMaterialListType((BlockInfoListType) type.cycle(mouseButton == 0));
                    materialList.recreateMaterialList();
                    break;

                case HIDE_AVAILABLE:
                    materialList.setHideAvailable(! materialList.getHideAvailable());
                    materialList.refreshPreFilteredList();
                    materialList.recreateFilteredList();
                    break;

                case TOGGLE_INFO_HUD:
                    MaterialListHudRenderer renderer = materialList.getHudRenderer();
                    renderer.toggleShouldRender();

                    if (materialList.getHudRenderer().getShouldRender())
                    {
                        InfoHud.getInstance().addInfoHudRenderer(renderer, true);
                    }
                    else
                    {
                        InfoHud.getInstance().removeInfoHudRenderersOfType(renderer.getClass(), true);
                    }

                    break;

                case CLEAR_IGNORED:
                    materialList.clearIgnored();
                    break;

                case WRITE_TO_FILE:
                    File dir = new File(LiteLoader.getCommonConfigFolder(), Reference.MOD_ID);
                    DataDump dump = new DataDump(2, DataDump.Format.ASCII);
                    this.addLinesToDump(dump, materialList);
                    File file = DataDump.dumpDataToFile(dir, "material_list", dump.getLines());

                    if (file != null)
                    {
                        String key = "litematica.message.material_list_written_to_file";
                        this.parent.addMessage(MessageType.SUCCESS, key, file.getName());
                        StringUtils.sendOpenFileChatMessage(this.parent.mc.player, key, file);
                    }
                    break;

                case CHANGE_MULTIPLIER:
                {
                    int amount = mouseButton == 1 ? -1 : 1;
                    if (GuiScreen.isShiftKeyDown()) { amount *= 8; }
                    if (GuiScreen.isAltKeyDown()) { amount *= 4; }
                    materialList.setMultiplier(materialList.getMultiplier() + amount);
                    break;
                }
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        private void addLinesToDump(DataDump dump, MaterialListBase materialList)
        {
            int multiplier = materialList.getMultiplier();

            for (MaterialListEntry entry : materialList.getMaterialsAll())
            {
                int count = entry.getCountTotal() * multiplier;
                dump.addData(entry.getStack().getDisplayName(), String.valueOf(count));
            }

            String titleTotal = multiplier > 1 ? String.format("Total (x%d)", multiplier) : "Total";
            dump.addTitle("Item", titleTotal);
            dump.addHeader(materialList.getTitle());
            dump.setColumnProperties(1, DataDump.Alignment.RIGHT, true); // total
            dump.setSort(true);
            dump.setUseColumnSeparator(true);
        }

        public enum Type
        {
            REFRESH_LIST        ("litematica.gui.button.material_list.refresh_list"),
            LIST_TYPE           ("litematica.gui.button.material_list.list_type"),
            HIDE_AVAILABLE      ("litematica.gui.button.material_list.hide_available"),
            TOGGLE_INFO_HUD     ("litematica.gui.button.material_list.toggle_info_hud"),
            CLEAR_IGNORED       ("litematica.gui.button.material_list.clear_ignored"),
            WRITE_TO_FILE       ("litematica.gui.button.material_list.write_to_file"),
            CHANGE_MULTIPLIER   ("");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            public String getDisplayName(Object... args)
            {
                return I18n.format(this.translationKey, args);
            }
        }
    }
}
