package teachingtutorials.newtutorial;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.guis.Gui;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.listeners.texteditorbooks.BookCloseAction;
import teachingtutorials.listeners.texteditorbooks.TextEditorBookListener;
import teachingtutorials.tutorialobjects.Stage;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Category;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

/**
 * A menu for editing a tutorial
 */
public class NewTutorialMenu extends Gui
{
    /** A reference to the parent session which is managing this new tutorial creation */
    TutorialCreationSession tutorialCreationSession;

    /** A reference to the creator */
    final User creator;

    /** A reference to the Tutorial object which holds the information on the new tutorial being created */
    private Tutorial tutorial;

    /** The books used for editing the relevance fields */
    private final TextEditorBookListener[] relevanceBooks;

    /** The book used for editing the name of the tutorial */
    private final TextEditorBookListener nameEditor;

    /** The number of pages in this menu */
    private int iPages;

    /** The current page that the player is on */
    private int iPage;

    /** Information book */
    private final ItemStack information;

    public NewTutorialMenu(TutorialCreationSession tutorialCreationSession, Tutorial tutorial)
    {
        super(54, TutorialGUIUtils.inventoryTitle("New Tutorial"));
        this.tutorialCreationSession = tutorialCreationSession;
        this.tutorial = tutorial;
        this.creator = tutorialCreationSession.creator;

        //Initialise the relevance books
        relevanceBooks = new TextEditorBookListener[5];
        for (int i = 0 ; i < 5 ; i++)
        {
            relevanceBooks[i] = new TextEditorBookListener(tutorialCreationSession.plugin, creator, this, "Relevance of " + Category.values()[i],
                    new BookCloseAction() {
                        @Override
                        public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                            //Unregister the book
                            textEditorBookListener.unregister();

                            //Always remove the book
                            creator.player.getInventory().getItemInMainHand().setAmount(0);

                            //Verify that it is the correct number
                            try
                            {
                                int iRelevance = Integer.parseInt(szNewContent);
                                if (iRelevance < 0 || iRelevance > 100)
                                {
                                    creator.player.sendMessage(Display.errorText("You must enter a number in the range 0 - 100"));
                                    return false;
                                }
                                else
                                {
                                    return true;
                                }
                            }
                            catch (NumberFormatException e)
                            {
                                creator.player.sendMessage(Display.errorText("You must enter a number from 0 - 100"));
                                return false;
                            }
                        }

                        @Override
                        public void runPostClose() {
                            //Refresh the menu to update the lore on the book
                            refresh();

                            //Open the menu
                            open(creator);
                        }
                    }, "0");
        }

