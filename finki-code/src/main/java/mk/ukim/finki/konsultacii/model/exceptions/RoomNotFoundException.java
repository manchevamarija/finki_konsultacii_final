package mk.ukim.finki.konsultacii.model.exceptions;

public class RoomNotFoundException extends RuntimeException{
    public RoomNotFoundException(String name) {
        super("Room with " +  name + " was not found");
    }
}
