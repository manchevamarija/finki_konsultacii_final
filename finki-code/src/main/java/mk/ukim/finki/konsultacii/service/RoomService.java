package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.enumerations.RoomType;

import java.net.URL;
import java.util.List;


public interface RoomService {

    List<Room> getAllRooms();

    Room getByName(String name);

    List<Room> getRoomsByTypeNotLike(RoomType type);
}
