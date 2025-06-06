package teachingtutorials.newtutorial;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.Gui;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.listeners.texteditorbooks.BookCloseAction;
import teachingtutorials.listeners.texteditorbooks.TextEditorBookListener;
import teachingtutorials.tutorialobjects.Task;
import teachingtutorials.tutorialobjects.CommandActionType;
import teachingtutorials.tutorialplaythrough.FundamentalTaskType;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

/**
 * A menu to edit a task of a tutorial
 */
public class TaskMenu extends Gui
{
    /** The parent group menu */
    final GroupMenu groupMenu;

    /** A reference to the creator */
    final User creator;

    /** A reference to the task which this Task Menu is editing */
    private final Task task;

    /** A reference to the list of tasks which this task is a part of */
    private final ArrayList<Task> tasks;

    /** A book for setting the 'perfect' accuracy for a tpll task */
    private final TextEditorBookListener perfectTpllAccuracy;

    /** A book for setting the minimal acceptable distance for a tpll task */
    private final TextEditorBookListener acceptableTpllAccuracy;

    public TaskMenu(GroupMenu groupMenu, Task task, ArrayList<Task> tasks)
    {
        super(45, TutorialGUIUtils.inventoryTitle("Task Menu"));
        this.groupMenu = groupMenu;
        this.creator = groupMenu.creator;
        this.task = task;
        this.tasks = tasks;

        TeachingTutorials plugin = groupMenu.stepMenu.stageMenu.tutorialMenu.tutorialCreationSession.plugin;

        perfectTpllAccuracy = new TextEditorBookListener(plugin,
                creator, this, "Perfect Tpll Distance", new BookCloseAction() {
            @Override
            public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                //Unregister the book
                textEditorBookListener.unregister();

                //Always remove the book
                creator.player.getInventory().getItemInMainHand().setAmount(0);

                try
                {
                    float perfectDistance = Float.parseFloat(szNewContent);
                    if (perfectDistance < 0)
                    {
                        creator.player.sendMessage(Display.errorText("The perfect distance cannot be negative!"));
                        return false;
                    }
                    else if (perfectDistance > task.getAcceptableDistance())
                    {
                        creator.player.sendMessage(Display.errorText("The perfect distance cannot be more than the acceptable distance"));
                        return false;
                    }
                    task.setPerfectDistance(perfectDistance);
                    return true;

                }
                catch (NumberFormatException e)
                {
                    creator.player.sendMessage(Display.errorText("The perfect distance must be a number"));
                    return false;
                }
            }

            @Override
            public void runPostClose() {
                //Refresh the menu to update the lore of the items in the menu
                refresh();
                open(creator);
            }
        }, task.getPerfectDistance()+"");


