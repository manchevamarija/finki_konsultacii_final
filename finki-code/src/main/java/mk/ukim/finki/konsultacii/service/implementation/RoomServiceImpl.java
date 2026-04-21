package mk.ukim.finki.konsultacii.service.implementation;

import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.enumerations.RoomType;
import mk.ukim.finki.konsultacii.model.exceptions.RoomNotFoundException;
import mk.ukim.finki.konsultacii.repository.RoomRepository;
import mk.ukim.finki.konsultacii.service.RoomService;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.List;


@Service
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public Room getByName(String name) {
        return (Room) roomRepository.findByName(name)
                .orElseThrow(() -> new RoomNotFoundException(name));
    }

    @Override
    public List<Room> getRoomsByTypeNotLike(RoomType type) {
        return roomRepository.findAllByTypeIsNotLike(type);
    }
}
