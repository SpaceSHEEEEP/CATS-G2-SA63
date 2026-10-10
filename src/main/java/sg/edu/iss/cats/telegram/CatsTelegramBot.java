package sg.edu.iss.cats.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import sg.edu.iss.cats.dto.CourseDTO;

import org.springframework.web.client.RestClient;

@Component
//LongPollingSingleThreadUpdateConsumer inheriting this allows the class to know "how to deal with updates received from Telegram"
public class CatsTelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final RestClient restClient;

    public CatsTelegramBot(
            @Value("${telegram.bot.token}") String botToken) {

        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.restClient = RestClient.create("http://localhost:8080");
    }

    
    
    @Override
    public void consume(Update update) {
    	
        // Check that the update contains a text message
        if (update.hasMessage() && update.getMessage().hasText()) {

            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            
            System.out.println("TELEGRAM RECEIVED: [" + messageText + "]");

            // Check if user entered /courses
            if (messageText.equals("/courses")) {
                try {
                    CourseDTO[] courses = restClient.get()
                            .uri("/api/courses")
                            .retrieve()
                            .body(CourseDTO[].class);

                    String response = 
                    		"Welcome to CourseBot!\n"
                    		+"Click links below to view individual course details:\n\n"
//                    		+ "\t\t Type /course + x (where x is the id)\n"
//                    		+ "\t\t - e.g. /course1\n\n"
                    		+ "These are our available courses:\n\n";

                    for (CourseDTO course : courses) {

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

                	String courseId =
                	        messageText.substring("/course".length());

                	CourseDTO course = restClient.get()
                	        .uri("/api/courses/" + courseId)
                	        .retrieve()
                	        .body(CourseDTO.class);

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
