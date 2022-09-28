package teachingtutorials.newlocation.surfacebuilder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import teachingtutorials.TeachingTutorials;
//import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class APIService
{
    public static String directory = TeachingTutorials.getInstance().getDataFolder().getAbsolutePath() + "/Elevation";
    public File file;

    public boolean downloadImage(String url, int xTile, int yTile, int zoom)
    {
        boolean bDownloaded;

        //BufferedImage image = null;
        File newFile;
        File newDirs;

        //Determines the file name
        String fileName = directory+"/"+zoom+"-"+xTile+"-"+yTile+".png";
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Attempting to download file from "+url +" to " +fileName +" ...");

        newDirs = new File(directory);
        newFile = new File(fileName);

        InputStream in = null;
        FileWriter fileWriter = null;
        ReadableByteChannel readChannel = null;
        FileOutputStream fileOS = null;
        FileChannel writeChannel = null;

        //Downloads the file if it doesn't exist
        if (!newFile.exists())
        {
            try
            {
                //Creates the file
                newDirs.mkdirs();
                if (newFile.createNewFile())
                    Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"Created new file");
                else
                    return false;

                //Creates the link to the source
                URL website = new URL(url);
                in = website.openStream();

                //Copies the file
                //  Files.copy(in, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
                readChannel = Channels.newChannel(in);
                fileOS = new FileOutputStream(fileName);
                writeChannel = fileOS.getChannel();
                long iBytesTransferred = writeChannel
                        .transferFrom(readChannel, 0, Long.MAX_VALUE);

                Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA +"" +iBytesTransferred +" bytes have been copied from the source");
                bDownloaded = true;
            }
            catch (Exception e)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not copy elevation data file: "+e.getMessage());
                bDownloaded = false;
            }
            finally
            {
                try {
                    if (writeChannel != null)
                        writeChannel.close();
                }
                catch (Exception IOe) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not close write file channel: "+IOe.getMessage());
                }

                try {
                    if (fileOS != null)
                        fileOS.close();
                }
                catch (Exception IOe) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not close file output stream: "+IOe.getMessage());
                }

                try {
                    if (readChannel != null)
                        readChannel.close();
                }
                catch (Exception IOe) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not close read channel: "+IOe.getMessage());
                }

                try {
                    if (in != null)
                        in.close();
                }
                catch (Exception IOe) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not close input stream: "+IOe.getMessage());
                }

                try {
                    if (fileWriter != null)
                        fileWriter.close();
                }
                catch (Exception IOe) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Could not close file writer: "+IOe.getMessage());
                }
            }
        }
        else
            bDownloaded = true;

        this.file = newFile;

        return bDownloaded;
    }
}
