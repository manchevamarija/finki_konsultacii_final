package mk.ukim.finki.konsultacii.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.repository.RoomRepository;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


@Component
@AllArgsConstructor
public class InitDatabase {

    private final RoomRepository roomRepository;

    @PostConstruct
    private void init() {
        if (roomRepository.count() == 0) {
            List<String> roomsData = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/Rooms.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length > 0) {
                        roomsData.add(parts[0].trim());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            roomsData.forEach(r -> roomRepository.save(new Room(r)));
        }
    }
}


