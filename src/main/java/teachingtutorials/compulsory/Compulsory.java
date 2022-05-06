package teachingtutorials.compulsory;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.tutorials.Lesson;
import teachingtutorials.utils.User;

public class Compulsory extends Lesson
{
    public Compulsory(TeachingTutorials plugin, User user)
    {
        //Create a new lesson with compulsory set to true
        super(user, plugin, true);
    }

    public void startLesson()
    {
        if (user.bInLesson)
            resumeLesson();
        else
            startLesson();
        user.player.sendMessage("Congrats");
        user.triggerCompulsory();
    }
}