        nameEditor = new TextEditorBookListener(tutorialCreationSession.plugin, creator, this, "Tutorial Name",
                new BookCloseAction() {
                    @Override
                    public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                        //Unregister the book
                        textEditorBookListener.unregister();

                        //Always remove the book
                        creator.player.getInventory().getItemInMainHand().setAmount(0);

                        //Check that it is the correct length etc
                        if (szNewContent.length() > 45)
                        {
                            creator.player.sendMessage(Display.errorText("The name must not be more than 45 characters"));
                            return false;
                        }
                        else
                        {
                            tutorial.setTutorialName(szNewContent);
                            return true;
                        }
                    }

                    @Override
                    public void runPostClose() {
                        //Edit the menu title with the new name and reopen
                        NewTutorialMenu.this.editName(TutorialGUIUtils.inventoryTitle(tutorial.getTutorialName()), creator);
                    }
                }, "");

        this.iPages = (tutorial.stages.size()/36)+1;
        this.iPage = 1;

        information = Utils.createItem(Material.WRITTEN_BOOK, 1, TutorialGUIUtils.optionTitle("Information"));
        BookMeta bookMeta = (BookMeta) information.getItemMeta();
        bookMeta.addPages(Component.text("" +
                "-Stages are high level divisions of tutorials.\n" +
                "\n" +
                "-Each stage is completed in order.\n" +
                "-Each stage contains 'steps' which also must be completed in a set order.\n" +
                "\n" +
                "-A stage may be 'Building Outlines'.\n" +
                "-A step may be 'The Corners'"));
        information.setItemMeta(bookMeta);

        addItems();
    }

    private void addItems()
    {
        //Cancel
        setItem(0, Utils.createItem(Material.BARRIER, 1, TutorialGUIUtils.optionTitle("Cancel Creation")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }

                    @Override
                    public void leftClick(User u) {
                        //Create new cancel confirmation menu
                        DeleteConfirmation deleteConfirmation = new DeleteConfirmation(NewTutorialMenu.this, () ->
                                tutorialCreationSession.terminateSession(), TutorialGUIUtils.optionLore("End the tutorial creation"), TutorialGUIUtils.optionLore("Back to tutorial creation"));

                        //Open it
                        deleteConfirmation.open(creator);
                    }
                });

        //Relevances
        for (int i = 0 ; i < 5 ; i++)
        {
            int finalI = i;
            setItem(i+2, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("Relevance in "+Category.values()[i]),
                            TutorialGUIUtils.optionLore(((TextComponent) ((BookMeta) relevanceBooks[finalI].getBook().getItemMeta()).pages().getFirst()).content())),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }

                        @Override
                        public void leftClick(User u) {
                            u.player.sendMessage(Display.aquaText("Use the book to set the relevance. It must be an integer between 0 and 100"));
                            relevanceBooks[finalI].startEdit("Relevance in "+Category.values()[finalI]);
                        }
                    });
        }

        //Attempt save
        setItem(8, Utils.createItem(Material.EMERALD, 1, TutorialGUIUtils.optionTitle("Attempt Add"),
                        TutorialGUIUtils.optionLore("Attempts to add the tutorial")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }

                    @Override
                    public void leftClick(User u) {
                        tutorialCreationSession.attemptSave();
                    }
                });

        //Change name
        setItem(12, Utils.createItem(Material.NAME_TAG, 1, TutorialGUIUtils.optionTitle("Set Tutorial Name")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }

                    @Override
                    public void leftClick(User u) {
                        u.player.sendMessage(Display.aquaText("Use the name editor book to enter a new name. It must be no more than 45 characters"));
                        nameEditor.startEdit("Tutorial Name Editor");
                    }
                });

        //Stage information
        setItem(14, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, TutorialGUIUtils.optionTitle("Information"),
                        TutorialGUIUtils.optionLore("Information about stages")),
                new guiAction() {
                    @Override
                    public void rightClick(User u) {
                        leftClick(u);
                    }

                    @Override
                    public void leftClick(User u) {
                        Bukkit.getScheduler().runTask(tutorialCreationSession.plugin, () -> creator.player.openBook(information));
                    }
                });

        //Page back
        if (iPage > 1)
        {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2",
                    Material.ACACIA_BOAT, 1,
                    TutorialGUIUtils.optionTitle("Page back"));
            super.setItem(9, pageBack, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u) {
                    iPage--;
                    refresh();
                    creator.mainGui = NewTutorialMenu.this;
                    creator.mainGui.open(creator);
                }
            });
        }

        //Page forwards
        if (iPage < iPages)
        {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44",
                    Material.ACACIA_BOAT, 1,
                    TutorialGUIUtils.optionTitle("Page forwards"));
            super.setItem(17, pageBack, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u) {
                    iPage++;
                    refresh();
                    creator.mainGui = NewTutorialMenu.this;
                    creator.mainGui.open(creator);
                }
            });
        }

        //Adds the Stages
        int iStart = (iPage-1)*36;
        int iMax = Math.min(iStart+36, tutorial.stages.size());
        int i;

        for (i = iStart ; i < iMax ; i++)
        {
            int finalI = i;

            Material material;
            if (tutorial.stages.get(i).isComplete())
                material = Material.WRITTEN_BOOK;
            else
                material = Material.BOOK;

            ItemStack stageIcon = Utils.createItem(material, 1,
                    TutorialGUIUtils.optionTitle(tutorial.stages.get(i).getName()),
                    TutorialGUIUtils.optionLore(tutorial.stages.get(i).getOrder()+""));

            super.setItem(i-iStart+18, stageIcon, new guiAction() {
                @Override
                public void rightClick(User u) {
                    leftClick(u);
                }
                @Override
                public void leftClick(User u) {
                    creator.mainGui = new StageMenu(NewTutorialMenu.this, creator, tutorial.stages.get(finalI), tutorial.stages);
                    creator.mainGui.open(creator);
                }
            });
        }

        int iOrder = i;

        //Add the + button - will add the option then refresh. It won't open it.
        if (iPage == iPages) //If on last page
        {
            int iLocationOfAdd = iOrder-iStart+18;

            super.setItem(iLocationOfAdd,
                    Utils.createItem(Material.WRITABLE_BOOK, 1,
                            TutorialGUIUtils.optionTitle("Add Stage"),
                            TutorialGUIUtils.optionLore("Click to add another stage")),
                    new guiAction() {
                        @Override
                        public void rightClick(User u) {
                            leftClick(u);
                        }
                        @Override
                        public void leftClick(User u) {
                            //Adds a new option
                            tutorial.stages.add(new Stage("", iOrder+1));
                            refresh();
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
        //Clears the icons and actions
        this.clearGui();

        //Recalculate number of pages required
        iPages = (tutorial.stages.size()/36)+1;

        //Knock the player back a page if they're now too far forwards
        if (iPage > iPages)
            iPage = iPages;

        //Re-adds the icons and actions
        this.addItems();
    }
}

