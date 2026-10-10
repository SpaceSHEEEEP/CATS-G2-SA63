package sg.edu.iss.cats.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.CourseRepository;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class CatsTelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final CourseRepository courses;
    private final Set<Long> allowedChats;

    public CatsTelegramBot(@Value("${telegram.bot.token:}") String botToken,
            @Value("${telegram.allowed-chat-ids:}") String chatIds, CourseRepository courses) {
        this.telegramClient = botToken.isBlank() ? null : new OkHttpTelegramClient(botToken);
        this.courses = courses;
        this.allowedChats = Arrays.stream(chatIds.split(","))
                .map(String::trim).filter(s -> !s.isBlank())
                .map(Long::parseLong).collect(Collectors.toUnmodifiableSet());
    }

    
    
    @Override
    public void consume(Update update) {
        if (telegramClient == null || update == null) return;
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            Long chatId = update.getMessage().getChatId();
            // Deny by default: course catalogue is staff information.
            if (!allowedChats.contains(chatId)) return;

            // Check if user entered /courses
            if (messageText.equals("/courses")) {
                try {
                    var availableCourses = courses.findAll().stream()
                            .filter(c -> !Boolean.TRUE.equals(c.getArchived()))
                            .limit(30).toList();
                    String response = 
                    		"Welcome to CourseBot!\n"
                    		+"Click links below to view individual course details:\n\n"
//                    		+ "\t\t Type /course + x (where x is the id)\n"
//                    		+ "\t\t - e.g. /course1\n\n"
                    		+ "These are our available courses:\n\n";

                    for (Course course : availableCourses) {

                        response += 
                        		"[/course" + course.getCourseId() + "] "
                        		+ course.getCourseName()
                                + " - "
                                + course.getStartDate()
                                + "\n";
                    
                    // 2nd layer: more detailed
                    	                   	
                    }
                    
                    SendMessage message = SendMessage.builder()
                            .chatId(chatId)
                            .text(response)
                            .build();

                    telegramClient.execute(message);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            //for 2nd layer of more detailed code
            else if (messageText.startsWith("/course")
                    && !messageText.equals("/courses")) {

                try {

                    int courseId = Integer.parseInt(messageText.substring("/course".length()));
                    Course course = courses.findById(courseId)
                            .filter(c -> !Boolean.TRUE.equals(c.getArchived()))
                            .orElseThrow(() -> new IllegalArgumentException("Unknown course"));
                    String response =
                            "Course Details\n\n"
                            + "Course Name: " + course.getCourseName() + "\n"
                            + "Type: " + course.getCourseType() + "\n"
                            + "Start Date: " + course.getStartDate() + "\n"
                            + "End Date: " + course.getEndDate() + "\n"
                            + "Fee: $" + course.getFee() + "\n"
                    		+ "Location: " + course.getLocation() + "\n"
                    		+ "Training Provider: " +course.getTrainingProvider();

                    SendMessage message = SendMessage.builder()
                            .chatId(chatId)
                            .text(response)
                            .build();

                    telegramClient.execute(message);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
