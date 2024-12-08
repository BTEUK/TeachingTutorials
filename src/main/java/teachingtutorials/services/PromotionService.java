package teachingtutorials.services;

import org.bukkit.entity.Player;

/**
 * Handles promotion of a player after completion of the compulsory tutorial
 */
public interface PromotionService {

    /**
     * Calls the promotion logic for the promotion of the given player upon completion of the compulsory tutorial
     * @param player The Bukkit Player object of the player to promote
     */
    void promote(Player player);

    /**
     *
     * @return A description of the promotion service
     */
    String getDescription();

}
