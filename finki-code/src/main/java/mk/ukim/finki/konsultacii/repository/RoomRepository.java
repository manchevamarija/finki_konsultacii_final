package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.Room;
import mk.ukim.finki.konsultacii.model.enumerations.RoomType;

import java.util.List;
import java.util.Optional;


public interface RoomRepository extends JpaSpecificationRepository<Room, String> {

    Optional<Room> findByName(String name);

    List<Room> findAllByTypeIsNotLike(RoomType type);
}
