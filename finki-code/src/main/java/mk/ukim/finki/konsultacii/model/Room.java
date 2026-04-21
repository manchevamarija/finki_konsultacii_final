package mk.ukim.finki.konsultacii.model;

import jakarta.persistence.*;
import lombok.Getter;
import mk.ukim.finki.konsultacii.model.enumerations.RoomType;


@Entity
@Table(name = "room")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
public class Room {

    @Id
    private String name;

    private String locationDescription;

    private String equipmentDescription;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    private Long capacity;

    public Room(String name) {
        this.name = name;
    }

    public Room(String name, String locationDescription, String equipmentDescription,
                RoomType type, Long capacity) {
        this.name = name;
        this.locationDescription = locationDescription;
        this.equipmentDescription = equipmentDescription;
        this.type = type;
        this.capacity = capacity;
    }

    public Room() {

    }
}