        acceptableTpllAccuracy = new TextEditorBookListener(plugin,
                creator, this, "Acceptable Tpll Distance", new BookCloseAction() {
            @Override
            public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                //Unregister the book
                textEditorBookListener.unregister();

                //Always remove the book
                creator.player.getInventory().getItemInMainHand().setAmount(0);

                try
                {
                    float acceptableDistance = Float.parseFloat(szNewContent);
                    if (acceptableDistance < 0)
                    {
                        creator.player.sendMessage(Display.errorText("The acceptable distance cannot be negative!"));
                        return false;
                    }
                    else if (acceptableDistance < task.getAcceptableDistance())
                    {
                        creator.player.sendMessage(Display.errorText("The acceptable distance cannot be less than the perfect distance"));
                        return false;
                    }
                    task.setAcceptableDistance(acceptableDistance);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    creator.player.sendMessage(Display.errorText("The acceptable distance must be a number"));
                    return false;
                }
            }

            @Override
            public void runPostClose() {
                //Refresh the menu to update the lore of the items in the menu
                refresh();
                open(creator);
            }
        }, task.getAcceptableDistance()+"");

        addItems();
    }

    private void addItems()
    {
        //Delete task
        setItem(36, Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("Delete Task")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }

                    @Override
                    public void leftClick(User u) {
                        //Create new cancel confirmation menu
                        DeleteConfirmation deleteConfirmation = new DeleteConfirmation(TaskMenu.this, () -> {
                            //Removes the task
                            tasks.remove(task);

                            //Adjust the orders
                            for (int i = 0 ; i < tasks.size() ; i++)
                            {
                                tasks.get(i).setOrder(i+1);
                            }

                            //Refreshes the group menu
                            groupMenu.refresh();

                            //Opens the group menu
                            creator.mainGui = groupMenu;
                            groupMenu.open(creator);

                            //Deletes this menu
                            delete();
                        }, TutorialGUIUtils.optionLore("Delete task"), TutorialGUIUtils.optionLore("Back to tasks menu"));

                        //Open it
                        deleteConfirmation.open(creator);
                    }
                });

        //Back to group
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, TutorialGUIUtils.optionTitle("Back to Group"), TutorialGUIUtils.optionLore("Back to the group menu")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }

                    @Override
                    public void leftClick(User u) {
                        //Delete this menu and reopen the parent menu
                        delete();
                        groupMenu.refresh();
                        u.mainGui = groupMenu;
                        u.mainGui.open(u);
                    }
                });

        //Type selector

        //Tpll
        Component tpllText = TutorialGUIUtils.optionTitle("Tpll Task");
        if (task.getType() != null)
            if (task.getType().equals(FundamentalTaskType.tpll))
                tpllText = tpllText.decoration(TextDecoration.BOLD, true);

        setItem(2, Utils.createItem(Material.COMPASS, 1, tpllText), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                if (task.getType() != null)
                {
                    if (task.getType().equals(FundamentalTaskType.tpll))
                        task.setType(null);
                    else
                        task.setType(FundamentalTaskType.tpll);
                }
                else
                    task.setType(FundamentalTaskType.tpll);

                refresh();
                open(creator);
            }
        });

        //Command
        Component commandText = TutorialGUIUtils.optionTitle("Command Task");
        if (task.getType() != null)
            if (task.getType().equals(FundamentalTaskType.command))
                commandText = commandText.decoration(TextDecoration.BOLD, true);

        setItem(3, Utils.createItem(Material.COMMAND_BLOCK, 1, commandText), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                if (task.getType() != null)
                {
                    if (task.getType().equals(FundamentalTaskType.command))
                        task.setType(null);
                    else
                        task.setType(FundamentalTaskType.command);
                }
                else
                    task.setType(FundamentalTaskType.command);

                refresh();
                open(creator);
            }
        });

        //Place
        Component placeText = TutorialGUIUtils.optionTitle("Place Task");
        if (task.getType() != null)
            if (task.getType().equals(FundamentalTaskType.place))
                placeText = placeText.decoration(TextDecoration.BOLD, true);

        setItem(4, Utils.createItem(Material.ORANGE_CONCRETE, 1, placeText), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                if (task.getType() != null)
                {
                    if (task.getType().equals(FundamentalTaskType.place))
                        task.setType(null);
                    else
                        task.setType(FundamentalTaskType.place);
                }
                else
                    task.setType(FundamentalTaskType.place);

                refresh();
                open(creator);
            }
        });

        //Selection
        Component selectionText = TutorialGUIUtils.optionTitle("Selection Task");
        if (task.getType() != null)
            if (task.getType().equals(FundamentalTaskType.selection))
                selectionText = selectionText.decoration(TextDecoration.BOLD, true);

        setItem(5, Utils.createItem(Material.WOODEN_AXE, 1, selectionText), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                if (task.getType() != null)
                {
                    if (task.getType().equals(FundamentalTaskType.selection))
                        task.setType(null);
                    else
                        task.setType(FundamentalTaskType.selection);
                }
                else
                    task.setType(FundamentalTaskType.selection);

                refresh();
                open(creator);
            }
        });

        //Chat
        Component chatText = TutorialGUIUtils.optionTitle("Chat Task");
        if (task.getType() != null)
            if (task.getType().equals(FundamentalTaskType.chat))
                chatText = chatText.decoration(TextDecoration.BOLD, true);

        setItem(6, Utils.createItem(Material.OAK_SIGN, 1, chatText), new guiAction() {
            @Override
            public void rightClick(User u) {
                leftClick(u);
            }
            @Override
            public void leftClick(User u) {
                if (task.getType() != null)
                {
                    if (task.getType().equals(FundamentalTaskType.chat))
                        task.setType(null);
                    else
                        task.setType(FundamentalTaskType.chat);
                }
                else
                    task.setType(FundamentalTaskType.chat);

                refresh();
                open(creator);
            }
        });

        //Add additional options for setting the details
        if (task.getType() != null)
        {
            //Books for the accuracy
            if (task.getType().equals(FundamentalTaskType.tpll))
            {
                //Acceptable distance
                setItem(21, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("Acceptable Distance"), TutorialGUIUtils.optionLore("The maximum acceptable distance")),
                        new guiAction() {
                            @Override
                            public void rightClick(User u) {
                                leftClick(u);
                            }

                            @Override
                            public void leftClick(User u) {
                                u.player.sendMessage(Display.aquaText("Use the book to set the acceptable distance."));
                                acceptableTpllAccuracy.startEdit("Acceptable Distance Editor");
                            }
                        });

                //Perfect distance
                setItem(23, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("Perfect Distance"),
                                TutorialGUIUtils.optionLore("The maximum distance"), TutorialGUIUtils.optionLore("for a perfect score & message")),
                        new guiAction() {
                            @Override
                            public void rightClick(User u) {
                                leftClick(u);
                            }

                            @Override
                            public void leftClick(User u) {
                                u.player.sendMessage(Display.aquaText("Use the book to set the perfect distance."));
                                perfectTpllAccuracy.startEdit("Perfect Distance Editor");
                            }
                        });
            }

            //Options to set the command type
            if (task.getType().equals(FundamentalTaskType.command))
            {

                //None
                Component noneText = TutorialGUIUtils.optionTitle("No Action");
                if (task.getCommandActionType() != null)
                    if (task.getCommandActionType().equals(CommandActionType.none))
                        noneText = noneText.decoration(TextDecoration.BOLD, true);

                setItem(20, Utils.createItem(Material.BARRIER, 1, noneText, TutorialGUIUtils.optionLore("Command will do nothing")), new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        if (task.getCommandActionType() != null)
                        {
                            if (task.getCommandActionType().equals(CommandActionType.none))
                                task.setCommandActionType(null);
                            else
                                task.setCommandActionType(CommandActionType.none);
                        }
                        else
                            task.setCommandActionType(CommandActionType.none);

                        refresh();
                        open(creator);
                    }
                });


                //Virtual blocks
                Component virtualBlocksText = TutorialGUIUtils.optionTitle("Virtual Blocks");
                if (task.getCommandActionType() != null)
                    if (task.getCommandActionType().equals(CommandActionType.virtualBlocks))
                        virtualBlocksText = virtualBlocksText.decoration(TextDecoration.BOLD, true);

                setItem(22, Utils.createItem(Material.ORANGE_CONCRETE, 1, virtualBlocksText, TutorialGUIUtils.optionLore("Command is a WorldEdit world change command")), new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        if (task.getCommandActionType() != null)
                        {
                            if (task.getCommandActionType().equals(CommandActionType.virtualBlocks))
                                task.setCommandActionType(null);
                            else
                                task.setCommandActionType(CommandActionType.virtualBlocks);
                        }
                        else
                            task.setCommandActionType(CommandActionType.virtualBlocks);

                        refresh();
                        open(creator);
                    }
                });


                //Full
                Component fullText = TutorialGUIUtils.optionTitle("Full");
                if (task.getCommandActionType() != null)
                    if (task.getCommandActionType().equals(CommandActionType.full))
                        fullText = fullText.decoration(TextDecoration.BOLD, true);

                setItem(24, Utils.createItem(Material.COMMAND_BLOCK, 1, fullText, TutorialGUIUtils.optionLore("Command will actually be performed")), new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }
                    @Override
                    public void leftClick(User u) {
                        if (task.getCommandActionType() != null)
                        {
                            if (task.getCommandActionType().equals(CommandActionType.full))
                                task.setCommandActionType(null);
                            else
                                task.setCommandActionType(CommandActionType.full);
                        }
                        else
                            task.setCommandActionType(CommandActionType.full);

                        refresh();
                        open(creator);
                    }
                });
            }
        }
    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    @Override
    public void refresh() {
        super.clearGui();

        this.addItems();
    }
}
