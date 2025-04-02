package teachingtutorials.guis;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import teachingtutorials.tutorialplaythrough.TutorialPlaythrough;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * Represents the Navigation Menu provided to players who are playing a Tutorial to assist with navigation.
 */
public class TutorialNavigationMenu extends Gui
{
    /**
     * A reference to the Tutorial playthrough for which this menu is for
     */
    private final TutorialPlaythrough tutorialPlaythrough;

    public TutorialNavigationMenu(TutorialPlaythrough tutorialPlaythrough)
    {
        super(3*9, Display.colouredText("Tutorial Navigation Menu", NamedTextColor.AQUA));

        this.tutorialPlaythrough = tutorialPlaythrough;
    }

    private void addItems()
    {
        //Stage back
        if (tutorialPlaythrough.canMoveBackStage())
            super.setItem(0, Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2", Material.ACACIA_BOAT,
                    1, TutorialGUIUtils.optionTitle("Stage Back"), TutorialGUIUtils.optionLore("Go back a stage")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            tutorialPlaythrough.previousStage();
                        }
                    });

        //Stage forwards
        if (tutorialPlaythrough.canMoveForwardsStage())
            super.setItem(8, Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44", Material.ACACIA_BOAT,
                            1, TutorialGUIUtils.optionTitle("Stage Forwards"), TutorialGUIUtils.optionLore("Skip to next stage")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            tutorialPlaythrough.skipStage();
                        }
                    });

        //Step back
        if (tutorialPlaythrough.canMoveBackStep())
            super.setItem(2, Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2", Material.ACACIA_BOAT,
                            1, TutorialGUIUtils.optionTitle("Step Back"), TutorialGUIUtils.optionLore("Go back a step")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            tutorialPlaythrough.previousStep();
                        }
                    });

        //Stage forwards
        if (tutorialPlaythrough.canMoveForwardsStep())
            super.setItem(6, Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44", Material.ACACIA_BOAT,
                            1, TutorialGUIUtils.optionTitle("Step Forwards"), TutorialGUIUtils.optionLore("Skip to next step")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            tutorialPlaythrough.skipStep();
                        }
                    });

        //Spawn
        super.setItem(20, Utils.createItem(Material.HORSE_SPAWN_EGG, 1, TutorialGUIUtils.optionTitle("TP to start of step")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        tutorialPlaythrough.tpToStepStart();
                    }
                });

        //Exit
        super.setItem(22, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Pause and Save"), TutorialGUIUtils.optionLore("Back to lobby")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        tutorialPlaythrough.terminateEarly();
                    }
                });

        //Video
        if (tutorialPlaythrough.currentStepHasVideoLink())
        {
            super.setItem(24, Utils.createItem(Material.PAINTING, 1, TutorialGUIUtils.optionTitle("Video Link")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            tutorialPlaythrough.callVideoLink();
                        }
                    });
        }
    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    @Override
    public void refresh()
    {
        this.clearGui();

        this.addItems();
    }
}
